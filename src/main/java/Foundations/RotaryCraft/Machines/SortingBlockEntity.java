package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SortingBlockEntity extends BlockEntity {
    private static final int ENERGY_CAPACITY = 50_000;
    private static final int MIN_ENERGY = 16;
    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, 160, 0);
    private final ItemStackHandler filters = new ItemStackHandler(9){@Override protected void onContentsChanged(int slot){setChanged();}};
    private final ItemStackHandler input = new ItemStackHandler(1){@Override protected void onContentsChanged(int slot){setChanged();}};
    private int facingIndex;

    public SortingBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.SORTING_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SortingBlockEntity sorter) {
        sorter.tickSorter(level);
    }

    private void tickSorter(Level level) {
        if (energy.getEnergyStored() < MIN_ENERGY || input.getStackInSlot(0).isEmpty()) {
            return;
        }
        ItemStack stack = input.getStackInSlot(0);
        int route = getRoute(stack);
        Direction direction = getOutputDirection(route);
        IItemHandler destination = level.getCapability(
                Capabilities.ItemHandler.BLOCK, worldPosition.relative(direction), direction.getOpposite());
        if (destination == null) {
            return;
        }
        ItemStack moved = input.extractItem(0, 1, true);
        for (int slot = 0; slot < destination.getSlots() && !moved.isEmpty(); slot++) {
            moved = destination.insertItem(slot, moved, false);
        }
        if (moved.isEmpty()) {
            input.extractItem(0, 1, false);
            energy.extractEnergy(MIN_ENERGY, false);
            setChanged();
        }
    }

    private int getRoute(ItemStack stack) {
        for (int slot = 0; slot < filters.getSlots(); slot++) {
            if (!filters.getStackInSlot(slot).isEmpty()
                    && ItemStack.isSameItemSameComponents(filters.getStackInSlot(slot), stack)) {
                return slot / 3;
            }
        }
        return 2;
    }

    private Direction getOutputDirection(int route) {
        Direction[] outputs = {Direction.NORTH, Direction.SOUTH, Direction.DOWN};
        return outputs[(route + facingIndex) % outputs.length];
    }

    public EnergyStorage getEnergyStorage() { return energy; }
    public ItemStackHandler getFilters() { return filters; }
    public ItemStackHandler getInput() { return input; }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.deserializeNBT(registries, tag.getCompound("energy"));
        filters.deserializeNBT(registries, tag.getCompound("filters"));
        input.deserializeNBT(registries, tag.getCompound("input"));
        facingIndex = tag.getInt("facing");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
        tag.put("filters", filters.serializeNBT(registries));
        tag.put("input", input.serializeNBT(registries));
        tag.putInt("facing", facingIndex);
    }
}
