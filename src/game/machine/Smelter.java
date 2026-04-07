package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;

import java.util.List;

/**
 * Smelter: Schmilzt Erz zu Platten und verbraucht Kohle als Brennstoff.
 * 1 Kohle reicht für {@value #SMELTINGS_PER_COAL} Schmelzvorgänge.
 *
 * IO-Slots:
 * - inputBuffer: Erz (IRON_ORE / COPPER_ORE)
 * - coalUnits: Kohle-Vorrat (separater Brennstoff-Slot, kein inputBuffer)
 * - outputBuffer: produzierte Platten
 */
public class Smelter extends BaseMachine {

   static final int SMELTINGS_PER_COAL = 8;
   private static final int MAX_COAL_BUFFER = 32;

   private int coalUnits = 0;
   private int smeltingCredits = 0;

   public Smelter(Tile tile) {
      super("Smelter", tile, new SmeltingStrategy());
   }

   @Override
   public boolean tryInsertInput(ItemStack item) {
      if (item == null || item.getAmount() <= 0)
         return false;

      if (item.getType() == ItemType.COAL) {
         return tryInsertCoal(item.getAmount());
      }

      if (item.getType() == ItemType.IRON_ORE || item.getType() == ItemType.COPPER_ORE) {
         return super.tryInsertInput(item);
      }

      return false;
   }

   @Override
   public boolean tryInsertInputFromSide(ItemStack item, Direction incomingSide) {
      return tryInsertInput(item);
   }

   /**
    * Versucht Brennstoff für einen Schmelzvorgang zu verbrauchen.
    *
    * @return true wenn der Zyklus produzieren darf, false bei fehlendem Brennstoff
    */
   boolean consumeFuelForCycle() {
      if (smeltingCredits <= 0) {
         if (coalUnits <= 0)
            return false;
         coalUnits--;
         smeltingCredits = SMELTINGS_PER_COAL;
      }
      smeltingCredits--;
      return true;
   }

   /**
    * Gibt an, ob genug Brennstoff für mindestens einen weiteren Schmelzvorgang
    * vorhanden ist.
    */
   public boolean hasFuel() {
      return smeltingCredits > 0 || coalUnits > 0;
   }

   public int getCoalUnits() {
      return coalUnits;
   }

   public int getSmeltingCredits() {
      return smeltingCredits;
   }

   @Override
   public List<ItemStack> drainStoredItems() {
      List<ItemStack> drained = super.drainStoredItems();
      if (coalUnits > 0) {
         drained.add(new ItemStack(ItemType.COAL, coalUnits));
      }
      coalUnits = 0;
      smeltingCredits = 0;
      return drained;
   }

   /**
    * Gibt an, ob der Smelter aktiv arbeitet (für Animation und Pollution).
    * Inaktiv bei fehlendem Brennstoff, vollem Output-Buffer oder fehlendem
    * Erz-Input.
    */
   @Override
   public boolean isActiveForAnimation() {
      if (!hasFuel())
         return false;

      if (isOutputFull())
         return false;

      if (!hasInput())
         return false;

      ItemType t = inputBuffer.getType();
      return t == ItemType.IRON_ORE || t == ItemType.COPPER_ORE;
   }

   private boolean tryInsertCoal(int amount) {
      if (amount <= 0)
         return false;
      if (coalUnits + amount > MAX_COAL_BUFFER)
         return false;
      coalUnits += amount;
      return true;
   }
}
