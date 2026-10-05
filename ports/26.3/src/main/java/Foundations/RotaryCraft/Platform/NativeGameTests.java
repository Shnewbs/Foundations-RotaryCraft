package Foundations.RotaryCraft.Platform;

import Foundations.RotaryCraft.Mechanical.ShaftPower;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.transfer.energy.*;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** World-backed regressions for the native 26.x transfer implementation. */
public final class NativeGameTests {
  public static void register(RegisterGameTestsEvent event) {
    var environment = event.registerEnvironment(id("native_core"));
    register(event, environment, "partial_transfer", NativeGameTests::partialTransfer);
    register(event, environment, "rejected_commit_rolls_back", NativeGameTests::rollback);
    register(event, environment, "bent_cable", NativeGameTests::bentCable);
    register(event, environment, "shared_cable_budget", NativeGameTests::sharedBudget);
    register(event, environment, "battery_saved_charge", NativeGameTests::savedCharge);
    register(event, environment, "idle_battery", NativeGameTests::idleBattery);
    register(event, environment, "gear_conservation", NativeGameTests::gearConservation);
    register(event, environment, "generator_cable_processing", NativeGameTests::processing);
    register(event, environment, "menu_input_restrictions", NativeGameTests::menuRestrictions);
  }

  private static Identifier id(String name) {
    return Identifier.fromNamespaceAndPath("rotarycraft", name);
  }

  private static void register(
      RegisterGameTestsEvent event,
      net.minecraft.core.Holder<TestEnvironmentDefinition<?>> environment,
      String name,
      Consumer<GameTestHelper> body) {
    var data = new TestData<>(environment, id("power_network_test"), 160, 0, true);
    event.registerTest(
        id(name),
        new GameTestInstance(data) {
          @Override
          public void run(GameTestHelper helper) {
            body.accept(helper);
          }

          @Override
          public MapCodec<? extends GameTestInstance> codec() {
            return MapCodec.unit(this);
          }

          @Override
          protected MutableComponent typeDescription() {
            return Component.literal("RotaryCraft native regression");
          }
        });
  }

  private static PowerEntity place(GameTestHelper helper, BlockPos pos, String kind) {
    helper.setBlock(pos, PowerContent.ALL.get(kind).get());
    return (PowerEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
  }

  private static void partialTransfer(GameTestHelper helper) {
    var source = new SimpleEnergyHandler(1000);
    source.set(500);
    var target = new SimpleEnergyHandler(1000, 37, 0);
    helper.assertTrue(EnergyTransfer.transfer(source, target, 500) == 37, "Ignored receiving rate");
    helper.assertTrue(
        source.getAmountAsInt() == 463 && target.getAmountAsInt() == 37,
        "Partial transfer lost energy");
    helper.succeed();
  }

  private static void rollback(GameTestHelper helper) {
    var source = new SimpleEnergyHandler(1000);
    source.set(500);
    var stored = new SimpleEnergyHandler(1000);
    var target =
        new EnergyHandler() {
          int attempts;

          public long getAmountAsLong() {
            return stored.getAmountAsLong();
          }

          public long getCapacityAsLong() {
            return 1000;
          }

          public int insert(int amount, TransactionContext transaction) {
            return ++attempts == 1 ? stored.insert(amount, transaction) : 0;
          }

          public int extract(int amount, TransactionContext transaction) {
            return 0;
          }
        };
    helper.assertTrue(
        EnergyTransfer.transfer(source, target, 100) == 0, "Rejected commit reported transfer");
    helper.assertTrue(
        source.getAmountAsInt() == 500 && stored.getAmountAsInt() == 0,
        "Failed commit did not roll back both sides");
    helper.succeed();
  }

  private static void bentCable(GameTestHelper helper) {
    var first = place(helper, new BlockPos(1, 1, 1), "power_cable");
    var middle = place(helper, new BlockPos(2, 1, 1), "power_cable");
    var corner = place(helper, new BlockPos(2, 1, 2), "power_cable");
    var target = place(helper, new BlockPos(3, 1, 2), "grindstone");
    first.energy.set(100);
    CableNetwork.distribute(helper.getLevel(), first);
    helper.assertTrue(target.energy.getAmountAsInt() == 100, "Bend stalled power");
    helper.assertTrue(
        first.energy.getAmountAsInt()
                + middle.energy.getAmountAsInt()
                + corner.energy.getAmountAsInt()
            == 0,
        "Intermediate cable trapped power");
    helper.succeed();
  }

  private static void sharedBudget(GameTestHelper helper) {
    var first = place(helper, new BlockPos(1, 1, 1), "power_cable");
    var second = place(helper, new BlockPos(2, 1, 1), "power_cable");
    var targets =
        List.of(
                new BlockPos(1, 1, 0),
                new BlockPos(1, 2, 1),
                new BlockPos(1, 1, 2),
                new BlockPos(2, 2, 1),
                new BlockPos(3, 1, 1))
            .stream()
            .map(pos -> place(helper, pos, "grindstone"))
            .toList();
    first.energy.set(500);
    second.energy.set(500);
    CableNetwork.distribute(helper.getLevel(), first);
    CableNetwork.invalidate(helper.getLevel());
    CableNetwork.distribute(helper.getLevel(), second);
    int delivered = targets.stream().mapToInt(target -> target.energy.getAmountAsInt()).sum();
    helper.assertTrue(delivered == 500, "Network exceeded shared cable rate");
    helper.assertTrue(
        delivered + first.energy.getAmountAsInt() + second.energy.getAmountAsInt() == 1000,
        "Network created or lost power");
    helper.succeed();
  }

  private static void savedCharge(GameTestHelper helper) {
    var cell = place(helper, new BlockPos(1, 1, 1), "power_cell");
    cell.energy.set(73000);
    var tag = cell.saveWithoutMetadata(helper.getLevel().registryAccess());
    var restored = new PowerEntity(cell.getBlockPos(), cell.getBlockState());
    restored.loadWithComponents(
        TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    helper.assertTrue(restored.energy.getAmountAsInt() == 73000, "Restart lost stored charge");
    tag.putInt("StoredEnergy", Integer.MAX_VALUE);
    restored.loadWithComponents(
        TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    helper.assertTrue(
        restored.energy.getAmountAsInt() == 100000, "Corrupt saved charge exceeded capacity");
    helper.succeed();
  }

  private static void idleBattery(GameTestHelper helper) {
    var cell = place(helper, new BlockPos(1, 1, 1), "power_cell");
    var cable = place(helper, new BlockPos(2, 1, 1), "power_cable");
    cell.energy.set(1000);
    helper.runAfterDelay(
        20,
        () -> {
          helper.assertTrue(
              cell.energy.getAmountAsInt() == 1000 && cable.energy.getAmountAsInt() == 0,
              "Idle battery circulated through cable");
          helper.succeed();
        });
  }

  private static void gearConservation(GameTestHelper helper) {
    var power = new ShaftPower(128, 1024);
    var result = power.gear(4, true, Integer.MAX_VALUE, Integer.MAX_VALUE);
    helper.assertTrue(
        result.watts() == power.watts() && result.torque() == 4096 && result.omega() == 32,
        "Gear conversion changed mechanical power");
    helper.succeed();
  }

  private static void processing(GameTestHelper helper) {
    var generator = place(helper, new BlockPos(1, 1, 1), "power_generator");
    place(helper, new BlockPos(2, 1, 1), "power_cable");
    var grinder = place(helper, new BlockPos(3, 1, 1), "grindstone");
    generator.setItem(
        0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));
    grinder.setItem(
        0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE));
    helper.runAfterDelay(
        115,
        () -> {
          helper.assertTrue(
              grinder.getItem(1).is(net.minecraft.world.item.Items.GRAVEL),
              "Fuel generator and cable failed to complete recipe");
          helper.assertTrue(
              grinder.getItem(0).isEmpty() && grinder.getItem(1).getCount() == 1,
              "Processing duplicated input or output");
          helper.succeed();
        });
  }

  private static void menuRestrictions(GameTestHelper helper) {
    var grinder = place(helper, new BlockPos(1, 1, 1), "grindstone");
    var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
    var menu = grinder.createMenu(1, player.getInventory());
    var item = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE);
    helper.assertTrue(
        menu.getSlot(0).mayPlace(item) && !menu.getSlot(1).mayPlace(item),
        "GUI permits placement into output slot");
    helper.assertTrue(!menu.getSlot(2).mayPlace(item), "Unused processor slots accept items");
    helper.succeed();
  }

  private NativeGameTests() {}
}
