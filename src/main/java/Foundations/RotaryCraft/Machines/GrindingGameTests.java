package Foundations.RotaryCraft.Machines;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import Foundations.RotaryCraft.Recipes.GrindingRecipe;
import Foundations.RotaryCraft.Recipes.RecipeContent;

@GameTestHolder("rotarycraft")
@PrefixGameTestTemplate(false)
public final class GrindingGameTests {
    private static GrindstoneBlockEntity place(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, MachineContent.GRINDSTONE.get());
        return (GrindstoneBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 150)
    public static void grindingConsumesExactEnergy(GameTestHelper helper) {
        var machine = place(helper);
        machine.getItemHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        machine.getEnergyStorage().receiveEnergy(100, false);
        helper.assertTrue(!machine.getEnergyStorage().canExtract(), "Grinder must not export FE");
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(machine.getItemHandler().getStackInSlot(0).isEmpty(), "Input not consumed");
            helper.assertTrue(machine.getItemHandler().getStackInSlot(1).is(Items.GRAVEL), "Wrong result");
            helper.assertTrue(machine.getItemHandler().getStackInSlot(1).getCount() == 1, "Wrong output count");
            helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == 0, "Grinding did not consume exactly 100 FE");
            helper.succeed();
        });
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 40)
    public static void fullOutputConsumesNothing(GameTestHelper helper) {
        var machine = place(helper);
        machine.getItemHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        machine.getItemHandler().setStackInSlot(1, new ItemStack(Items.GRAVEL, 64));
        machine.getEnergyStorage().receiveEnergy(100, false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == 100, "Blocked output wasted energy");
            helper.assertTrue(machine.getItemHandler().getStackInSlot(0).getCount() == 1, "Blocked output consumed input");
            helper.assertTrue(machine.getItemHandler().getStackInSlot(1).getCount() == 64, "Output overflow");
            helper.succeed();
        });
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 40)
    public static void mismatchedComponentsDoNotMerge(GameTestHelper helper) {
        var machine = place(helper);
        var named = new ItemStack(Items.GRAVEL);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Reserved"));
        machine.getItemHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        machine.getItemHandler().setStackInSlot(1, named);
        machine.getEnergyStorage().receiveEnergy(100, false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == 100, "Incompatible output wasted energy");
            helper.assertTrue(machine.getElapsed() == 0, "Incompatible output advanced");
            helper.succeed();
        });
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 40)
    public static void unknownRecipeConsumesNothing(GameTestHelper helper) {
        var machine = place(helper);
        machine.getItemHandler().setStackInSlot(0, new ItemStack(Items.DIAMOND));
        machine.getEnergyStorage().receiveEnergy(100, false);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == 100, "No-recipe input consumed power");
            helper.assertTrue(machine.getItemHandler().getStackInSlot(1).isEmpty(), "No-recipe input produced output");
            helper.succeed();
        });
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 40)
    public static void grindingProgressPersists(GameTestHelper helper) {
        var machine = place(helper);
        machine.getItemHandler().setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        machine.getEnergyStorage().receiveEnergy(100, false);
        helper.runAfterDelay(20, () -> {
            var restored = new GrindstoneBlockEntity(machine.getBlockPos(), machine.getBlockState());
            restored.loadAdditional(machine.saveWithoutMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
            helper.assertTrue(machine.getElapsed() > 0, "Original grinder did not advance");
            helper.assertTrue(restored.getElapsed() == machine.getElapsed(), "Progress did not persist");
            helper.assertTrue(restored.getEnergyStorage().getEnergyStored() == machine.getEnergyStorage().getEnergyStored(), "Energy did not persist");
            restored.setLevel(helper.getLevel());
            int oldProgress = restored.getElapsed();
            GrindstoneBlockEntity.serverTick(helper.getLevel(), restored.getBlockPos(), restored.getBlockState(), restored);
            helper.assertTrue(restored.getElapsed() == oldProgress + 1, "Restored work did not resume");
            helper.succeed();
        });
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 40)
    public static void simulationAndSlotsAreSafe(GameTestHelper helper) {
        var machine = place(helper);
        helper.assertTrue(machine.getEnergyStorage().receiveEnergy(50, true) == 50, "Simulation returned wrong amount");
        helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == 0, "Simulation mutated energy");
        helper.assertTrue(machine.getItemHandler().insertItem(1, new ItemStack(Items.GRAVEL), false).getCount() == 1, "Automation inserted into output");
        helper.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 40)
    public static void grindingCodecRoundTrips(GameTestHelper helper) {
        var serializer = RecipeContent.GRINDING_SERIALIZER.get();
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var json = JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:paper\"},\"result\":{\"id\":\"minecraft:book\",\"count\":2},\"duration\":4,\"energy_per_tick\":5}");
        GrindingRecipe recipe = serializer.codec().codec().parse(ops, json).getOrThrow();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            serializer.streamCodec().encode(buffer, recipe);
            var copy = serializer.streamCodec().decode(buffer);
            helper.assertTrue(copy.duration() == 4 && copy.energyPerTick() == 5, "Network cost changed");
            helper.assertTrue(copy.result().getCount() == 2 && copy.result().is(Items.BOOK), "Network output changed");
            helper.assertTrue(copy.matches(new SingleRecipeInput(new ItemStack(Items.PAPER)), helper.getLevel()), "Ingredient changed");
            var invalid = JsonParser.parseString("{\"ingredient\":{\"item\":\"minecraft:paper\"},\"result\":{\"id\":\"minecraft:book\"},\"duration\":0}");
            helper.assertTrue(serializer.codec().codec().parse(ops, invalid).error().isPresent(), "Zero-duration recipe accepted");
        } finally { buffer.release(); }
        helper.succeed();
    }
    @GameTest(template = "power_network_test", templateNamespace = "rotarycraft", timeoutTicks = 40)
    public static void kubeJsCustomRecipeWorks(GameTestHelper helper) {
        if (!ModList.get().isLoaded("kubejs")) { helper.succeed(); return; }
        var recipe = helper.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("rotarycraft", "ci_scripted_grinding"));
        helper.assertTrue(recipe.isPresent() && recipe.get().value() instanceof GrindingRecipe, "KubeJS custom recipe missing");
        var machine = place(helper);
        machine.getItemHandler().setStackInSlot(0, new ItemStack(Items.PAPER));
        machine.getEnergyStorage().receiveEnergy(8, false);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(machine.getItemHandler().getStackInSlot(1).is(Items.BOOK), "KubeJS output missing");
            helper.assertTrue(machine.getItemHandler().getStackInSlot(1).getCount() == 2, "KubeJS result count not respected");
            helper.assertTrue(machine.getEnergyStorage().getEnergyStored() == 0, "KubeJS FE cost not respected");
            helper.succeed();
        });
    }
}
