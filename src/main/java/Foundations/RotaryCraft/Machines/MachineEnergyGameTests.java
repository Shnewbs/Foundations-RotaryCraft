package Foundations.RotaryCraft.Machines;

import Foundations.RotaryCraft.Power.MachineEnergyStorage;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.IntTag;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class MachineEnergyGameTests {
  @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
  public static void machineEnergyRestoresFullChargeThroughRealBlockEntities(
      GameTestHelper helper) {
    var pos = new BlockPos(1, 1, 1);
    for (var block :
        List.of(
            MachineContent.FAN.get(),
            MachineContent.MOB_HARVESTER.get(),
            MachineContent.DEFOLIATOR.get(),
            MachineContent.SORTING.get(),
            MachineContent.ITEM_CANNON.get(),
            MachineContent.SMOKE_DETECTOR.get(),
            MachineContent.PLAYER_DETECTOR.get(),
            MachineContent.BLOWER.get(),
            MachineContent.WINDER.get(),
            MachineContent.ITEM_REFRESHER.get())) {
      helper.setBlock(pos, block);
      var absolute = helper.absolutePos(pos);
      var entity = helper.getLevel().getBlockEntity(absolute);
      var capability =
          helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, absolute, Direction.UP);
      helper.assertTrue(
          capability instanceof MachineEnergyStorage, "Missing machine energy port: " + block);
      var buffer = (MachineEnergyStorage) capability;
      for (int i = 0; i < 100; i++) buffer.receiveEnergy(100, false);
      var saved = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
      var restored = entity.getType().create(absolute, entity.getBlockState());
      restored.loadWithComponents(saved, helper.getLevel().registryAccess());
      var restoredTag = restored.saveWithoutMetadata(helper.getLevel().registryAccess());
      helper.assertTrue(
          restoredTag.getInt("energy") == Math.min(10000, buffer.getMaxEnergyStored()),
          "Restart lost machine charge: " + block);
      saved.putInt("energy", Integer.MAX_VALUE);
      restored.loadWithComponents(saved, helper.getLevel().registryAccess());
      helper.assertTrue(
          restored.saveWithoutMetadata(helper.getLevel().registryAccess()).getInt("energy")
              == buffer.getMaxEnergyStored(),
          "Corrupt charge bypassed capacity: " + block);
    }
    helper.succeed();
  }

  @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
  public static void poweredFanSpendsEnergyWithoutExposingOutput(GameTestHelper helper) {
    var pos = new BlockPos(1, 1, 1);
    helper.setBlock(pos, MachineContent.FAN.get());
    var fan = (FanBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    fan.getEnergyStorage().receiveEnergy(160, false);
    FanBlockEntity.serverTick(helper.getLevel(), fan.getBlockPos(), fan.getBlockState(), fan);
    helper.assertTrue(
        fan.getEnergyStorage().getEnergyStored() == 144, "Fan did not consume operating energy");
    helper.assertTrue(
        fan.getEnergyStorage().extractEnergy(160, false) == 0,
        "Consumer exposed an energy output port");
    for (int i = 0; i < 10; i++)
      FanBlockEntity.serverTick(helper.getLevel(), fan.getBlockPos(), fan.getBlockState(), fan);
    helper.assertTrue(
        fan.getEnergyStorage().getEnergyStored() == 0
            && !fan.getBlockState().getValue(FanBlock.ACTIVE),
        "Fan continued operating without power");
    helper.succeed();
  }

  @GameTest(template = "power_network_test", templateNamespace = "rotarycraft")
  public static void internalConsumptionIsExactAndSimulationIsReadOnly(GameTestHelper helper) {
    int[] changes = {0};
    var energy = new MachineEnergyStorage(1000, 100, 0, () -> changes[0]++);
    energy.restore(IntTag.valueOf(300));
    helper.assertTrue(
        energy.receiveEnergy(100, true) == 100
            && energy.getEnergyStored() == 300
            && changes[0] == 0,
        "Simulation mutated consumer buffer");
    helper.assertTrue(
        !energy.consume(301) && energy.getEnergyStored() == 300,
        "Insufficient power was partially consumed");
    helper.assertTrue(
        energy.consume(300) && energy.getEnergyStored() == 0 && changes[0] == 1,
        "Internal work failed to spend exact energy");
    helper.succeed();
  }

  private MachineEnergyGameTests() {}
}
