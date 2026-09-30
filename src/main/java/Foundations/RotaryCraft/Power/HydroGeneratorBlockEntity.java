package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class HydroGeneratorBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_PER_WATER_BLOCK_PER_TICK = 8;
    public static final int MAX_OUTPUT_PER_TICK = 160;

    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage();
    private int firstOutputSide;

    public HydroGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.HYDRO_GENERATOR_ENTITY.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, HydroGeneratorBlockEntity generator) {
        generator.tickGenerator(level, pos);
    }

    static int countAdjacentWater(Level level, BlockPos pos) {
        int count = 0;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            if (level.hasChunkAt(neighborPos)
                    && level.getFluidState(neighborPos).is(FluidTags.WATER)) {
                count++;
            }
        }
        return count;
    }

    private void tickGenerator(Level level, BlockPos pos) {
        distributeEnergy(level, pos);

        int adjacentWater = countAdjacentWater(level, pos);
        int generated = Math.min(
                adjacentWater * ENERGY_PER_WATER_BLOCK_PER_TICK,
                ENERGY_CAPACITY - energy.getEnergyStored()
        );
        energy.generate(generated);

        boolean generating = adjacentWater > 0 && energy.getEnergyStored() < ENERGY_CAPACITY;
        if (getBlockState().getValue(HydroGeneratorBlock.LIT) != generating) {
            level.setBlock(pos, getBlockState().setValue(HydroGeneratorBlock.LIT, generating), 3);
        }
        if (level.getGameTime() % 20 == 0) {
            setChanged();
        }
    }

    private void distributeEnergy(Level level, BlockPos pos) {
        int transferBudget = MAX_OUTPUT_PER_TICK;
        int directionCount = Direction.values().length;
        for (int i = 0; i < directionCount && transferBudget > 0; i++) {
            Direction direction = Direction.from3DDataValue((firstOutputSide + i) % directionCount);
            BlockPos neighborPos = pos.relative(direction);
            if (!level.hasChunkAt(neighborPos)) {
                continue;
            }

            IEnergyStorage neighbor = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    neighborPos,
                    direction.getOpposite()
            );
            if (neighbor == null || !neighbor.canReceive()) {
                continue;
            }

            int remainingDirections = directionCount - i;
            int perDirectionBudget = (transferBudget + remainingDirections - 1) / remainingDirections;
            transferBudget -= EnergyTransfer.transfer(
                    energy,
                    neighbor,
                    Math.min(transferBudget, perDirectionBudget)
            );
        }
        firstOutputSide = (firstOutputSide + 1) % directionCount;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StoredEnergy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("StoredEnergy"));
    }

    private final class GeneratorEnergyStorage implements IEnergyStorage {
        private int storedEnergy;

        private void generate(int amount) {
            if (amount <= 0) {
                return;
            }
            storedEnergy = Math.min(ENERGY_CAPACITY, storedEnergy + amount);
            setChanged();
        }

        private void setEnergy(int amount) {
            storedEnergy = Math.max(0, Math.min(ENERGY_CAPACITY, amount));
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.min(Math.min(maxExtract, MAX_OUTPUT_PER_TICK), storedEnergy);
            if (!simulate && extracted > 0) {
                storedEnergy -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return storedEnergy;
        }

        @Override
        public int getMaxEnergyStored() {
            return ENERGY_CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return storedEnergy > 0;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
