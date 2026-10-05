package Foundations.RotaryCraft.Platform;

import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.registries.*;

public final class PowerContent {
  private static final DeferredRegister.Blocks BLOCKS =
      DeferredRegister.createBlocks("rotarycraft");
  private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("rotarycraft");
  private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "rotarycraft");
  public static final Map<String, DeferredBlock<? extends Block>> ALL = new LinkedHashMap<>();

  static {
    for (var id : List.of("power_cable", "power_cell")) {
      var block =
          BLOCKS.registerBlock(
              id, CableBlock::new, () -> BlockBehaviour.Properties.of().noOcclusion().strength(2));
      ALL.put(id, block);
      ITEMS.registerSimpleBlockItem(id, block);
    }
    for (var id :
        List.of(
            "power_generator",
            "solar_generator",
            "wind_generator",
            "hydro_generator",
            "steam_generator",
            "geothermal_generator",
            "grindstone")) {
      var block =
          BLOCKS.registerBlock(
              id,
              PowerBlock::new,
              () -> BlockBehaviour.Properties.of().noOcclusion().strength(2.5F));
      ALL.put(id, block);
      ITEMS.registerSimpleBlockItem(id, block);
    }
    var gate =
        BLOCKS.registerBlock(
            "power_switch",
            SwitchBlock::new,
            () -> BlockBehaviour.Properties.of().noOcclusion().strength(2));
    ALL.put("power_switch", gate);
    ITEMS.registerSimpleBlockItem("power_switch", gate);
  }

  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerEntity>> ENTITY =
      ENTITIES.register(
          "power_node",
          () ->
              new BlockEntityType<>(
                  PowerEntity::new,
                  Set.copyOf(ALL.values().stream().map(DeferredBlock::get).toList())));

  public static void register(IEventBus bus) {
    BLOCKS.register(bus);
    ITEMS.register(bus);
    ENTITIES.register(bus);
    bus.addListener(
        (RegisterCapabilitiesEvent event) -> {
          event.registerBlockEntity(
              Capabilities.Energy.BLOCK, ENTITY.get(), (machine, side) -> machine.energyPort(side));
          event.registerBlockEntity(
              Capabilities.Item.BLOCK,
              ENTITY.get(),
              (machine, side) -> machine.hasInventory() ? machine.itemPort : null);
        });
    bus.addListener(
        (net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
          if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS)
            ALL.values().forEach(block -> event.accept(block.get()));
        });
  }

  private PowerContent() {}
}
