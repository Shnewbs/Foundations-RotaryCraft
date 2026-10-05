package Foundations.RotaryCraft;

import Foundations.RotaryCraft.Mechanical.MechanicalContent;
import Foundations.RotaryCraft.Platform.PowerContent;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ModelEvent;

@Mod(value = "rotarycraft", dist = Dist.CLIENT)
public final class NativeClient {
  public NativeClient(IEventBus bus) {
    bus.addListener(NativeClient::baked);
  }

  private static void baked(ModelEvent.BakingCompleted event) {
    if (!Boolean.getBoolean("rotarycraft.visualSmoke")) return;
    var result = event.getBakingResult();
    var blocks = new ArrayList<Block>();
    MechanicalContent.ALL.forEach(block -> blocks.add(block.get()));
    PowerContent.ALL.values().forEach(block -> blocks.add(block.get()));
    int states = 0, quads = 0;
    for (var block : blocks) {
      for (var state : block.getStateDefinition().getPossibleStates()) {
        var model = result.blockStateModels().get(state);
        if (model == null || model == result.missingModels().block())
          throw new IllegalStateException("Missing native block model: " + state);
        var parts = new ArrayList<BlockStateModelPart>();
        model.collectParts(RandomSource.create(0), parts);
        int count = 0;
        for (var part : parts) {
          if (part.particleMaterial().sprite().contents().name().getPath().equals("missingno"))
            throw new IllegalStateException("Missing native texture: " + state);
          count += part.getQuads(null).size();
          for (var direction : net.minecraft.core.Direction.values())
            count += part.getQuads(direction).size();
        }
        if (count == 0) throw new IllegalStateException("Empty native block geometry: " + state);
        quads += count;
        states++;
      }
      var id = BuiltInRegistries.ITEM.getKey(block.asItem());
      var item = result.itemStackModels().get(id);
      if (item == null || item == result.missingModels().item())
        throw new IllegalStateException("Missing native item model: " + id);
    }
    System.out.println(
        "ROTARYCRAFT_NATIVE_VISUAL_OK states="
            + states
            + " items="
            + blocks.size()
            + " quads="
            + quads);
    Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
  }
}
