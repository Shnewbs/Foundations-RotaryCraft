package Foundations.RotaryCraft.Recipes;

import Foundations.RotaryCraft.RotaryCraftNeoForge;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class RecipeContent {
  private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
      DeferredRegister.create(Registries.RECIPE_SERIALIZER, RotaryCraftNeoForge.MOD_ID);
  private static final DeferredRegister<RecipeType<?>> TYPES =
      DeferredRegister.create(Registries.RECIPE_TYPE, RotaryCraftNeoForge.MOD_ID);
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GrindingRecipe>>
      GRINDING_SERIALIZER = SERIALIZERS.register("grinding", GrindingRecipe.Serializer::create);
  public static final DeferredHolder<RecipeType<?>, RecipeType<GrindingRecipe>> GRINDING_TYPE =
      TYPES.register(
          "grinding",
          () ->
              new RecipeType<GrindingRecipe>() {
                @Override
                public String toString() {
                  return "rotarycraft:grinding";
                }
              });

  private RecipeContent() {}

  public static void register(IEventBus bus) {
    SERIALIZERS.register(bus);
    TYPES.register(bus);
  }
}
