package Foundations.RotaryCraft.Client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import Foundations.RotaryCraft.Mechanical.MechanicalContent;
import Foundations.RotaryCraft.Machines.MachineContent;

@EventBusSubscriber(modid="rotarycraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class RotaryClient {
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {event.register(Foundations.RotaryCraft.Gui.MachineMenus.MACHINE.get(),MachineScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        for (String name : java.util.List.of("power_generator","steam_generator","wind_generator","hydro_generator","geothermal_generator")) {
            event.registerBlockEntityRenderer(net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.get(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("rotarycraft", name)), AnimatedMachineRenderer::new);
        }
        event.registerBlockEntityRenderer(MechanicalContent.ENTITY.get(),AnimatedMachineRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.MOB_HARVESTER_BLOCK_ENTITY.get(),AnimatedMachineRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.GRINDSTONE_BLOCK_ENTITY.get(),AnimatedMachineRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.FAN_BLOCK_ENTITY.get(),AnimatedMachineRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.WINDER_BLOCK_ENTITY.get(),AnimatedMachineRenderer::new);
        event.registerBlockEntityRenderer(MachineContent.DEFOLIATOR_BLOCK_ENTITY.get(),AnimatedMachineRenderer::new);
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event) {event.registerReloadListener(AnimatedMachineRenderer.RELOAD);}
}
