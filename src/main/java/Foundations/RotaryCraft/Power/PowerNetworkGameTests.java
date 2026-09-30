package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class PowerNetworkGameTests {
    private PowerNetworkGameTests() {
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void fuelGeneratorChargesAdjacentCable(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos cablePos = generatorPos.east();
        helper.setBlock(generatorPos, PowerContent.POWER_GENERATOR.get());
        helper.setBlock(
                cablePos,
                PowerContent.POWER_CABLE.get().defaultBlockState().setValue(
                        PowerNodeBlock.propertyFor(Direction.WEST),
                        true
                )
        );

        PowerGeneratorBlockEntity generator = (PowerGeneratorBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(cablePos));
        helper.assertTrue(generator != null, "Generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");
        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(cablePos))
                        .getValue(PowerNodeBlock.propertyFor(Direction.WEST)),
                "Cable did not connect to the adjacent generator"
        );
        generator.getFuelHandler().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL));

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    cable.getEnergyStorage().getEnergyStored() > 0,
                    "Fuel-powered generator did not transfer energy into the adjacent cable"
            );
            helper.assertTrue(
                    PowerNodeBlock.signalStrength(
                            cable.getEnergyStorage().getEnergyStored(),
                            cable.getEnergyStorage().getMaxEnergyStored()
                    ) > 0,
                    "Charged cable did not produce a comparator signal"
            );
        });
    }
}
