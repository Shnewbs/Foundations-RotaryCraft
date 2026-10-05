package Foundations.RotaryCraft.Power;

import Foundations.RotaryCraft.Machines.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class CableFlowGameTests {
    private static PowerNodeBlockEntity node(GameTestHelper h,BlockPos p,boolean cell){h.setBlock(p,cell?PowerContent.POWER_CELL.get():PowerContent.POWER_CABLE.get());return (PowerNodeBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));}
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void cableFeedsConsumerAcrossBendWithoutFillingIntermediates(GameTestHelper h){
        var input=node(h,new BlockPos(1,1,1),false);var middle=node(h,new BlockPos(2,1,1),false);var corner=node(h,new BlockPos(2,1,2),false);
        var targetPos=new BlockPos(3,1,2);h.setBlock(targetPos,MachineContent.GRINDSTONE.get());
        var target=(GrindstoneBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(targetPos));
        input.getEnergyStorage().receiveEnergy(500,false);
        PowerNodeBlockEntity.serverTick(h.getLevel(),input.getBlockPos(),input.getBlockState(),input);
        h.assertTrue(target.getEnergyStorage().getEnergyStored()==500,"Bent cable failed to deliver its rated power");
        h.assertTrue(input.getEnergyStorage().getEnergyStored()==0&&middle.getEnergyStorage().getEnergyStored()==0&&corner.getEnergyStorage().getEnergyStored()==0,"Power was trapped in intermediate buffers");h.succeed();
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void sharedCableBudgetAndEnergyConservation(GameTestHelper h){
        var first=node(h,new BlockPos(1,1,1),false);var second=node(h,new BlockPos(2,1,1),false);
        var p=new BlockPos(3,1,1);h.setBlock(p,MachineContent.GRINDSTONE.get());var target=(GrindstoneBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
        first.getEnergyStorage().receiveEnergy(500,false);second.getEnergyStorage().receiveEnergy(500,false);
        PowerNodeBlockEntity.serverTick(h.getLevel(),first.getBlockPos(),first.getBlockState(),first);PowerNodeBlockEntity.serverTick(h.getLevel(),second.getBlockPos(),second.getBlockState(),second);
        h.assertTrue(target.getEnergyStorage().getEnergyStored()==500,"Connected cables exceeded shared per-tick rate");
        h.assertTrue(first.getEnergyStorage().getEnergyStored()+second.getEnergyStorage().getEnergyStored()+target.getEnergyStorage().getEnergyStored()==1000,"Network created or lost energy");h.succeed();
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void batteryKeepsFullSavedChargeAndClampsCorruption(GameTestHelper h){
        var battery=node(h,new BlockPos(1,1,1),true);for(int i=0;i<73;i++)battery.getEnergyStorage().receiveEnergy(1000,false);
        var saved=battery.saveWithoutMetadata(h.getLevel().registryAccess());var restored=new PowerNodeBlockEntity(battery.getBlockPos(),battery.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(restored.getEnergyStorage().getEnergyStored()==73000,"Restart discarded battery energy above transfer rate");
        saved.putInt("StoredEnergy",Integer.MAX_VALUE);restored.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(restored.getEnergyStorage().getEnergyStored()==100000,"Corrupt battery charge exceeded capacity");h.succeed();
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft",timeoutTicks=40)
    public static void batteryDoesNotPingPongIntoIdleCable(GameTestHelper h){
        var battery=node(h,new BlockPos(1,1,1),true);var cable=node(h,new BlockPos(2,1,1),false);battery.getEnergyStorage().receiveEnergy(1000,false);
        h.runAfterDelay(20,()->{h.assertTrue(battery.getEnergyStorage().getEnergyStored()==1000&&cable.getEnergyStorage().getEnergyStored()==0,"Idle battery bounced power into cable");h.succeed();});
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft",timeoutTicks=40)
    public static void lateCreatedMachinePortUpdatesCableConnection(GameTestHelper h){
        var cable=node(h,new BlockPos(1,1,1),false);h.setBlock(new BlockPos(2,1,1),MachineContent.GRINDSTONE.get());
        h.runAfterDelay(25,()->{h.assertTrue(cable.getBlockState().getValue(PowerNodeBlock.propertyFor(net.minecraft.core.Direction.EAST)),"Cable did not discover adjacent machine's sided energy port");h.succeed();});
    }
}
