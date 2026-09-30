package Foundations.RotaryCraft.Power;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import Foundations.RotaryCraft.RotaryCraftNeoForge;

public final class PowerContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RotaryCraftNeoForge.MOD_ID);

    public static final DeferredBlock<PowerNodeBlock> POWER_CABLE = BLOCKS.register(
            "power_cable",
            () -> new PowerNodeBlock(BlockBehaviour.Properties.of().strength(1.5F).noOcclusion())
    );
    public static final DeferredBlock<PowerNodeBlock> POWER_CELL = BLOCKS.register(
            "power_cell",
            () -> new PowerNodeBlock(BlockBehaviour.Properties.of().strength(2.5F).noOcclusion())
    );
    public static final DeferredBlock<PowerSwitchBlock> POWER_SWITCH = BLOCKS.register(
            "power_switch",
            () -> new PowerSwitchBlock(BlockBehaviour.Properties.of().strength(2.5F).requiresCorrectToolForDrops())
    );
    public static final DeferredBlock<PowerGeneratorBlock> POWER_GENERATOR = BLOCKS.register(
            "power_generator",
            () -> new PowerGeneratorBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops())
    );
    public static final DeferredBlock<SolarGeneratorBlock> SOLAR_GENERATOR = BLOCKS.register(
            "solar_generator",
            () -> new SolarGeneratorBlock(BlockBehaviour.Properties.of().strength(2.5F).requiresCorrectToolForDrops())
    );
    public static final DeferredBlock<WindGeneratorBlock> WIND_GENERATOR = BLOCKS.register(
            "wind_generator",
            () -> new WindGeneratorBlock(BlockBehaviour.Properties.of().strength(2.5F).requiresCorrectToolForDrops())
    );
    public static final DeferredBlock<HydroGeneratorBlock> HYDRO_GENERATOR = BLOCKS.register(
            "hydro_generator",
            () -> new HydroGeneratorBlock(BlockBehaviour.Properties.of().strength(2.5F).requiresCorrectToolForDrops())
    );
    public static final DeferredBlock<SteamGeneratorBlock> STEAM_GENERATOR = BLOCKS.register(
            "steam_generator",
            () -> new SteamGeneratorBlock(BlockBehaviour.Properties.of().strength(3.5F).requiresCorrectToolForDrops())
    );
    private static final DeferredItem<BlockItem> POWER_CABLE_ITEM = ITEMS.registerSimpleBlockItem("power_cable", POWER_CABLE);
    private static final DeferredItem<BlockItem> POWER_CELL_ITEM = ITEMS.registerSimpleBlockItem("power_cell", POWER_CELL);
    private static final DeferredItem<BlockItem> POWER_SWITCH_ITEM = ITEMS.registerSimpleBlockItem("power_switch", POWER_SWITCH);
    private static final DeferredItem<BlockItem> POWER_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("power_generator", POWER_GENERATOR);
    private static final DeferredItem<BlockItem> SOLAR_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("solar_generator", SOLAR_GENERATOR);
    private static final DeferredItem<BlockItem> WIND_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("wind_generator", WIND_GENERATOR);
    private static final DeferredItem<BlockItem> HYDRO_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("hydro_generator", HYDRO_GENERATOR);
    private static final DeferredItem<BlockItem> STEAM_GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("steam_generator", STEAM_GENERATOR);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerNodeBlockEntity>> POWER_NODE =
            BLOCK_ENTITY_TYPES.register(
                    "power_node",
                    () -> BlockEntityType.Builder.of(
                            PowerNodeBlockEntity::new,
                            POWER_CABLE.get(),
                            POWER_CELL.get()
                    ).build(null)
            );
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerGeneratorBlockEntity>> POWER_GENERATOR_ENTITY =
                    BLOCK_ENTITY_TYPES.register(
                            "power_generator",
                            () -> BlockEntityType.Builder.of(PowerGeneratorBlockEntity::new, POWER_GENERATOR.get()).build(null)
                    );
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerSwitchBlockEntity>> POWER_SWITCH_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "power_switch",
                    () -> BlockEntityType.Builder.of(PowerSwitchBlockEntity::new, POWER_SWITCH.get()).build(null)
            );
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarGeneratorBlockEntity>> SOLAR_GENERATOR_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "solar_generator",
                    () -> BlockEntityType.Builder.of(SolarGeneratorBlockEntity::new, SOLAR_GENERATOR.get()).build(null)
            );
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindGeneratorBlockEntity>> WIND_GENERATOR_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "wind_generator",
                    () -> BlockEntityType.Builder.of(WindGeneratorBlockEntity::new, WIND_GENERATOR.get()).build(null)
            );
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HydroGeneratorBlockEntity>> HYDRO_GENERATOR_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "hydro_generator",
                    () -> BlockEntityType.Builder.of(HydroGeneratorBlockEntity::new, HYDRO_GENERATOR.get()).build(null)
            );
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SteamGeneratorBlockEntity>> STEAM_GENERATOR_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "steam_generator",
                    () -> BlockEntityType.Builder.of(SteamGeneratorBlockEntity::new, STEAM_GENERATOR.get()).build(null)
            );

    private PowerContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(PowerContent::registerCapabilities);
        modEventBus.addListener(PowerContent::addToCreativeTab);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                POWER_NODE.get(),
                (node, side) -> node.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                POWER_SWITCH_ENTITY.get(),
                (powerSwitch, side) -> powerSwitch.isEnabled() ? powerSwitch.getEnergyStorage(side) : null
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                POWER_GENERATOR_ENTITY.get(),
                (generator, side) -> generator.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                SOLAR_GENERATOR_ENTITY.get(),
                (generator, side) -> generator.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                WIND_GENERATOR_ENTITY.get(),
                (generator, side) -> generator.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                HYDRO_GENERATOR_ENTITY.get(),
                (generator, side) -> generator.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                STEAM_GENERATOR_ENTITY.get(),
                (generator, side) -> generator.getEnergyStorage()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                POWER_GENERATOR_ENTITY.get(),
                (generator, side) -> generator.getFuelHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                STEAM_GENERATOR_ENTITY.get(),
                (generator, side) -> generator.getItemHandler()
        );
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(POWER_CABLE_ITEM.get());
            event.accept(POWER_CELL_ITEM.get());
            event.accept(POWER_SWITCH_ITEM.get());
            event.accept(POWER_GENERATOR_ITEM.get());
            event.accept(SOLAR_GENERATOR_ITEM.get());
            event.accept(WIND_GENERATOR_ITEM.get());
            event.accept(HYDRO_GENERATOR_ITEM.get());
            event.accept(STEAM_GENERATOR_ITEM.get());
        }
    }
}
