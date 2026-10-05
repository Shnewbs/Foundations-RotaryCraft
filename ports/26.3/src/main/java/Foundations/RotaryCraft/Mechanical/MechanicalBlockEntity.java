package Foundations.RotaryCraft.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Transmission stores configuration in blockstate; only engines retain rotational state. */
public final class MechanicalBlockEntity extends BlockEntity implements ShaftNode {
  private int visualSpeed;

  public int visualSpeed() {
    return visualSpeed;
  }

  private DcEngineState engine = DcEngineState.STOPPED;

  public MechanicalBlockEntity(BlockPos pos, BlockState state) {
    super(MechanicalContent.ENTITY.get(), pos, state);
  }

  public Direction outputSide() {
    return getBlockState().getValue(MechanicalBlock.FACING);
  }

  public ShaftNode capability(Direction side) {
    return side == outputSide() ? this : null;
  }

  @Override
  public Input input() {
    if (getBlockState().is(MechanicalContent.DC_ENGINE.get())) return null;
    return new Input(worldPosition.relative(outputSide().getOpposite()), outputSide());
  }

  @Override
  public ShaftPower apply(ShaftPower upstream) {
    if (isRemoved() || level == null || level.isClientSide()) return ShaftPower.STOPPED;
    if (getBlockState().is(MechanicalContent.DC_ENGINE.get())) {
      return engine.power();
    }
    int ratio = MechanicalContent.ratio(getBlockState().getBlock());
    return ratio == 1
        ? upstream
        : upstream.gear(
            ratio,
            getBlockState().getValue(MechanicalBlock.REDUCTION),
            Integer.MAX_VALUE,
            Integer.MAX_VALUE);
  }

  public static void serverTick(
      net.minecraft.world.level.Level level,
      BlockPos pos,
      BlockState state,
      MechanicalBlockEntity machine) {
    if (level.isClientSide() || machine.isRemoved()) return;
    if (state.is(MechanicalContent.DC_ENGINE.get())) {
      DcEngineState next = machine.engine.tick(level.hasNeighborSignal(pos));
      if (!next.equals(machine.engine)) {
        machine.engine = next;
        machine.setChanged();
      }
    }
    // Stagger display snapshots, and send only changes; gameplay never reads this snapshot.
    if (Math.floorMod(level.getGameTime() + pos.asLong(), 5) == 0) {
      int speed = ShaftNetwork.resolveVisual(level, pos, machine.outputSide()).omega();
      if (speed != machine.visualSpeed) {
        machine.visualSpeed = speed;
        level.sendBlockUpdated(pos, state, state, 2);
      }
    }
  }

  @Override
  public net.minecraft.nbt.CompoundTag getUpdateTag(
      net.minecraft.core.HolderLookup.Provider registries) {
    var tag = saveWithoutMetadata(registries);
    tag.putInt("visual_speed", visualSpeed);
    return tag;
  }

  @Override
  public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
    return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
    super.saveAdditional(output);
    output.putInt("visual_speed", visualSpeed);
    if (getBlockState().is(MechanicalContent.DC_ENGINE.get())) {
      output.putInt("dc_speed", engine.speed());
      output.putInt("dc_torque", engine.torque());
    }
  }

  @Override
  protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
    super.loadAdditional(input);
    visualSpeed = Math.max(0, input.getIntOr("visual_speed", 0));
    engine =
        getBlockState().is(MechanicalContent.DC_ENGINE.get())
            ? DcEngineState.restore(input.getIntOr("dc_speed", 0), input.getIntOr("dc_torque", 0))
            : DcEngineState.STOPPED;
  }

  public ShaftPower power() {
    return level == null
        ? ShaftPower.STOPPED
        : ShaftNetwork.resolve(level, worldPosition, outputSide());
  }
}
