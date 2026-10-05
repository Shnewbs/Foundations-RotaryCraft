package Foundations.RotaryCraft.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Live network step. Providers must not recursively resolve other nodes. */
public interface ShaftNode {
  record Input(BlockPos position, Direction outputSide) {}

  @Nullable
  Input input();

  ShaftPower apply(ShaftPower upstream);
}
