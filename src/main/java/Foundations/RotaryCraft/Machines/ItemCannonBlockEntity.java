package Foundations.RotaryCraft.Machines;

import Foundations.RotaryCraft.Power.MachineEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ItemCannonBlockEntity extends BlockEntity {
  private static final int ENERGY_CAPACITY = 524_288;
  private static final int MIN_ENERGY = 1_024;
  private static final int OPERATION_TICKS = 8;
  private final MachineEnergyStorage energy =
      new MachineEnergyStorage(ENERGY_CAPACITY, 4_096, 0, this::setChanged);
  private final ItemStackHandler items =
      new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
        }
      };
  private BlockPos target;
  private int operationTicks;

  public ItemCannonBlockEntity(BlockPos pos, BlockState state) {
    super(MachineContent.ITEM_CANNON_BLOCK_ENTITY.get(), pos, state);
  }

  public static void serverTick(
      Level level, BlockPos pos, BlockState state, ItemCannonBlockEntity cannon) {
    cannon.tickCannon(level);
  }

  private void tickCannon(Level level) {
    if (target == null || energy.getEnergyStored() < MIN_ENERGY || isInventoryEmpty()) {
      return;
    }
    if (++operationTicks < OPERATION_TICKS) {
      return;
    }
    operationTicks = 0;
    IItemHandler destination = level.getCapability(Capabilities.ItemHandler.BLOCK, target, null);
    if (destination == null) {
      return;
    }
    for (int slot = 0; slot < items.getSlots(); slot++) {
      if (items.getStackInSlot(slot).isEmpty()) {
        continue;
      }
      var stack =
          items.extractItem(
              slot,
              energy.getEnergyStored() >= ENERGY_CAPACITY
                  ? items.getStackInSlot(slot).getCount()
                  : 1,
              true);
      var remainder = insert(destination, stack);
      int moved = stack.getCount() - remainder.getCount();
      if (moved > 0) {
        items.extractItem(slot, moved, false);
        energy.consume(MIN_ENERGY);
      }
      break;
    }
    setChanged();
  }

  public boolean isInventoryEmpty() {
    for (int slot = 0; slot < items.getSlots(); slot++) {
      if (!items.getStackInSlot(slot).isEmpty()) {
        return false;
      }
    }
    return true;
  }

  private static net.minecraft.world.item.ItemStack insert(
      IItemHandler destination, net.minecraft.world.item.ItemStack stack) {
    net.minecraft.world.item.ItemStack remainder = stack;
    for (int slot = 0; slot < destination.getSlots() && !remainder.isEmpty(); slot++) {
      remainder = destination.insertItem(slot, remainder, false);
    }
    return remainder;
  }

  public EnergyStorage getEnergyStorage() {
    return energy;
  }

  public ItemStackHandler getItemHandler() {
    return items;
  }

  public BlockPos getTarget() {
    return target;
  }

  public void setTarget(BlockPos target) {
    this.target = target;
    setChanged();
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    energy.restore(tag.get("energy"));
    items.deserializeNBT(registries, tag.getCompound("items"));
    operationTicks = tag.getInt("operation_ticks");
    if (tag.contains("target")) {
      target = BlockPos.of(tag.getLong("target"));
    }
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.put("energy", energy.serializeNBT(registries));
    tag.put("items", items.serializeNBT(registries));
    tag.putInt("operation_ticks", operationTicks);
    if (target != null) {
      tag.putLong("target", target.asLong());
    }
  }
}
