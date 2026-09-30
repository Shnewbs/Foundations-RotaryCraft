package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class MobHarvesterGameTests {
    private MobHarvesterGameTests() {
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void mobHarvesterRecipeIsRegistered(GameTestHelper helper) {
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("rotarycraft", "mob_harvester");
        var recipe = helper.getLevel().getRecipeManager().byKey(recipeId);
        helper.assertTrue(recipe.isPresent(), "Missing Mob Harvester recipe");
        helper.assertTrue(
                recipe.get().value().getResultItem(helper.getLevel().registryAccess())
                        .is(MachineContent.MOB_HARVESTER_ITEM.get()),
                "Mob Harvester recipe has an unexpected result"
        );
        helper.succeed();
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void mobHarvesterCapabilityStoresAndPersistsEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.MOB_HARVESTER.get());
        BlockPos absolutePos = helper.absolutePos(pos);
        var capability = helper.getLevel().getCapability(
                Capabilities.EnergyStorage.BLOCK, absolutePos, null);
        helper.assertTrue(capability != null, "Mob Harvester energy capability was not registered");
        helper.assertTrue(capability.receiveEnergy(160, false) == 160, "Mob Harvester did not accept FE");

        MobHarvesterBlockEntity harvester = getHarvester(helper, pos);
        var saved = harvester.saveWithoutMetadata(helper.getLevel().registryAccess());
        helper.assertTrue(
                saved.getInt("energy") == 160,
                "Mob Harvester did not persist stored energy"
        );
        helper.succeed();
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void mobHarvesterDamagesTargetAfterTwentyPoweredTicks(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.MOB_HARVESTER.get());
        MobHarvesterBlockEntity harvester = getHarvester(helper, pos);
        harvester.getEnergyStorage().receiveEnergy(1_000, false);
        var cow = helper.spawn(EntityType.COW, new BlockPos(1, 2, 1));
        cow.setNoAi(true);
        float initialHealth = cow.getHealth();

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(cow.getHealth() == initialHealth, "Harvester damaged the target before its first cycle");
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(
                        cow.getHealth() == initialHealth - 6,
                        "Harvester did not deal exactly 6 damage at the first 20-tick cycle; health="
                                + cow.getHealth()
                );
                helper.assertTrue(
                        helper.getLevel().getBlockState(helper.absolutePos(pos)).getValue(MobHarvesterBlock.ACTIVE),
                        "Harvester did not show its active state with a target"
                );
                helper.assertTrue(
                        MachineContent.MOB_HARVESTER.get().getAnalogOutputSignal(
                                helper.getLevel().getBlockState(helper.absolutePos(pos)),
                                helper.getLevel(), helper.absolutePos(pos)) == 15,
                        "Harvester did not emit a redstone signal for its target"
                );
                helper.succeed();
            });
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void mobHarvesterExcludesPlayersAndVillagers(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.MOB_HARVESTER.get());
        MobHarvesterBlockEntity harvester = getHarvester(helper, pos);
        harvester.getEnergyStorage().receiveEnergy(1_000, false);

        var villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 3, 1));
        villager.setNoAi(true);
        villager.setPos(helper.absolutePos(pos).getX() + 1.8, helper.absolutePos(pos).getY() + 3,
                helper.absolutePos(pos).getZ() + 1.8);
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos absolutePos = helper.absolutePos(pos);
        player.setPos(absolutePos.getX() + 1.1, absolutePos.getY() + 5, absolutePos.getZ() + 1.1);

        helper.runAfterDelay(1, () -> {
            float villagerHealth = villager.getHealth();
            float playerHealth = player.getHealth();
            helper.runAfterDelay(21, () -> {
                helper.assertTrue(villager.getHealth() == villagerHealth, "Harvester damaged a villager");
                helper.assertTrue(player.getHealth() == playerHealth, "Harvester damaged a player");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void mobHarvesterStopsAtObstructions(GameTestHelper helper) {
        BlockPos blockedPos = new BlockPos(1, 1, 1);
        helper.setBlock(blockedPos, MachineContent.MOB_HARVESTER.get());
        MobHarvesterBlockEntity blocked = getHarvester(helper, blockedPos);
        blocked.getEnergyStorage().receiveEnergy(1_000, false);
        helper.setBlock(new BlockPos(1, 3, 1), Blocks.STONE);
        var protectedCow = helper.spawn(EntityType.COW, new BlockPos(1, 4, 1));
        protectedCow.setNoAi(true);

        helper.runAfterDelay(1, () -> {
            float protectedHealth = protectedCow.getHealth();
            helper.runAfterDelay(24, () -> {
                helper.assertTrue(protectedCow.getHealth() == protectedHealth,
                        "Harvester targeted through a solid obstruction");
                helper.assertTrue(!blocked.hasTarget(), "Obstructed Harvester reported a target");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void mobHarvesterDoesNotOperateWithoutEnergy(GameTestHelper helper) {
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, MachineContent.MOB_HARVESTER.get());
        var cow = helper.spawn(EntityType.COW, new BlockPos(4, 2, 4));
        cow.setNoAi(true);

        helper.runAfterDelay(1, () -> {
            float initialHealth = cow.getHealth();
            MobHarvesterBlockEntity harvester = getHarvester(helper, pos);
            helper.runAfterDelay(24, () -> {
                helper.assertTrue(cow.getHealth() == initialHealth,
                        "Unpowered Harvester damaged a mob");
                helper.assertTrue(harvester.getEnergyStorage().getEnergyStored() == 0,
                        "Unpowered Harvester consumed energy");
                helper.assertTrue(!harvester.hasTarget(), "Unpowered Harvester reported a target");
                helper.assertTrue(
                        !helper.getLevel().getBlockState(helper.absolutePos(pos))
                                .getValue(MobHarvesterBlock.ACTIVE),
                        "Unpowered Harvester showed its active state"
                );
                helper.succeed();
            });
        });
    }

    private static MobHarvesterBlockEntity getHarvester(GameTestHelper helper, BlockPos pos) {
        return (MobHarvesterBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
}
