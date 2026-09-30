package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Winder: A mechanical device for controlling wind and speed in powered operations.
 * Acts as an intermediate power conditioner between generators and machines.
 * Accepts power input and regulates output with configurable speed constraints.
 */
public class WinderBlockEntity extends BlockEntity {

    private static final int MAX_ENERGY = 50000;
    private static final int MAX_INPUT_PER_TICK = 160;
    private static final int MAX_OUTPUT_PER_TICK = 160;
    
    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY, MAX_INPUT_PER_TICK, MAX_OUTPUT_PER_TICK, 0);
    private int operationTicks = 0;

    public WinderBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.WINDER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WinderBlockEntity entity) {
        if (level == null || level.isClientSide) return;
        
        entity.tickWinder();
        boolean active = entity.energyStorage.getEnergyStored() > 0;
        var property = net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;
        if (state.getValue(property) != active) level.setBlock(pos, state.setValue(property, active), 2);
    }

    private void tickWinder() {
        if (energyStorage.getEnergyStored() > 0) {
            operationTicks++;
        } else {
            operationTicks = 0;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            energyStorage.deserializeNBT(registries, tag.getCompound("energy"));
        }
        operationTicks = tag.getInt("operation_ticks");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energyStorage.serializeNBT(registries));
        tag.putInt("operation_ticks", operationTicks);
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public int getOperationTicks() {
        return operationTicks;
    }

    public float getOperationProgress() {
        return Math.min(operationTicks / 20.0f, 1.0f);
    }
}
