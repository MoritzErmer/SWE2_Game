package game.entity;

/**
 * Repräsentiert einen Stapel von Items eines Typs.
 */
public class ItemStack {
   private final ItemType type;
   private int amount;

   public ItemStack(ItemType type, int amount) {
      this.type = type;
      this.amount = amount;
   }

   public ItemType getType() {
      return type;
   }

   public int getAmount() {
      return amount;
   }

   public void add(int count) {
      amount += count;
   }

   public void remove(int count) {
      amount = Math.max(0, amount - count);
   }
}
