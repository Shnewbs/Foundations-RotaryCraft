package Foundations.RotaryCraft.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class MechanicalBlock extends BaseEntityBlock {
  public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
  public static final BooleanProperty REDUCTION = BooleanProperty.create("reduction");

  public MechanicalBlock(Properties properties) {
    super(properties);
    registerDefaultState(
        stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(REDUCTION, true));
  }

  @Override
  public net.minecraft.world.phys.shapes.VoxelShape getShape(
      BlockState state,
      net.minecraft.world.level.BlockGetter level,
      BlockPos pos,
      net.minecraft.world.phys.shapes.CollisionContext context) {
    return Foundations.RotaryCraft.Geometry.MachineShapes.shape(state);
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(FACING, REDUCTION);
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new MechanicalBlockEntity(pos, state);
  }

  @Override
  public <T extends BlockEntity>
      net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
          Level level,
          BlockState state,
          net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
    if (level.isClientSide()) return null;
    return createTickerHelper(
        type, MechanicalContent.ENTITY.get(), MechanicalBlockEntity::serverTick);
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (MechanicalContent.ratio(state.getBlock()) == 1) return InteractionResult.PASS;
    if (!level.isClientSide()) level.setBlock(pos, state.cycle(REDUCTION), Block.UPDATE_ALL);
    return InteractionResult.SUCCESS;
  }

  @Override
  protected void onPlace(
      BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
    super.onPlace(state, level, pos, oldState, moved);
    if (!state.equals(oldState)) level.invalidateCapabilities(pos);
  }
}
