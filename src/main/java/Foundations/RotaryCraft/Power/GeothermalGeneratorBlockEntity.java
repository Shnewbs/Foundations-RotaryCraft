package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class GeothermalGeneratorBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_PER_TICK = 40;
    public static final int LAVA_TICKS = 1_000;
    public static final int MAX_OUTPUT_PER_TICK = 160;

    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage();
    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && stack.is(Items.LAVA_BUCKET);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int burnTimeRemaining;
    private int firstOutputSide;

    public GeothermalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.GEOTHERMAL_GENERATOR_ENTITY.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public ItemStackHandler getItemHandler() {
        return inventory;
    }

    public ItemStack takeBucketForHand() {
        return inventory.extractItem(0, 1, false);
    }

    public ItemStack takeBucketForDrop() {
        return inventory.extractItem(0, inventory.getStackInSlot(0).getCount(), false);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GeothermalGeneratorBlockEntity generator
    ) {
        generator.tickGenerator(level, pos);
    }

    private void tickGenerator(Level level, BlockPos pos) {
        distributeEnergy(level, pos);

        boolean generating = false;
        if (energy.getEnergyStored() < ENERGY_CAPACITY) {
            if (burnTimeRemaining <= 0) {
                startBurningLava();
            }
            if (burnTimeRemaining > 0) {
                burnTimeRemaining--;
                energy.generate(ENERGY_PER_TICK);
                generating = true;
            }
        }

        if (getBlockState().getValue(GeothermalGeneratorBlock.LIT) != generating) {
            level.setBlock(pos, getBlockState().setValue(GeothermalGeneratorBlock.LIT, generating), 3);
        }
        if (level.getGameTime() % 20 == 0) {
            setChanged();
        }
    }

    private void startBurningLava() {
        ItemStack lavaBucket = inventory.getStackInSlot(0);
        if (!lavaBucket.is(Items.LAVA_BUCKET)) {
            return;
        }

        ItemStack remainder = lavaBucket.copy();
        remainder.shrink(1);
        if (remainder.isEmpty()) {
            remainder = new ItemStack(Items.BUCKET);
        }
        inventory.setStackInSlot(0, remainder);
        burnTimeRemaining = LAVA_TICKS;
        setChanged();
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
        tag.putInt("BurnTimeRemaining", burnTimeRemaining);
        tag.putInt("FirstOutputSide", firstOutputSide);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("StoredEnergy"));
        burnTimeRemaining = Math.max(0, Math.min(LAVA_TICKS, tag.getInt("BurnTimeRemaining")));
        firstOutputSide = Math.floorMod(tag.getInt("FirstOutputSide"), Direction.values().length);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
    }

    private final class GeneratorEnergyStorage implements IEnergyStorage {
        private int storedEnergy;

        private void generate(int amount) {
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
