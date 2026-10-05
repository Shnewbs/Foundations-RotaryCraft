package Foundations.RotaryCraft.Platform;

import java.util.*;
import Foundations.RotaryCraft.Mechanical.*;
import Foundations.RotaryCraft.Recipes.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.*;
import net.neoforged.neoforge.transfer.item.*;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/** Native transaction capabilities and Value IO, with no legacy API emulation. */
public final class PowerEntity extends BaseContainerBlockEntity {
    final SimpleEnergyHandler energy;
    private final NonNullList<ItemStack> items=NonNullList.withSize(5,ItemStack.EMPTY);
    final ItemStacksResourceHandler itemPort;
    private final EnergyHandler port;
    private int burnTicks,waterTicks,progress;
    private net.minecraft.resources.Identifier activeRecipe;
    private static final ThreadLocal<Set<PowerEntity>> ACTIVE_SWITCHES=ThreadLocal.withInitial(HashSet::new);
    public PowerEntity(BlockPos pos,BlockState state){
        super(PowerContent.ENTITY.get(),pos,state);
        energy=new SimpleEnergyHandler(capacity(),rate(),rate()){
            @Override protected void onEnergyChanged(int before){setChanged();if(level!=null&&!level.isClientSide())level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}
        };
        itemPort=new ItemStacksResourceHandler(items){
            @Override public boolean isValid(int slot,ItemResource resource){return PowerEntity.this.valid(slot,resource.toStack());}
            @Override public int extract(int slot,ItemResource resource,int amount,TransactionContext transaction){if(kind().equals("grindstone")&&slot!=1)return 0;return super.extract(slot,resource,amount,transaction);}
            @Override protected void onContentsChanged(int slot,ItemStack before){setChanged();}
        };
        port=new EnergyHandler(){
            public long getAmountAsLong(){return energy.getAmountAsLong();}
            public long getCapacityAsLong(){return energy.getCapacityAsLong();}
            public int insert(int amount,TransactionContext transaction){return generator()?0:energy.insert(amount,transaction);}
            public int extract(int amount,TransactionContext transaction){return kind().equals("grindstone")?0:energy.extract(amount,transaction);}
        };
    }
    String kind(){return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();}
    boolean generator(){return kind().endsWith("generator");}
    boolean isCell(){return kind().equals("power_cell");}
    boolean isWire(){return kind().equals("power_cable")||isCell()||kind().equals("power_switch")&&getBlockState().getValue(SwitchBlock.ENABLED);}
    int rate(){return switch(kind()){case "power_cable"->500;case "power_cell","grindstone","power_switch"->1000;case "solar_generator"->320;default->160;};}
    int capacity(){return switch(kind()){case "power_cable"->10000;case "power_cell","grindstone"->100000;case "power_switch"->0;default->50000;};}
    public boolean hasInventory(){return Set.of("power_generator","steam_generator","geothermal_generator","grindstone").contains(kind());}
    public EnergyHandler energyPort(Direction side){
        if(!kind().equals("power_switch"))return port;
        if(!getBlockState().getValue(SwitchBlock.ENABLED))return null;
        return new EnergyHandler(){
            public long getAmountAsLong(){return 0;}public long getCapacityAsLong(){return 0;}
            public int extract(int amount,TransactionContext transaction){return 0;}
            public int insert(int amount,TransactionContext transaction){
                if(amount<=0||level==null||!getBlockState().getValue(SwitchBlock.ENABLED))return 0;
                var active=ACTIVE_SWITCHES.get();if(!active.add(PowerEntity.this))return 0;
                try{int accepted=0;for(var direction:Direction.values()){
                    if(direction==side||accepted==amount)continue;var targetPos=worldPosition.relative(direction);if(!level.hasChunkAt(targetPos))continue;
                    var target=level.getCapability(Capabilities.Energy.BLOCK,targetPos,direction.getOpposite());if(target!=null)accepted+=target.insert(amount-accepted,transaction);
                }return accepted;}finally{active.remove(PowerEntity.this);if(active.isEmpty())ACTIVE_SWITCHES.remove();}
            }
        };
    }
    public Component status(){return Component.literal(getBlockState().getBlock().getName().getString()+": "+energy.getAmountAsInt()+" / "+energy.getCapacityAsInt()+" FE");}
    public static void tick(Level level,BlockPos pos,BlockState state,PowerEntity entity){
        if(level.isClientSide())return;
        if(entity.kind().equals("power_switch")){
            boolean enabled=!level.hasNeighborSignal(pos);if(state.getValue(SwitchBlock.ENABLED)!=enabled){level.setBlock(pos,state.setValue(SwitchBlock.ENABLED,enabled),3);level.invalidateCapabilities(pos);CableNetwork.invalidate(level);}return;
        }
        if(state.getBlock() instanceof CableBlock){
            if(Math.floorMod(level.getGameTime()+pos.asLong(),20)==0){var updated=state;for(var d:Direction.values())updated=updated.setValue(CableBlock.PORTS[d.get3DDataValue()],CableBlock.port(level,pos.relative(d),d.getOpposite()));if(updated!=state)level.setBlock(pos,updated,2);}
            if(entity.energy.getAmountAsInt()>0)CableNetwork.distribute(level,entity);return;
        }
        boolean active=entity.kind().equals("grindstone")?entity.grind((ServerLevel)level):entity.generate((ServerLevel)level);
        if(state.getValue(BlockStateProperties.LIT)!=active)level.setBlock(pos,state.setValue(BlockStateProperties.LIT,active),2);
        if(entity.generator())entity.export(level);
    }
    private int fuelDuration(ItemStack stack){return level instanceof ServerLevel server?ResolvableInt.getFromItem(stack,DataComponents.COOKING_FUEL,CookingFuel::burnTime,getLootContext(server),0):0;}
    private boolean valid(int slot,ItemStack stack){
        return switch(kind()){case "grindstone"->slot==0;case "power_generator"->stack.has(DataComponents.COOKING_FUEL);case "steam_generator"->stack.is(Items.WATER_BUCKET)||stack.has(DataComponents.COOKING_FUEL);case "geothermal_generator"->stack.is(Items.LAVA_BUCKET);default->false;};
    }
    private int find(java.util.function.Predicate<ItemStack> predicate){for(int i=0;i<items.size();i++)if(predicate.test(items.get(i)))return i;return -1;}
    private void bucket(int slot){items.set(slot,new ItemStack(Items.BUCKET));setChanged();}
    private boolean generate(ServerLevel server){
        if(energy.getAmountAsInt()>=capacity())return false;
        int generated=0;
        switch(kind()){
            case "solar_generator"->{if(server.isBrightOutside()&&server.canSeeSky(worldPosition.above()))generated=32;}
            case "wind_generator"->{if(worldPosition.getY()>=server.getSeaLevel()+32&&server.canSeeSky(worldPosition.above()))generated=16;}
            case "hydro_generator"->{for(var d:Direction.values()){var p=worldPosition.relative(d);if(server.hasChunkAt(p)&&server.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER))generated+=8;}}
            case "geothermal_generator"->{if(burnTicks==0){int slot=find(stack->stack.is(Items.LAVA_BUCKET));if(slot>=0){bucket(slot);burnTicks=1000;}}if(burnTicks>0){burnTicks--;generated=40;}}
            case "steam_generator","power_generator"->{
                boolean steam=kind().equals("steam_generator");
                if(steam&&waterTicks==0){int slot=find(stack->stack.is(Items.WATER_BUCKET));if(slot>=0){bucket(slot);waterTicks=1000;}}
                if((!steam||waterTicks>0)&&burnTicks==0){int slot=find(stack->fuelDuration(stack)>0);if(slot>=0){burnTicks=fuelDuration(items.get(slot));var stack=items.get(slot);var remainder=stack.getCraftingRemainder();stack.shrink(1);if(stack.isEmpty())items.set(slot,remainder);setChanged();}}
                if(burnTicks>0&&(!steam||waterTicks>0)){burnTicks--;if(steam)waterTicks--;generated=40;}
            }
            default->{}
        }
        if(generated>0){energy.set(Math.min(capacity(),energy.getAmountAsInt()+generated));setChanged();return true;}return false;
    }
    private void export(Level level){int remaining=rate();int start=(int)Math.floorMod(level.getGameTime(),6);for(int i=0;i<6&&remaining>0;i++){var d=Direction.from3DDataValue((start+i)%6);var pos=worldPosition.relative(d);if(!level.hasChunkAt(pos))continue;var target=level.getCapability(Capabilities.Energy.BLOCK,pos,d.getOpposite());if(target!=null)remaining-=EnergyTransfer.transfer(port,target,remaining);}}
    private boolean grind(ServerLevel server){
        var input=items.get(0);if(input.isEmpty()){progress=0;activeRecipe=null;return false;}
        var holder=server.recipeAccess().getRecipeFor(RecipeContent.GRINDING_TYPE.get(),new SingleRecipeInput(input),server);if(holder.isEmpty()){progress=0;activeRecipe=null;return false;}
        var recipe=holder.get().value();var id=holder.get().id().identifier();if(!id.equals(activeRecipe)){progress=0;activeRecipe=id;}
        var output=items.get(1);var result=recipe.result();if(!output.isEmpty()&&(!ItemStack.isSameItemSameComponents(output,result)||output.getCount()+result.getCount()>output.getMaxStackSize()))return false;
        var side=getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();var sourcePos=worldPosition.relative(side);var shaft=server.hasChunkAt(sourcePos)?server.getCapability(ShaftNetwork.CAPABILITY,sourcePos,side.getOpposite()):null;
        if(shaft!=null){var power=ShaftNetwork.resolve(server,sourcePos,side.getOpposite());if(!new PowerRequirement(128,1,4096).isSatisfiedBy(power))return false;}
        else if(energy.getAmountAsInt()<recipe.energyPerTick())return false;
        else energy.set(energy.getAmountAsInt()-recipe.energyPerTick());
        if(++progress>=recipe.duration()){input.shrink(1);if(output.isEmpty())items.set(1,result.copy());else output.grow(result.getCount());progress=0;}setChanged();return true;
    }
    @Override public int getContainerSize(){return 5;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> values){for(int i=0;i<items.size();i++)items.set(i,i<values.size()?values.get(i):ItemStack.EMPTY);}
    @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new HopperMenu(id,inventory,this);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return valid(slot,stack);}
    @Override protected void saveAdditional(ValueOutput output){super.saveAdditional(output);output.putInt("StoredEnergy",energy.getAmountAsInt());output.putInt("burn_ticks",burnTicks);output.putInt("water_ticks",waterTicks);output.putInt("progress",progress);if(activeRecipe!=null)output.putString("active_recipe",activeRecipe.toString());ContainerHelper.saveAllItems(output,items);}
    @Override protected void loadAdditional(ValueInput input){super.loadAdditional(input);energy.set(Math.max(0,Math.min(capacity(),input.getIntOr("StoredEnergy",0))));burnTicks=Math.clamp(input.getIntOr("burn_ticks",0),0,72000);waterTicks=Math.clamp(input.getIntOr("water_ticks",0),0,1000);progress=Math.clamp(input.getIntOr("progress",0),0,72000);activeRecipe=net.minecraft.resources.Identifier.tryParse(input.getStringOr("active_recipe",""));ContainerHelper.loadAllItems(input,items);}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState next){if(level!=null&&!level.isClientSide()&&next.getBlock()!=getBlockState().getBlock()){Containers.dropContents(level,worldPosition,this);CableNetwork.invalidate(level);}super.preRemoveSideEffects(pos,next);}
}
