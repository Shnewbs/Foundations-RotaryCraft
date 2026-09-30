package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.energy.EnergyStorage;

public class FanBlockEntity extends BlockEntity {
    private static final int ENERGY_CAPACITY = 50_000;
    private static final int MAX_ENERGY_INPUT = 160;
    private static final int ENERGY_PER_TICK = 16;
    private static final int AIRFLOW_RANGE = 8;
    private static final double MAX_AIR_SPEED = 0.5;
    private static final double SPEED_INCREASE = 0.08;

    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }
    };

    public FanBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.FAN_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FanBlockEntity fan) {
        fan.tickFan(level, state);
    }

    private void tickFan(Level level, BlockState state) {
        boolean active = energy.getEnergyStored() >= ENERGY_PER_TICK;
        if (state.getValue(FanBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(FanBlock.ACTIVE, active), 3);
        }
        if (!active) {
            return;
        }

        energy.extractEnergy(ENERGY_PER_TICK, false);
        Direction facing = state.getValue(FanBlock.FACING);
        int range = getClearRange(level, facing);
        if (range > 0) {
            pushEntities(level, facing, range);
        }
        setChanged();
    }

    private int getClearRange(Level level, Direction facing) {
        for (int distance = 1; distance <= AIRFLOW_RANGE; distance++) {
            BlockPos target = worldPosition.relative(facing, distance);
            if (!level.getBlockState(target).getCollisionShape(level, target).isEmpty()) {
                return distance - 1;
            }
        }
        return AIRFLOW_RANGE;
    }

    private void pushEntities(Level level, Direction facing, int range) {
        int stepX = facing.getStepX();
        int stepZ = facing.getStepZ();
        AABB airflow = new AABB(worldPosition.relative(facing))
                .expandTowards(stepX * (range - 1), 0, stepZ * (range - 1))
                .inflate(stepX == 0 ? 1.5 : 0, 1.5, stepZ == 0 ? 1.5 : 0);
        Vec3 direction = new Vec3(stepX, 0, stepZ);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, airflow, Entity::isAlive)) {
            double speed = entity.getDeltaMovement().dot(direction);
            if (speed >= MAX_AIR_SPEED) {
                continue;
            }
            double boost = Math.min(SPEED_INCREASE, MAX_AIR_SPEED - speed);
            entity.setDeltaMovement(entity.getDeltaMovement().add(stepX * boost, 0, stepZ * boost));
            entity.hasImpulse = true;
        }
    }

    public EnergyStorage getEnergyStorage() {
        return energy;
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
