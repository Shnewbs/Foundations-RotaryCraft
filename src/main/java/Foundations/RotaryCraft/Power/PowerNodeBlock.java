package Foundations.RotaryCraft.Power;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public final class PowerNodeBlock extends BaseEntityBlock {
  private static final MapCodec<PowerNodeBlock> CODEC = simpleCodec(PowerNodeBlock::new);
  private static final BooleanProperty[] CONNECTIONS = {
    BlockStateProperties.DOWN,
    BlockStateProperties.UP,
    BlockStateProperties.NORTH,
    BlockStateProperties.SOUTH,
    BlockStateProperties.WEST,
    BlockStateProperties.EAST
  };

  public PowerNodeBlock(BlockBehaviour.Properties properties) {
    super(properties);
    registerDefaultState(
        stateDefinition
            .any()
            .setValue(BlockStateProperties.DOWN, false)
            .setValue(BlockStateProperties.UP, false)
            .setValue(BlockStateProperties.NORTH, false)
            .setValue(BlockStateProperties.SOUTH, false)
            .setValue(BlockStateProperties.WEST, false)
            .setValue(BlockStateProperties.EAST, false));
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
    builder.add(CONNECTIONS);
  }

  @Nullable
  @Override
  public BlockState getStateForPlacement(BlockPlaceContext context) {
    BlockState state = defaultBlockState();
    for (Direction direction : Direction.values()) {
      state =
          state.setValue(
              propertyFor(direction),
              isPowerNetworkBlock(
                      context.getLevel().getBlockState(context.getClickedPos().relative(direction)))
                  || hasEnergyPort(
                      context.getLevel(),
                      context.getClickedPos().relative(direction),
                      direction.getOpposite()));
    }
    return state;
  }

  @Override
  protected BlockState updateShape(
      BlockState state,
      Direction direction,
      BlockState neighborState,
      net.minecraft.world.level.LevelAccessor level,
      BlockPos pos,
      BlockPos neighborPos) {
    if (level instanceof Level world) CableNetwork.invalidate(world);
    return state.setValue(
        propertyFor(direction),
        isPowerNetworkBlock(neighborState)
            || level instanceof Level world
                && hasEnergyPort(world, neighborPos, direction.getOpposite()));
  }

  @Override
  public boolean hasAnalogOutputSignal(BlockState state) {
    return true;
  }

  @Override
  public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
    if (!(level.getBlockEntity(pos) instanceof PowerNodeBlockEntity node)) {
      return 0;
    }
    return signalStrength(
        node.getEnergyStorage().getEnergyStored(), node.getEnergyStorage().getMaxEnergyStored());
  }

  static boolean hasEnergyPort(Level level, BlockPos pos, Direction side) {
    return level.hasChunkAt(pos)
        && level.getCapability(
                net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, side)
            != null;
  }

  static int signalStrength(int stored, int capacity) {
    if (stored <= 0 || capacity <= 0) {
      return 0;
    }
    return Math.min(15, (int) Math.ceil(stored * 15.0 / capacity));
  }

  static BooleanProperty propertyFor(Direction direction) {
    return CONNECTIONS[direction.get3DDataValue()];
  }

  static boolean isPowerNetworkBlock(BlockState state) {
    Block block = state.getBlock();
    return block instanceof PowerNodeBlock
        || block instanceof PowerGeneratorBlock
        || block instanceof SolarGeneratorBlock
        || block instanceof WindGeneratorBlock
        || block instanceof HydroGeneratorBlock
        || block instanceof SteamGeneratorBlock
        || block instanceof GeothermalGeneratorBlock
        || block instanceof PowerSwitchBlock && state.getValue(PowerSwitchBlock.ENABLED);
  }

  @Override
  protected void onRemove(
      BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
    if (!state.is(newState.getBlock())) {
      CableNetwork.invalidate(level);
      super.onRemove(state, level, pos, newState, movedByPiston);
    }
  }

  private static final java.util.Map<Integer, net.minecraft.world.phys.shapes.VoxelShape> SHAPES =
      new java.util.concurrent.ConcurrentHashMap<>();

  @Override
  protected net.minecraft.world.phys.shapes.VoxelShape getShape(
      BlockState state,
      net.minecraft.world.level.BlockGetter level,
      BlockPos pos,
      net.minecraft.world.phys.shapes.CollisionContext context) {
    if (state.is(PowerContent.POWER_CELL.get()))
      return Foundations.RotaryCraft.Geometry.MachineShapes.shape(state);
    int mask = 0;
    for (var direction : Direction.values())
      if (state.getValue(propertyFor(direction))) mask |= 1 << direction.get3DDataValue();
    final int key = mask;
    return SHAPES.computeIfAbsent(key, PowerNodeBlock::cableShape);
  }

  private static net.minecraft.world.phys.shapes.VoxelShape cableShape(int mask) {
    var shape = Block.box(7, 7, 7, 9, 9, 9);
    for (var direction : Direction.values())
      if ((mask & (1 << direction.get3DDataValue())) != 0)
        shape =
            net.minecraft.world.phys.shapes.Shapes.or(
                shape,
                switch (direction) {
                  case NORTH -> Block.box(7.25, 7.25, 0, 8.75, 8.75, 8);
                  case SOUTH -> Block.box(7.25, 7.25, 8, 8.75, 8.75, 16);
                  case WEST -> Block.box(0, 7.25, 7.25, 8, 8.75, 8.75);
                  case EAST -> Block.box(8, 7.25, 7.25, 16, 8.75, 8.75);
                  case DOWN -> Block.box(7.25, 0, 7.25, 8.75, 8, 8.75);
                  case UP -> Block.box(7.25, 8, 7.25, 8.75, 16, 8.75);
                });
    return shape;
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }

  @Nullable
  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new PowerNodeBlockEntity(pos, state);
  }

  @Nullable
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
      Level level, BlockState state, BlockEntityType<T> type) {
    if (level.isClientSide) {
      return null;
    }
    return createTickerHelper(
        type, PowerContent.POWER_NODE.get(), PowerNodeBlockEntity::serverTick);
  }
}
