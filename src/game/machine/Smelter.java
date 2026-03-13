package game.machine;

import game.world.Tile;

/**
 * Smelter: Schmilzt Erz zu Platten.
 * Nutzt SmeltingStrategy als Produktionsstrategie.
 */
public class Smelter extends BaseMachine {

   public Smelter(Tile tile) {
      super("Smelter", tile, new SmeltingStrategy());
   }
}
