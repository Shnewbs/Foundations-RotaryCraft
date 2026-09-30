package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;

public class SmokeDetectorBlockEntity extends BlockEntity {
    private static final int MAX_ENERGY = 1_200;
    private static final int DETECTION_RANGE = 8;
    private static final int ENERGY_PER_TICK = 1;
    private final EnergyStorage energy = new EnergyStorage(MAX_ENERGY, 160, 0);
    private boolean alarming;
    private boolean lowBattery;

    public SmokeDetectorBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.SMOKE_DETECTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SmokeDetectorBlockEntity detector) {
        detector.tickDetector(level);
    }

    private void tickDetector(Level level) {
        lowBattery = energy.getEnergyStored() <= MAX_ENERGY / 8;
        if (energy.getEnergyStored() < ENERGY_PER_TICK) {
            alarming = false;
            return;
        }
        energy.extractEnergy(ENERGY_PER_TICK, false);
        alarming = false;
        BlockPos min = worldPosition.offset(-DETECTION_RANGE, -DETECTION_RANGE, -DETECTION_RANGE);
        BlockPos max = worldPosition.offset(DETECTION_RANGE, DETECTION_RANGE, DETECTION_RANGE);
        for (BlockPos nearby : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(nearby).is(Blocks.FIRE) || level.getBlockState(nearby).is(Blocks.SOUL_FIRE)) {
                alarming = true;
                break;
            }
        }
        setChanged();
    }

    public EnergyStorage getEnergyStorage() {
        return energy;
    }

    public boolean isAlarming() {
        return alarming;
    }

    public boolean isLowBattery() {
        return lowBattery;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.deserializeNBT(registries, tag.getCompound("energy"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
    }
}
