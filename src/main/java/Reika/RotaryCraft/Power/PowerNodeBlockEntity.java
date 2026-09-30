package Reika.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class PowerNodeBlockEntity extends BlockEntity {
    private final EnergyStorage energy;
    private final int transferRate;
    private int firstSide;

    public PowerNodeBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.POWER_NODE.get(), pos, state);
        boolean isCell = state.is(PowerContent.POWER_CELL.get());
        int capacity = isCell ? 100_000 : 10_000;
        transferRate = isCell ? 1_000 : 500;
        energy = new EnergyStorage(capacity, transferRate, transferRate) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (received > 0 && !simulate) {
                    setChanged();
                }
                return received;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int extracted = super.extractEnergy(maxExtract, simulate);
                if (extracted > 0 && !simulate) {
                    setChanged();
                }
                return extracted;
            }
        };
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerNodeBlockEntity node) {
        node.distributeEnergy(level, pos);
    }

    private void distributeEnergy(Level level, BlockPos pos) {
        if (energy.getEnergyStored() == 0) {
            return;
        }

        int amountPerSide = Math.max(1, energy.getEnergyStored() / Direction.values().length);
        for (int i = 0; i < Direction.values().length; i++) {
            Direction direction = Direction.from3DDataValue((firstSide + i) % Direction.values().length);
            IEnergyStorage neighbor = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    pos.relative(direction),
                    direction.getOpposite()
            );
            if (neighbor == null || !neighbor.canReceive()) {
                continue;
            }

            int offered = Math.min(amountPerSide, transferRate);
            int accepted = neighbor.receiveEnergy(offered, true);
            if (accepted <= 0) {
                continue;
            }

            int extracted = energy.extractEnergy(accepted, false);
            int received = neighbor.receiveEnergy(extracted, false);
            if (received < extracted) {
                energy.receiveEnergy(extracted - received, false);
            }
        }
        firstSide = (firstSide + 1) % Direction.values().length;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StoredEnergy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.receiveEnergy(tag.getInt("StoredEnergy"), false);
    }
}
