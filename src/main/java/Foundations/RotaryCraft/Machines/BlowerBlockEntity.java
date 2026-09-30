package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Blower: An air/pressure generation machine.
 * Produces directional air flow for pneumatic systems and material transport.
 * Requires continuous power input to operate.
 */
public class BlowerBlockEntity extends BlockEntity {

    private static final int MAX_ENERGY = 50000;
    private static final int MAX_INPUT_PER_TICK = 160;
    private static final int POWER_REQUIREMENT_PER_TICK = 80;
    
    private final EnergyStorage energyStorage = new EnergyStorage(MAX_ENERGY, MAX_INPUT_PER_TICK, 0, 0);
    private boolean operating = false;

    public BlowerBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.BLOWER_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BlowerBlockEntity entity) {
        if (level == null || level.isClientSide) return;
        
        entity.tickBlower();
    }

    private void tickBlower() {
        operating = energyStorage.getEnergyStored() >= POWER_REQUIREMENT_PER_TICK;
        
        if (operating) {
            energyStorage.extractEnergy(POWER_REQUIREMENT_PER_TICK, false);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) {
            energyStorage.deserializeNBT(registries, tag.getCompound("energy"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energyStorage.serializeNBT(registries));
    }

    public EnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public boolean isOperating() {
        return operating;
    }
}
