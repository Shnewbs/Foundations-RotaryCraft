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
    public DecoTankRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(DecoTankBlockEntity tank,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var fluid=tank.getTank().getFluid();if(fluid.isEmpty())return;
        var extension=IClientFluidTypeExtensions.of(fluid.getFluid());var texture=extension.getStillTexture(fluid);if(texture==null)return;
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        int color=extension.getTintColor(fluid);
        float min=2/16f,max=14/16f,bottom=1.01f/16f,top=bottom+(13.98f/16f)*tank.getFluidAmount()/tank.getMaxFluidAmount();
        float[][][] faces={
            {{min,top,min},{min,top,max},{max,top,max},{max,top,min}},
            {{min,bottom,max},{min,bottom,min},{max,bottom,min},{max,bottom,max}},
            {{min,bottom,min},{min,top,min},{max,top,min},{max,bottom,min}},
            {{max,bottom,max},{max,top,max},{min,top,max},{min,bottom,max}},
            {{min,bottom,max},{min,top,max},{min,top,min},{min,bottom,min}},
            {{max,bottom,min},{max,top,min},{max,top,max},{max,bottom,max}}
        };
        float[][] normals={{0,1,0},{0,-1,0},{0,0,-1},{0,0,1},{-1,0,0},{1,0,0}};
        float[][] uv={{0,1},{0,0},{1,0},{1,1}};
        var consumer=buffers.getBuffer(RenderType.translucent());var matrix=pose.last();
        for(int face=0;face<faces.length;face++)for(int i=0;i<4;i++){
            var v=faces[face][i];var n=normals[face];
            consumer.addVertex(matrix,v[0],v[1],v[2]).setColor(color).setUv(sprite.getU(uv[i][0]),sprite.getV(uv[i][1])).setLight(light).setOverlay(overlay).setNormal(matrix,n[0],n[1],n[2]);
        }
    }
}
