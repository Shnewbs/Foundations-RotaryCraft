package Foundations.RotaryCraft.Platform;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class CableBlock extends BaseEntityBlock {
    private static final MapCodec<CableBlock> CODEC=simpleCodec(CableBlock::new);
    static final BooleanProperty[] PORTS={BlockStateProperties.DOWN,BlockStateProperties.UP,BlockStateProperties.NORTH,BlockStateProperties.SOUTH,BlockStateProperties.WEST,BlockStateProperties.EAST};
    private static final java.util.Map<Integer,VoxelShape> SHAPES=new java.util.concurrent.ConcurrentHashMap<>();
    public CableBlock(Properties properties){super(properties);var state=stateDefinition.any();for(var port:PORTS)state=state.setValue(port,false);registerDefaultState(state);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PORTS);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PowerEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,PowerContent.ENTITY.get(),PowerEntity::tick);}
    @Override protected BlockState updateShape(BlockState state,LevelReader reader,ScheduledTickAccess scheduled,BlockPos pos,Direction direction,BlockPos neighbor,BlockState neighborState,net.minecraft.util.RandomSource random){
        if(reader instanceof Level level){CableNetwork.invalidate(level);return state.setValue(PORTS[direction.get3DDataValue()],port(level,neighbor,direction.getOpposite()));}return state;
    }
    static boolean port(Level level,BlockPos pos,Direction side){return level.hasChunkAt(pos)&&level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.Energy.BLOCK,pos,side)!=null;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof PowerEntity entity)player.displayClientMessage(entity.status(),true);return InteractionResult.SUCCESS;}
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof PowerEntity entity?(int)Math.ceil(entity.energy.getAmountAsLong()*15.0/entity.energy.getCapacityAsLong()):0;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        if(state.is(PowerContent.ALL.get("power_cell").get()))return Foundations.RotaryCraft.Geometry.MachineShapes.shape(state);
        int mask=0;for(var d:Direction.values())if(state.getValue(PORTS[d.get3DDataValue()]))mask|=1<<d.get3DDataValue();return SHAPES.computeIfAbsent(mask,CableBlock::shape);
    }
    private static VoxelShape shape(int mask){var shape=Block.box(7,7,7,9,9,9);for(var d:Direction.values())if((mask&(1<<d.get3DDataValue()))!=0)shape=Shapes.or(shape,switch(d){case NORTH->Block.box(7.25,7.25,0,8.75,8.75,8);case SOUTH->Block.box(7.25,7.25,8,8.75,8.75,16);case WEST->Block.box(0,7.25,7.25,8,8.75,8.75);case EAST->Block.box(8,7.25,7.25,16,8.75,8.75);case DOWN->Block.box(7.25,0,7.25,8.75,8,8.75);case UP->Block.box(7.25,8,7.25,8.75,16,8.75);});return shape;}
}
