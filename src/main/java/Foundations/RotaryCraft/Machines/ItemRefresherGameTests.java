package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class ItemRefresherGameTests {
    private ItemRefresherGameTests() {
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void itemRefresherRecipeIsRegistered(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("rotarycraft", "item_refresher");
        var recipe = helper.getLevel().getRecipeManager().byKey(recipeId);
        helper.assertTrue(recipe.isPresent(), "Missing Item Refresher recipe");
        helper.assertTrue(
                recipe.get().value().getResultItem(helper.getLevel().registryAccess())
                        .is(MachineContent.ITEM_REFRESHER_ITEM.get()),
                "Item Refresher recipe has an unexpected result"
        );
        helper.succeed();
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void poweredRefresherExtendsNearbyItemLifespans(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.ITEM_REFRESHER.get());
        BlockPos absolutePos = helper.absolutePos(pos);
        var capability = helper.getLevel().getCapability(
                Capabilities.EnergyStorage.BLOCK, absolutePos, Direction.UP);
        helper.assertTrue(capability != null, "Item Refresher energy capability was not registered");
        helper.assertTrue(capability.receiveEnergy(160, false) == 160, "Item Refresher did not accept FE");
        helper.assertTrue(capability.extractEnergy(1, false) == 0, "Item Refresher exposed energy output");

        ItemEntity nearby = createItem(helper, pos.getX() + 2.5, pos.getY() + 1.5, pos.getZ() + 0.5);
        helper.succeedWhen(() -> {
            helper.assertTrue(
                    nearby.getAge() < 0,
                    "Powered Item Refresher did not extend the dropped item's lifespan"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(absolutePos).getValue(ItemRefresherBlock.ACTIVE),
                    "Powered Item Refresher did not show its active state"
            );
            helper.assertTrue(
                    ((ItemRefresherBlockEntity) helper.getLevel().getBlockEntity(absolutePos))
                            .getEnergyStorage().getEnergyStored() < 160,
                    "Powered Item Refresher did not consume energy"
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void itemRefresherEnergyPersists(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.ITEM_REFRESHER.get());
        BlockPos absolutePos = helper.absolutePos(pos);
        ItemRefresherBlockEntity refresher =
                (ItemRefresherBlockEntity) helper.getLevel().getBlockEntity(absolutePos);
        refresher.getEnergyStorage().receiveEnergy(137, false);

        var saved = refresher.saveWithoutMetadata(helper.getLevel().registryAccess());
        ItemRefresherBlockEntity restored = new ItemRefresherBlockEntity(
                absolutePos, helper.getLevel().getBlockState(absolutePos));
        restored.loadAdditional(saved, helper.getLevel().registryAccess());
        helper.assertTrue(
                restored.getEnergyStorage().getEnergyStored() == 137,
                "Item Refresher did not restore its stored energy"
        );
        helper.succeed();
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void refresherOnlyAffectsItemsWithinItsRange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.ITEM_REFRESHER.get());
        ItemRefresherBlockEntity refresher =
                (ItemRefresherBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        refresher.getEnergyStorage().receiveEnergy(160, false);

        ItemEntity nearby = createItem(helper, pos.getX() + 2.5, pos.getY() + 1.5, pos.getZ() + 0.5);
        ItemEntity distant = createItem(helper, pos.getX() + 6.5, pos.getY() + 1.5, pos.getZ() + 0.5);

        helper.runAfterDelay(5, () -> {
            helper.assertTrue(nearby.getAge() < 0, "Item inside the effect range was not refreshed");
            helper.assertTrue(
                    distant.getAge() > 0,
                    "Item outside the effect range was unexpectedly refreshed"
            );
            helper.succeed();
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void unpoweredRefresherDoesNotExtendLifespan(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.ITEM_REFRESHER.get());
        ItemEntity item = createItem(helper, pos.getX() + 2.5, pos.getY() + 1.5, pos.getZ() + 0.5);

        helper.runAfterDelay(3, () -> {
            helper.assertTrue(item.getAge() > 0, "Unpowered Item Refresher extended the dropped item's lifespan");
            helper.assertTrue(
                    !helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(ItemRefresherBlock.ACTIVE),
                    "Unpowered Item Refresher showed its active state"
            );
            helper.succeed();
        });
    }

    private static ItemEntity createItem(GameTestHelper helper, double x, double y, double z) {
        var level = helper.getLevel();
        ItemEntity item = new ItemEntity(
                level,
                helper.absolutePos(BlockPos.ZERO).getX() + x,
                helper.absolutePos(BlockPos.ZERO).getY() + y,
                helper.absolutePos(BlockPos.ZERO).getZ() + z,
                new ItemStack(Items.DIAMOND)
        );
        level.addFreshEntity(item);
        return item;
    }
}
