package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * DecoTank: A decorative fluid storage and display block.
 * Stores up to 16 buckets (16,000 mB) of any fluid for display or crafting purposes.
 */
public class DecoTankBlockEntity extends BlockEntity {

    private static final int MAX_FLUID_AMOUNT = 16000;
    
    private int fluidAmount = 0;
    private String fluidName = "";

    public DecoTankBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.DECO_TANK_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DecoTankBlockEntity entity) {
        if (level == null || level.isClientSide) return;
        // Tanks don't need active ticking; they're passive storage
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidAmount = tag.getInt("fluid_amount");
        fluidName = tag.getString("fluid_name");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("fluid_amount", fluidAmount);
        tag.putString("fluid_name", fluidName);
    }

    public int getFluidAmount() {
        return fluidAmount;
    }

    public void setFluidAmount(int amount) {
        this.fluidAmount = Math.min(Math.max(amount, 0), MAX_FLUID_AMOUNT);
        setChanged();
    }

    public String getFluidName() {
        return fluidName;
    }

    public void setFluidName(String name) {
        this.fluidName = name != null ? name : "";
        setChanged();
    }

    public int getMaxFluidAmount() {
        return MAX_FLUID_AMOUNT;
    }

    public int getFluidLevel() {
        return fluidAmount > 0 ? Math.max(1, (fluidAmount * 15) / MAX_FLUID_AMOUNT) : 0;
    }

    public boolean isEmpty() {
        return fluidAmount == 0 || fluidName.isEmpty();
    }
}

