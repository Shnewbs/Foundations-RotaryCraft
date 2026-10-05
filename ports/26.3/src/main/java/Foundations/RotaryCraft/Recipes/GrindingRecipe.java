package Foundations.RotaryCraft.Recipes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record GrindingRecipe(
    Ingredient ingredient, ItemStackTemplate output, int duration, int energyPerTick)
    implements Recipe<SingleRecipeInput> {
  public GrindingRecipe {
    if (ingredient.isEmpty()
        || output.count() < 1
        || duration < 1
        || duration > 72000
        || energyPerTick < 1
        || energyPerTick > 100000) {
      throw new IllegalArgumentException("Invalid grinding recipe");
    }
  }

  public ItemStack result() {
    return output.create();
  }

  @Override
  public boolean matches(SingleRecipeInput input, Level level) {
    return ingredient.test(input.item());
  }

  @Override
  public ItemStack assemble(SingleRecipeInput input) {
    return output.create();
  }

  @Override
  public RecipeSerializer<GrindingRecipe> getSerializer() {
    return RecipeContent.GRINDING_SERIALIZER.get();
  }

  @Override
  public RecipeType<GrindingRecipe> getType() {
    return RecipeContent.GRINDING_TYPE.get();
  }

  @Override
  public boolean isSpecial() {
    return true;
  }

  @Override
  public boolean showNotification() {
    return false;
  }

  @Override
  public String group() {
    return "";
  }

  @Override
  public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
    return net.minecraft.world.item.crafting.PlacementInfo.create(ingredient);
  }

  @Override
  public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
    return net.minecraft.world.item.crafting.RecipeBookCategories.FURNACE_MISC;
  }

  public static final class Serializer {
    private static final MapCodec<GrindingRecipe> CODEC =
        RecordCodecBuilder.mapCodec(
            instance ->
                instance
                    .group(
                        Ingredient.CODEC
                            .fieldOf("ingredient")
                            .forGetter(GrindingRecipe::ingredient),
                        ItemStackTemplate.CODEC.fieldOf("result").forGetter(GrindingRecipe::output),
                        Codec.intRange(1, 72000)
                            .optionalFieldOf("duration", 100)
                            .forGetter(GrindingRecipe::duration),
                        Codec.intRange(1, 100000)
                            .optionalFieldOf("energy_per_tick", 1)
                            .forGetter(GrindingRecipe::energyPerTick))
                    .apply(instance, GrindingRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, GrindingRecipe> STREAM_CODEC =
        StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            GrindingRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC,
            GrindingRecipe::output,
            ByteBufCodecs.VAR_INT,
            GrindingRecipe::duration,
            ByteBufCodecs.VAR_INT,
            GrindingRecipe::energyPerTick,
            GrindingRecipe::new);

    public static RecipeSerializer<GrindingRecipe> create() {
      return new RecipeSerializer<>(CODEC, STREAM_CODEC);
    }
  }
}
