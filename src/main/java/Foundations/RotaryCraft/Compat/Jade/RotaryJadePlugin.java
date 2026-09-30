package Foundations.RotaryCraft.Compat.Jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import Foundations.RotaryCraft.Machines.GrindstoneBlock;
import Foundations.RotaryCraft.Machines.GrindstoneBlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class RotaryJadePlugin implements IWailaPlugin {
    @Override public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(GrindingProvider.INSTANCE, GrindstoneBlockEntity.class);
        registration.registerBlockDataProvider(MechanicalProvider.INSTANCE, Foundations.RotaryCraft.Mechanical.MechanicalBlockEntity.class);
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(GrindingProvider.INSTANCE, GrindstoneBlock.class);
        registration.registerBlockComponent(MechanicalProvider.INSTANCE, Foundations.RotaryCraft.Mechanical.MechanicalBlock.class);
    }
    public enum GrindingProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("rotarycraft", "grinding_status"); }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof GrindstoneBlockEntity machine) {
                CompoundTag status = new CompoundTag();
                status.putInt("energy", machine.getEnergyStorage().getEnergyStored());
                status.putInt("capacity", machine.getEnergyStorage().getMaxEnergyStored());
                status.putInt("elapsed", machine.getElapsed());
                status.putInt("duration", machine.getDuration());
                status.putBoolean("operating", machine.isOperating());
                var shaft = machine.getMechanicalInput();
                status.putBoolean("shaft_connected", shaft.connected());
                status.putInt("shaft_speed", shaft.power().omega());
                status.putInt("shaft_torque", shaft.power().torque());
                status.putLong("shaft_watts", shaft.power().watts());
                data.put("rotarycraft:grinding", status);
            }
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getServerData().contains("rotarycraft:grinding")) return;
            CompoundTag status = accessor.getServerData().getCompound("rotarycraft:grinding");
            if (status.getBoolean("shaft_connected")) {
                tooltip.add(Component.translatable("jade.rotarycraft.shaft", status.getInt("shaft_speed"), status.getInt("shaft_torque"), status.getLong("shaft_watts")));
                tooltip.add(Component.translatable("jade.rotarycraft.grinder_requirement"));
            } else tooltip.add(Component.translatable("jade.rotarycraft.energy", status.getInt("energy"), status.getInt("capacity")));
            tooltip.add(Component.translatable("jade.rotarycraft.progress", status.getInt("elapsed"), status.getInt("duration")));
            tooltip.add(Component.translatable(status.getBoolean("operating") ? "jade.rotarycraft.running" : "jade.rotarycraft.idle"));
        }
    }
    public enum MechanicalProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        INSTANCE;
        @Override public ResourceLocation getUid() { return ResourceLocation.fromNamespaceAndPath("rotarycraft", "mechanical_status"); }
        @Override public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof Foundations.RotaryCraft.Mechanical.MechanicalBlockEntity machine) {
                var power = machine.power();
                CompoundTag status = new CompoundTag();
                if (machine.getBlockState().is(Foundations.RotaryCraft.Mechanical.MechanicalContent.DC_ENGINE.get())) {
                    status.putBoolean("engine", true);
                    status.putBoolean("powered", accessor.getLevel().hasNeighborSignal(machine.getBlockPos()));
                }
                status.putInt("speed", power.omega());
                status.putInt("torque", power.torque());
                status.putLong("watts", power.watts());
                status.putInt("ratio", Foundations.RotaryCraft.Mechanical.MechanicalContent.ratio(machine.getBlockState().getBlock()));
                status.putBoolean("reduction", machine.getBlockState().getValue(Foundations.RotaryCraft.Mechanical.MechanicalBlock.REDUCTION));
                data.put("rotarycraft:mechanical", status);
            }
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getServerData().contains("rotarycraft:mechanical")) return;
            CompoundTag status = accessor.getServerData().getCompound("rotarycraft:mechanical");
            tooltip.add(Component.translatable("jade.rotarycraft.shaft", status.getInt("speed"), status.getInt("torque"), status.getLong("watts")));
            if (status.getBoolean("engine")) tooltip.add(Component.translatable(status.getBoolean("powered")
                    ? "jade.rotarycraft.dc_powered" : status.getInt("speed") > 0 ? "jade.rotarycraft.dc_coasting" : "jade.rotarycraft.dc_stopped"));
            if (status.getInt("ratio") > 1) tooltip.add(Component.translatable(status.getBoolean("reduction") ? "jade.rotarycraft.reduction" : "jade.rotarycraft.acceleration", status.getInt("ratio")));
        }
    }
}
