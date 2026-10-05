package Foundations.RotaryCraft.Compat.Jade;

import Foundations.RotaryCraft.Mechanical.*;
import Foundations.RotaryCraft.Platform.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.*;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public final class RotaryJadePlugin implements IWailaPlugin {
  @Override
  public void register(IWailaCommonRegistration registration) {
    registration.registerBlockDataProvider(ServerStatus.INSTANCE, PowerEntity.class);
    registration.registerBlockDataProvider(ServerStatus.INSTANCE, MechanicalBlockEntity.class);
  }

  @Override
  public void registerClient(IWailaClientRegistration registration) {
    registration.registerBlockComponent(ClientStatus.INSTANCE, PowerBlock.class);
    registration.registerBlockComponent(ClientStatus.INSTANCE, CableBlock.class);
    registration.registerBlockComponent(ClientStatus.INSTANCE, SwitchBlock.class);
    registration.registerBlockComponent(ClientStatus.INSTANCE, MechanicalBlock.class);
  }

  public enum ServerStatus implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public Identifier getUid() {
      return Identifier.fromNamespaceAndPath("rotarycraft", "native_status");
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
      var status = new CompoundTag();
      if (accessor.getBlockEntity() instanceof PowerEntity power) {
        var energy = power.energyPort(null);
        if (energy != null) {
          status.putInt("energy", energy.getAmountAsInt());
          status.putInt("capacity", energy.getCapacityAsInt());
        }
        status.putInt("elapsed", power.progress());
        status.putInt("duration", power.duration());
        if (power
            .getBlockState()
            .hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT))
          status.putBoolean(
              "operating",
              power
                  .getBlockState()
                  .getValue(
                      net.minecraft.world.level.block.state.properties.BlockStateProperties.LIT));
      } else if (accessor.getBlockEntity() instanceof MechanicalBlockEntity mechanical) {
        var signal = mechanical.power();
        status.putBoolean("mechanical", true);
        status.putInt("speed", signal.omega());
        status.putInt("torque", signal.torque());
        status.putLong("watts", signal.watts());
      }
      data.put("rotarycraft:native_status", status);
    }
  }

  public enum ClientStatus implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public Identifier getUid() {
      return Identifier.fromNamespaceAndPath("rotarycraft", "native_status");
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
      var status = accessor.getServerData().getCompound("rotarycraft:native_status").orElse(null);
      if (status == null) return;
      if (status.getBooleanOr("mechanical", false))
        tooltip.add(
            Component.translatable(
                "jade.rotarycraft.shaft",
                status.getIntOr("speed", 0),
                status.getIntOr("torque", 0),
                status.getLongOr("watts", 0)));
      else {
        if (status.contains("energy"))
          tooltip.add(
              Component.translatable(
                  "jade.rotarycraft.energy",
                  status.getIntOr("energy", 0),
                  status.getIntOr("capacity", 0)));
        if (status.getIntOr("duration", 0) > 0) {
          tooltip.add(
              Component.translatable(
                  "jade.rotarycraft.progress",
                  status.getIntOr("elapsed", 0),
                  status.getIntOr("duration", 0)));
          tooltip.add(
              Component.translatable(
                  status.getBooleanOr("operating", false)
                      ? "jade.rotarycraft.running"
                      : "jade.rotarycraft.idle"));
        }
      }
    }
  }
}
