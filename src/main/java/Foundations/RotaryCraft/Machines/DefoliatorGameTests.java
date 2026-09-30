package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class DefoliatorGameTests {
    private DefoliatorGameTests() {
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void defoliatorRecipeAndCapabilitiesAreRegistered(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("rotarycraft", "defoliator");
        var recipe = helper.getLevel().getRecipeManager().byKey(recipeId);
        helper.assertTrue(recipe.isPresent(), "Missing Defoliator recipe");
        helper.assertTrue(
                recipe.get().value().getResultItem(helper.getLevel().registryAccess()).is(MachineContent.DEFOLIATOR_ITEM.get()),
                "Defoliator recipe has an unexpected result"
        );

        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.DEFOLIATOR.get());
        var energy = helper.getLevel().getCapability(
                Capabilities.EnergyStorage.BLOCK, helper.absolutePos(pos), null);
        var items = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), null);
        helper.assertTrue(energy != null, "Defoliator energy capability was not registered");
        helper.assertTrue(items != null, "Defoliator item capability was not registered");
        helper.assertTrue(energy.receiveEnergy(160, false) == 160, "Defoliator did not accept FE");

        ItemStack poisonPotion = PotionContents.createItemStack(Items.POTION, Potions.POISON);
        ItemStack rejected = items.insertItem(
                0, PotionContents.createItemStack(Items.POTION, Potions.WATER), false);
        helper.assertTrue(!rejected.isEmpty(), "Defoliator accepted an invalid potion input");
        helper.assertTrue(items.insertItem(0, poisonPotion, false).isEmpty(), "Defoliator rejected a poison potion");
        helper.succeed();
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void poweredDefoliatorClearsVegetationAndReturnsBottle(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockPos leavesPos = pos.above();
        helper.setBlock(pos, MachineContent.DEFOLIATOR.get());
        helper.setBlock(leavesPos, Blocks.OAK_LEAVES);

        DefoliatorBlockEntity defoliator = (DefoliatorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(pos));
        defoliator.getEnergyStorage().receiveEnergy(1_000, false);
        defoliator.getItemHandler().insertItem(
                0, PotionContents.createItemStack(Items.POTION, Potions.POISON), false);

        helper.runAfterDelay(18, () -> {
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(DefoliatorBlock.ACTIVE),
                    "Defoliator did not show its active state while working"
            );
            helper.assertTrue(
                    MachineContent.DEFOLIATOR.get().getAnalogOutputSignal(
                            helper.getLevel().getBlockState(helper.absolutePos(pos)),
                            helper.getLevel(), helper.absolutePos(pos)) == 15,
                    "Defoliator did not emit a redstone signal while active"
            );
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(
                        helper.getLevel().getBlockState(helper.absolutePos(leavesPos)).isAir(),
                        "Powered Defoliator did not clear nearby leaves"
                );
                helper.assertTrue(
                        defoliator.getItemHandler().getStackInSlot(1).is(Items.GLASS_BOTTLE),
                        "Defoliator did not return an empty bottle"
                );
                helper.assertTrue(
                        defoliator.saveWithoutMetadata(helper.getLevel().registryAccess())
                                .getInt("poison_charge") == 999,
                        "Defoliator did not consume and persist one poison charge"
                );
                helper.succeed();
            });
        });
    }
}
