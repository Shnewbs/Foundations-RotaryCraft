package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class SteamGeneratorBlockEntity extends BlockEntity {
    public static final int WATER_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int ENERGY_PER_TICK = 40;
    public static final int STEAM_TICKS_PER_WATER_BUCKET = 1_000;
    public static final int MAX_OUTPUT_PER_TICK = 160;

    private final GeneratorEnergyStorage energy = new GeneratorEnergyStorage();
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case WATER_SLOT -> stack.is(Items.WATER_BUCKET);
                case FUEL_SLOT -> getFuelDuration(stack.getItem()) > 0;
                default -> false;
            };
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int burnTimeRemaining;
    private int steamTicksRemaining;
    private int firstOutputSide;

    public SteamGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.STEAM_GENERATOR_ENTITY.get(), pos, state);
    }

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public ItemStackHandler getItemHandler() {
        return inventory;
    }

    public ItemStack takeItemForHand() {
        for (int slot = WATER_SLOT; slot <= FUEL_SLOT; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                return inventory.extractItem(slot, 1, false);
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack[] takeItemsForDrop() {
        return new ItemStack[] {
                inventory.extractItem(WATER_SLOT, inventory.getStackInSlot(WATER_SLOT).getCount(), false),
                inventory.extractItem(FUEL_SLOT, inventory.getStackInSlot(FUEL_SLOT).getCount(), false)
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SteamGeneratorBlockEntity generator) {
        generator.tickGenerator(level, pos);
    }

    private void tickGenerator(Level level, BlockPos pos) {
        distributeEnergy(level, pos);

        boolean generating = false;
        if (energy.getEnergyStored() < ENERGY_CAPACITY) {
            if (burnTimeRemaining <= 0) {
                startBurningFuel();
            }
            if (burnTimeRemaining > 0 && steamTicksRemaining <= 0) {
                loadWater();
            }
            if (burnTimeRemaining > 0 && steamTicksRemaining > 0) {
                burnTimeRemaining--;
                steamTicksRemaining--;
                energy.generate(ENERGY_PER_TICK);
                generating = true;
            }
        }

        if (getBlockState().getValue(SteamGeneratorBlock.LIT) != generating) {
            level.setBlock(pos, getBlockState().setValue(SteamGeneratorBlock.LIT, generating), 3);
        }
        if (level.getGameTime() % 20 == 0) {
            setChanged();
        }
    }

    private void startBurningFuel() {
        ItemStack fuelStack = inventory.getStackInSlot(FUEL_SLOT);
        int duration = getFuelDuration(fuelStack.getItem());
        if (duration <= 0) {
            return;
        }

        burnTimeRemaining = duration;
        ItemStack remainingFuel = fuelStack.copy();
        remainingFuel.shrink(1);
        inventory.setStackInSlot(FUEL_SLOT, remainingFuel);
        setChanged();
    }

    private void loadWater() {
        ItemStack water = inventory.getStackInSlot(WATER_SLOT);
        if (!water.is(Items.WATER_BUCKET)) {
            return;
        }

        ItemStack remainingWater = water.copy();
        remainingWater.shrink(1);
        if (remainingWater.isEmpty()) {
            remainingWater = new ItemStack(Items.BUCKET);
        }
        inventory.setStackInSlot(WATER_SLOT, remainingWater);
        steamTicksRemaining = STEAM_TICKS_PER_WATER_BUCKET;
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

    private static int getFuelDuration(Item item) {
        if (item == Items.LAVA_BUCKET || item == Items.WATER_BUCKET) {
            return 0;
        }
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(item, 0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StoredEnergy", energy.getEnergyStored());
        tag.putInt("BurnTimeRemaining", burnTimeRemaining);
        tag.putInt("SteamTicksRemaining", steamTicksRemaining);
        tag.putInt("FirstOutputSide", firstOutputSide);
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("StoredEnergy"));
        burnTimeRemaining = Math.max(0, tag.getInt("BurnTimeRemaining"));
        steamTicksRemaining = Math.max(0, Math.min(STEAM_TICKS_PER_WATER_BUCKET, tag.getInt("SteamTicksRemaining")));
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
