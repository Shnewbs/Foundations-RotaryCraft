package Foundations.RotaryCraft.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Transmission stores configuration in blockstate, never stored shaft energy. */
public final class MechanicalBlockEntity extends BlockEntity implements ShaftNode {
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
            return level.hasNeighborSignal(worldPosition) ? new ShaftPower(256, 4) : ShaftPower.STOPPED;
        }
        int ratio = MechanicalContent.ratio(getBlockState().getBlock());
        return ratio == 1 ? upstream : upstream.gear(ratio, getBlockState().getValue(MechanicalBlock.REDUCTION), Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    public ShaftPower power() { return level == null ? ShaftPower.STOPPED : ShaftNetwork.resolve(level, worldPosition, outputSide()); }
}
