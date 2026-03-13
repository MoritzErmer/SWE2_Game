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
            if (!from.hasItem())
               return false; // Quelle hat kein Item
            if (to.hasItem())
               return false; // Ziel ist bereits belegt

            ItemStack item = from.pickupItem();
            to.setItemOnGround(item);
            return true;
         } finally {
            secondLock.unlock();
         }
      } finally {
         firstLock.unlock();
      }
   }

   public int getWidth() {
      return width;
   }

   public int getHeight() {
      return height;
   }
}
