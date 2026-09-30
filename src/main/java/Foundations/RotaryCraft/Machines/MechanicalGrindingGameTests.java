package Foundations.RotaryCraft.Machines;

import Foundations.RotaryCraft.Mechanical.MechanicalContent;
import Foundations.RotaryCraft.Mechanical.MechanicalBlock;
import Foundations.RotaryCraft.Mechanical.MechanicalBlockEntity;
import Foundations.RotaryCraft.Mechanical.ShaftPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class MechanicalGrindingGameTests {
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    /** Injects signals at the input boundary; real world/DC capability tests are separate. */
    private static final class Fixture extends GrindstoneBlockEntity {
        private MechanicalInput signal = new MechanicalInput(true, ShaftPower.STOPPED);
        Fixture(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) { super(pos, state); }
        @Override protected MechanicalInput readMechanicalInput() { return signal; }
    }
    private static Fixture fixture(GameTestHelper h) {
        h.setBlock(POS, MachineContent.GRINDSTONE.get());
        var position = h.absolutePos(POS);
        h.getLevel().removeBlockEntity(position);
        var machine = new Fixture(position, h.getLevel().getBlockState(position));
        h.getLevel().setBlockEntity(machine);
        machine.getItemHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        machine.getEnergyStorage().receiveEnergy(100, false);
        return machine;
    }
    private static void tick(GameTestHelper h, GrindstoneBlockEntity machine, int count) {
        for (int i = 0; i < count; i++) GrindstoneBlockEntity.serverTick(h.getLevel(), machine.getBlockPos(), machine.getBlockState(), machine);
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void sufficientShaftPowerProcessesWithoutFE(GameTestHelper h) {
        var machine = fixture(h);
        machine.signal = new GrindstoneBlockEntity.MechanicalInput(true, new ShaftPower(32, 128));
        tick(h, machine, 100);
        h.assertTrue(machine.getItemHandler().getStackInSlot(1).is(Items.GRAVEL), "Mechanical input did not process recipe");
        h.assertTrue(machine.getItemHandler().getStackInSlot(0).isEmpty(), "Input not consumed");
        h.assertTrue(machine.getEnergyStorage().getEnergyStored() == 100, "Mechanical operation consumed FE");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void insufficientTorqueOrWattsCannotUseFE(GameTestHelper h) {
        var machine = fixture(h);
        for (var power : new ShaftPower[]{new ShaftPower(64, 64), new ShaftPower(1, 128), ShaftPower.STOPPED}) {
            machine.signal = new GrindstoneBlockEntity.MechanicalInput(true, power);
            tick(h, machine, 5);
            h.assertTrue(machine.getElapsed() == 0 && !machine.isOperating(), "Insufficient mechanical signal advanced recipe");
            h.assertTrue(machine.getEnergyStorage().getEnergyStored() == 100, "FE bypassed shaft requirements");
        }
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void lossOfShaftPowerPausesAndResumes(GameTestHelper h) {
        var machine = fixture(h);
        machine.signal = new GrindstoneBlockEntity.MechanicalInput(true, new ShaftPower(32, 128));
        tick(h, machine, 50);
        machine.signal = new GrindstoneBlockEntity.MechanicalInput(true, ShaftPower.STOPPED);
        tick(h, machine, 20);
        h.assertTrue(machine.getElapsed() == 50, "Stopped connected source did not pause progress");
        machine.signal = new GrindstoneBlockEntity.MechanicalInput(true, new ShaftPower(32, 128));
        tick(h, machine, 50);
        h.assertTrue(machine.getItemHandler().getStackInSlot(1).is(Items.GRAVEL), "Mechanical recipe failed to resume");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void fullOutputBlocksMechanicalProcessing(GameTestHelper h) {
        var machine = fixture(h);
        machine.signal = new GrindstoneBlockEntity.MechanicalInput(true, new ShaftPower(32, 128));
        machine.getItemHandler().setStackInSlot(1, new ItemStack(Items.GRAVEL, 64));
        tick(h, machine, 100);
        h.assertTrue(machine.getElapsed() == 0 && !machine.isOperating(), "Full output advanced mechanically");
        h.assertTrue(machine.getItemHandler().getStackInSlot(0).getCount() == 1, "Blocked mechanical input consumed");
        h.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
    public static void realDCInputIsSidedAndCannotMeetGrinderThreshold(GameTestHelper h) {
        h.setBlock(POS, MachineContent.GRINDSTONE.get());
        var machine = (GrindstoneBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(POS));
        machine.getItemHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        machine.getEnergyStorage().receiveEnergy(100, false);
        var source = POS.south();
        h.setBlock(source, MechanicalContent.DC_ENGINE.get().defaultBlockState().setValue(MechanicalBlock.FACING, Direction.NORTH));
        h.setBlock(source.below(), Blocks.REDSTONE_BLOCK);
        var engine = (MechanicalBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(source));
        for (int i = 0; i < 8; i++) MechanicalBlockEntity.serverTick(h.getLevel(), engine.getBlockPos(), engine.getBlockState(), engine);
        tick(h, machine, 1);
        h.assertTrue(machine.getMechanicalInput().connected(), "Rear shaft capability not detected");
        h.assertTrue(machine.getMechanicalInput().power().equals(new ShaftPower(256, 4)), "World shaft signal not read");
        h.assertTrue(machine.getElapsed() == 0, "DC engine bypassed grinder requirements");
        h.setBlock(source, engine.getBlockState().setValue(MechanicalBlock.FACING, Direction.EAST));
        tick(h, machine, 1);
        h.assertTrue(!machine.getMechanicalInput().connected(), "Wrong output side connected");
        h.assertTrue(machine.getElapsed() == 1 && machine.getEnergyStorage().getEnergyStored() == 99, "Disconnected machine failed to retain interim FE behavior");
        h.succeed();
    }
}
