package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;
import game.world.WorldMap;

/**
 * Greifer (Grabber/Inserter): Kohlebetriebene Maschine, die Items von einem
 * Quell-Tile zum Ziel-Tile transportiert. Verbraucht 1 Kohle pro 8 Transfers.
 *
 * Der Greifer hat eine Richtung (source → destination) und arbeitet wie folgt:
 * - Nimmt Items vom Output-Buffer einer Maschine auf dem Quell-Tile
 * ODER vom Boden des Quell-Tiles
 * - Legt sie in den Input-Buffer einer Maschine auf dem Ziel-Tile
 * ODER auf den Boden des Ziel-Tiles
 * - Verbraucht Kohle aus seinem eigenen Input-Buffer als Brennstoff
 *
 * Thread-Sicherheit: tick() wird vom ScheduledExecutorService aufgerufen.
 * Lock-Ordering: Quell-Tile → Ziel-Tile (nach identityHashCode) um Deadlocks zu
 * vermeiden.
 */
public class Grabber extends BaseMachine {

   private int sourceDx;
   private int sourceDy;
   private int destDx;
   private int destDy;
   private final WorldMap worldMap;
   private final int tileX;
   private final int tileY;
   private int fuelCounter = 0; // Zählt Transfers, alle 8 wird 1 Kohle verbraucht

   private static final int TRANSFERS_PER_COAL = 8;

   /**
    * Erstellt einen Greifer.
    *
    * @param tile     Das Tile auf dem der Greifer steht
    * @param worldMap Referenz auf die Weltkarte für Nachbar-Zugriff
    * @param tileX    X-Position des Greifers
    * @param tileY    Y-Position des Greifers
    * @param sourceDx Relativer X-Offset zum Quell-Tile (-1, 0 oder 1)
    * @param sourceDy Relativer Y-Offset zum Quell-Tile (-1, 0 oder 1)
    * @param destDx   Relativer X-Offset zum Ziel-Tile (-1, 0 oder 1)
    * @param destDy   Relativer Y-Offset zum Ziel-Tile (-1, 0 oder 1)
    */
   public Grabber(Tile tile, WorldMap worldMap, int tileX, int tileY,
         int sourceDx, int sourceDy, int destDx, int destDy) {
      super("Grabber", tile, new GrabberStrategy());
      this.worldMap = worldMap;
      this.tileX = tileX;
      this.tileY = tileY;
      this.sourceDx = sourceDx;
      this.sourceDy = sourceDy;
      this.destDx = destDx;
      this.destDy = destDy;
      this.direction = Direction.fromDxDy(destDx, destDy);
   }

   /**
    * Rotiert den Greifer 90° im Uhrzeigersinn.
    * Quell- und Ziel-Richtung werden entsprechend angepasst.
    * Rotationsformel: (dx, dy) → (-dy, dx)
    */
   public void rotate() {
      int newSourceDx = -sourceDy;
      int newSourceDy = sourceDx;
      int newDestDx = -destDy;
      int newDestDy = destDx;
      this.sourceDx = newSourceDx;
      this.sourceDy = newSourceDy;
      this.destDx = newDestDx;
      this.destDy = newDestDy;
      this.direction = Direction.fromDxDy(destDx, destDy);
   }

   /**
    * Überschreibt tick() für Lock-Ordering über mehrere Tiles.
    * Lockt Quell-, eigenes und Ziel-Tile in einer festen Reihenfolge.
    */
   @Override
   public void tick() {
      // Berechne absolute Positionen
      int srcX = tileX + sourceDx;
      int srcY = tileY + sourceDy;
      int dstX = tileX + destDx;
      int dstY = tileY + destDy;

      if (!worldMap.inBounds(srcX, srcY) || !worldMap.inBounds(dstX, dstY))
         return;

      Tile srcTile = worldMap.getTile(srcX, srcY);
      Tile dstTile = worldMap.getTile(dstX, dstY);

      // Lock-Ordering: Sortiere nach identityHashCode um Deadlocks zu vermeiden
      Tile first, second, third;
      Tile[] sorted = sortByHash(tile, srcTile, dstTile);
      sorted[0].getLock().lock();
      try {
         sorted[1].getLock().lock();
         try {
            sorted[2].getLock().lock();
            try {
               doTransfer(srcTile, dstTile);
            } finally {
               sorted[2].getLock().unlock();
            }
         } finally {
            sorted[1].getLock().unlock();
         }
      } finally {
         sorted[0].getLock().unlock();
      }
   }

   /**
    * Führt den eigentlichen Transfer durch (alle Locks müssen gehalten werden).
    */
   private void doTransfer(Tile srcTile, Tile dstTile) {
      // Brennstoff prüfen
      if (!hasFuel())
         return;

      // Item von Quelle holen
      ItemStack item = extractFromSource(srcTile);
      if (item == null)
         return;

      // Item am Ziel ablegen
      boolean delivered = deliverToDestination(dstTile, item);
      if (!delivered) {
         // Zurücklegen wenn Ziel voll
         returnToSource(srcTile, item);
         return;
      }

      // Brennstoff verbrauchen
      consumeFuel();
   }

   /**
    * Entnimmt 1 Item von der Quelle (Maschinen-Output > Boden-Item).
    */
   private ItemStack extractFromSource(Tile srcTile) {
      // Priorität 1: Output-Buffer einer Maschine auf dem Quell-Tile
      if (srcTile.hasMachine() && srcTile.getMachine().hasOutput()) {
         ItemStack out = srcTile.getMachine().getOutputBuffer();
         ItemType type = out.getType();
         out.remove(1);
         if (out.getAmount() <= 0) {
            srcTile.getMachine().setOutputBuffer(null);
         }
         return new ItemStack(type, 1);
      }

      // Priorität 2: Item auf dem Boden
      if (srcTile.hasItem()) {
         ItemStack ground = srcTile.getItemOnGround();
         ItemType type = ground.getType();
         ground.remove(1);
         if (ground.getAmount() <= 0) {
            srcTile.setItemOnGround(null);
         }
         return new ItemStack(type, 1);
      }

      return null;
   }

   /**
    * Liefert 1 Item an das Ziel (Maschinen-Input > Boden).
    */
   private boolean deliverToDestination(Tile dstTile, ItemStack item) {
      // Priorität 1: Input-Buffer einer Maschine auf dem Ziel-Tile
      if (dstTile.hasMachine()) {
         return dstTile.getMachine().tryInsertInput(item);
      }

      // Priorität 2: Auf den Boden legen
      if (!dstTile.hasItem()) {
         dstTile.setItemOnGround(item);
         return true;
      }

      // Auf bestehenden Stack addieren
      if (dstTile.getItemOnGround().getType() == item.getType()) {
         dstTile.getItemOnGround().add(item.getAmount());
         return true;
      }

      return false; // Ziel belegt mit anderem Item
   }

   /**
    * Legt ein Item zurück zur Quelle wenn Ziel voll war.
    */
   private void returnToSource(Tile srcTile, ItemStack item) {
      if (srcTile.hasMachine() && srcTile.getMachine().hasOutput()) {
         srcTile.getMachine().getOutputBuffer().add(item.getAmount());
      } else if (srcTile.hasMachine()) {
         srcTile.getMachine().setOutputBuffer(item);
      } else if (srcTile.hasItem()) {
         srcTile.getItemOnGround().add(item.getAmount());
      } else {
         srcTile.setItemOnGround(item);
      }
   }

   /**
    * Prüft ob genug Brennstoff vorhanden ist.
    */
   private boolean hasFuel() {
      // Erste Transfers sind kostenlos bis der Zähler voll ist
      if (fuelCounter < TRANSFERS_PER_COAL)
         return true;
      // Kohle im Input-Buffer?
      return hasInput() && inputBuffer.getType() == ItemType.COAL;
   }

   /**
    * Verbraucht Brennstoff nach einem erfolgreichen Transfer.
    */
   private void consumeFuel() {
      fuelCounter++;
      if (fuelCounter >= TRANSFERS_PER_COAL) {
         fuelCounter = 0;
         if (hasInput() && inputBuffer.getType() == ItemType.COAL) {
            inputBuffer.remove(1);
            if (inputBuffer.getAmount() <= 0) {
               inputBuffer = null;
            }
         }
      }
   }

   /**
    * Sortiert drei Tiles nach identityHashCode für Deadlock-freies Locking.
    */
   private Tile[] sortByHash(Tile a, Tile b, Tile c) {
      Tile[] arr = { a, b, c };
      // Simple Bubble Sort für 3 Elemente
      for (int i = 0; i < 2; i++) {
         for (int j = 0; j < 2 - i; j++) {
            if (System.identityHashCode(arr[j]) > System.identityHashCode(arr[j + 1])) {
               Tile tmp = arr[j];
               arr[j] = arr[j + 1];
               arr[j + 1] = tmp;
            }
         }
      }
      return arr;
   }

   // Getter für Rendering
   public int getSourceDx() {
      return sourceDx;
   }

   public int getSourceDy() {
      return sourceDy;
   }

   public int getDestDx() {
      return destDx;
   }

   public int getDestDy() {
      return destDy;
   }
}
