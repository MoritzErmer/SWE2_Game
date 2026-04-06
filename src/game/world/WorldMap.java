package game.world;

import game.entity.ItemStack;

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

/**
 * WorldMap repräsentiert das 2D-Grid der Spielwelt.
 * Jede Zelle (Tile) besitzt ein eigenes Lock für feingranulare Synchronisation.
 *
 * Warum ReentrantLock statt synchronized?
 * - synchronized auf dem gesamten Grid (this) würde ALLE parallelen Zugriffe
 * serialisieren.
 * - synchronized auf einzelnen Tiles wäre möglich, schränkt aber die
 * Flexibilität ein:
 * kein tryLock(), kein faires Scheduling, keine Timeouts.
 * - Mit ReentrantLock pro Tile können verschiedene Threads verschiedene Tiles
 * gleichzeitig bearbeiten → maximale Parallelität.
 */
public class WorldMap {
   private final Tile[][] grid;
   private final int width;
   private final int height;

   public WorldMap(int width, int height) {
      this.width = width;
      this.height = height;
      this.grid = new Tile[width][height];
      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            grid[x][y] = new Tile();
         }
      }
   }

   /**
    * Generiert zufällige Ressourcen-Vorkommen auf der Karte.
    * 
    * @param density Wahrscheinlichkeit (0.0–1.0) dass ein Tile ein Vorkommen wird.
    */
   public void generateResources(double density) {
      Random rng = new Random();
      TileType[] deposits = { TileType.IRON_DEPOSIT, TileType.COPPER_DEPOSIT, TileType.COAL_DEPOSIT };
      for (int x = 0; x < width; x++) {
         for (int y = 0; y < height; y++) {
            if (rng.nextDouble() < density) {
               grid[x][y].setType(deposits[rng.nextInt(deposits.length)]);
            }
         }
      }
   }

   public Tile getTile(int x, int y) {
      if (!inBounds(x, y))
         throw new IndexOutOfBoundsException("Tile (" + x + "," + y + ") out of bounds");
      return grid[x][y];
   }

   public int[] findRandomAreaOrigin(int areaWidth, int areaHeight, Random rng) {
      if (areaWidth <= 0 || areaHeight <= 0) {
         throw new IllegalArgumentException("Area size must be positive.");
      }
      if (areaWidth > width || areaHeight > height) {
         throw new IllegalArgumentException("Area does not fit within map bounds.");
      }

      Random effectiveRng = rng != null ? rng : new Random();
      int x = effectiveRng.nextInt(width - areaWidth + 1);
      int y = effectiveRng.nextInt(height - areaHeight + 1);
      return new int[] { x, y };
   }

   public void setAreaType(int originX, int originY, int areaWidth, int areaHeight, TileType type) {
      if (type == null) {
         throw new IllegalArgumentException("Tile type must not be null.");
      }
      if (!inBounds(originX, originY)
            || !inBounds(originX + areaWidth - 1, originY + areaHeight - 1)) {
         throw new IllegalArgumentException("Area is out of bounds.");
      }

      for (int x = originX; x < originX + areaWidth; x++) {
         for (int y = originY; y < originY + areaHeight; y++) {
            Tile tile = grid[x][y];
            tile.getLock().lock();
            try {
               if (tile.hasMachine()) {
                  tile.removeMachine();
               }
               tile.setItemOnGround(null);
               tile.setType(type);
            } finally {
               tile.getLock().unlock();
            }
         }
      }
   }

   public boolean inBounds(int x, int y) {
      return x >= 0 && y >= 0 && x < width && y < height;
   }

   /**
    * Thread-sichere Übertragung eines Items zwischen zwei Tiles.
    *
    * Deadlock-Prävention: Locks werden immer in konsistenter Reihenfolge
    * (nach System.identityHashCode) angefordert. Dadurch kann kein zyklisches
    * Warten zwischen zwei Threads entstehen.
    *
    * Warum nicht synchronized?
    * - Zwei synchronized-Blöcke verschachteln (synchronized(from) {
    * synchronized(to) })
    * ist anfällig für Deadlocks, weil die Reihenfolge nicht garantiert ist.
    * - Mit expliziten Locks und konsistenter Ordnung ist Deadlock-Freiheit
    * beweisbar.
    *
      * @return true wenn Transfer erfolgreich, false wenn Quelle leer oder Ziel
      *         belegt.
    */
   public boolean transferItem(Tile from, Tile to) {
      if (from == null || to == null || from == to)
         return false;

      // Konsistente Lock-Ordnung nach identityHashCode (sicherer als hashCode())
      int fromHash = System.identityHashCode(from);
      int toHash = System.identityHashCode(to);
      ReentrantLock firstLock, secondLock;
      if (fromHash < toHash) {
         firstLock = from.getLock();
         secondLock = to.getLock();
      } else if (fromHash > toHash) {
         firstLock = to.getLock();
         secondLock = from.getLock();
      } else {
         // Extrem selten: gleicher identityHashCode → beide Locks sequenziell
         firstLock = from.getLock();
         secondLock = to.getLock();
      }

      firstLock.lock();
      try {
         secondLock.lock();
         try {
            if (!from.isConveyorBelt())
               return false; // Boden-Items dürfen nur auf Förderbändern liegen
            if (!from.hasItem())
               return false; // Quelle hat kein Item
            if (!canAcceptGroundItemCompat(to))
               return false; // Ziel belegt oder Maschine

            // Welt-Items werden immer als einzelne Einheiten transportiert.
            ItemStack sourceItem = from.getItemOnGround();
            if (sourceItem == null)
               return false; // Defensive Absicherung gegen inkonsistenten Tile-Zustand

            ItemStack movedItem = new ItemStack(sourceItem.getType(), 1);

            sourceItem.remove(1);
            if (sourceItem.getAmount() == 0) {
               from.setItemOnGround(null);
            }

            to.setItemOnGround(movedItem);
            return true;
         } finally {
            secondLock.unlock();
         }
      } finally {
         firstLock.unlock();
      }
   }

   private boolean canAcceptGroundItemCompat(Tile tile) {
      try {
         java.lang.reflect.Method method = tile.getClass().getMethod("canAcceptGroundItem");
         Object result = method.invoke(tile);
         if (result instanceof Boolean) {
            return (Boolean) result;
         }
      } catch (NoSuchMethodException ignored) {
         // Fallback unten
      } catch (ReflectiveOperationException e) {
         return false;
      }

      return tile.getType() == TileType.CONVEYOR_BELT && !tile.hasMachine() && !tile.hasItem();
   }

   public int getWidth() {
      return width;
   }

   public int getHeight() {
      return height;
   }
}
