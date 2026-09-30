package Foundations.RotaryCraft.Power;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

public final class SteamGeneratorBlock extends BaseEntityBlock {
    private static final MapCodec<SteamGeneratorBlock> CODEC = simpleCodec(SteamGeneratorBlock::new);
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT;

    public SteamGeneratorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        return Foundations.RotaryCraft.Geometry.MachineShapes.shape(state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(LIT, net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamGeneratorBlockEntity(pos, state);
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
        return createTickerHelper(
                type,
                PowerContent.STEAM_GENERATOR_ENTITY.get(),
                SteamGeneratorBlockEntity::serverTick
        );
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            net.minecraft.world.InteractionHand hand,
            BlockHitResult hit
    ) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SteamGeneratorBlockEntity generator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        int slot = generator.getItemHandler().isItemValid(SteamGeneratorBlockEntity.WATER_SLOT, stack)
                ? SteamGeneratorBlockEntity.WATER_SLOT
                : SteamGeneratorBlockEntity.FUEL_SLOT;
        if (!generator.getItemHandler().isItemValid(slot, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        ItemStack remainder = generator.getItemHandler().insertItem(slot, stack.copyWithCount(1), false);
        if (remainder.isEmpty()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!(level.getBlockEntity(pos) instanceof SteamGeneratorBlockEntity generator)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        ItemStack extracted = generator.takeItemForHand();
        if (extracted.isEmpty()) {
            return InteractionResult.PASS;
        }
        ItemHandlerHelper.giveItemToPlayer(player, extracted);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof SteamGeneratorBlockEntity generator) {
                for (ItemStack stack : generator.takeItemsForDrop()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
