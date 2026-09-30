package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities;
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
    public static void powerNetworkRecipesAreRegistered(GameTestHelper helper) {
        String[] recipeIds = {
                "power_cable",
                "power_cell",
                "power_switch",
                "power_generator",
                "solar_generator",
                "wind_generator",
                "hydro_generator",
                "steam_generator",
                "geothermal_generator"
        };

        for (String recipeId : recipeIds) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("rotarycraft", recipeId);
            var recipe = helper.getLevel().getRecipeManager().byKey(id);
            helper.assertTrue(recipe.isPresent(), "Missing power-network recipe " + id);
            helper.assertTrue(
                    BuiltInRegistries.ITEM.getKey(
                            recipe.get().value().getResultItem(helper.getLevel().registryAccess()).getItem()
                    ).equals(id),
                    "Power-network recipe " + id + " has an unexpected result"
            );
        }
        helper.succeed();
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

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void steamGeneratorConsumesWaterAndFuelAndChargesCable(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos cablePos = generatorPos.east();
        helper.setBlock(generatorPos, PowerContent.STEAM_GENERATOR.get());
        helper.setBlock(
                cablePos,
                PowerContent.POWER_CABLE.get().defaultBlockState().setValue(
                        PowerNodeBlock.propertyFor(Direction.WEST),
                        true
                )
        );

        SteamGeneratorBlockEntity generator = (SteamGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(cablePos));
        helper.assertTrue(generator != null, "Steam generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");
        generator.getItemHandler().setStackInSlot(
                SteamGeneratorBlockEntity.WATER_SLOT,
                new net.minecraft.world.item.ItemStack(Items.WATER_BUCKET)
        );
        generator.getItemHandler().setStackInSlot(
                SteamGeneratorBlockEntity.FUEL_SLOT,
                new net.minecraft.world.item.ItemStack(Items.COAL)
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    cable.getEnergyStorage().getEnergyStored() > 0,
                    "Steam generator did not transfer energy into the adjacent cable"
            );
            helper.assertTrue(
                    generator.getItemHandler().getStackInSlot(SteamGeneratorBlockEntity.WATER_SLOT).is(Items.BUCKET),
                    "Steam generator did not return an empty bucket after consuming water"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(generatorPos))
                            .getValue(SteamGeneratorBlock.LIT),
                    "Steam generator did not enter its generating state"
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void steamGeneratorDoesNotGenerateWithoutInputs(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        helper.setBlock(generatorPos, PowerContent.STEAM_GENERATOR.get());
        SteamGeneratorBlockEntity generator = (SteamGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        helper.assertTrue(generator != null, "Steam generator block entity was not created");

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(
                    generator.getEnergyStorage().getEnergyStored() == 0,
                    "Steam generator produced energy without water or fuel"
            );
            helper.assertTrue(
                    !helper.getLevel().getBlockState(helper.absolutePos(generatorPos))
                            .getValue(SteamGeneratorBlock.LIT),
                    "Steam generator showed its generating state without inputs"
            );
            helper.succeed();
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void steamGeneratorBuffersEnergyWhenNetworkIsBlocked(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos cellPos = generatorPos.east();
        helper.setBlock(generatorPos, PowerContent.STEAM_GENERATOR.get());
        helper.setBlock(
                cellPos,
                PowerContent.POWER_CELL.get().defaultBlockState().setValue(
                        PowerNodeBlock.propertyFor(Direction.WEST),
                        true
                )
        );

        SteamGeneratorBlockEntity generator = (SteamGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cell = (PowerNodeBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(cellPos));
        helper.assertTrue(generator != null, "Steam generator block entity was not created");
        helper.assertTrue(cell != null, "Power cell block entity was not created");
        IEnergyStorage cellEnergy = cell.getEnergyStorage();
        while (cellEnergy.getEnergyStored() < cellEnergy.getMaxEnergyStored()) {
            cellEnergy.receiveEnergy(cellEnergy.getMaxEnergyStored(), false);
        }
        generator.getItemHandler().setStackInSlot(
                SteamGeneratorBlockEntity.WATER_SLOT,
                new net.minecraft.world.item.ItemStack(Items.WATER_BUCKET)
        );
        generator.getItemHandler().setStackInSlot(
                SteamGeneratorBlockEntity.FUEL_SLOT,
                new net.minecraft.world.item.ItemStack(Items.COAL)
        );

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    cellEnergy.getEnergyStored() == cellEnergy.getMaxEnergyStored(),
                    "Blocked network test cell unexpectedly accepted energy"
            );
            helper.assertTrue(
                    generator.getEnergyStorage().getEnergyStored() > 0,
                    "Steam generator did not buffer energy when the adjacent cell was full"
            );
            helper.assertTrue(
                    generator.getEnergyStorage().getEnergyStored() <= SteamGeneratorBlockEntity.ENERGY_CAPACITY,
                    "Steam generator exceeded its bounded energy capacity"
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void enabledPowerSwitchPassesGeneratorEnergyToCable(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos switchPos = generatorPos.east();
        BlockPos cablePos = switchPos.east();
        helper.setBlock(generatorPos, PowerContent.POWER_GENERATOR.get());
        helper.setBlock(switchPos, PowerContent.POWER_SWITCH.get());
        helper.setBlock(cablePos, PowerContent.POWER_CABLE.get());

        PowerGeneratorBlockEntity generator = (PowerGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(cablePos));
        helper.assertTrue(generator != null, "Generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");
        helper.assertTrue(
                helper.getLevel().getBlockState(helper.absolutePos(switchPos))
                        .getValue(PowerSwitchBlock.ENABLED),
                "Unpowered switch should be enabled"
        );
        generator.getFuelHandler().setStackInSlot(0, new net.minecraft.world.item.ItemStack(Items.COAL));

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    cable.getEnergyStorage().getEnergyStored() > 0,
                    "Enabled switch did not pass generator energy into the cable"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(cablePos))
                            .getValue(PowerNodeBlock.propertyFor(Direction.WEST)),
                    "Cable did not show its connection to the enabled switch"
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void poweredPowerSwitchBlocksGeneratorEnergy(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos switchPos = generatorPos.east();
        BlockPos cablePos = switchPos.east();
        BlockPos signalPos = switchPos.north();
        helper.setBlock(signalPos, Blocks.REDSTONE_BLOCK);
        helper.setBlock(generatorPos, PowerContent.POWER_GENERATOR.get());
        helper.setBlock(switchPos, PowerContent.POWER_SWITCH.get());
        helper.setBlock(cablePos, PowerContent.POWER_CABLE.get());

        PowerGeneratorBlockEntity generator = (PowerGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(cablePos));
        helper.assertTrue(generator != null, "Generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");

        helper.runAfterDelay(5, () -> {
            BlockPos absoluteSwitchPos = helper.absolutePos(switchPos);
            helper.assertTrue(
                    !helper.getLevel().getBlockState(absoluteSwitchPos).getValue(PowerSwitchBlock.ENABLED),
                    "Redstone-powered switch did not disable"
            );
            helper.assertTrue(
                    helper.getLevel().getCapability(
                            Capabilities.EnergyStorage.BLOCK,
                            absoluteSwitchPos,
                            Direction.WEST
                    ) == null,
                    "Disabled switch still exposed an energy capability"
            );
            generator.getFuelHandler().setStackInSlot(0, new net.minecraft.world.item.ItemStack(Items.COAL));

            helper.runAfterDelay(20, () -> {
                helper.assertTrue(
                        cable.getEnergyStorage().getEnergyStored() == 0,
                        "Powered switch allowed generator energy into the cable"
                );
                helper.assertTrue(
                        !helper.getLevel().getBlockState(helper.absolutePos(cablePos))
                                .getValue(PowerNodeBlock.propertyFor(Direction.WEST)),
                        "Cable still showed a connection to the disabled switch"
                );
                helper.succeed();
            });
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void powerSwitchSignalTransitionRestoresEnergyFlow(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos switchPos = generatorPos.east();
        BlockPos cablePos = switchPos.east();
        BlockPos signalPos = switchPos.north();
        helper.setBlock(generatorPos, PowerContent.POWER_GENERATOR.get());
        helper.setBlock(switchPos, PowerContent.POWER_SWITCH.get());
        helper.setBlock(cablePos, PowerContent.POWER_CABLE.get());

        PowerGeneratorBlockEntity generator = (PowerGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(cablePos));
        helper.assertTrue(generator != null, "Generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");
        generator.getFuelHandler().setStackInSlot(0, new net.minecraft.world.item.ItemStack(Items.COAL));

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(
                    cable.getEnergyStorage().getEnergyStored() > 0,
                    "Enabled switch did not initially pass generator energy"
            );
            helper.setBlock(signalPos, Blocks.REDSTONE_BLOCK);
            helper.runAfterDelay(5, () -> {
                BlockPos absoluteSwitchPos = helper.absolutePos(switchPos);
                helper.assertTrue(
                        !helper.getLevel().getBlockState(absoluteSwitchPos).getValue(PowerSwitchBlock.ENABLED),
                        "Redstone signal did not disable the switch"
                );
                helper.assertTrue(
                        helper.getLevel().getCapability(
                                Capabilities.EnergyStorage.BLOCK,
                                absoluteSwitchPos,
                                Direction.WEST
                        ) == null,
                        "Disabled switch capability was not invalidated"
                );
                int storedBeforeReconnect = cable.getEnergyStorage().getEnergyStored();
                helper.setBlock(signalPos, Blocks.AIR);
                helper.runAfterDelay(5, () -> {
                    helper.assertTrue(
                            helper.getLevel().getBlockState(absoluteSwitchPos).getValue(PowerSwitchBlock.ENABLED),
                            "Removing the redstone signal did not re-enable the switch"
                    );
                    helper.assertTrue(
                            helper.getLevel().getCapability(
                                    Capabilities.EnergyStorage.BLOCK,
                                    absoluteSwitchPos,
                                    Direction.WEST
                            ) != null,
                            "Enabled switch did not restore its energy capability"
                    );
                    helper.assertTrue(
                            helper.getLevel().getBlockState(helper.absolutePos(cablePos))
                                    .getValue(PowerNodeBlock.propertyFor(Direction.WEST)),
                            "Cable did not reconnect visually after the switch was enabled"
                    );
                    helper.succeedWhen(() -> helper.assertTrue(
                            cable.getEnergyStorage().getEnergyStored() > storedBeforeReconnect,
                            "Generator energy did not resume flowing after switch re-enable"
                    ));
                });
            });
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

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void geothermalGeneratorConsumesLavaBucketAndChargesCable(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        BlockPos cablePos = generatorPos.east();
        helper.setBlock(generatorPos, PowerContent.GEOTHERMAL_GENERATOR.get());
        helper.setBlock(
                cablePos,
                PowerContent.POWER_CABLE.get().defaultBlockState().setValue(
                        PowerNodeBlock.propertyFor(Direction.WEST),
                        true
                )
        );

        GeothermalGeneratorBlockEntity generator = (GeothermalGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        PowerNodeBlockEntity cable = (PowerNodeBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(cablePos));
        helper.assertTrue(generator != null, "Geothermal generator block entity was not created");
        helper.assertTrue(cable != null, "Cable block entity was not created");
        generator.getItemHandler().setStackInSlot(0, new net.minecraft.world.item.ItemStack(Items.LAVA_BUCKET));

        helper.succeedWhen(() -> {
            helper.assertTrue(
                    cable.getEnergyStorage().getEnergyStored() > 0,
                    "Geothermal generator did not transfer energy into the adjacent cable"
            );
            helper.assertTrue(
                    generator.getItemHandler().getStackInSlot(0).is(Items.BUCKET),
                    "Geothermal generator did not return an empty bucket after consuming lava"
            );
            helper.assertTrue(
                    helper.getLevel().getBlockState(helper.absolutePos(generatorPos))
                            .getValue(GeothermalGeneratorBlock.LIT),
                    "Geothermal generator did not enter its generating state"
            );
        });
    }

    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 100)
    public static void geothermalGeneratorDoesNotGenerateWithoutLava(GameTestHelper helper) {
        BlockPos generatorPos = new BlockPos(1, 1, 1);
        helper.setBlock(generatorPos, PowerContent.GEOTHERMAL_GENERATOR.get());
        GeothermalGeneratorBlockEntity generator = (GeothermalGeneratorBlockEntity) helper.getLevel()
                .getBlockEntity(helper.absolutePos(generatorPos));
        helper.assertTrue(generator != null, "Geothermal generator block entity was not created");

        helper.runAfterDelay(10, () -> {
            helper.assertTrue(
                    generator.getEnergyStorage().getEnergyStored() == 0,
                    "Geothermal generator produced energy without a lava bucket"
            );
            helper.assertTrue(
                    !helper.getLevel().getBlockState(helper.absolutePos(generatorPos))
                            .getValue(GeothermalGeneratorBlock.LIT),
                    "Geothermal generator showed its generating state without lava"
            );
            helper.succeed();
        });
    }
}
