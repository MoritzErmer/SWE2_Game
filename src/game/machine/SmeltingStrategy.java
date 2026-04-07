package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;

/**
 * SmeltingStrategy: Schmilzt Erz-Items aus dem Input-Buffer zu Platten im
 * Output-Buffer.
 * Der Smelter nimmt Erz aus dem Input, verarbeitet es und legt Platten in den
 * Output. Benötigt Kohle als Brennstoff (verwaltet im Smelter).
 * Greifer füllen den Input und leeren den Output.
 */
public class SmeltingStrategy implements ProductionStrategy {

   private static final int MAX_OUTPUT = 5;

   @Override
   public void produce(BaseMachine machine) {
      // Output-Buffer-Limit prüfen
      if (machine.hasOutput() && machine.getOutputBuffer().getAmount() >= MAX_OUTPUT) {
         return;
      }

      // Input prüfen
      if (!machine.hasInput())
         return;

      ItemStack input = machine.getInputBuffer();
      ItemType output = smelt(input.getType());
      if (output == null)
         return; // Nicht schmelzbar

      // Brennstoff prüfen und verbrauchen
      if (machine instanceof Smelter) {
         Smelter smelter = (Smelter) machine;
         if (!smelter.consumeFuelForCycle()) {
            return;
         }
      }

      // 2 Erz verbrauchen (wie manuelles Rezept)
      if (input.getAmount() < 2) {
         return;
      }
      input.remove(2);
      if (input.getAmount() <= 0) {
         machine.setInputBuffer(null);
      }

      // Platte in Output-Buffer legen
      if (machine.getOutputBuffer() == null) {
         machine.setOutputBuffer(new ItemStack(output, 1));
      } else if (machine.getOutputBuffer().getType() == output) {
         machine.getOutputBuffer().add(1);
      }
   }

   /**
    * Mappt Erz-Typen auf geschmolzene Platten.
    */
   private ItemType smelt(ItemType input) {
      switch (input) {
         case IRON_ORE:
            return ItemType.IRON_PLATE;
         case COPPER_ORE:
            return ItemType.COPPER_PLATE;
         default:
            return null;
      }
   }
}
