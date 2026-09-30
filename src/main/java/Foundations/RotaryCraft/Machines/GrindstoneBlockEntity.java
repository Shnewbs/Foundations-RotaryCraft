package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Grindstone: Rotational-powered grinding machine.
 * Grinds various items into powder, dust, or other products based on recipes.
 * Requires continuous power input to operate.
 */
public class GrindstoneBlockEntity extends BlockEntity {

    private static final int MAX_ENERGY = 100000;
    private static final int POWER_REQUIREMENT = 100;
    private static final int GRIND_TIME_TICKS = 100;
    
    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY, POWER_REQUIREMENT, 0, 0);
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            GrindstoneBlockEntity.this.setChanged();
        }
    };

    private int grindProgress = 0;
    private int grindTime = 0;

    public GrindstoneBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.GRINDSTONE_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GrindstoneBlockEntity entity) {
        if (level == null || level.isClientSide) return;
        
        entity.tryGrind();
    }

    private void tryGrind() {
        ItemStack input = itemHandler.getStackInSlot(0);
        ItemStack output = itemHandler.getStackInSlot(1);

        if (input.isEmpty()) {
            grindProgress = 0;
            grindTime = 0;
            return;
        }

        if (energyStorage.getEnergyStored() >= POWER_REQUIREMENT) {
            if (grindTime < GRIND_TIME_TICKS) {
                grindTime++;
                energyStorage.extractEnergy(POWER_REQUIREMENT / GRIND_TIME_TICKS, false);
            } else {
                grindProgress++;
                grindTime = 0;
                
                if (grindProgress >= 10) {
                    completeGrind(input, output);
                    grindProgress = 0;
                }
            }
        } else {
            grindTime = 0;
        }
    }

    private void completeGrind(ItemStack input, ItemStack output) {
        ItemStack result = GrindstoneRecipes.grind(input);
        if (!result.isEmpty()) {
            if (output.isEmpty()) {
                itemHandler.setStackInSlot(1, result.copy());
                input.shrink(1);
            } else if (ItemStack.isSameItem(output, result) && output.getCount() < output.getMaxStackSize()) {
                output.grow(result.getCount());
                input.shrink(1);
            }
            setChanged();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            energyStorage.deserializeNBT(registries, tag.getCompound("energy"));
        }
        if (tag.contains("items")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        }
        grindProgress = tag.getInt("grind_progress");
        grindTime = tag.getInt("grind_time");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energyStorage.serializeNBT(registries));
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putInt("grind_progress", grindProgress);
        tag.putInt("grind_time", grindTime);
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public float getGrindProgress() {
        return grindProgress / 10.0f;
    }
}
