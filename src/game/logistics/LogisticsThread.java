package game.logistics;

import game.world.Tile;
import game.world.WorldMap;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LogisticsThread verarbeitet alle Fließbänder und bewegt Items entlang der
 * Belts.
 * Läuft als eigenständiger Thread im Logistics-Executor.
 *
 * Nutzt WorldMap.transferItem() für thread-sichere Item-Bewegung.
 */
public class LogisticsThread implements Runnable {
   private final WorldMap map;
   private final List<ConveyorBelt> belts;
   private final AtomicBoolean running;

   public LogisticsThread(WorldMap map, List<ConveyorBelt> belts, AtomicBoolean running) {
      this.map = map;
      this.belts = belts;
      this.running = running;
   }

   @Override
   public void run() {
      while (running.get() && !Thread.currentThread().isInterrupted()) {
         for (ConveyorBelt belt : belts) {
            processBelt(belt);
         }
         try {
            Thread.sleep(200); // Belt-Tick alle 200ms
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
         }
      }
   }

   /**
    * Bewegt ein Item auf einem Belt zum nächsten Tile in Belt-Richtung.
    */
   private void processBelt(ConveyorBelt belt) {
      Tile source = belt.getTile();
      if (!source.hasItem())
         return;

      ConveyorBelt.Direction dir = belt.getDirection();
      // Berechne Ziel-Koordinaten (wir brauchen x,y des Belts → suche im Grid)
      for (int x = 0; x < map.getWidth(); x++) {
         for (int y = 0; y < map.getHeight(); y++) {
            if (map.getTile(x, y) == source) {
               int tx = x + dir.dx;
               int ty = y + dir.dy;
               if (map.inBounds(tx, ty)) {
                  map.transferItem(source, map.getTile(tx, ty));
               }
               return;
            }
         }
      }
   }
}
