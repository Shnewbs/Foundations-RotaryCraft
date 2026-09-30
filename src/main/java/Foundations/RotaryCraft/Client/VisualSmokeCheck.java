package Foundations.RotaryCraft.Client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
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
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if(checked || !Boolean.getBoolean("rotarycraft.visualSmoke")) return;
        var client=Minecraft.getInstance();
        if(!(client.screen instanceof TitleScreen) || client.getOverlay()!=null) return;
        checked=true;
        try {
            var missing=client.getModelManager().getMissingModel();
            int states=0,items=0;
            for(var block:BuiltInRegistries.BLOCK) {
                if(!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("rotarycraft"))continue;
                for(var state:block.getStateDefinition().getPossibleStates()) {
                    if(client.getBlockRenderer().getBlockModel(state)==missing)
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
            client.stop();
        } catch(Throwable failure) {
            failure.printStackTrace();
            System.exit(1);
        }
    }
}
