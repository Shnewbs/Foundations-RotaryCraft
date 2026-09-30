package Foundations.RotaryCraft.Machines;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;
import Foundations.RotaryCraft.Recipes.GrindingRecipe;
import Foundations.RotaryCraft.Recipes.RecipeContent;

/** Material processing under the preserved grindstone save ID; tool-repair parity is separate. */
public class GrindstoneBlockEntity extends BlockEntity {
    public static final Foundations.RotaryCraft.Mechanical.PowerRequirement MECHANICAL_REQUIREMENT =
            new Foundations.RotaryCraft.Mechanical.PowerRequirement(128, 1, 4096);
    public record MechanicalInput(boolean connected, Foundations.RotaryCraft.Mechanical.ShaftPower power) {}
    private MechanicalInput shaft = new MechanicalInput(false, Foundations.RotaryCraft.Mechanical.ShaftPower.STOPPED);
    private static final int MAX_ENERGY = 100000;
    private final MachineEnergy energyStorage = new MachineEnergy();
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot == 0; }
        @Override protected void onContentsChanged(int slot) { GrindstoneBlockEntity.this.setChanged(); }
    };
    private int elapsed;
    private int duration;
    private ResourceLocation activeRecipe;
    private GrindingRecipe activeDefinition;
    private boolean operating;

    public GrindstoneBlockEntity(BlockPos pos, BlockState state) {
        super(MachineContent.GRINDSTONE_BLOCK_ENTITY.get(), pos, state);
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, GrindstoneBlockEntity entity) {
        if (!level.isClientSide) entity.tryGrind();
    }
    private void tryGrind() {
        if (level == null) return;
        operating = false;
        shaft = readMechanicalInput();
        ItemStack input = itemHandler.getStackInSlot(0);
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        // Stable ID ordering avoids nondeterministic overlapping tag recipes.
        var matched = level.getRecipeManager().getAllRecipesFor(RecipeContent.GRINDING_TYPE.get()).stream()
                .filter(holder -> holder.value().matches(recipeInput, level))
                .min(Comparator.comparing(holder -> holder.id().toString()));
        if (input.isEmpty() || matched.isEmpty()) { resetProgress(); return; }
        RecipeHolder<GrindingRecipe> holder = matched.get();
        GrindingRecipe recipe = holder.value();
        // A reload changes the recipe instance; never complete with stale progress or output.
        if (!holder.id().equals(activeRecipe) || (activeDefinition != null && activeDefinition != recipe)) {
            resetProgress();
            activeRecipe = holder.id();
        }
        activeDefinition = recipe;
        duration = recipe.duration();
        ItemStack result = recipe.assemble(recipeInput, level.registryAccess());
        ItemStack output = itemHandler.getStackInSlot(1);
        if (!canAccept(output, result)) return;
        if (shaft.connected()) {
            if (!MECHANICAL_REQUIREMENT.isSatisfiedBy(shaft.power())) return;
        } else {
            if (energyStorage.getEnergyStored() < recipe.energyPerTick()) return;
            energyStorage.consume(recipe.energyPerTick());
        }
        operating = true;
        elapsed++;
        if (elapsed >= duration) {
            itemHandler.setStackInSlot(1, output.isEmpty() ? result : output.copyWithCount(output.getCount() + result.getCount()));
            itemHandler.extractItem(0, 1, false);
            elapsed = 0;
        }
        setChanged();
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }
    protected MechanicalInput readMechanicalInput() {
        if (level == null || level.isClientSide) return new MechanicalInput(false, Foundations.RotaryCraft.Mechanical.ShaftPower.STOPPED);
        var inputSide = getBlockState().getValue(GrindstoneBlock.FACING).getOpposite();
        var pos = worldPosition.relative(inputSide);
        var outputSide = inputSide.getOpposite();
        boolean connected = level.hasChunkAt(pos) && level.getCapability(Foundations.RotaryCraft.Mechanical.ShaftNetwork.CAPABILITY, pos, outputSide) != null;
        var power = connected ? Foundations.RotaryCraft.Mechanical.ShaftNetwork.resolve(level, pos, outputSide)
                : Foundations.RotaryCraft.Mechanical.ShaftPower.STOPPED;
        return new MechanicalInput(connected, power);
    }
    public MechanicalInput getMechanicalInput() { return shaft; }
    private boolean canAccept(ItemStack output, ItemStack result) {
        int limit = Math.min(itemHandler.getSlotLimit(1), result.getMaxStackSize());
        return result.getCount() <= limit && (output.isEmpty()
                || (ItemStack.isSameItemSameComponents(output, result) && output.getCount() <= limit - result.getCount()));
    }
    private void resetProgress() {
        boolean changed = elapsed != 0 || duration != 0 || activeRecipe != null;
        elapsed = 0; duration = 0; activeRecipe = null; activeDefinition = null;
        if (changed) {
            setChanged();
            if (level != null) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("energy")) energyStorage.restore(tag.getInt("energy"));
        if (tag.contains("items")) itemHandler.deserializeNBT(registries, tag.getCompound("items"));
        duration = Math.max(0, Math.min(72000, tag.getInt("grind_duration")));
        elapsed = Math.max(0, Math.min(Math.max(0, duration - 1), tag.getInt("grind_elapsed")));
        activeRecipe = ResourceLocation.tryParse(tag.getString("grind_recipe"));
        activeDefinition = null;
        operating = false;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("energy", energyStorage.serializeNBT(registries));
        tag.put("items", itemHandler.serializeNBT(registries));
        tag.putInt("grind_elapsed", elapsed);
        tag.putInt("grind_duration", duration);
        if (activeRecipe != null) tag.putString("grind_recipe", activeRecipe.toString());
    }
    public EnergyStorage getEnergyStorage() { return energyStorage; }
    public ItemStackHandler getItemHandler() { return itemHandler; }
    public float getGrindProgress() { return duration > 0 ? Math.min(1F, elapsed / (float) duration) : 0F; }
    public int getElapsed() { return elapsed; }
    public int getDuration() { return duration; }
    public boolean isOperating() { return operating; }
    private final class MachineEnergy extends EnergyStorage {
        private MachineEnergy() { super(MAX_ENERGY, 100, 0); }
        @Override public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (!simulate && received > 0) setChanged();
            return received;
        }
        private void restore(int amount) { energy = Math.max(0, Math.min(MAX_ENERGY, amount)); }
        private void consume(int amount) { energy -= amount; setChanged(); }
    }
}
