package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;

/**
 * Miner: Baut Erz von einem Ressourcen-Vorkommen ab.
 * Nutzt MiningStrategy als Produktionsstrategie und benoetigt Kohle als
 * Brennstoff.
 */
public class Miner extends BaseMachine {

   private static final int PRODUCTIONS_PER_COAL = 8;
   private static final int MAX_COAL_BUFFER = 64;

   private int productionCredits = 0;

   public Miner(Tile tile) {
      super("Miner", tile, new MiningStrategy());
   }

   @Override
   public boolean tryInsertInput(ItemStack item) {
      if (item == null || item.getAmount() <= 0) {
         return false;
      }
      if (item.getType() != ItemType.COAL) {
         return false;
      }

      if (inputBuffer == null) {
         if (item.getAmount() > MAX_COAL_BUFFER) {
            return false;
         }
         inputBuffer = new ItemStack(ItemType.COAL, item.getAmount());
         return true;
      }

      if (inputBuffer.getType() != ItemType.COAL) {
         return false;
      }

      if (inputBuffer.getAmount() + item.getAmount() > MAX_COAL_BUFFER) {
         return false;
      }

      inputBuffer.add(item.getAmount());
      return true;
   }

   @Override
   public boolean tryInsertInputFromSide(ItemStack item, Direction incomingSide) {
      return tryInsertInput(item);
   }

   /**
    * Verbraucht Brennstoff fuer einen Produktionszyklus.
    *
    * @param producedType Der Item-Typ, den der Miner auf diesem Tile abbauen
    *                     wuerde.
    * @return true wenn dieser Zyklus produzieren darf, false wenn wegen fehlendem
    *         Brennstoff ausgesetzt werden muss.
    */
   boolean consumeFuelForCycle(ItemType producedType) {
      if (productionCredits <= 0) {
         if (!refillProductionCreditsFromInput()) {
            // Kohle-Miner versorgen sich selbst: ein Abbauzyklus dient als Treibstoff,
            // produziert aber kein Output-Item.
            if (producedType == ItemType.COAL) {
               productionCredits = PRODUCTIONS_PER_COAL;
            }
            return false;
         }
      }

      productionCredits--;
      return true;
   }

   private boolean refillProductionCreditsFromInput() {
      if (!(hasInput() && inputBuffer.getType() == ItemType.COAL)) {
         return false;
      }

      inputBuffer.remove(1);
      if (inputBuffer.getAmount() <= 0) {
         inputBuffer = null;
      }

      productionCredits = PRODUCTIONS_PER_COAL;
      return true;
   }
}
