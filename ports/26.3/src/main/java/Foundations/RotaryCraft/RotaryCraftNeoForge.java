package Foundations.RotaryCraft;

import Foundations.RotaryCraft.Mechanical.MechanicalContent;
import Foundations.RotaryCraft.Platform.PowerContent;
import Foundations.RotaryCraft.Recipes.RecipeContent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(RotaryCraftNeoForge.MOD_ID)
public final class RotaryCraftNeoForge {
  public static final String MOD_ID = "rotarycraft";

  public RotaryCraftNeoForge(IEventBus bus) {
    MechanicalContent.register(bus);
    PowerContent.register(bus);
    RecipeContent.register(bus);
    bus.addListener(Foundations.RotaryCraft.Platform.NativeGameTests::register);
  }
}
