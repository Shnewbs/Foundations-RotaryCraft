package Foundations.RotaryCraft.Client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in CI check against models baked by an actual OpenGL Minecraft client. */
@EventBusSubscriber(modid="rotarycraft",value=Dist.CLIENT)
public final class VisualSmokeCheck {
    private static boolean checked;
    private static int galleryTicks;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static final AnimatedMachineRenderer previewRenderer=new AnimatedMachineRenderer(null);
    private static final class Gallery extends Screen {
        private final List<ItemStack> items=new ArrayList<>();
        private long nanos;private int samples;
        Gallery(){super(Component.literal("RotaryCraft visual gallery"));for(var item:BuiltInRegistries.ITEM)if(BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("rotarycraft"))items.add(new ItemStack(item));}
        @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial) {
            long start=System.nanoTime();
            gui.fill(0,0,width,height,0xff24303c);
            gui.drawString(font,"RotaryCraft 1.6 — baked inventory models",8,8,0xffffff);
            for(int i=0;i<items.size();i++) {
                int x=12+(i%8)*(width/8), y=30+(i/8)*62;
                gui.pose().pushPose();gui.pose().translate(x,y,0);gui.pose().scale(2,2,2);
                gui.renderItem(items.get(i),0,0);gui.pose().popPose();
                if(items.get(i).getItem() instanceof net.minecraft.world.item.BlockItem blockItem) {
                    var state=blockItem.getBlock().defaultBlockState();
                    if(galleryTicks>=30) {
                        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT,true);
                        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,galleryTicks<50?net.minecraft.core.Direction.EAST:net.minecraft.core.Direction.UP);
                        if(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7))state=state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AGE_7,7);
                    }
                    gui.flush();gui.pose().pushPose();gui.pose().translate(x+42,y+28,100);gui.pose().scale(22,-22,22);
                    gui.pose().mulPose(new org.joml.Quaternionf().rotationXYZ((float)Math.toRadians(25),(float)Math.toRadians(galleryTicks<30?35:215),0));
                    gui.pose().translate(-.5,-.5,-.5);
                    var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
                    Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,gui.pose(),buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                    previewRenderer.renderPreview(state,galleryTicks*6,gui.pose(),buffers,15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                    buffers.endBatch();gui.pose().popPose();
                }
                gui.drawString(font,BuiltInRegistries.ITEM.getKey(items.get(i).getItem()).getPath(),x,y+34,0xffffff,false);
            }
            if(galleryTicks>5){nanos+=System.nanoTime()-start;samples++;}
            if(galleryTicks==50)System.out.println("ROTARYCRAFT_GALLERY_RENDER_CPU mean_ms="+(nanos/1e6/Math.max(1,samples))+" samples="+samples+" models="+items.size()+" softwareGL="+System.getenv("LIBGL_ALWAYS_SOFTWARE"));
        }
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(!Boolean.getBoolean("rotarycraft.visualSmoke")) return;
        if(checked) {
            if(++galleryTicks==20||galleryTicks==40||galleryTicks==55)Screenshot.grab(Minecraft.getInstance().gameDirectory,Minecraft.getInstance().getMainRenderTarget(),message->System.out.println(message.getString()));
            if(galleryTicks==60)reload=Minecraft.getInstance().reloadResourcePacks();
            if(galleryTicks>60 && reload!=null && reload.isDone() && Minecraft.getInstance().getOverlay()==null) {
                reload.join();System.out.println("ROTARYCRAFT_RESOURCE_RELOAD_OK");Minecraft.getInstance().stop();
            }
            return;
        }
        var client=Minecraft.getInstance();
        if(!(client.screen instanceof TitleScreen) || client.getOverlay()!=null) return;
        checked=true;
        try {
            var missing=client.getModelManager().getMissingModel();
            int states=0,items=0;
            for(var block:BuiltInRegistries.BLOCK) {
                if(!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("rotarycraft"))continue;
                for(var state:block.getStateDefinition().getPossibleStates()) {
                    var model=client.getBlockRenderer().getBlockModel(state);
                    if(model.getParticleIcon().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))throw new IllegalStateException("Missing block texture: "+state);
                    if(model==missing)
                        throw new IllegalStateException("Missing baked block model: "+state);
                    states++;
                }
            }
            for(var item:BuiltInRegistries.ITEM) {
                if(!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("rotarycraft"))continue;
                if(client.getItemRenderer().getModel(new ItemStack(item),null,null,0)==missing)
                    throw new IllegalStateException("Missing baked item model: "+BuiltInRegistries.ITEM.getKey(item));
                items++;
            }
            System.out.println("ROTARYCRAFT_VISUAL_SMOKE_OK states="+states+" items="+items);
            client.setScreen(new Gallery());
        } catch(Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
