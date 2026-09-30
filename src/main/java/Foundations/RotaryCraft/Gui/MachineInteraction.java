package Foundations.RotaryCraft.Gui;

@net.neoforged.fml.common.EventBusSubscriber(modid="rotarycraft")
public final class MachineInteraction {
    @net.neoforged.bus.api.SubscribeEvent
    public static void interact(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var player=event.getEntity();
        if(event.isCanceled()||event.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND||player.isSpectator())return;
        // Holding a block keeps placement usable next to another machine.
        if(event.getItemStack().getItem() instanceof net.minecraft.world.item.BlockItem)return;
        if(!event.getLevel().mayInteract(player,event.getPos()))return;
        var entity=event.getLevel().getBlockEntity(event.getPos());
        if(entity==null||!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).getNamespace().equals("rotarycraft"))return;
        boolean fluidItem=entity instanceof Foundations.RotaryCraft.Machines.DecoTankBlockEntity&&net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(event.getItemStack()).isPresent();
        boolean sprinklerBucket=entity instanceof Foundations.RotaryCraft.Farming.SprinklerBlockEntity&&event.getItemStack().is(net.minecraft.world.item.Items.WATER_BUCKET);
        if(player.isShiftKeyDown()||fluidItem||sprinklerBucket){
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
            return;
        }
        if(MachineMenus.open(event.getLevel(),event.getPos(),player)) {
            event.setCanceled(true);event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
}
