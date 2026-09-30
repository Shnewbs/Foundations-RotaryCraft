package Reika.RotaryCraft.Power;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
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

import Reika.RotaryCraft.RotaryCraftNeoForge;

public final class PowerContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RotaryCraftNeoForge.MOD_ID);

    public static final DeferredBlock<PowerNodeBlock> POWER_CABLE = BLOCKS.register(
            "power_cable",
            () -> new PowerNodeBlock(BlockBehaviour.Properties.of().strength(1.5F))
    );
    public static final DeferredBlock<PowerNodeBlock> POWER_CELL = BLOCKS.register(
            "power_cell",
            () -> new PowerNodeBlock(BlockBehaviour.Properties.of().strength(2.5F))
    );
    private static final DeferredItem<BlockItem> POWER_CABLE_ITEM = ITEMS.registerSimpleBlockItem("power_cable", POWER_CABLE);
    private static final DeferredItem<BlockItem> POWER_CELL_ITEM = ITEMS.registerSimpleBlockItem("power_cell", POWER_CELL);

    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerNodeBlockEntity>> POWER_NODE =
            BLOCK_ENTITY_TYPES.register(
                    "power_node",
                    () -> BlockEntityType.Builder.of(
                            PowerNodeBlockEntity::new,
                            POWER_CABLE.get(),
                            POWER_CELL.get()
                    ).build(null)
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
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(POWER_CABLE_ITEM.get());
            event.accept(POWER_CELL_ITEM.get());
        }
    }
}
