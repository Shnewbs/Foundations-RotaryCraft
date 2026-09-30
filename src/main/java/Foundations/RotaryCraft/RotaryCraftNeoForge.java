package Foundations.RotaryCraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import Foundations.RotaryCraft.Farming.FarmingContent;
import Foundations.RotaryCraft.Machines.MachineContent;
import Foundations.RotaryCraft.Power.PowerContent;
import Foundations.RotaryCraft.Recipes.RecipeContent;

@Mod(RotaryCraftNeoForge.MOD_ID)
public final class RotaryCraftNeoForge {
    public static final String MOD_ID = "rotarycraft";

    public RotaryCraftNeoForge(IEventBus modEventBus) {
        Foundations.RotaryCraft.Mechanical.MechanicalContent.register(modEventBus);
        RecipeContent.register(modEventBus);
        Foundations.RotaryCraft.Gui.MachineMenus.register(modEventBus);
        PowerContent.register(modEventBus);
        FarmingContent.register(modEventBus);
        MachineContent.register(modEventBus);
    }
}
