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

    @Test
    void craftForgeRecipeConsumesIngredientsAndAddsForgeKit() {
        CraftingManager manager = new CraftingManager();
        PlayerCharacter player = new PlayerCharacter(0, 0);
        player.addItem(ItemType.IRON_GEAR, 2);
        player.addItem(ItemType.IRON_PLATE, 4);
        player.addItem(ItemType.COPPER_PLATE, 1);
        player.addItem(ItemType.COAL, 1);

        CraftingRecipe forgeRecipe = manager.getRecipes().stream()
                .filter(r -> "Forge".equals(r.getName()))
                .findFirst()
                .orElseThrow();

        boolean crafted = manager.craft(forgeRecipe, player);

        assertTrue(crafted);
        assertEquals(0, player.getItemCount(ItemType.IRON_GEAR));
        assertEquals(0, player.getItemCount(ItemType.IRON_PLATE));
        assertEquals(0, player.getItemCount(ItemType.COPPER_PLATE));
        assertEquals(0, player.getItemCount(ItemType.COAL));
        assertEquals(1, player.getItemCount(ItemType.FORGE_KIT));
    }
}
