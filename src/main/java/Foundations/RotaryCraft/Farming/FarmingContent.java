package Foundations.RotaryCraft.Farming;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemNameBlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import Foundations.RotaryCraft.RotaryCraftNeoForge;

public final class FarmingContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RotaryCraftNeoForge.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RotaryCraftNeoForge.MOD_ID);

    public static final DeferredBlock<CanolaCropBlock> CANOLA_CROP =
            BLOCKS.register("canola_crop", CanolaCropBlock::new);
    public static final DeferredItem<ItemNameBlockItem> CANOLA_SEEDS = ITEMS.register(
            "canola_seeds",
            () -> new ItemNameBlockItem(CANOLA_CROP.get(), new net.minecraft.world.item.Item.Properties())
    );

    private FarmingContent() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        modEventBus.addListener(FarmingContent::addToCreativeTab);
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(CANOLA_SEEDS.get());
        }
    }
}
