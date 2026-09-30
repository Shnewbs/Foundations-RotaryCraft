package Foundations.RotaryCraft.Compat.Jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import Foundations.RotaryCraft.Machines.MachineContent;
import Foundations.RotaryCraft.Recipes.GrindingRecipe;
import Foundations.RotaryCraft.Recipes.RecipeContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

@JeiPlugin
public final class RotaryJeiPlugin implements IModPlugin {
    public static final RecipeType<GrindingRecipe> GRINDING = RecipeType.create("rotarycraft", "grinding", GrindingRecipe.class);
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("rotarycraft", "jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new GrindingCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(MachineContent.GRINDSTONE.get()), GRINDING);
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) registration.addRecipes(GRINDING,
                level.getRecipeManager().getAllRecipesFor(RecipeContent.GRINDING_TYPE.get()).stream()
                        .map(holder -> holder.value()).toList());
    }
    private static final class GrindingCategory implements IRecipeCategory<GrindingRecipe> {
        private final IDrawable icon;
        private GrindingCategory(IGuiHelper helper) {
            icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(MachineContent.GRINDSTONE.get()));
        }
        @Override public RecipeType<GrindingRecipe> getRecipeType() { return GRINDING; }
        @Override public Component getTitle() { return Component.translatable("jei.rotarycraft.grinding"); }
        @Override public IDrawable getIcon() { return icon; }
        @Override public int getWidth() { return 150; }
        @Override public int getHeight() { return 64; }
        @Override public void setRecipe(IRecipeLayoutBuilder builder, GrindingRecipe recipe, IFocusGroup focuses) {
            builder.addSlot(RecipeIngredientRole.INPUT, 20, 4).addIngredients(recipe.ingredient());
            builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 4).addItemStack(recipe.result());
        }
        @Override public void draw(GrindingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
            var font = Minecraft.getInstance().font;
            graphics.drawString(font, "->", 68, 8, 0xFF555555, false);
            graphics.drawString(font, Component.translatable("jei.rotarycraft.duration", recipe.duration()), 4, 30, 0xFF555555, false);
            graphics.drawString(font, Component.translatable("jei.rotarycraft.energy", recipe.energyPerTick()), 4, 44, 0xFF555555, false);
        }
    }
}
