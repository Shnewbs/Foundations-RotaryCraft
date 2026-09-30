package Foundations.RotaryCraft.Machines;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * DecoTank: A decorative fluid storage and display block.
 * Stores various fluids for display or later use in crafting systems.
 */
public class DecoTankBlock extends BaseEntityBlock {
    
    private static final MapCodec<DecoTankBlock> CODEC = simpleCodec(DecoTankBlock::new);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);

    public DecoTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DecoTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, MachineContent.DECO_TANK_BLOCK_ENTITY.get(), DecoTankBlockEntity::serverTick);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent()) {
            if(level.isClientSide)return net.minecraft.world.ItemInteractionResult.SUCCESS;
            return net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,level,pos,hit.getDirection())?net.minecraft.world.ItemInteractionResult.SUCCESS:net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof DecoTankBlockEntity tank) {
            return tank.getFluidLevel();
        }
        return 0;
    }
}
