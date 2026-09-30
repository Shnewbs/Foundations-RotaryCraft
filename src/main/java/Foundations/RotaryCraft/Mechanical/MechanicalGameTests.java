package Foundations.RotaryCraft.Mechanical;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class MechanicalGameTests {
    private static final BlockPos SOURCE = new BlockPos(1, 2, 1);
    private static void source(GameTestHelper h) {
        h.setBlock(SOURCE, MechanicalContent.DC_ENGINE.get().defaultBlockState().setValue(MechanicalBlock.FACING, Direction.EAST));
        h.setBlock(SOURCE.below(), Blocks.REDSTONE_BLOCK);
    }
    private static ShaftPower power(GameTestHelper h, BlockPos pos, Direction side) {
        return ShaftNetwork.resolve(h.getLevel(), h.absolutePos(pos), side);
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void mechanicalRecipesRegistered(GameTestHelper h) {
        for (String id : new String[]{"dc_engine", "shaft", "gearbox_2", "gearbox_4", "gearbox_8", "gearbox_16"}) {
            h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("rotarycraft", id)).isPresent(), "Missing recipe " + id);
        }
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void engineOutputIsSided(GameTestHelper h) {
        source(h);
        for (Direction side : Direction.values()) {
            var capability = h.getLevel().getCapability(ShaftNetwork.CAPABILITY, h.absolutePos(SOURCE), side);
            h.assertTrue((capability != null) == (side == Direction.EAST), "Wrong output face " + side);
        }
        h.assertTrue(h.getLevel().getCapability(ShaftNetwork.CAPABILITY, h.absolutePos(SOURCE), null) == null, "Unsided query must not bypass facing");
        h.assertTrue(power(h, SOURCE, Direction.EAST).equals(new ShaftPower(256, 4)), "DC output differs from legacy steady output");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void shaftsTransmitAndStopWhenBroken(GameTestHelper h) {
        source(h);
        BlockPos shaft = SOURCE.east(), end = shaft.east();
        h.setBlock(shaft, MechanicalContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.FACING, Direction.EAST));
        h.setBlock(end, MechanicalContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.FACING, Direction.EAST));
        h.assertTrue(power(h, end, Direction.EAST).equals(new ShaftPower(256, 4)), "Shaft lost signal");
        h.setBlock(shaft, Blocks.AIR);
        h.assertTrue(power(h, end, Direction.EAST).watts() == 0, "Broken shaft retained signal");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void disabledSourceStopsImmediately(GameTestHelper h) {
        source(h);
        h.assertTrue(power(h, SOURCE, Direction.EAST).watts() == 1024, "Source did not start");
        h.setBlock(SOURCE.below(), Blocks.AIR);
        h.assertTrue(power(h, SOURCE, Direction.EAST).watts() == 0, "Disabled source retained power");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void allGearboxRatiosConserveIdealPower(GameTestHelper h) {
        source(h);
        var blocks = new net.minecraft.world.level.block.Block[]{MechanicalContent.GEARBOX_2.get(), MechanicalContent.GEARBOX_4.get(), MechanicalContent.GEARBOX_8.get(), MechanicalContent.GEARBOX_16.get()};
        int[] ratios = {2, 4, 8, 16};
        for (int i = 0; i < blocks.length; i++) {
            var state = blocks[i].defaultBlockState().setValue(MechanicalBlock.FACING, Direction.EAST);
            h.setBlock(SOURCE.east(), state);
            h.assertTrue(power(h, SOURCE.east(), Direction.EAST).equals(new ShaftPower(256 / ratios[i], 4 * ratios[i])), "Wrong reduction ratio");
            h.setBlock(SOURCE.east(), state.setValue(MechanicalBlock.REDUCTION, false));
            var expected = new ShaftPower(256 * ratios[i], 4 / ratios[i]);
            h.assertTrue(power(h, SOURCE.east(), Direction.EAST).equals(expected), "Wrong acceleration/integer truncation");
            h.assertTrue(expected.watts() <= 1024, "Gearbox created power");
        }
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void reversedShaftRejectsInput(GameTestHelper h) {
        source(h);
        h.setBlock(SOURCE.east(), MechanicalContent.SHAFT.get().defaultBlockState().setValue(MechanicalBlock.FACING, Direction.WEST));
        h.assertTrue(power(h, SOURCE.east(), Direction.WEST).watts() == 0, "Backwards shaft accepted wrong face");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void unloadedQueriesDoNotLoadChunks(GameTestHelper h) {
        BlockPos remote = new BlockPos(30000000, 80, 30000000);
        h.assertTrue(!h.getLevel().hasChunkAt(remote), "Test requires unloaded chunk");
        h.assertTrue(ShaftNetwork.resolve(h.getLevel(), remote, Direction.EAST).watts() == 0, "Unloaded chunk produced power");
        h.assertTrue(!h.getLevel().hasChunkAt(remote), "Query loaded a chunk");
        h.succeed();
    }
    private static ShaftNode step(BlockPos next) {
        return new ShaftNode() {
            public Input input() { return next == null ? null : new Input(next, Direction.EAST); }
            public ShaftPower apply(ShaftPower upstream) { return next == null ? new ShaftPower(256, 4) : upstream; }
        };
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void loopStopsWithoutRecursion(GameTestHelper h) {
        var first = BlockPos.ZERO;
        var second = first.east();
        var power = ShaftNetwork.resolve(first, Direction.EAST, pos -> true,
                (pos, side) -> step(pos.equals(first) ? second : first));
        h.assertTrue(power.watts() == 0, "Cycle generated power");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void networkTraversalIsBounded(GameTestHelper h) {
        int[] calls = {0};
        var power = ShaftNetwork.resolve(BlockPos.ZERO, Direction.EAST, pos -> true, (pos, side) -> {
            calls[0]++;
            return step(pos.east());
        });
        h.assertTrue(power.watts() == 0 && calls[0] == ShaftNetwork.MAX_NODES, "Unbounded network traversal");
        var boundary = ShaftNetwork.resolve(BlockPos.ZERO, Direction.EAST, pos -> true,
                (pos, side) -> step(pos.getX() == ShaftNetwork.MAX_NODES - 1 ? null : pos.east()));
        h.assertTrue(boundary.watts() == 1024, "Maximum legal path rejected");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void unloadedBoundaryNeverQueriesProvider(GameTestHelper h) {
        int[] calls = {0};
        var power = ShaftNetwork.resolve(BlockPos.ZERO, Direction.EAST, pos -> pos.getX() == 0, (pos, side) -> {
            calls[0]++;
            return step(pos.east());
        });
        h.assertTrue(power.watts() == 0 && calls[0] == 1, "Walker queried an unloaded node");
        h.succeed();
    }
}
