package game.machine;

/**
 * Repräsentiert die Ausrichtung einer Maschine in 4 Himmelsrichtungen.
 * Wird für Rotation (R-Taste), Greifer-Richtung und visuelle Darstellung
 * verwendet.
 *
 * Die Reihenfolge UP → RIGHT → DOWN → LEFT ermöglicht einfache Rotation
 * über den ordinal()-Index.
 */
public enum Direction {
   UP(0, -1),
   RIGHT(1, 0),
   DOWN(0, 1),
   LEFT(-1, 0);

   private final int dx;
   private final int dy;

   Direction(int dx, int dy) {
      this.dx = dx;
      this.dy = dy;
   }

   public int getDx() {
      return dx;
   }

   public int getDy() {
      return dy;
   }

   /** Rotiert 90° im Uhrzeigersinn: UP → RIGHT → DOWN → LEFT → UP */
   public Direction rotateClockwise() {
      Direction[] vals = values();
      return vals[(ordinal() + 1) % vals.length];
   }

   /** Gegenteilige Richtung (180°). */
   public Direction opposite() {
      Direction[] vals = values();
      return vals[(ordinal() + 2) % vals.length];
   }

   /** Bestimmt die Richtung aus dx/dy-Offsets. */
   public static Direction fromDxDy(int dx, int dy) {
      if (dx == 1)
         return RIGHT;
      if (dx == -1)
         return LEFT;
      if (dy == -1)
         return UP;
      if (dy == 1)
         return DOWN;
      return RIGHT; // Default
   }

   /**
    * Gibt den Rotationswinkel in Radiant zurück (für AffineTransform).
    * Basis-Richtung ist RIGHT (0°).
    */
   public double getRotationAngle() {
      switch (this) {
         case UP:
            return -Math.PI / 2;
         case RIGHT:
            return 0;
         case DOWN:
            return Math.PI / 2;
         case LEFT:
            return Math.PI;
         default:
            return 0;
      }
   }
}
