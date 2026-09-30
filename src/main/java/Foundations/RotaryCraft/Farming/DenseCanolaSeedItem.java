package Foundations.RotaryCraft.Farming;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class DenseCanolaSeedItem extends ItemNameBlockItem {
    public static final int FIELD_RADIUS = 1;

    public DenseCanolaSeedItem(CanolaCropBlock crop, Properties properties) {
        super(crop, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        BlockPos center = clickedState.canBeReplaced()
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());
        Player player = context.getPlayer();

        if (!canPlantAt(level, center)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (plantField(level, center, player, context.getItemInHand()) == 0) {
            return InteractionResult.PASS;
        }
        return InteractionResult.CONSUME;
    }

    int plantField(Level level, BlockPos center, @Nullable Player player, ItemStack stack) {
        int planted = 0;
        for (int x = -FIELD_RADIUS; x <= FIELD_RADIUS; x++) {
            for (int z = -FIELD_RADIUS; z <= FIELD_RADIUS; z++) {
                BlockPos cropPos = center.offset(x, 0, z);
                if (!level.hasChunkAt(cropPos) || !canPlantAt(level, cropPos)) {
                    continue;
                }
                if (player != null && !player.mayUseItemAt(cropPos, Direction.UP, stack)) {
                    continue;
                }
                if (level.setBlock(cropPos, FarmingContent.CANOLA_CROP.get().defaultBlockState(), 3)) {
                    planted++;
                }
            }
        }
        if (planted > 0 && (player == null || !player.getAbilities().instabuild)) {
            stack.shrink(1);
        }
        return planted;
    }

    private static boolean canPlantAt(Level level, BlockPos cropPos) {
        BlockState existingState = level.getBlockState(cropPos);
        return existingState.canBeReplaced()
                && FarmingContent.CANOLA_CROP.get().defaultBlockState().canSurvive(level, cropPos);
    }
}
