package Foundations.RotaryCraft.Power;

import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.energy.EnergyStorage;

/** Machine work spends its own buffer independently of the external output port. */
public final class MachineEnergyStorage extends EnergyStorage {
  private final Runnable changed;

  public MachineEnergyStorage(int capacity, int input, int output, Runnable changed) {
    super(capacity, input, output);
    this.changed = changed;
  }

  @Override
  public int receiveEnergy(int amount, boolean simulate) {
    int received = super.receiveEnergy(amount, simulate);
    if (received > 0 && !simulate) changed.run();
    return received;
  }

  @Override
  public int extractEnergy(int amount, boolean simulate) {
    int extracted = super.extractEnergy(amount, simulate);
    if (extracted > 0 && !simulate) changed.run();
    return extracted;
  }

  public boolean consume(int amount) {
    if (amount <= 0 || energy < amount) return false;
    energy -= amount;
    changed.run();
    return true;
  }

  /** Load the existing numeric NBT field without applying the input transfer rate. */
  public void restore(Tag saved) {
    energy =
        saved instanceof IntTag number ? Math.max(0, Math.min(capacity, number.getAsInt())) : 0;
  }
}
