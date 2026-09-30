package Foundations.RotaryCraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import Foundations.RotaryCraft.Farming.FarmingContent;
import Foundations.RotaryCraft.Machines.MachineContent;
import Foundations.RotaryCraft.Power.PowerContent;

@Mod(RotaryCraftNeoForge.MOD_ID)
public final class RotaryCraftNeoForge {
    public static final String MOD_ID = "rotarycraft";

    public RotaryCraftNeoForge(IEventBus modEventBus) {
        PowerContent.register(modEventBus);
        FarmingContent.register(modEventBus);
        MachineContent.register(modEventBus);
    }
}
