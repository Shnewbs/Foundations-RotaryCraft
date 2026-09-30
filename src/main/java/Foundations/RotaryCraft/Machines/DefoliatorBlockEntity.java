package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class DefoliatorBlockEntity extends BlockEntity {
    private static final int ENERGY_CAPACITY = 50_000;
    private static final int MAX_ENERGY_INPUT = 160;
    private static final int ENERGY_PER_TICK = 8;
    private static final int WORK_INTERVAL_TICKS = 20;
    private static final int POISON_CAPACITY = 4_000;
    private static final int POISON_PER_POTION = 1_000;
    private static final int AREA_RANGE = 3;

    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int received = super.receiveEnergy(maxReceive, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = super.extractEnergy(maxExtract, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }
    };

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? isPoisonPotion(stack) : stack.is(Items.GLASS_BOTTLE);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == 0 ? 1 : super.getSlotLimit(slot);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == 0 ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate);
        }
    };

    private int poisonCharge;
    private int workTicks;
    private boolean hasTarget;

    public DefoliatorBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.DEFOLIATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DefoliatorBlockEntity defoliator) {
        defoliator.tickDefoliator(level, state);
    }

    private void tickDefoliator(Level level, BlockState state) {
        loadPoisonPotion();
        BlockPos target = findTarget(level);
        hasTarget = target != null && poisonCharge > 0 && energy.getEnergyStored() >= ENERGY_PER_TICK;
        if (state.getValue(DefoliatorBlock.ACTIVE) != hasTarget) {
            level.setBlock(worldPosition, state.setValue(DefoliatorBlock.ACTIVE, hasTarget), 3);
            level.updateNeighborsAt(worldPosition, state.getBlock());
        }

        if (!hasTarget) {
            workTicks = 0;
            return;
        }

        energy.extractEnergy(ENERGY_PER_TICK, false);
        if (++workTicks >= WORK_INTERVAL_TICKS) {
            workTicks = 0;
            if (target != null && level.destroyBlock(target, true)) {
                poisonCharge--;
                poisonNearbyEntities(level, target);
                setChanged();
            }
        }
    }

    private void loadPoisonPotion() {
        if (poisonCharge > POISON_CAPACITY - POISON_PER_POTION
                || !isPoisonPotion(inventory.getStackInSlot(0))
                || !canStoreBottle()) {
            return;
        }

        ItemStack input = inventory.getStackInSlot(0);
        inventory.setStackInSlot(0, input.getCount() == 1 ? ItemStack.EMPTY : input.copyWithCount(input.getCount() - 1));
        inventory.insertItem(1, Items.GLASS_BOTTLE.getDefaultInstance(), false);
        poisonCharge += POISON_PER_POTION;
        setChanged();
    }

    private boolean canStoreBottle() {
        return inventory.insertItem(1, Items.GLASS_BOTTLE.getDefaultInstance(), true).isEmpty();
    }

    private BlockPos findTarget(Level level) {
        for (int y = -AREA_RANGE; y <= AREA_RANGE; y++) {
            for (int x = -AREA_RANGE; x <= AREA_RANGE; x++) {
                for (int z = -AREA_RANGE; z <= AREA_RANGE; z++) {
                    BlockPos candidate = worldPosition.offset(x, y, z);
                    if (level.hasChunkAt(candidate) && isDefoliatable(level.getBlockState(candidate))) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private boolean isDefoliatable(BlockState state) {
        Block block = state.getBlock();
        return state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.SAPLINGS)
                || block instanceof BushBlock
                || block instanceof VineBlock
                || state.is(Blocks.CACTUS);
    }

    private void poisonNearbyEntities(Level level, BlockPos target) {
        AABB area = new AABB(target).inflate(3);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 1));
        }
    }

    private static boolean isPoisonPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.potion().filter(potion -> potion.is(Potions.POISON)).isPresent();
    }

    public EnergyStorage getEnergyStorage() {
        return energy;
    }

    public ItemStackHandler getItemHandler() {
        return inventory;
    }

    public boolean hasTarget() {
        return hasTarget;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.deserializeNBT(registries, tag.getCompound("energy"));
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        poisonCharge = Math.max(0, Math.min(tag.getInt("poison_charge"), POISON_CAPACITY));
        workTicks = Math.max(0, Math.min(tag.getInt("work_ticks"), WORK_INTERVAL_TICKS - 1));
        hasTarget = tag.getBoolean("has_target");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energy.serializeNBT(registries));
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("poison_charge", poisonCharge);
        tag.putInt("work_ticks", workTicks);
        tag.putBoolean("has_target", hasTarget);
    }
}
