package Foundations.RotaryCraft.Power;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public final class PowerSwitchBlockEntity extends BlockEntity {
    private static final ThreadLocal<Set<PowerSwitchBlockEntity>> ACTIVE_ROUTES =
            ThreadLocal.withInitial(HashSet::new);

    public PowerSwitchBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.POWER_SWITCH_ENTITY.get(), pos, state);
    }

    public boolean isEnabled() {
        return getBlockState().getValue(PowerSwitchBlock.ENABLED);
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction ingressSide) {
        return new ForwardingEnergyStorage(ingressSide);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerSwitchBlockEntity powerSwitch) {
        powerSwitch.updateEnabledState();
    }

    void updateEnabledState() {
        if (level == null || level.isClientSide) {
            return;
        }

        BlockState currentState = getBlockState();
        boolean shouldEnable = !level.hasNeighborSignal(worldPosition);
        if (currentState.getValue(PowerSwitchBlock.ENABLED) != shouldEnable) {
            level.setBlock(worldPosition, currentState.setValue(PowerSwitchBlock.ENABLED, shouldEnable), 3);
            level.invalidateCapabilities(worldPosition);
            CableNetwork.invalidate(level);
            setChanged();
        }
    }

    private boolean withRoute(Supplier<Boolean> operation) {
        Set<PowerSwitchBlockEntity> activeRoutes = ACTIVE_ROUTES.get();
        if (!activeRoutes.add(this)) {
            return false;
        }
        try {
            return operation.get();
        } finally {
            activeRoutes.remove(this);
            if (activeRoutes.isEmpty()) {
                ACTIVE_ROUTES.remove();
            }
        }
    }

    private final class ForwardingEnergyStorage implements IEnergyStorage {
        @Nullable
        private final Direction ingressSide;

        private ForwardingEnergyStorage(@Nullable Direction ingressSide) {
            this.ingressSide = ingressSide;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0 || !isEnabled() || level == null) {
                return 0;
            }
            return routeEnergy(maxReceive, simulate);
        }

        private int routeEnergy(int maxReceive, boolean simulate) {
            Set<PowerSwitchBlockEntity> activeRoutes = ACTIVE_ROUTES.get();
            if (!activeRoutes.add(PowerSwitchBlockEntity.this)) {
                return 0;
            }
            try {
                int accepted = 0;
                for (Direction direction : Direction.values()) {
                    if (direction == ingressSide || maxReceive == accepted) {
                        continue;
                    }
                    BlockPos targetPos = worldPosition.relative(direction);
                    if (!level.hasChunkAt(targetPos)) {
                        continue;
                    }
                    IEnergyStorage target = level.getCapability(
                            Capabilities.EnergyStorage.BLOCK,
                            targetPos,
                            direction.getOpposite()
                    );
                    if (target != null && target.canReceive()) {
                        accepted += target.receiveEnergy(maxReceive - accepted, simulate);
                    }
                }
                return accepted;
            } finally {
                activeRoutes.remove(PowerSwitchBlockEntity.this);
                if (activeRoutes.isEmpty()) {
                    ACTIVE_ROUTES.remove();
                }
            }
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            if (!isEnabled() || level == null) {
                return false;
            }
            return withRoute(() -> {
                for (Direction direction : Direction.values()) {
                    if (direction == ingressSide) {
                        continue;
                    }
                    BlockPos targetPos = worldPosition.relative(direction);
                    if (!level.hasChunkAt(targetPos)) {
                        continue;
                    }
                    IEnergyStorage target = level.getCapability(
                            Capabilities.EnergyStorage.BLOCK,
                            targetPos,
                            direction.getOpposite()
                    );
                    if (target != null && target.canReceive()) {
                        return true;
                    }
                }
                return false;
            });
        }
    }
}
