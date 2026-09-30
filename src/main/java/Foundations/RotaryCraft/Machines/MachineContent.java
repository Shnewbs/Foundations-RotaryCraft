package Foundations.RotaryCraft.Machines;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockBehaviour;

import Foundations.RotaryCraft.RotaryCraftNeoForge;

public class MachineContent {
    
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RotaryCraftNeoForge.MOD_ID);

    // Grindstone
    public static final DeferredBlock<GrindstoneBlock> GRINDSTONE = BLOCKS.register("grindstone", 
        () -> new GrindstoneBlock(BlockBehaviour.Properties.of().strength(3.0f, 10.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> GRINDSTONE_ITEM = ITEMS.registerSimpleBlockItem("grindstone", GRINDSTONE);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrindstoneBlockEntity>> GRINDSTONE_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("grindstone", () -> 
            BlockEntityType.Builder.of(GrindstoneBlockEntity::new, GRINDSTONE.get()).build(null));

    // Winder
    public static final DeferredBlock<WinderBlock> WINDER = BLOCKS.register("winder", 
        () -> new WinderBlock(BlockBehaviour.Properties.of().strength(2.5f, 8.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> WINDER_ITEM = ITEMS.registerSimpleBlockItem("winder", WINDER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WinderBlockEntity>> WINDER_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("winder", () -> 
            BlockEntityType.Builder.of(WinderBlockEntity::new, WINDER.get()).build(null));

    // Blower
    public static final DeferredBlock<BlowerBlock> BLOWER = BLOCKS.register("blower", 
        () -> new BlowerBlock(BlockBehaviour.Properties.of().strength(2.0f, 6.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> BLOWER_ITEM = ITEMS.registerSimpleBlockItem("blower", BLOWER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BlowerBlockEntity>> BLOWER_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("blower", () -> 
            BlockEntityType.Builder.of(BlowerBlockEntity::new, BLOWER.get()).build(null));

    // DecoTank
    public static final DeferredBlock<DecoTankBlock> DECO_TANK = BLOCKS.register("deco_tank", 
        () -> new DecoTankBlock(BlockBehaviour.Properties.of().strength(2.0f, 5.0f).noOcclusion()));
    public static final DeferredItem<BlockItem> DECO_TANK_ITEM = ITEMS.registerSimpleBlockItem("deco_tank", DECO_TANK);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DecoTankBlockEntity>> DECO_TANK_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("deco_tank", () -> 
            BlockEntityType.Builder.of(DecoTankBlockEntity::new, DECO_TANK.get()).build(null));

    // Player Detector
    public static final DeferredBlock<PlayerDetectorBlock> PLAYER_DETECTOR = BLOCKS.register("player_detector",
        () -> new PlayerDetectorBlock(BlockBehaviour.Properties.of().strength(2.5f, 8.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> PLAYER_DETECTOR_ITEM =
        ITEMS.registerSimpleBlockItem("player_detector", PLAYER_DETECTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PlayerDetectorBlockEntity>> PLAYER_DETECTOR_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("player_detector", () ->
            BlockEntityType.Builder.of(PlayerDetectorBlockEntity::new, PLAYER_DETECTOR.get()).build(null));

    // Smoke Detector
    public static final DeferredBlock<SmokeDetectorBlock> SMOKE_DETECTOR = BLOCKS.register("smoke_detector",
        () -> new SmokeDetectorBlock(BlockBehaviour.Properties.of().strength(1.5f, 4.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> SMOKE_DETECTOR_ITEM =
        ITEMS.registerSimpleBlockItem("smoke_detector", SMOKE_DETECTOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SmokeDetectorBlockEntity>> SMOKE_DETECTOR_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("smoke_detector", () ->
            BlockEntityType.Builder.of(SmokeDetectorBlockEntity::new, SMOKE_DETECTOR.get()).build(null));

    // Item Cannon
    public static final DeferredBlock<ItemCannonBlock> ITEM_CANNON = BLOCKS.register("item_cannon",
        () -> new ItemCannonBlock(BlockBehaviour.Properties.of().strength(3.0f, 10.0f).requiresCorrectToolForDrops()));
    public static final DeferredItem<BlockItem> ITEM_CANNON_ITEM =
        ITEMS.registerSimpleBlockItem("item_cannon", ITEM_CANNON);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemCannonBlockEntity>> ITEM_CANNON_BLOCK_ENTITY =
        BLOCK_ENTITY_TYPES.register("item_cannon", () ->
            BlockEntityType.Builder.of(ItemCannonBlockEntity::new, ITEM_CANNON.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(MachineContent::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            PLAYER_DETECTOR_BLOCK_ENTITY.get(),
            (detector, side) -> detector.getEnergyStorage()
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            SMOKE_DETECTOR_BLOCK_ENTITY.get(),
            (detector, side) -> detector.getEnergyStorage()
        );
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ITEM_CANNON_BLOCK_ENTITY.get(),
            (cannon, side) -> cannon.getEnergyStorage()
        );
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ITEM_CANNON_BLOCK_ENTITY.get(),
            (cannon, side) -> cannon.getItemHandler()
        );
    }
}
