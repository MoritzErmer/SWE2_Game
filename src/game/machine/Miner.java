package game.machine;

import game.world.Tile;

/**
 * Miner: Baut Erz von einem Ressourcen-Vorkommen ab.
 * Nutzt MiningStrategy als Produktionsstrategie.
 */
public class Miner extends BaseMachine {

   public Miner(Tile tile) {
      super("Miner", tile, new MiningStrategy());
   }
}
