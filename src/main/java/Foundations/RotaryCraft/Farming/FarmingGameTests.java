package Foundations.RotaryCraft.Farming;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class FarmingGameTests {
    private FarmingGameTests() {
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void canolaSeedsAreCraftableAndGrowOnFarmland(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("rotarycraft", "canola_seeds");
        var recipe = helper.getLevel().getRecipeManager().byKey(recipeId);
        helper.assertTrue(recipe.isPresent(), "Missing canola seed recipe");
        helper.assertTrue(
                recipe.get().value().getResultItem(helper.getLevel().registryAccess()).is(FarmingContent.CANOLA_SEEDS.get()),
                "Canola seed recipe has an unexpected result"
        );

        BlockPos cropPos = new BlockPos(1, 1, 1);
        helper.setBlock(
                cropPos.below(),
                Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE)
        );
        helper.setBlock(cropPos, FarmingContent.CANOLA_CROP.get());
        BlockPos absoluteCropPos = helper.absolutePos(cropPos);
        CanolaCropBlock crop = FarmingContent.CANOLA_CROP.get();
        var cropState = helper.getLevel().getBlockState(absoluteCropPos);
        helper.assertTrue(cropState.canSurvive(helper.getLevel(), absoluteCropPos), "Canola should survive on farmland");

        crop.performBonemeal(helper.getLevel(), helper.getLevel().getRandom(), absoluteCropPos, cropState);
        int age = helper.getLevel().getBlockState(absoluteCropPos).getValue(CropBlock.AGE);
        helper.assertTrue(age > 0, "Bonemeal did not advance canola growth");
        helper.assertTrue(age <= CropBlock.MAX_AGE, "Canola grew beyond its mature stage");
        helper.assertTrue(
                crop.getStateForAge(CropBlock.MAX_AGE).canSurvive(helper.getLevel(), absoluteCropPos),
                "Mature canola should remain planted on farmland"
        );
        helper.assertTrue(Items.WHEAT_SEEDS != FarmingContent.CANOLA_SEEDS.get(), "Canola must use its own seed item");
        helper.succeed();
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void denseCanolaSeedsPlantNineCrops(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("rotarycraft", "dense_canola_seeds");
        var recipe = helper.getLevel().getRecipeManager().byKey(recipeId);
        helper.assertTrue(recipe.isPresent(), "Missing dense canola seed recipe");
        helper.assertTrue(
                recipe.get().value().getResultItem(helper.getLevel().registryAccess())
                        .is(FarmingContent.DENSE_CANOLA_SEEDS.get()),
                "Dense canola seed recipe has an unexpected result"
        );

        BlockPos center = new BlockPos(2, 1, 2);
        BlockPos absoluteCenter = helper.absolutePos(center);
        for (int x = -DenseCanolaSeedItem.FIELD_RADIUS; x <= DenseCanolaSeedItem.FIELD_RADIUS; x++) {
            for (int z = -DenseCanolaSeedItem.FIELD_RADIUS; z <= DenseCanolaSeedItem.FIELD_RADIUS; z++) {
                helper.getLevel().setBlock(
                        absoluteCenter.offset(x, 0, z),
                        Blocks.AIR.defaultBlockState(),
                        3
                );
                helper.getLevel().setBlock(
                        absoluteCenter.offset(x, -1, z),
                        Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE),
                        3
                );
            }
        }

        DenseCanolaSeedItem denseSeeds = FarmingContent.DENSE_CANOLA_SEEDS.get();
        helper.assertTrue(helper.getLevel().hasChunkAt(absoluteCenter), "Dense seed test field chunk is not loaded");
        helper.assertTrue(
                helper.getLevel().getBlockState(absoluteCenter.below()).is(Blocks.FARMLAND),
                "Dense seed test center has no farmland"
        );
        helper.assertTrue(
                helper.getLevel().getBlockState(absoluteCenter).canBeReplaced(),
                "Dense seed test center is not replaceable"
        );
        helper.assertTrue(
                FarmingContent.CANOLA_CROP.get().defaultBlockState().canSurvive(helper.getLevel(), absoluteCenter),
                "Canola cannot survive in the dense seed test center"
        );
        for (int x = -DenseCanolaSeedItem.FIELD_RADIUS; x <= DenseCanolaSeedItem.FIELD_RADIUS; x++) {
            for (int z = -DenseCanolaSeedItem.FIELD_RADIUS; z <= DenseCanolaSeedItem.FIELD_RADIUS; z++) {
                BlockPos cropPos = absoluteCenter.offset(x, 0, z);
                helper.assertTrue(
                        helper.getLevel().getBlockState(cropPos).canBeReplaced(),
                        "Dense seed test crop position is not replaceable: " + cropPos
                );
                helper.assertTrue(
                        FarmingContent.CANOLA_CROP.get().defaultBlockState().canSurvive(helper.getLevel(), cropPos),
                        "Canola cannot survive at dense seed test crop position: " + cropPos
                );
            }
        }
        net.minecraft.world.item.ItemStack seedStack = new net.minecraft.world.item.ItemStack(denseSeeds, 2);
        int planted = denseSeeds.plantField(helper.getLevel(), absoluteCenter, null, seedStack);
        helper.assertTrue(planted == 9, "Dense canola seed should plant a 3x3 patch");
        helper.assertTrue(seedStack.getCount() == 1, "A successful 3x3 planting should consume one dense seed");
        for (int x = -DenseCanolaSeedItem.FIELD_RADIUS; x <= DenseCanolaSeedItem.FIELD_RADIUS; x++) {
            for (int z = -DenseCanolaSeedItem.FIELD_RADIUS; z <= DenseCanolaSeedItem.FIELD_RADIUS; z++) {
                helper.assertTrue(
                        helper.getLevel().getBlockState(absoluteCenter.offset(x, 0, z))
                                .is(FarmingContent.CANOLA_CROP.get()),
                        "Dense seed failed to plant the complete 3x3 field"
                );
            }
        }
        helper.succeed();
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void sprinklerWatersFarmlandAndAcceleratesCanola(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("rotarycraft", "sprinkler");
        var recipe = helper.getLevel().getRecipeManager().byKey(recipeId);
        helper.assertTrue(recipe.isPresent(), "Missing sprinkler recipe");
        helper.assertTrue(
                recipe.get().value().getResultItem(helper.getLevel().registryAccess())
                        .is(FarmingContent.SPRINKLER.get().asItem()),
                "Sprinkler recipe has an unexpected result"
        );

        BlockPos sprinklerPos = new BlockPos(1, 1, 1);
        BlockPos cropPos = sprinklerPos.east();
        BlockPos farmlandPos = cropPos.below();
        helper.setBlock(
                farmlandPos,
                Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 0)
        );
        helper.setBlock(cropPos, FarmingContent.CANOLA_CROP.get());
        helper.setBlock(sprinklerPos, FarmingContent.SPRINKLER.get());

        SprinklerBlockEntity sprinkler = (SprinklerBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(sprinklerPos));
        helper.assertTrue(sprinkler != null, "Sprinkler block entity was not created");
        helper.assertTrue(sprinkler.addWaterBucket(), "Sprinkler rejected a water bucket while empty");
        helper.assertTrue(!sprinkler.canAcceptWaterBucket(), "Sprinkler accepted more than one stored water bucket");

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(farmlandPos)).getValue(FarmBlock.MOISTURE)
                            == FarmBlock.MAX_MOISTURE,
                    "Sprinkler did not hydrate nearby farmland"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(cropPos)).getValue(CropBlock.AGE) > 0,
                    "Sprinkler did not accelerate nearby canola growth"
            );
            helper.assertTrue(
                    sprinkler.getStoredWater() == SprinklerBlockEntity.WATER_PER_BUCKET
                            - SprinklerBlockEntity.WATER_PER_OPERATION,
                    "Sprinkler did not consume water after operating"
            );
        });
    }
}
