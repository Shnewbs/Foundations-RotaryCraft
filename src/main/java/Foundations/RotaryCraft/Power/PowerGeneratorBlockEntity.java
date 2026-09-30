package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class PowerGeneratorBlockEntity extends BlockEntity {
    private static final int ENERGY_CAPACITY = 100_000;
    private static final int ENERGY_PER_TICK = 80;
    private static final int MAX_OUTPUT_PER_TICK = 640;

    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage();
    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 && getFuelDuration(stack.getItem()) > 0;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int burnTimeRemaining;
    private int burnTimeTotal;
    private int firstOutputSide;

    public PowerGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.POWER_GENERATOR_ENTITY.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public ItemStackHandler getFuelHandler() {
        return fuel;
    }

    public ItemStack takeFuelForDrop() {
        ItemStack remaining = fuel.getStackInSlot(0);
        fuel.setStackInSlot(0, ItemStack.EMPTY);
        return remaining;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerGeneratorBlockEntity generator) {
        generator.tickGenerator(level, pos);
    }

    private void tickGenerator(Level level, BlockPos pos) {
        distributeEnergy(level, pos);

        if (energy.getEnergyStored() < ENERGY_CAPACITY) {
            if (burnTimeRemaining <= 0) {
                startBurningFuel();
            }
            if (burnTimeRemaining > 0) {
                burnTimeRemaining--;
                energy.generate(ENERGY_PER_TICK);
            }
        }

        boolean lit = burnTimeRemaining > 0;
        if (getBlockState().getValue(PowerGeneratorBlock.LIT) != lit) {
            level.setBlock(pos, getBlockState().setValue(PowerGeneratorBlock.LIT, lit), 3);
        }
        if (level.getGameTime() % 20 == 0) {
            setChanged();
        }
    }

    private void distributeEnergy(Level level, BlockPos pos) {
        int transferBudget = MAX_OUTPUT_PER_TICK;
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
                    Math.min(transferBudget, MAX_OUTPUT_PER_TICK / Direction.values().length)
            );
        }
        firstOutputSide = (firstOutputSide + 1) % Direction.values().length;
    }

    private void startBurningFuel() {
        ItemStack fuelStack = fuel.getStackInSlot(0);
        int duration = getFuelDuration(fuelStack.getItem());
        if (duration <= 0) {
            return;
        }

        burnTimeRemaining = duration;
        ItemStack remainingFuel = fuelStack.copy();
        remainingFuel.shrink(1);
        fuel.setStackInSlot(0, remainingFuel);
        setChanged();
    }

    private static int getFuelDuration(Item item) {
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) {
            return 0;
        }
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(item, 0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("BurnTimeRemaining", burnTimeRemaining);
        tag.putInt("StoredEnergy", energy.getEnergyStored());
        tag.put("Fuel", fuel.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        burnTimeRemaining = tag.getInt("BurnTimeRemaining");
        energy.setEnergy(tag.getInt("StoredEnergy"));
        if (tag.contains("Fuel")) {
            fuel.deserializeNBT(registries, tag.getCompound("Fuel"));
        }
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
