package Foundations.RotaryCraft.Power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class PowerNodeBlockEntity extends BlockEntity {
    private final NodeEnergyStorage energy;
    private final int transferRate;

    public PowerNodeBlockEntity(BlockPos pos, BlockState state) {
        super(PowerContent.POWER_NODE.get(), pos, state);
        boolean isCell = state.is(PowerContent.POWER_CELL.get());
        int capacity = isCell ? 100_000 : 10_000;
        transferRate = isCell ? 1_000 : 500;
        energy = new NodeEnergyStorage(capacity,transferRate);
    }
    private final class NodeEnergyStorage extends EnergyStorage {
        NodeEnergyStorage(int capacity,int rate){super(capacity,rate,rate);}
        @Override public int receiveEnergy(int amount,boolean simulate){int moved=super.receiveEnergy(amount,simulate);if(moved>0&&!simulate)contentsChanged();return moved;}
        @Override public int extractEnergy(int amount,boolean simulate){int moved=super.extractEnergy(amount,simulate);if(moved>0&&!simulate)contentsChanged();return moved;}
        void restore(int stored){energy=Math.max(0,Math.min(capacity,stored));}
    }
    private void contentsChanged(){setChanged();if(level!=null&&!level.isClientSide)level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}
    int transferRate(){return transferRate;}
    boolean isCell(){return getBlockState().is(PowerContent.POWER_CELL.get());}

    public IEnergyStorage getEnergyStorage() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PowerNodeBlockEntity node) {
        if(Math.floorMod(level.getGameTime()+pos.asLong(),20)==0){
            var updated=state;
            for(var direction:Direction.values())updated=updated.setValue(PowerNodeBlock.propertyFor(direction),PowerNodeBlock.hasEnergyPort(level,pos.relative(direction),direction.getOpposite()));
            if(updated!=state)level.setBlock(pos,updated,2);
        }
        node.distributeEnergy(level, pos);
    }

    private void distributeEnergy(Level level,BlockPos pos){
        if(level.isClientSide||energy.getEnergyStored()==0)return;
        CableNetwork.distribute(level,this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("StoredEnergy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.restore(tag.getInt("StoredEnergy"));
    }
}
