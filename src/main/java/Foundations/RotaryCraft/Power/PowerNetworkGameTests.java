package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
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
        generator.getFuelHandler().setStackInSlot(0, new net.minecraft.world.item.ItemStack(Items.COAL));

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

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void solarGeneratorChargesAdjacentCableInDaylight(GameTestHelper helper) {
        helper.getLevel().setDayTime(6_000);
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos cablePos = generatorPos.east();
        helper.setBlock(generatorPos, PowerContent.SOLAR_GENERATOR.get());
        helper.setBlock(
                cablePos,
                PowerContent.POWER_CABLE.get().defaultBlockState().setValue(
                        PowerNodeBlock.propertyFor(Direction.WEST),
                        true
                )
        );

        SolarGeneratorBlockEntity generator = (SolarGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(cablePos));
        helper.assertTrue(generator != null, "Solar generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");
        helper.assertTrue(
                SolarGeneratorBlockEntity.hasSunlight(helper.getLevel(), helper.absolutePos(generatorPos)),
                "Test solar generator must have daylight and an unobstructed sky"
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    cable.getEnergyStorage().getEnergyStored() > 0,
                    "Solar generator did not transfer energy into the adjacent cable"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(generatorPos))
                            .getValue(SolarGeneratorBlock.LIT),
                    "Solar generator did not enter its generating state"
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void solarGeneratorDoesNotGenerateWithoutSkyAccess(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        helper.setBlock(generatorPos.above(), Blocks.STONE);
        helper.runAfterDelay(5, () -> {
            BlockPos absolutePos = helper.absolutePos(generatorPos);
            helper.assertTrue(
                    !SolarGeneratorBlockEntity.hasSunlight(helper.getLevel(), absolutePos),
                    "Test solar generator must be sheltered from the sky"
            );
            helper.setBlock(generatorPos, PowerContent.SOLAR_GENERATOR.get());
            SolarGeneratorBlockEntity generator = (SolarGeneratorBlockEntity) helper.getLevel()
                    .getBlockEntity(absolutePos);
            helper.assertTrue(generator != null, "Solar generator block entity was not created");
            helper.runAfterDelay(10, () -> {
                helper.assertTrue(
                        generator.getEnergyStorage().getEnergyStored() == 0,
                        "Solar generator produced energy without sky access"
                );
                helper.assertTrue(
                        !helper.getLevel().getBlockState(absolutePos).getValue(SolarGeneratorBlock.LIT),
                        "Solar generator showed its generating state without sky access"
                );
                helper.succeed();
            });
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void windGeneratorChargesAdjacentCableAtElevation(GameTestHelper helper) {
        BlockPos generatorPos = elevatedPosition(helper);
        BlockPos cablePos = generatorPos.east();
        helper.getLevel().setBlock(generatorPos, PowerContent.WIND_GENERATOR.get().defaultBlockState(), 3);
        helper.getLevel().setBlock(
                cablePos,
                PowerContent.POWER_CABLE.get().defaultBlockState().setValue(
                        PowerNodeBlock.propertyFor(Direction.WEST),
                        true
                ),
                3
        );

        WindGeneratorBlockEntity generator = (WindGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(generatorPos);
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel().getBlockEntity(cablePos);
        helper.assertTrue(generator != null, "Wind generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");
        helper.assertTrue(
                WindGeneratorBlockEntity.hasWind(helper.getLevel(), generatorPos),
                "Test wind generator must be elevated with an unobstructed sky"
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    cable.getEnergyStorage().getEnergyStored() > 0,
                    "Wind generator did not transfer energy into the adjacent cable"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(generatorPos).getValue(WindGeneratorBlock.LIT),
                    "Wind generator did not enter its generating state"
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void windGeneratorDoesNotGenerateBelowElevation(GameTestHelper helper) {
        BlockPos generatorPos = lowPosition(helper);
        helper.getLevel().setBlock(generatorPos, PowerContent.WIND_GENERATOR.get().defaultBlockState(), 3);
        WindGeneratorBlockEntity generator = (WindGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(generatorPos);
        helper.assertTrue(generator != null, "Wind generator block entity was not created");
        helper.assertTrue(
                helper.getLevel().canSeeSky(generatorPos.above()),
                "Test low-elevation wind generator must have an open sky"
        );
        helper.assertTrue(
                !WindGeneratorBlockEntity.hasWind(helper.getLevel(), generatorPos),
                "Wind generator must reject an open-air location below the elevation threshold"
        );

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(
                    generator.getEnergyStorage().getEnergyStored() == 0,
                    "Wind generator produced energy below the elevation threshold"
            );
            helper.assertTrue(
                    !helper.getLevel().getBlockState(generatorPos).getValue(WindGeneratorBlock.LIT),
                    "Wind generator showed its generating state below the elevation threshold"
            );
            helper.succeed();
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void windGeneratorDoesNotGenerateUnderRoof(GameTestHelper helper) {
        BlockPos generatorPos = elevatedPosition(helper);
        helper.getLevel().setBlock(generatorPos.above(), Blocks.STONE.defaultBlockState(), 3);
        helper.getLevel().setBlock(generatorPos, PowerContent.WIND_GENERATOR.get().defaultBlockState(), 3);
        WindGeneratorBlockEntity generator = (WindGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(generatorPos);
        helper.assertTrue(generator != null, "Wind generator block entity was not created");
        helper.assertTrue(
                helper.getLevel().getBlockState(generatorPos.above()).is(Blocks.STONE),
                "Test wind generator must have a solid roof"
        );
        helper.assertTrue(
                !WindGeneratorBlockEntity.hasWind(helper.getLevel(), generatorPos),
                "Test wind generator must not have open sky"
        );

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(
                    generator.getEnergyStorage().getEnergyStored() == 0,
                    "Wind generator produced energy without open sky"
            );
            helper.assertTrue(
                    !helper.getLevel().getBlockState(generatorPos).getValue(WindGeneratorBlock.LIT),
                    "Wind generator showed its generating state without open sky"
            );
            helper.succeed();
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void hydroGeneratorChargesAdjacentCableWhenTouchingWater(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos cablePos = generatorPos.east();
        helper.setBlock(generatorPos.above(), Blocks.WATER);
        helper.setBlock(generatorPos.north(), Blocks.WATER);
        helper.setBlock(
                cablePos,
                PowerContent.POWER_CABLE.get().defaultBlockState().setValue(
                        PowerNodeBlock.propertyFor(Direction.WEST),
                        true
                )
        );
        helper.setBlock(generatorPos, PowerContent.HYDRO_GENERATOR.get());

        HydroGeneratorBlockEntity generator = (HydroGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(cablePos));
        helper.assertTrue(generator != null, "Hydro generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");
        helper.assertTrue(
                HydroGeneratorBlockEntity.countAdjacentWater(
                        helper.getLevel(),
                        helper.absolutePos(generatorPos)
                ) == 2,
                "Hydro generator must count its two face-adjacent water blocks"
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    cable.getEnergyStorage().getEnergyStored() > 0,
                    "Hydro generator did not transfer energy into the adjacent cable"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(generatorPos))
                            .getValue(HydroGeneratorBlock.LIT),
                    "Hydro generator did not enter its generating state"
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void hydroGeneratorDoesNotGenerateWhenDry(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        helper.setBlock(generatorPos, PowerContent.HYDRO_GENERATOR.get());
        HydroGeneratorBlockEntity generator = (HydroGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        helper.assertTrue(generator != null, "Hydro generator block entity was not created");

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(
                    HydroGeneratorBlockEntity.countAdjacentWater(
                            helper.getLevel(),
                            helper.absolutePos(generatorPos)
                    ) == 0,
                    "Dry hydro generator test must have no adjacent water"
            );
            helper.assertTrue(
                    generator.getEnergyStorage().getEnergyStored() == 0,
                    "Hydro generator produced energy without adjacent water"
            );
            helper.assertTrue(
                    !helper.getLevel().getBlockState(helper.absolutePos(generatorPos))
                            .getValue(HydroGeneratorBlock.LIT),
                    "Hydro generator showed its generating state while dry"
            );
            helper.succeed();
        });
    }

    private static BlockPos elevatedPosition(GameTestHelper helper) {
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int y = Math.max(128, helper.getLevel().getSeaLevel() + WindGeneratorBlockEntity.MIN_ELEVATION_ABOVE_SEA_LEVEL);
        return new BlockPos(templateOrigin.getX() + 1, y, templateOrigin.getZ() + 1);
    }

    private static BlockPos lowPosition(GameTestHelper helper) {
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        int y = Math.max(
                helper.getLevel().getMinBuildHeight() + 1,
                helper.getLevel().getSeaLevel() + WindGeneratorBlockEntity.MIN_ELEVATION_ABOVE_SEA_LEVEL - 1
        );
        return new BlockPos(templateOrigin.getX() + 1, y, templateOrigin.getZ() + 1);
    }
}
