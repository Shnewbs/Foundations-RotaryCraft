package Foundations.RotaryCraft.Gui;

@net.neoforged.fml.common.EventBusSubscriber(modid="rotarycraft")
public final class MachineInteraction {
    @net.neoforged.bus.api.SubscribeEvent
    public static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var player=event.getEntity();
        if(event.isCanceled()||event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||player.isShiftKeyDown()||player.isSpectator())return;
        // Holding a block keeps placement usable next to another machine.
        if(event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem)return;
        if(!event.getLevel().mayInteract(player,event.getPos()))return;
        if(MachineMenus.open(event.getLevel(),event.getPos(),player)) {
            event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
}
