package Foundations.RotaryCraft.Farming;

import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.ItemLike;

public final class CanolaCropBlock extends CropBlock {
    public CanolaCropBlock() {
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT));
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return FarmingContent.CANOLA_SEEDS.get();
    }
}
