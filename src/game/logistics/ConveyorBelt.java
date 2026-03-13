package game.logistics;

import game.world.Tile;
import game.world.TileType;

/**
 * ConveyorBelt repräsentiert ein Fließband auf einem Tile.
 * Es hat eine Richtung und transportiert Items zum nächsten Tile.
 */
public class ConveyorBelt {
   private final Tile tile;
   private final Direction direction;

   public enum Direction {
      UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

      public final int dx, dy;

      Direction(int dx, int dy) {
         this.dx = dx;
         this.dy = dy;
      }
   }

   public ConveyorBelt(Tile tile, Direction direction) {
      this.tile = tile;
      this.direction = direction;
      tile.setType(TileType.CONVEYOR_BELT);
   }

   public Tile getTile() {
      return tile;
   }

   public Direction getDirection() {
      return direction;
   }
}
