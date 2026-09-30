package Foundations.RotaryCraft.Gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.world.SimpleMenuProvider;

public final class MachineMenus {
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,"rotarycraft");
    public static final DeferredHolder<MenuType<?>,MenuType<MachineMenu>> MACHINE=MENUS.register("machine",()->IMenuTypeExtension.create(MachineMenu::client));
    public static void register(IEventBus bus){MENUS.register(bus);}
    public static void dropInventory(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos) {
        if(level.isClientSide)return;
        var entity=level.getBlockEntity(pos);if(entity==null)return;
        var inventories=entity instanceof Foundations.RotaryCraft.Machines.SortingBlockEntity sorter?java.util.List.of(sorter.getInput()):MachineMenu.handlers(entity);
        for(var handler:inventories)if(handler instanceof net.neoforged.neoforge.items.IItemHandlerModifiable mutable)for(int slot=0;slot<handler.getSlots();slot++) {
            var stack=handler.getStackInSlot(slot).copy();mutable.setStackInSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);
            if(!stack.isEmpty())net.minecraft.world.Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack);
        }
    }
    public static boolean open(net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player) {
        var entity=level.getBlockEntity(pos);
        if(entity==null || !net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).getNamespace().equals("rotarycraft"))return false;
        if(player instanceof net.minecraft.server.level.ServerPlayer server) {
            int slots=MachineMenu.handlers(entity).stream().mapToInt(net.neoforged.neoforge.items.IItemHandler::getSlots).sum();
            server.openMenu(new SimpleMenuProvider((id,inventory,p)->new MachineMenu(id,inventory,entity),entity.getBlockState().getBlock().getName()),buffer->{buffer.writeBlockPos(pos);buffer.writeVarInt(slots);buffer.writeBoolean(entity instanceof Foundations.RotaryCraft.Machines.SortingBlockEntity);});
        }
        return true;
    }
}
