package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class WindGeneratorBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_PER_TICK = 16;
    public static final int MAX_OUTPUT_PER_TICK = 160;
    public static final int MIN_ELEVATION_ABOVE_SEA_LEVEL = 32;

    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage();
    private int firstOutputSide;

    public WindGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.WIND_GENERATOR_ENTITY.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WindGeneratorBlockEntity generator) {
        generator.tickGenerator(level, pos);
    }

    static boolean hasWind(Level level, BlockPos pos) {
        int minimumElevation = level.getSeaLevel() + MIN_ELEVATION_ABOVE_SEA_LEVEL;
        boolean suitableElevation = pos.getY() >= minimumElevation
                && pos.getY() < level.getMaxBuildHeight() - 1;
        BlockPos above = pos.above();
        return suitableElevation && level.isEmptyBlock(above) && level.canSeeSky(above);
    }

    private void tickGenerator(Level level, BlockPos pos) {
        distributeEnergy(level, pos);

        boolean generating = hasWind(level, pos) && energy.getEnergyStored() < ENERGY_CAPACITY;
        if (generating) {
            energy.generate(ENERGY_PER_TICK);
        }

        if (getBlockState().getValue(WindGeneratorBlock.LIT) != generating) {
            level.setBlock(pos, getBlockState().setValue(WindGeneratorBlock.LIT, generating), 3);
        }
        if (level.getGameTime() % 20 == 0) {
            setChanged();
        }
    }

    private void distributeEnergy(Level level, BlockPos pos) {
        int transferBudget = MAX_OUTPUT_PER_TICK;
        int transferPerSide = Math.max(1, MAX_OUTPUT_PER_TICK / Direction.values().length);
        for (int i = 0; i < Direction.values().length && transferBudget > 0; i++) {
            Direction direction = Direction.from3DDataValue((firstOutputSide + i) % Direction.values().length);
            IEnergyStorage neighbor = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    pos.relative(direction),
                    direction.getOpposite()
            );
            if (neighbor == null || !neighbor.canReceive()) {
                continue;
            }

            transferBudget -= EnergyTransfer.transfer(
                    energy,
                    neighbor,
                    Math.min(transferBudget, transferPerSide)
            );
        }
        firstOutputSide = (firstOutputSide + 1) % Direction.values().length;
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
            storedEnergy = Math.min(ENERGY_CAPACITY, storedEnergy + amount);
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
