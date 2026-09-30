package Foundations.RotaryCraft.Farming;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import Foundations.RotaryCraft.RotaryCraftNeoForge;

public final class FarmingContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RotaryCraftNeoForge.MOD_ID);

    public static final DeferredBlock<CanolaCropBlock> CANOLA_CROP =
            BLOCKS.register("canola_crop", CanolaCropBlock::new);
    public static final DeferredBlock<SprinklerBlock> SPRINKLER = BLOCKS.register(
            "sprinkler",
            () -> new SprinklerBlock(BlockBehaviour.Properties.of().strength(2.5F).noOcclusion())
    );
    public static final DeferredItem<ItemNameBlockItem> CANOLA_SEEDS = ITEMS.register(
            "canola_seeds",
            () -> new ItemNameBlockItem(CANOLA_CROP.get(), new Item.Properties())
    );
    private static final DeferredItem<BlockItem> SPRINKLER_ITEM = ITEMS.registerSimpleBlockItem("sprinkler", SPRINKLER);
    static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SprinklerBlockEntity>> SPRINKLER_ENTITY =
            BLOCK_ENTITY_TYPES.register(
                    "sprinkler",
                    () -> BlockEntityType.Builder.of(SprinklerBlockEntity::new, SPRINKLER.get()).build(null)
            );

    private FarmingContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(FarmingContent::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(CANOLA_SEEDS.get());
        } else if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(SPRINKLER_ITEM.get());
        }
    }
}
