package Foundations.RotaryCraft.Machines;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Simple recipe system for the grindstone.
 * Maps input items to their ground output products.
 */
public class GrindstoneRecipes {
    
    private static final Map<ItemStack, ItemStack> RECIPES = new HashMap<>();
    
    static {
        // Vanilla grinding recipes
        addRecipe(new ItemStack(Items.COBBLESTONE), new ItemStack(Items.GRAVEL));
        addRecipe(new ItemStack(Items.GRAVEL), new ItemStack(Items.SAND));
        addRecipe(new ItemStack(Items.SAND), new ItemStack(Items.SAND)); // Identity for now
        
        // Ore -> dust recipes can be added here
        // Example: addRecipe(new ItemStack(Items.RAW_IRON), new ItemStack(RotaryCraftItems.IRON_DUST, 2));
    }
    
    private static void addRecipe(ItemStack input, ItemStack output) {
        RECIPES.put(input, output);
    }
    
    /**
     * Get the grinding result for an input item.
     * Returns an empty ItemStack if no recipe exists.
     */
    public static ItemStack grind(ItemStack input) {
        for (Map.Entry<ItemStack, ItemStack> entry : RECIPES.entrySet()) {
            if (ItemStack.isSameItem(entry.getKey(), input)) {
                return entry.getValue().copy();
            }
        }
        return ItemStack.EMPTY;
    }
}
