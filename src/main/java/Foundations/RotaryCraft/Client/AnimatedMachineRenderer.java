package Foundations.RotaryCraft.Client;

import java.util.*;
import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Quaternionf;
import Foundations.RotaryCraft.Mechanical.MechanicalBlockEntity;

/** Cached source meshes; poses move components without rebuilding geometry or uploading textures. */
public final class AnimatedMachineRenderer implements BlockEntityRenderer<BlockEntity> {
    private record Group(float[][] operations, float[][] vertices) {}
    private record Mesh(float orientation, List<Group> groups) {}
    private static volatile Map<String,Mesh> meshes = Map.of();
    private static final WeakHashMap<BlockEntity,Spin> spins = new WeakHashMap<>();
    private static final class Spin { double time; double angle; }
    public AnimatedMachineRenderer(BlockEntityRendererProvider.Context context) {}
    public static final ResourceManagerReloadListener RELOAD = AnimatedMachineRenderer::reload;
    private static void reload(ResourceManager resources) {
        Map<String,Mesh> loaded = new HashMap<>();
        for (String name : List.of("dc_engine","shaft","gearbox_2","gearbox_4","gearbox_8","gearbox_16","grindstone","fan","winder","defoliator","power_generator","steam_generator","wind_generator","hydro_generator")) {
            var id=ResourceLocation.fromNamespaceAndPath("rotarycraft","motion/"+name+".json");
            var resource=resources.getResource(id);
            if (resource.isEmpty()) throw new IllegalStateException("Missing animation mesh "+id);
            try (var reader=resource.get().openAsReader()) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                List<Group> groups=new ArrayList<>();
                for (var item:json.getAsJsonArray("groups")) {
                    var group=item.getAsJsonObject();List<float[]> ops=new ArrayList<>();List<float[]> vertices=new ArrayList<>();
                    for(var operation:group.getAsJsonArray("operations")) {
                        var op=operation.getAsJsonObject();var data=op.has("translate")?op.getAsJsonArray("translate"):op.getAsJsonArray("rotate");
                        float[] values=new float[data.size()+1];values[0]=op.has("translate")?0:1;
                        for(int i=0;i<data.size();i++)values[i+1]=data.get(i).getAsFloat();ops.add(values);
                    }
                    for(var face:group.getAsJsonArray("faces")) {
                        float[][] points=new float[4][5];
                        for(int i=0;i<4;i++)for(int j=0;j<5;j++)points[i][j]=face.getAsJsonArray().get(i).getAsJsonArray().get(j).getAsFloat();
                        float ax=points[1][0]-points[0][0],ay=points[1][1]-points[0][1],az=points[1][2]-points[0][2];
                        float bx=points[2][0]-points[0][0],by=points[2][1]-points[0][1],bz=points[2][2]-points[0][2];
                        float nx=ay*bz-az*by,ny=az*bx-ax*bz,nz=ax*by-ay*bx;float length=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                        if(length<1e-8F)continue;
                        for(var point:points)vertices.add(new float[]{point[0],point[1],point[2],point[3],point[4],nx/length,ny/length,nz/length});
                    }
                    groups.add(new Group(ops.toArray(float[][]::new),vertices.toArray(float[][]::new)));
                }
                loaded.put(name,new Mesh(json.get("orientation").getAsFloat(),List.copyOf(groups)));
            } catch(Exception exception){throw new IllegalStateException("Cannot load "+id,exception);}
        }
        meshes=Map.copyOf(loaded);spins.clear();
    }
    @Override public void render(BlockEntity entity,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(entity.getLevel()==null)return;
        String name=BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).getPath();Mesh mesh=meshes.get(name);if(mesh==null)return;
        int speed=0;
        if(entity instanceof MechanicalBlockEntity mechanical)speed=mechanical.visualSpeed();
        else if(entity.getBlockState().hasProperty(BlockStateProperties.LIT)&&entity.getBlockState().getValue(BlockStateProperties.LIT))speed=256;
        Spin spin=spins.computeIfAbsent(entity,key->new Spin());double now=entity.getLevel().getGameTime()+partialTick;
        if(spin.time!=0&&now>=spin.time)spin.angle=(spin.angle+Math.min(2,now-spin.time)*Math.pow(Math.log(speed+1)/Math.log(2),1.25))%360;
        spin.time=now;float angle=(float)spin.angle;
        pose.pushPose();pose.translate(0.5,0.5,0.5);
        if(entity.getBlockState().hasProperty(BlockStateProperties.FACING)) {
            var facing=entity.getBlockState().getValue(BlockStateProperties.FACING);
            switch(facing){case EAST->pose.mulPose(new Quaternionf().rotationY((float)-Math.PI/2));case SOUTH->pose.mulPose(new Quaternionf().rotationY((float)Math.PI));case WEST->pose.mulPose(new Quaternionf().rotationY((float)Math.PI/2));case UP->pose.mulPose(new Quaternionf().rotationX((float)Math.PI/2));case DOWN->pose.mulPose(new Quaternionf().rotationX((float)-Math.PI/2));default->{}}
        } else if(entity.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            float yaw=entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot();pose.mulPose(new Quaternionf().rotationY((float)Math.toRadians(180-yaw)));
        }
        pose.mulPose(new Quaternionf().rotationY((float)Math.toRadians(mesh.orientation)));pose.translate(0,1,0);pose.scale(1,-1,-1);
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(ResourceLocation.fromNamespaceAndPath("rotarycraft","textures/legacy/"+name+".png")));
        for(Group group:mesh.groups){pose.pushPose();for(float[] op:group.operations){if(op[0]==0)pose.translate(op[1],op[2],op[3]);else pose.mulPose(new Quaternionf().rotationAxis((float)Math.toRadians(op[1]*angle+op[2]),op[3],op[4],op[5]));}
            var matrix=pose.last();for(float[] v:group.vertices)consumer.addVertex(matrix,v[0],v[1],v[2]).setColor(255,255,255,255).setUv(v[3],v[4]).setOverlay(overlay).setLight(light).setNormal(matrix,v[5],v[6],v[7]);pose.popPose();}
        pose.popPose();
    }
}
