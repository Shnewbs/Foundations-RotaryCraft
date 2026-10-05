package Foundations.RotaryCraft.Mechanical;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MechanicalContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("rotarycraft");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("rotarycraft");
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "rotarycraft");
    public static final DeferredBlock<MechanicalBlock> DC_ENGINE = block("dc_engine");
    public static final DeferredBlock<MechanicalBlock> SHAFT = block("shaft");
    public static final DeferredBlock<MechanicalBlock> GEARBOX_2 = block("gearbox_2");
    public static final DeferredBlock<MechanicalBlock> GEARBOX_4 = block("gearbox_4");
    public static final DeferredBlock<MechanicalBlock> GEARBOX_8 = block("gearbox_8");
    public static final DeferredBlock<MechanicalBlock> GEARBOX_16 = block("gearbox_16");
    public static final List<DeferredBlock<MechanicalBlock>> ALL = List.of(DC_ENGINE, SHAFT, GEARBOX_2, GEARBOX_4, GEARBOX_8, GEARBOX_16);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MechanicalBlockEntity>> ENTITY = ENTITIES.register("mechanical",
            () -> new BlockEntityType<>(MechanicalBlockEntity::new,java.util.Set.copyOf(ALL.stream().map(DeferredBlock::get).toList())));
    private static DeferredBlock<MechanicalBlock> block(String name) {
        var block = BLOCKS.registerBlock(name,MechanicalBlock::new,BlockBehaviour.Properties.of().noOcclusion().strength(2.5F));
        ITEMS.registerSimpleBlockItem(name, block);
        return block;
    }
    public static int ratio(Block block) {
        if (block == GEARBOX_2.get()) return 2;
        if (block == GEARBOX_4.get()) return 4;
        if (block == GEARBOX_8.get()) return 8;
        if (block == GEARBOX_16.get()) return 16;
        return 1;
    }
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener(net.neoforged.bus.api.EventPriority.HIGH, MechanicalContent::capabilities);
        bus.addListener(MechanicalContent::creativeTab);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(ShaftNetwork.CAPABILITY, ENTITY.get(), MechanicalBlockEntity::capability);
        event.setNonProxyable(ShaftNetwork.CAPABILITY);
    }
    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) ALL.forEach(block -> event.accept(block.get()));
    }
    private MechanicalContent() {}
}
