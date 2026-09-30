package Foundations.RotaryCraft.Machines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Passive storage with sided pipe access, bucket interaction, persistence and client updates. */
public class DecoTankBlockEntity extends BlockEntity {
    private static final int MAX_FLUID_AMOUNT=16000;
    private final FluidTank tank=new FluidTank(MAX_FLUID_AMOUNT){
        @Override protected void onContentsChanged(){
            setChanged();
            if(level!=null&&!level.isClientSide){
                level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
                level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());
            }
        }
    };
    public DecoTankBlockEntity(BlockPos pos,BlockState state){super(MachineContent.DECO_TANK_BLOCK_ENTITY.get(),pos,state);}
    public static void serverTick(Level level,BlockPos pos,BlockState state,DecoTankBlockEntity entity){}
    public FluidTank getTank(){return tank;}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);
        tank.readFromNBT(registries,tag.getCompound("tank"));
        if(!tag.contains("tank")&&tag.getInt("fluid_amount")>0){
            var id=ResourceLocation.tryParse(tag.getString("fluid_name"));
            if(id!=null&&BuiltInRegistries.FLUID.containsKey(id))tank.setFluid(new FluidStack(BuiltInRegistries.FLUID.get(id),Math.min(MAX_FLUID_AMOUNT,tag.getInt("fluid_amount"))));
        }
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.put("tank",tank.writeToNBT(registries,new CompoundTag()));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public int getFluidAmount(){return tank.getFluidAmount();}
    public String getFluidName(){return tank.isEmpty()?"":BuiltInRegistries.FLUID.getKey(tank.getFluid().getFluid()).toString();}
    public int getMaxFluidAmount(){return MAX_FLUID_AMOUNT;}
    public int getFluidLevel(){return tank.isEmpty()?0:Math.max(1,(tank.getFluidAmount()*15)/MAX_FLUID_AMOUNT);}
    public boolean isEmpty(){return tank.isEmpty();}
}
