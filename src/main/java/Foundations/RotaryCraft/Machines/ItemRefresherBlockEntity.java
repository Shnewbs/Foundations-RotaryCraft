package Foundations.RotaryCraft.Machines;

import Foundations.RotaryCraft.Power.MachineEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.EnergyStorage;

public class ItemRefresherBlockEntity extends BlockEntity {
  private static final int ENERGY_CAPACITY = 50_000;
  private static final int MAX_ENERGY_INPUT = 160;
  private static final int ENERGY_PER_TICK = 16;
  private static final int EFFECT_RANGE = 4;

  private final MachineEnergyStorage energy =
      new MachineEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT, 0, this::setChanged);

  public ItemRefresherBlockEntity(BlockPos pos, BlockState state) {
    super(MachineContent.ITEM_REFRESHER_BLOCK_ENTITY.get(), pos, state);
  }

  public static void serverTick(
      Level level, BlockPos pos, BlockState state, ItemRefresherBlockEntity refresher) {
    refresher.tickRefresher(level, state);
  }

  private void tickRefresher(Level level, BlockState state) {
    boolean active = energy.getEnergyStored() >= ENERGY_PER_TICK;
    if (state.getValue(ItemRefresherBlock.ACTIVE) != active) {
      level.setBlock(worldPosition, state.setValue(ItemRefresherBlock.ACTIVE, active), 3);
    }
    if (!active || !energy.consume(ENERGY_PER_TICK)) {
      return;
    }

    AABB effectArea =
        new AABB(
            worldPosition.getX() - EFFECT_RANGE,
            worldPosition.getY() - EFFECT_RANGE,
            worldPosition.getZ() - EFFECT_RANGE,
            worldPosition.getX() + 1 + EFFECT_RANGE,
            worldPosition.getY() + 1 + EFFECT_RANGE,
            worldPosition.getZ() + 1 + EFFECT_RANGE);
    for (ItemEntity item :
        level.getEntitiesOfClass(ItemEntity.class, effectArea, ItemEntity::isAlive)) {
      item.setExtendedLifetime();
      if (item.getDeltaMovement().y == 0) {
        item.setDeltaMovement(item.getDeltaMovement().x, 0.4, item.getDeltaMovement().z);
        item.hasImpulse = true;
      }
    }
    setChanged();
  }

  public EnergyStorage getEnergyStorage() {
    return energy;
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    energy.restore(tag.get("energy"));
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putInt("energy", energy.getEnergyStored());
  }
}
