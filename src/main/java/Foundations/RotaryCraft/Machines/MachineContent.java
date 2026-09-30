package Foundations.RotaryCraft.Machines;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
