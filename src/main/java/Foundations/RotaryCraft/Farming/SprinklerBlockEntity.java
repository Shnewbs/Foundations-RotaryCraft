package Foundations.RotaryCraft.Farming;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SprinklerBlockEntity extends BlockEntity {
    public static final int OPERATION_INTERVAL = 40;
    public static final int WATER_PER_BUCKET = 1_000;
    public static final int WATER_PER_OPERATION = 20;
    public static final int FIELD_RADIUS = 2;

    private int storedWater;
    private int ticksUntilOperation = OPERATION_INTERVAL;

    public SprinklerBlockEntity(BlockPos pos, BlockState state) {
        super(FarmingContent.SPRINKLER_ENTITY.get(), pos, state);
    }

    public boolean canAcceptWaterBucket() {
        return storedWater == 0;
    }

    public boolean addWaterBucket() {
        if (!canAcceptWaterBucket()) {
            return false;
        }
        storedWater = WATER_PER_BUCKET;
        setActive(true);
        setChanged();
        return true;
    }

    public int getStoredWater() {
        return storedWater;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SprinklerBlockEntity sprinkler
    ) {
        if (level instanceof ServerLevel serverLevel) {
            sprinkler.tickSprinkler(serverLevel, pos);
        }
    }

    private void tickSprinkler(ServerLevel level, BlockPos pos) {
        if (storedWater == 0) {
            ticksUntilOperation = OPERATION_INTERVAL;
            setActive(false);
            return;
        }
        if (--ticksUntilOperation > 0) {
            return;
        }

        ticksUntilOperation = OPERATION_INTERVAL;
        storedWater = Math.max(0, storedWater - WATER_PER_OPERATION);
        waterFarmland(level, pos);
        growOneCrop(level, pos, level.getRandom());
        setActive(storedWater > 0);
        setChanged();
    }

    private void waterFarmland(ServerLevel level, BlockPos pos) {
        for (int x = -FIELD_RADIUS; x <= FIELD_RADIUS; x++) {
            for (int z = -FIELD_RADIUS; z <= FIELD_RADIUS; z++) {
                BlockPos cropPos = pos.offset(x, 0, z);
                if (cropPos.equals(pos) || !level.hasChunkAt(cropPos)) {
                    continue;
                }
                BlockPos farmlandPos = cropPos.below();
                BlockState farmlandState = level.getBlockState(farmlandPos);
                if (farmlandState.getBlock() instanceof FarmBlock
                        && farmlandState.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
                    level.setBlock(
                            farmlandPos,
                            farmlandState.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE),
                            2
                    );
                }
            }
        }
    }

    private void growOneCrop(ServerLevel level, BlockPos pos, RandomSource random) {
        List<BlockPos> crops = new ArrayList<>();
        for (int x = -FIELD_RADIUS; x <= FIELD_RADIUS; x++) {
            for (int z = -FIELD_RADIUS; z <= FIELD_RADIUS; z++) {
                BlockPos cropPos = pos.offset(x, 0, z);
                if (cropPos.equals(pos) || !level.hasChunkAt(cropPos)) {
                    continue;
                }
                BlockState cropState = level.getBlockState(cropPos);
                if (cropState.getBlock() instanceof CropBlock crop
                        && crop.isValidBonemealTarget(level, cropPos, cropState)) {
                    crops.add(cropPos.immutable());
                }
            }
        }
        if (!crops.isEmpty()) {
            BlockPos cropPos = crops.get(random.nextInt(crops.size()));
            CropBlock crop = (CropBlock) level.getBlockState(cropPos).getBlock();
            crop.performBonemeal(level, random, cropPos, level.getBlockState(cropPos));
        }
    }

    private void setActive(boolean active) {
        if (level != null && !level.isClientSide && getBlockState().getValue(SprinklerBlock.LIT) != active) {
            level.setBlock(
                    worldPosition,
                    getBlockState().setValue(SprinklerBlock.LIT, active),
                    3
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StoredWater", storedWater);
        tag.putInt("TicksUntilOperation", ticksUntilOperation);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedWater = Math.max(0, Math.min(WATER_PER_BUCKET, tag.getInt("StoredWater")));
        ticksUntilOperation = Math.max(1, Math.min(OPERATION_INTERVAL, tag.getInt("TicksUntilOperation")));
    }
}
