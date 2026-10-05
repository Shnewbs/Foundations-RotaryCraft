package Foundations.RotaryCraft.Platform;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class SwitchBlock extends BaseEntityBlock {
    static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    private static final MapCodec<SwitchBlock> CODEC=simpleCodec(SwitchBlock::new);
    public SwitchBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(ENABLED,true));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(ENABLED);}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new PowerEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide()?null:createTickerHelper(type,PowerContent.ENTITY.get(),PowerEntity::tick);}
}
