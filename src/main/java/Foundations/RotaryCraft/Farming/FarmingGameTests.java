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
}
