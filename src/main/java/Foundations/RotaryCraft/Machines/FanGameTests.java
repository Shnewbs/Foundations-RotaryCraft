package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class FanGameTests {
    private FanGameTests() {
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void fanRecipeIsRegistered(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("rotarycraft", "fan");
        var recipe = helper.getLevel().getRecipeManager().byKey(recipeId);
        helper.assertTrue(recipe.isPresent(), "Missing fan recipe");
        helper.assertTrue(
                BuiltInRegistries.ITEM.getKey(
                        recipe.get().value().getResultItem(helper.getLevel().registryAccess()).getItem()
                ).equals(recipeId),
                "Fan recipe has an unexpected result"
        );
        helper.succeed();
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void poweredFanPushesEntitiesInFacingDirection(GameTestHelper helper) {
        BlockPos fanPos = new BlockPos(1, 1, 1);
        clearAirflowPath(helper);
        helper.setBlock(fanPos, MachineContent.FAN.get().defaultBlockState().setValue(FanBlock.FACING, Direction.EAST));
        FanBlockEntity fan = (FanBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(fanPos));
        helper.assertTrue(fan != null, "Fan block entity was not created");
        var energy = helper.getLevel().getCapability(
                Capabilities.EnergyStorage.BLOCK, helper.absolutePos(fanPos), Direction.UP);
        helper.assertTrue(energy != null, "Fan energy capability was not registered");
        helper.assertTrue(energy.receiveEnergy(160, false) == 160, "Fan did not accept energy");
        var pig = helper.spawn(EntityType.PIG, new BlockPos(4, 1, 1));
        helper.succeedWhen(() -> {
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(fanPos)).getValue(FanBlock.ACTIVE),
                    "Powered fan did not show its active state"
            );
            helper.assertTrue(
                    pig.getDeltaMovement().x > 0.05,
                    "Fan did not push the pig forward; velocity=" + pig.getDeltaMovement()
                            + ", position=" + pig.position()
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void fanAirflowStopsAtSolidBlocks(GameTestHelper helper) {
        BlockPos fanPos = new BlockPos(1, 1, 1);
        clearAirflowPath(helper);
        helper.setBlock(fanPos, MachineContent.FAN.get().defaultBlockState().setValue(FanBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        FanBlockEntity fan = (FanBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(fanPos));
        helper.assertTrue(fan != null, "Fan block entity was not created");
        fan.getEnergyStorage().receiveEnergy(160, false);
        var pig = helper.spawn(EntityType.PIG, new BlockPos(5, 1, 1));
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(pig.getDeltaMovement().x == 0, "Fan airflow passed through a solid block");
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(fanPos)).getValue(FanBlock.ACTIVE),
                    "Powered fan did not remain active"
            );
            helper.succeed();
        });
    }

    private static void clearAirflowPath(GameTestHelper helper) {
        for (int x = 2; x <= 9; x++) {
            for (int y = 1; y <= 3; y++) {
                for (int z = 0; z <= 2; z++) {
                    helper.setBlock(new BlockPos(x, y, z), Blocks.AIR);
                }
            }
        }
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void fanDoesNotPushEntitiesWithoutEnergy(GameTestHelper helper) {
        BlockPos fanPos = new BlockPos(1, 1, 1);
        helper.setBlock(fanPos, MachineContent.FAN.get().defaultBlockState().setValue(FanBlock.FACING, Direction.EAST));
        var pig = helper.spawn(EntityType.PIG, new BlockPos(3, 1, 1));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(pig.getDeltaMovement().x == 0, "Unpowered fan pushed an entity");
            helper.assertTrue(
                    !helper.getLevel().getBlockState(helper.absolutePos(fanPos)).getValue(FanBlock.ACTIVE),
                    "Unpowered fan showed its active state"
            );
            helper.succeed();
        });
    }
}
