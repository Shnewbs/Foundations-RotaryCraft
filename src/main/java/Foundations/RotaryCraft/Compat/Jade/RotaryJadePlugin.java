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
    }
    @Override public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(GrindingProvider.INSTANCE, GrindstoneBlock.class);
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
                data.put("rotarycraft:grinding", status);
            }
        }
        @Override public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (!accessor.getServerData().contains("rotarycraft:grinding")) return;
            CompoundTag status = accessor.getServerData().getCompound("rotarycraft:grinding");
            tooltip.add(Component.translatable("jade.rotarycraft.energy", status.getInt("energy"), status.getInt("capacity")));
            tooltip.add(Component.translatable("jade.rotarycraft.progress", status.getInt("elapsed"), status.getInt("duration")));
            tooltip.add(Component.translatable(status.getBoolean("operating") ? "jade.rotarycraft.running" : "jade.rotarycraft.idle"));
        }
    }
}
