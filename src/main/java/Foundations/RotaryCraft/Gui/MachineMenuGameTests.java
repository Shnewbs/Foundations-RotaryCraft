package Foundations.RotaryCraft.Gui;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import Foundations.RotaryCraft.Machines.*;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class MachineMenuGameTests {
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void menuMovesRealItemsAndRejectsDistantUse(GameTestHelper helper){
        var pos=new BlockPos(1,1,1);helper.setBlock(pos,MachineContent.GRINDSTONE.get());
        var entity=(GrindstoneBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolute=entity.getBlockPos();player.setPos(absolute.getX()+.5,absolute.getY()+.5,absolute.getZ()+.5);
        var menu=new MachineMenu(1,player.getInventory(),entity);
        player.getInventory().setItem(9,new ItemStack(Items.COBBLESTONE,12));
        menu.quickMoveStack(player,menu.machineSlots);
        helper.assertTrue(entity.getItemHandler().getStackInSlot(0).getCount()==12,"Menu did not insert into actual inventory");
        helper.assertTrue(player.getInventory().getItem(9).isEmpty(),"Menu duplicated inserted items");
        entity.getItemHandler().setStackInSlot(1,new ItemStack(Items.GRAVEL,7));
        helper.assertTrue(menu.quickMoveStack(player,1).getCount()==7,"GUI could not extract output");
        helper.assertTrue(entity.getItemHandler().getStackInSlot(1).isEmpty(),"GUI output was duplicated");
        player.setPos(absolute.getX()+20,absolute.getY(),absolute.getZ());
        helper.assertTrue(!menu.stillValid(player),"Menu stayed valid outside reach");
        helper.assertTrue(!menu.clickMenuButton(player,0),"Distant player rotated machine");
        helper.succeed();
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void sorterFiltersAreGhostsAndStatusPreserves32Bits(GameTestHelper helper){
        var pos=new BlockPos(1,1,1);helper.setBlock(pos,MachineContent.SORTING.get());
        var entity=(SortingBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var absolute=entity.getBlockPos();player.setPos(absolute.getX()+.5,absolute.getY()+.5,absolute.getZ()+.5);
        var menu=new MachineMenu(1,player.getInventory(),entity);
        menu.setCarried(new ItemStack(Items.DIAMOND,12));menu.clicked(1,0,ClickType.PICKUP,player);
        helper.assertTrue(entity.getFilters().getStackInSlot(0).getCount()==1,"Ghost filter was not set");
        helper.assertTrue(menu.getCarried().getCount()==12,"Ghost filter consumed held items");
        helper.assertTrue(menu.quickMoveStack(player,1).isEmpty(),"Ghost filter could be extracted");
        menu.setCarried(ItemStack.EMPTY);menu.clicked(1,0,ClickType.PICKUP,player);
        helper.assertTrue(entity.getFilters().getStackInSlot(0).isEmpty(),"Ghost filter could not be cleared");
        helper.assertTrue(menu.value(1)==50000,"Capacity was truncated to a signed short");
        player.getInventory().setItem(9,new ItemStack(Items.COAL,64));menu.quickMoveStack(player,menu.machineSlots);
        helper.assertTrue(entity.getInput().getStackInSlot(0).getCount()==64,"Shift-click skipped actual sorter input");
        helper.assertTrue(entity.getFilters().getStackInSlot(0).isEmpty(),"Shift-click inserted real items into ghost filters");
        helper.succeed();
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void tankSupportsPipesSimulationAndPersistence(GameTestHelper helper){
        var pos=new BlockPos(1,1,1);helper.setBlock(pos,MachineContent.DECO_TANK.get());
        var absolute=helper.absolutePos(pos);var entity=(DecoTankBlockEntity)helper.getLevel().getBlockEntity(absolute);
        var handler=helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,absolute,net.minecraft.core.Direction.UP);
        helper.assertTrue(handler!=null,"Tank has no pipe capability");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER,20000),FluidAction.SIMULATE)==16000,"Tank simulation capacity is wrong");
        helper.assertTrue(entity.isEmpty(),"Simulation changed tank contents");
        handler.fill(new FluidStack(Fluids.WATER,20000),FluidAction.EXECUTE);
        helper.assertTrue(entity.getFluidLevel()==15,"Full tank comparator did not update");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE)==0,"Tank mixed fluids");
        var saved=entity.saveWithoutMetadata(helper.getLevel().registryAccess());
        var restored=new DecoTankBlockEntity(absolute,entity.getBlockState());restored.loadWithComponents(saved,helper.getLevel().registryAccess());
        helper.assertTrue(restored.getFluidAmount()==16000&&restored.getFluidName().equals("minecraft:water"),"Tank lost persisted fluid");
        helper.assertTrue(handler.drain(1000,FluidAction.EXECUTE).getAmount()==1000&&entity.getFluidAmount()==15000,"Pipe drain lost fluid conservation");
        helper.succeed();
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void partialMachinesDoNotOccludeAndGeneratorsHaveFacing(GameTestHelper helper){
        for(var block:java.util.List.of(Foundations.RotaryCraft.Power.PowerContent.STEAM_GENERATOR.get(),Foundations.RotaryCraft.Power.PowerContent.HYDRO_GENERATOR.get(),MachineContent.FAN.get())) {
            var state=block.defaultBlockState();helper.assertTrue(!state.canOcclude(),"Partial machine occludes moving-part lighting");
            helper.assertTrue(state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING),"Machine cannot face placement direction");
        }
        helper.succeed();
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void generatorPlacementFacesPlayerAndRotationUsesMenu(GameTestHelper helper){
        var player=helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var pos=helper.absolutePos(new BlockPos(1,1,1));player.setPos(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);
        for(var block:java.util.List.of(Foundations.RotaryCraft.Power.PowerContent.POWER_GENERATOR.get(),Foundations.RotaryCraft.Power.PowerContent.STEAM_GENERATOR.get(),Foundations.RotaryCraft.Power.PowerContent.GEOTHERMAL_GENERATOR.get(),Foundations.RotaryCraft.Power.PowerContent.HYDRO_GENERATOR.get(),Foundations.RotaryCraft.Power.PowerContent.WIND_GENERATOR.get(),Foundations.RotaryCraft.Power.PowerContent.SOLAR_GENERATOR.get()))for(float yaw:new float[]{0,90,180,270}) {
            player.setYRot(yaw);
            var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos.below()),net.minecraft.core.Direction.UP,pos.below(),false);
            var context=new net.minecraft.world.item.context.BlockPlaceContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(block),hit);
            var state=block.getStateForPlacement(context);
            helper.assertTrue(state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)==context.getHorizontalDirection().getOpposite(),"Generator ignored player's facing");
        }
        helper.setBlock(new BlockPos(1,1,1),Foundations.RotaryCraft.Power.PowerContent.STEAM_GENERATOR.get());
        var entity=helper.getLevel().getBlockEntity(pos);var menu=new MachineMenu(1,player.getInventory(),entity);
        var previous=entity.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        helper.assertTrue(menu.clickMenuButton(player,0),"In-range rotation was refused");
        helper.assertTrue(entity.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)==previous.getClockWise(),"Rotate button changed to wrong facing");
        helper.succeed();
    }
    @GameTest(template="power_network_test",templateNamespace="rotarycraft")
    public static void breakingSorterDropsInputButNeverGhostFilters(GameTestHelper helper){
        var pos=new BlockPos(1,1,1);helper.setBlock(pos,MachineContent.SORTING.get());
        var entity=(SortingBlockEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        entity.getInput().setStackInSlot(0,new ItemStack(Items.COAL,12));entity.getFilters().setStackInSlot(0,new ItemStack(Items.DIAMOND));
        helper.getLevel().setBlock(entity.getBlockPos(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
        var drops=helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(entity.getBlockPos()).inflate(1));
        int coal=drops.stream().filter(item->item.getItem().is(Items.COAL)).mapToInt(item->item.getItem().getCount()).sum();
        helper.assertTrue(coal==12,"Breaking sorter lost or duplicated stored input");
        helper.assertTrue(drops.stream().noneMatch(item->item.getItem().is(Items.DIAMOND)),"Breaking sorter duplicated a ghost filter");
        helper.succeed();
    }
}
