package Foundations.RotaryCraft.Machines;

import Foundations.RotaryCraft.Power.MachineEnergyStorage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.EnergyStorage;

public class PlayerDetectorBlockEntity extends BlockEntity {
  private static final int MAX_ENERGY = 100_000;
  private static final int ENERGY_PER_BLOCK = 128;
  private static final int MAX_RANGE = 64;
  private static final int REACTION_TICKS = 100;

  private final MachineEnergyStorage energy =
      new MachineEnergyStorage(MAX_ENERGY, 1_000, 0, this::setChanged);
  private int selectedRange = MAX_RANGE;
  private int detectionTicks;
  private int playerCount;
  private boolean analog;

  public PlayerDetectorBlockEntity(BlockPos pos, BlockState state) {
    super(MachineContent.PLAYER_DETECTOR_BLOCK_ENTITY.get(), pos, state);
  }

  public static void serverTick(
      Level level, BlockPos pos, BlockState state, PlayerDetectorBlockEntity detector) {
    detector.tickDetector(level);
  }

  private void tickDetector(Level level) {
    int range =
        Math.min(selectedRange, Math.min(MAX_RANGE, energy.getEnergyStored() / ENERGY_PER_BLOCK));
    List<Player> players =
        level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(range));
    if (players.isEmpty()) {
      detectionTicks = 0;
      playerCount = 0;
      return;
    }
    detectionTicks = Math.min(REACTION_TICKS, detectionTicks + 1);
    playerCount = Math.min(15, players.size());
    setChanged();
  }

  public EnergyStorage getEnergyStorage() {
    return energy;
  }

  public int getRedstoneOutput() {
    if (detectionTicks < REACTION_TICKS) {
      return 0;
    }
    return analog ? playerCount : 15;
  }

  public int getSelectedRange() {
    return selectedRange;
  }

  public void setSelectedRange(int range) {
    selectedRange = Math.max(1, Math.min(MAX_RANGE, range));
    setChanged();
  }

  public boolean isAnalog() {
    return analog;
  }

  public void setAnalog(boolean analog) {
    this.analog = analog;
    setChanged();
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    energy.restore(tag.get("energy"));
    selectedRange = tag.getInt("range");
    if (selectedRange <= 0) {
      selectedRange = MAX_RANGE;
    }
    analog = tag.getBoolean("analog");
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.put("energy", energy.serializeNBT(registries));
    tag.putInt("range", selectedRange);
    tag.putBoolean("analog", analog);
  }
}
