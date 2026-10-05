package Foundations.RotaryCraft.Machines;

import Foundations.RotaryCraft.Power.MachineEnergyStorage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.EnergyStorage;

public class MobHarvesterBlockEntity extends BlockEntity {
  private static final int ENERGY_CAPACITY = 50_000;
  private static final int MAX_ENERGY_INPUT = 160;
  private static final int ENERGY_PER_WORK_TICK = 8;
  private static final int DAMAGE_INTERVAL_TICKS = 20;
  private static final float DAMAGE_PER_CYCLE = 6.0F;
  private static final int VERTICAL_RANGE = 4;

  private final MachineEnergyStorage energy =
      new MachineEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT, 0, this::setChanged);

  private int workTicks;
  private boolean hasTarget;

  public MobHarvesterBlockEntity(BlockPos pos, BlockState state) {
    super(MachineContent.MOB_HARVESTER_BLOCK_ENTITY.get(), pos, state);
  }

  public static void serverTick(
      Level level, BlockPos pos, BlockState state, MobHarvesterBlockEntity harvester) {
    harvester.tickHarvester(level, state);
  }

  private void tickHarvester(Level level, BlockState state) {
    List<LivingEntity> targets = findTargets(level);
    hasTarget = !targets.isEmpty() && energy.getEnergyStored() >= ENERGY_PER_WORK_TICK;
    if (state.getValue(MobHarvesterBlock.ACTIVE) != hasTarget) {
      level.setBlock(worldPosition, state.setValue(MobHarvesterBlock.ACTIVE, hasTarget), 3);
      level.updateNeighborsAt(worldPosition, state.getBlock());
    }

    if (!hasTarget) {
      workTicks = 0;
      return;
    }
    energy.consume(ENERGY_PER_WORK_TICK);
    if (++workTicks >= DAMAGE_INTERVAL_TICKS) {
      workTicks = 0;
      for (LivingEntity target : targets) {
        target.hurt(level.damageSources().magic(), DAMAGE_PER_CYCLE);
      }
    }
    setChanged();
  }

  private List<LivingEntity> findTargets(Level level) {
    int clearHeight = 0;
    for (int offset = 1; offset <= VERTICAL_RANGE; offset++) {
      BlockPos above = worldPosition.above(offset);
      if (!level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
        break;
      }
      clearHeight++;
    }
    if (clearHeight == 0) {
      return List.of();
    }

    AABB area =
        new AABB(
            worldPosition.getX(),
            worldPosition.getY() + 1,
            worldPosition.getZ(),
            worldPosition.getX() + 1,
            worldPosition.getY() + clearHeight + 1,
            worldPosition.getZ() + 1);
    return level.getEntitiesOfClass(
        LivingEntity.class,
        area,
        entity ->
            entity.isAlive()
                && !(entity instanceof Player)
                && !(entity instanceof AbstractVillager));
  }

  public EnergyStorage getEnergyStorage() {
    return energy;
  }

  public boolean hasTarget() {
    return hasTarget;
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    energy.restore(tag.get("energy"));
    workTicks = tag.getInt("work_ticks");
    hasTarget = tag.getBoolean("has_target");
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.putInt("energy", energy.getEnergyStored());
    tag.putInt("work_ticks", workTicks);
    tag.putBoolean("has_target", hasTarget);
  }
}
