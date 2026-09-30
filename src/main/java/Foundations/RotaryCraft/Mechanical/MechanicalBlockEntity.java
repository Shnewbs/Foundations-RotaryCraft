package Foundations.RotaryCraft.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Transmission stores configuration in blockstate; only engines retain rotational state. */
public final class MechanicalBlockEntity extends BlockEntity implements ShaftNode {
    private DcEngineState engine = DcEngineState.STOPPED;
    public MechanicalBlockEntity(BlockPos pos, BlockState state) { super(MechanicalContent.ENTITY.get(), pos, state); }
    public Direction outputSide() { return getBlockState().getValue(MechanicalBlock.FACING); }
    public ShaftNode capability(Direction side) { return side == outputSide() ? this : null; }
    @Override public Input input() {
        if (getBlockState().is(MechanicalContent.DC_ENGINE.get())) return null;
        return new Input(worldPosition.relative(outputSide().getOpposite()), outputSide());
    }
    @Override public ShaftPower apply(ShaftPower upstream) {
        if (isRemoved() || level == null || level.isClientSide) return ShaftPower.STOPPED;
        if (getBlockState().is(MechanicalContent.DC_ENGINE.get())) {
            return engine.power();
        }
        int ratio = MechanicalContent.ratio(getBlockState().getBlock());
        return ratio == 1 ? upstream : upstream.gear(ratio, getBlockState().getValue(MechanicalBlock.REDUCTION), Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MechanicalBlockEntity machine) {
        if (level.isClientSide || machine.isRemoved() || !state.is(MechanicalContent.DC_ENGINE.get())) return;
        DcEngineState next = machine.engine.tick(level.hasNeighborSignal(pos));
        if (!next.equals(machine.engine)) {
            machine.engine = next;
            machine.setChanged();
        }
    }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (getBlockState().is(MechanicalContent.DC_ENGINE.get())) {
            tag.putInt("dc_speed", engine.speed());
            tag.putInt("dc_torque", engine.torque());
        }
    }
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        engine = getBlockState().is(MechanicalContent.DC_ENGINE.get())
                ? DcEngineState.restore(tag.getInt("dc_speed"), tag.getInt("dc_torque")) : DcEngineState.STOPPED;
    }
    public ShaftPower power() { return level == null ? ShaftPower.STOPPED : ShaftNetwork.resolve(level, worldPosition, outputSide()); }
}
