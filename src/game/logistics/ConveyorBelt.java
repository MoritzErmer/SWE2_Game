package game.logistics;

import game.world.Tile;
import game.world.TileType;

/**
 * ConveyorBelt repräsentiert ein Fließband auf einem Tile.
 * Es hat eine Richtung und transportiert Items zum nächsten Tile.
 */
public class ConveyorBelt {
   private final Tile tile;
   private final int x;
   private final int y;
   private final Direction direction;

   public enum Direction {
      UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);

      public final int dx, dy;

      Direction(int dx, int dy) {
         this.dx = dx;
         this.dy = dy;
      }
   }

   public ConveyorBelt(Tile tile, int x, int y, Direction direction) {
      this.tile = tile;
      this.x = x;
      this.y = y;
      this.direction = direction;
      tile.setType(TileType.CONVEYOR_BELT);
   }

   public Tile getTile() {
      return tile;
   }

   public int getX() {
      return x;
   }

   public int getY() {
      return y;
   }

   public Direction getDirection() {
      return direction;
   }
}

