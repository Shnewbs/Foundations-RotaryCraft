package Foundations.RotaryCraft.Gui;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import Foundations.RotaryCraft.Machines.*;

/** Vanilla container packets carry slots and split 32-bit status values; mutations run only on the server. */
public final class MachineMenu extends AbstractContainerMenu {
    public final BlockPos position;
    public final int machineSlots;
    private final BlockEntity entity;
    private final ContainerData values;
    public static final int STATUS_COUNT=16;
    public static List<IItemHandler> handlers(BlockEntity entity) {
        if(entity instanceof SortingBlockEntity sorter)return List.of(sorter.getInput(),sorter.getFilters());
        if(entity instanceof GrindstoneBlockEntity grinder)return List.of(grinder.getItemHandler());
        if(entity instanceof DefoliatorBlockEntity defoliator)return List.of(defoliator.getItemHandler());
        if(entity instanceof ItemCannonBlockEntity cannon)return List.of(cannon.getItemHandler());
        if(entity instanceof Foundations.RotaryCraft.Power.PowerGeneratorBlockEntity generator)return List.of(generator.getFuelHandler());
        if(entity instanceof Foundations.RotaryCraft.Power.SteamGeneratorBlockEntity generator)return List.of(generator.getItemHandler());
        if(entity instanceof Foundations.RotaryCraft.Power.GeothermalGeneratorBlockEntity generator)return List.of(generator.getItemHandler());
        var level=entity.getLevel();if(level==null)return List.of();
        IItemHandler handler=level.getCapability(Capabilities.ItemHandler.BLOCK,entity.getBlockPos(),null);
        if(handler==null)for(var side:Direction.values()){handler=level.getCapability(Capabilities.ItemHandler.BLOCK,entity.getBlockPos(),side);if(handler!=null)break;}
        return handler==null?List.of():List.of(handler);
    }
    public static MachineMenu client(int id,Inventory inventory,net.minecraft.network.FriendlyByteBuf buffer) {
        BlockPos pos=buffer.readBlockPos();int count=buffer.readVarInt();
        if(count<0||count>54)throw new IllegalArgumentException("Invalid machine slot count");
        return new MachineMenu(id,inventory,pos,null,List.of(new ItemStackHandler(count)),new SimpleContainerData(STATUS_COUNT*2));
    }
    public MachineMenu(int id,Inventory inventory,BlockEntity entity) {
        this(id,inventory,entity.getBlockPos(),entity,handlers(entity),new ContainerData(){
            public int getCount(){return STATUS_COUNT*2;}
            public int get(int index){int value=status(entity,index/2);return (value>>>((index%2)*16))&65535;}
            public void set(int index,int value){}
        });
    }
    private MachineMenu(int id,Inventory inventory,BlockPos pos,BlockEntity entity,List<IItemHandler> handlers,ContainerData values) {
        super(MachineMenus.MACHINE.get(),id);this.position=pos;this.entity=entity;this.values=values;
        machineSlots=handlers.stream().mapToInt(IItemHandler::getSlots).sum();
        int index=0;
        for(var handler:handlers)for(int slot=0;slot<handler.getSlots();slot++) {
            final int local=slot;var owner=entity;
            addSlot(new SlotItemHandler(handler,slot,8+(index%9)*18,88+(index/9)*18){
                @Override public void setChanged(){super.setChanged();if(owner!=null)owner.setChanged();}
                @Override public boolean mayPickup(Player player){return handler instanceof IItemHandlerModifiable;}
                @Override public ItemStack remove(int amount){
                    if(!(handler instanceof IItemHandlerModifiable mutable))return ItemStack.EMPTY;
                    var stack=handler.getStackInSlot(local).copy();var removed=stack.split(amount);mutable.setStackInSlot(local,stack);setChanged();return removed;
                }
            });index++;
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,8+col*18,144+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,202));
        addDataSlots(values);
    }
    public int value(int field){return (values.get(field*2)&65535)|((values.get(field*2+1)&65535)<<16);}
    public static int status(BlockEntity entity,int field) {
        var level=entity.getLevel();if(level==null)return 0;
        if(field<=1){var energy=level.getCapability(Capabilities.EnergyStorage.BLOCK,entity.getBlockPos(),null);if(energy==null)for(var side:Direction.values()){energy=level.getCapability(Capabilities.EnergyStorage.BLOCK,entity.getBlockPos(),side);if(energy!=null)break;}return energy==null?0:field==0?energy.getEnergyStored():energy.getMaxEnergyStored();}
        if(entity instanceof GrindstoneBlockEntity grinder){if(field==2)return grinder.getElapsed();if(field==3)return grinder.getDuration();if(field==4)return grinder.getMechanicalInput().power().omega();if(field==5)return grinder.getMechanicalInput().power().torque();}
        if(entity instanceof Foundations.RotaryCraft.Mechanical.MechanicalBlockEntity mechanical){if(field==4)return mechanical.power().omega();if(field==5)return mechanical.power().torque();}
        if(entity instanceof WinderBlockEntity winder){if(field==2)return Math.round(winder.getOperationProgress()*100);if(field==3)return 100;}
        if(entity instanceof Foundations.RotaryCraft.Farming.SprinklerBlockEntity sprinkler && field==6)return sprinkler.getStoredWater();
        if(entity instanceof DecoTankBlockEntity tank){if(field==6)return tank.getFluidAmount();if(field==7)return tank.getMaxFluidAmount();}
        if(entity instanceof PlayerDetectorBlockEntity detector){if(field==8)return detector.getSelectedRange();if(field==9)return detector.isAnalog()?1:0;if(field==10)return detector.getRedstoneOutput();}
        var state=entity.getBlockState();
        if(field==11)return state.hasProperty(BlockStateProperties.LIT)&&state.getValue(BlockStateProperties.LIT)?1:0;
        if(field==12)return state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)?state.getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue():state.hasProperty(BlockStateProperties.FACING)?state.getValue(BlockStateProperties.FACING).get3DDataValue():-1;
        if(field==13)return Foundations.RotaryCraft.Mechanical.MechanicalContent.ratio(state.getBlock())>1 && state.hasProperty(Foundations.RotaryCraft.Mechanical.MechanicalBlock.REDUCTION)?(state.getValue(Foundations.RotaryCraft.Mechanical.MechanicalBlock.REDUCTION)?1:0):-1;
        if(field==14)return state.hasProperty(Foundations.RotaryCraft.Power.PowerSwitchBlock.ENABLED)?(state.getValue(Foundations.RotaryCraft.Power.PowerSwitchBlock.ENABLED)?1:0):-1;
        return 0;
    }
    @Override public boolean stillValid(Player player){return entity==null?player.level().isClientSide:!entity.isRemoved()&&entity.getLevel()==player.level()&&player.level().getBlockEntity(position)==entity&&player.distanceToSqr(position.getX()+.5,position.getY()+.5,position.getZ()+.5)<=64;}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        var source=slot.getItem();var copy=source.copy();
        if(index<machineSlots){if(!moveItemStackTo(source,machineSlots,slots.size(),true))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(source,0,machineSlots,false))return ItemStack.EMPTY;
        if(source.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,source);return copy;
    }
    @Override public boolean clickMenuButton(Player player,int button) {
        if(entity==null||!stillValid(player)||player.level().isClientSide||!player.level().mayInteract(player,position))return false;
        var state=entity.getBlockState();
        if(button==0){if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());else if(state.hasProperty(BlockStateProperties.FACING))state=state.cycle(BlockStateProperties.FACING);else return false;}
        else if(button==1&&state.hasProperty(Foundations.RotaryCraft.Mechanical.MechanicalBlock.REDUCTION))state=state.cycle(Foundations.RotaryCraft.Mechanical.MechanicalBlock.REDUCTION);
        else if(entity instanceof PlayerDetectorBlockEntity detector&&button>=2&&button<=4){if(button==4)detector.setAnalog(!detector.isAnalog());else detector.setSelectedRange(detector.getSelectedRange()+(button==2?-1:1));entity.setChanged();return true;}
        else return false;
        player.level().setBlock(position,state,3);player.level().invalidateCapabilities(position);entity.setChanged();return true;
    }
}
