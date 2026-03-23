package game.crafting;

import game.entity.ItemType;
import game.entity.PlayerCharacter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftingManagerTest {

    @Test
    void craftConsumesIngredientsAndAddsResult() {
        CraftingManager manager = new CraftingManager();
        PlayerCharacter player = new PlayerCharacter(0, 0);
        player.addItem(ItemType.IRON_ORE, 2);
        player.addItem(ItemType.COAL, 1);

        CraftingRecipe ironPlateRecipe = manager.getRecipes().stream()
                .filter(r -> "Iron Plate".equals(r.getName()))
                .findFirst()
                .orElseThrow();

        boolean crafted = manager.craft(ironPlateRecipe, player);

        assertTrue(crafted);
        assertEquals(0, player.getItemCount(ItemType.IRON_ORE));
        assertEquals(0, player.getItemCount(ItemType.COAL));
        assertEquals(1, player.getItemCount(ItemType.IRON_PLATE));
    }

    @Test
    void craftFailsWhenIngredientsMissing() {
        CraftingManager manager = new CraftingManager();
        PlayerCharacter player = new PlayerCharacter(0, 0);
        player.addItem(ItemType.IRON_ORE, 1);

        CraftingRecipe ironPlateRecipe = manager.getRecipes().stream()
                .filter(r -> "Iron Plate".equals(r.getName()))
                .findFirst()
                .orElseThrow();

        boolean crafted = manager.craft(ironPlateRecipe, player);

        assertFalse(crafted);
        assertEquals(1, player.getItemCount(ItemType.IRON_ORE));
        assertEquals(0, player.getItemCount(ItemType.IRON_PLATE));
    }
}
