package game.machine;

import game.entity.ItemType;
import game.world.Tile;

/**
 * Smelter: Schmilzt Erz zu Platten.
 * Nutzt SmeltingStrategy als Produktionsstrategie.
 */
public class Smelter extends BaseMachine {

   public Smelter(Tile tile) {
      super("Smelter", tile, new SmeltingStrategy());
   }

   /**
    * Gibt an, ob der Smelter aktiv arbeitet (für Animation).
    * Inaktiv bei vollem Output-Buffer (blockierter Pfad) oder fehlendem Erz-Input.
    */
   public boolean isActiveForAnimation() {
      // Output-Buffer voll → blockierter Pfad
      if (isOutputFull())
         return false;

      // Kein Input → nichts zu schmelzen
      if (!hasInput())
         return false;

      // Nur schmelzbare Erze aktivieren die Animation
      ItemType t = inputBuffer.getType();
      return t == ItemType.IRON_ORE || t == ItemType.COPPER_ORE;
   }
}
