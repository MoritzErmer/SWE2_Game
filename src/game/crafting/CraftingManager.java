package game.crafting;

import game.entity.ItemType;
import game.entity.PlayerCharacter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Verwaltet alle verfügbaren Crafting-Rezepte und führt Crafting-Vorgänge
 * durch.
 * Prüft ob der Spieler genügend Materialien im Inventar hat und gibt das
 * Ergebnis.
 */
public class CraftingManager {
   private final List<CraftingRecipe> recipes;

   /** When true, all recipes can be crafted regardless of ingredients. */
   private volatile boolean creativeMode = false;

   public void setCreativeMode(boolean creative) {
      this.creativeMode = creative;
   }

   public boolean isCreativeMode() {
      return creativeMode;
   }

   public CraftingManager() {
      this.recipes = new ArrayList<>();
      registerDefaultRecipes();
   }

   /**
    * Registriert die Standard-Rezepte.
    */
   private void registerDefaultRecipes() {
      // Eisenplatten: 2 Eisenerz + 1 Kohle → 1 Eisenplatte
      recipes.add(CraftingRecipe.builder("Iron Plate")
            .ingredient(ItemType.IRON_ORE, 2)
            .ingredient(ItemType.COAL, 1)
            .result(ItemType.IRON_PLATE, 1)
            .build());

      // Kupferplatten: 2 Kupfererz + 1 Kohle → 1 Kupferplatte
      recipes.add(CraftingRecipe.builder("Copper Plate")
            .ingredient(ItemType.COPPER_ORE, 2)
            .ingredient(ItemType.COAL, 1)
            .result(ItemType.COPPER_PLATE, 1)
            .build());

      // Bulk-Eisenplatten: 6 Eisenerz + 3 Kohle → 4 Eisenplatten
      recipes.add(CraftingRecipe.builder("Iron Plates (Bulk)")
            .ingredient(ItemType.IRON_ORE, 6)
            .ingredient(ItemType.COAL, 3)
            .result(ItemType.IRON_PLATE, 4)
            .build());

      // Bulk-Kupferplatten: 6 Kupfererz + 3 Kohle → 4 Kupferplatten
      recipes.add(CraftingRecipe.builder("Copper Plates (Bulk)")
            .ingredient(ItemType.COPPER_ORE, 6)
            .ingredient(ItemType.COAL, 3)
            .result(ItemType.COPPER_PLATE, 4)
            .build());

      // Eisenzahnrad: 2 Eisenplatten → 1 Zahnrad
      recipes.add(CraftingRecipe.builder("Iron Gear")
            .ingredient(ItemType.IRON_PLATE, 8)
            .result(ItemType.IRON_GEAR, 1)
            .build());

      // Förderband: 1 Eisenzahnrad + 1 Eisenplatte → 2 Förderbänder
      recipes.add(CraftingRecipe.builder("Conveyor Belt")
            .ingredient(ItemType.IRON_GEAR, 1)
            .ingredient(ItemType.IRON_PLATE, 1)
            .result(ItemType.CONVEYOR_BELT_ITEM, 2)
            .build());

      // Miner: 2 Eisenzahnräder + 3 Eisenplatten + 1 Kupferplatte → 1 Miner
      recipes.add(CraftingRecipe.builder("Miner")
            .ingredient(ItemType.IRON_GEAR, 2)
            .ingredient(ItemType.IRON_PLATE, 3)
            .ingredient(ItemType.COPPER_PLATE, 1)
            .result(ItemType.MINER_KIT, 1)
            .build());

      // Smelter: 2 Eisenzahnräder + 4 Eisenplatten + 2 Kupferplatten → 1 Smelter
      recipes.add(CraftingRecipe.builder("Smelter")
            .ingredient(ItemType.IRON_GEAR, 2)
            .ingredient(ItemType.IRON_PLATE, 4)
            .ingredient(ItemType.COPPER_PLATE, 2)
            .result(ItemType.SMELTER_KIT, 1)
            .build());

      // Greifer: 1 Eisenzahnrad + 1 Eisenplatte + 1 Kupferplatte → 1 Greifer
      recipes.add(CraftingRecipe.builder("Grabber")
            .ingredient(ItemType.IRON_GEAR, 1)
            .ingredient(ItemType.IRON_PLATE, 1)
            .ingredient(ItemType.COPPER_PLATE, 1)
            .result(ItemType.GRABBER_KIT, 1)
            .build());

      // Forge: 2 Eisenzahnräder + 4 Eisenplatten + 1 Kupferplatte + 1 Kohle -> 1
      // Forge
      recipes.add(CraftingRecipe.builder("Forge")
         .ingredient(ItemType.IRON_GEAR, 2)
         .ingredient(ItemType.IRON_PLATE, 4)
         .ingredient(ItemType.COPPER_PLATE, 1)
         .ingredient(ItemType.COAL, 1)
         .result(ItemType.FORGE_KIT, 1)
         .build());
   }

   /**
    * Gibt die Liste aller Rezepte zurück.
    */
   public List<CraftingRecipe> getRecipes() {
      return Collections.unmodifiableList(recipes);
   }

   /**
    * Prüft ob der Spieler die benötigten Zutaten für ein Rezept besitzt.
    * Im Kreativmodus wird immer true zurückgegeben.
    */
   public boolean canCraft(CraftingRecipe recipe, PlayerCharacter player) {
      if (creativeMode) return true;
      for (Map.Entry<ItemType, Integer> entry : recipe.getIngredients().entrySet()) {
         if (player.getItemCount(entry.getKey()) < entry.getValue()) {
            return false;
         }
      }
      return true;
   }

   /**
    * Führt ein Crafting-Rezept aus: Entfernt Zutaten und fügt Ergebnis hinzu.
    * Im Kreativmodus werden keine Zutaten verbraucht.
    *
    * @return true wenn erfolgreich, false wenn Zutaten fehlen oder Inventar voll.
    */
   public boolean craft(CraftingRecipe recipe, PlayerCharacter player) {
      if (!canCraft(recipe, player))
         return false;

      if (!creativeMode) {
         // Zutaten entfernen
         for (Map.Entry<ItemType, Integer> entry : recipe.getIngredients().entrySet()) {
            if (!player.removeItem(entry.getKey(), entry.getValue())) {
               // Sollte nicht passieren (canCraft hat geprüft), aber sicherheitshalber
               return false;
            }
         }
      }

      // Ergebnis hinzufügen
      return player.addItem(recipe.getResult(), recipe.getResultAmount());
   }

   /**
    * Fügt ein neues Rezept zur Laufzeit hinzu.
    */
   public void addRecipe(CraftingRecipe recipe) {
      recipes.add(recipe);
   }
}
