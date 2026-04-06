package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;
import game.world.TileType;

/**
 * MiningStrategy: Produziert Erz basierend auf dem ursprünglichen
 * Ressourcen-Typ des Tiles.
 * Der Miner legt das produzierte Item in seinen Output-Buffer.
 * Ein Greifer kann den Output-Buffer dann entleeren.
 */
public class MiningStrategy implements ProductionStrategy {

   private static final int MAX_OUTPUT = 5; // Max Items im Output-Buffer

   @Override
   public void produce(BaseMachine machine) {
      Tile tile = machine.getTile();

      // Output-Buffer-Limit prüfen
      if (machine.hasOutput() && machine.getOutputBuffer().getAmount() >= MAX_OUTPUT) {
         return; // Buffer voll, warten bis Greifer abholt
      }

      // Nutze originalType um den Deposit-Typ zu ermitteln (type ist MACHINE)
      ItemType produced = mapToItem(tile.getOriginalType());
      if (produced == null)
         return;

      // Miner benötigt Kohle als Brennstoff. Auf Kohle-Vorkommen kann er sich
      // bei Bedarf selbst versorgen.
      if (machine instanceof Miner) {
         Miner miner = (Miner) machine;
         if (!miner.consumeFuelForCycle(produced)) {
            return;
         }
      }

      // In Output-Buffer legen
      if (machine.getOutputBuffer() == null) {
         machine.setOutputBuffer(new ItemStack(produced, 1));
      } else if (machine.getOutputBuffer().getType() == produced) {
         machine.getOutputBuffer().add(1);
      }
   }

   /**
    * Mappt einen TileType auf den entsprechenden Item-Typ.
    */
   private ItemType mapToItem(TileType type) {
      switch (type) {
         case IRON_DEPOSIT:
            return ItemType.IRON_ORE;
         case COPPER_DEPOSIT:
            return ItemType.COPPER_ORE;
         case COAL_DEPOSIT:
            return ItemType.COAL;
         default:
            return null;
      }
   }
}
