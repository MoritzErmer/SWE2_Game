package game.crafting;

import game.entity.ItemType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Repräsentiert ein einzelnes Crafting-Rezept.
 * Definiert benötigte Zutaten (ItemType → Menge) und das Ergebnis.
 */
public class CraftingRecipe {
   private final String name;
   private final Map<ItemType, Integer> ingredients;
   private final ItemType result;
   private final int resultAmount;

   private CraftingRecipe(String name, Map<ItemType, Integer> ingredients,
         ItemType result, int resultAmount) {
      this.name = name;
      this.ingredients = Collections.unmodifiableMap(ingredients);
      this.result = result;
      this.resultAmount = resultAmount;
   }

   public String getName() {
      return name;
   }

   public Map<ItemType, Integer> getIngredients() {
      return ingredients;
   }

   public ItemType getResult() {
      return result;
   }

   public int getResultAmount() {
      return resultAmount;
   }

   // ==================== Builder ====================

   public static Builder builder(String name) {
      return new Builder(name);
   }

   public static class Builder {
      private final String name;
      private final Map<ItemType, Integer> ingredients = new LinkedHashMap<>();
      private ItemType result;
      private int resultAmount = 1;

      private Builder(String name) {
         this.name = name;
      }

      public Builder ingredient(ItemType type, int amount) {
         ingredients.put(type, amount);
         return this;
      }

      public Builder result(ItemType type, int amount) {
         this.result = type;
         this.resultAmount = amount;
         return this;
      }

      public CraftingRecipe build() {
         if (result == null)
            throw new IllegalStateException("Result must be set");
         return new CraftingRecipe(name, ingredients, result, resultAmount);
      }
   }
}
