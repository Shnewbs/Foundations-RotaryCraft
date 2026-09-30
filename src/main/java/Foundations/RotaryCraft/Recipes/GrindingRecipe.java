package Foundations.RotaryCraft.Recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record GrindingRecipe(Ingredient ingredient, ItemStack result, int duration, int energyPerTick)
        implements Recipe<SingleRecipeInput> {
    public GrindingRecipe {
        if (ingredient.isEmpty() || result.isEmpty() || duration < 1 || duration > 72000
                || energyPerTick < 1 || energyPerTick > 100000) {
            throw new IllegalArgumentException("Invalid grinding recipe");
        }
        result = result.copy();
    }
    @Override public ItemStack result() { return result.copy(); }
    @Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
    @Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
    @Override public RecipeSerializer<?> getSerializer() { return RecipeContent.GRINDING_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return RecipeContent.GRINDING_TYPE.get(); }
    @Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }
    @Override public boolean isSpecial() { return true; }

    public static final class Serializer implements RecipeSerializer<GrindingRecipe> {
        private static final MapCodec<GrindingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(GrindingRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(GrindingRecipe::result),
                Codec.intRange(1, 72000).optionalFieldOf("duration", 100).forGetter(GrindingRecipe::duration),
                Codec.intRange(1, 100000).optionalFieldOf("energy_per_tick", 1).forGetter(GrindingRecipe::energyPerTick)
        ).apply(instance, GrindingRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, GrindingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, GrindingRecipe::ingredient,
                ItemStack.STREAM_CODEC, GrindingRecipe::result,
                ByteBufCodecs.VAR_INT, GrindingRecipe::duration,
                ByteBufCodecs.VAR_INT, GrindingRecipe::energyPerTick,
                GrindingRecipe::new);
        @Override public MapCodec<GrindingRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, GrindingRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
