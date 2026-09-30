package Foundations.RotaryCraft.Client;

import Foundations.RotaryCraft.Machines.DecoTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Uses the stored fluid's own atlas texture and tint, including fluids supplied by other mods. */
public final class DecoTankRenderer implements BlockEntityRenderer<DecoTankBlockEntity> {
    private static final float[][][] FACES={
        {{0,1,0},{0,1,1},{1,1,1},{1,1,0}},
        {{0,0,1},{0,0,0},{1,0,0},{1,0,1}},
        {{0,0,0},{0,1,0},{1,1,0},{1,0,0}},
        {{1,0,1},{1,1,1},{0,1,1},{0,0,1}},
        {{0,0,1},{0,1,1},{0,1,0},{0,0,0}},
        {{1,0,0},{1,1,0},{1,1,1},{1,0,1}}
    };
    private static final float[][] NORMALS={{0,1,0},{0,-1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
    private static final float[][] UV={{0,1},{0,0},{1,0},{1,1}};
    public DecoTankRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(DecoTankBlockEntity tank,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var fluid=tank.getTank().getFluid();if(fluid.isEmpty())return;
        var extension=IClientFluidTypeExtensions.of(fluid.getFluid());var texture=extension.getStillTexture(fluid);if(texture==null)return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int color=extension.getTintColor(fluid);
        float min=2/16f,max=14/16f,bottom=1.01f/16f,top=bottom+(13.98f/16f)*tank.getFluidAmount()/tank.getMaxFluidAmount();
        var consumer=buffers.getBuffer(RenderType.translucent());var matrix=pose.last();
        for(int face=0;face<FACES.length;face++)for(int i=0;i<4;i++){
            var v=FACES[face][i];var n=NORMALS[face];
            consumer.addVertex(matrix,min+v[0]*(max-min),bottom+v[1]*(top-bottom),min+v[2]*(max-min)).setColor(color).setUv(sprite.getU(UV[i][0]),sprite.getV(UV[i][1])).setLight(light).setOverlay(overlay).setNormal(matrix,n[0],n[1],n[2]);
        }
    }
}
