package game.logistics;

import game.world.Tile;
import game.world.WorldMap;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LogisticsThread verarbeitet alle Fließbänder und bewegt Items entlang der
 * Belts.
 * Läuft als eigenständiger Thread im Logistics-Executor.
 *
 * Nutzt WorldMap.transferItem() für thread-sichere Item-Bewegung.
 *
 * Robustheit: Exceptions einzelner Belts werden gefangen und geloggt, ohne den
 * Thread zu beenden. Nach {@link #MAX_CONSECUTIVE_ERRORS} aufeinanderfolgenden
 * Tick-Runden mit Fehlern beendet sich der Thread sauber, damit der Watchdog
 * im GameSupervisor ihn neu starten kann.
 */
public class LogisticsThread implements Runnable {

   /** Maximale Anzahl aufeinanderfolgender fehlerhafter Tick-Runden. */
   public static final int MAX_CONSECUTIVE_ERRORS = 5;

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
      int consecutiveErrorTicks = 0;
      while (running.get() && !Thread.currentThread().isInterrupted()) {
         boolean anyError = false;
         Set<Tile> movedThisTick = new HashSet<>();
         for (ConveyorBelt belt : belts) {
            try {
               processBelt(belt, movedThisTick);
            } catch (Exception e) {
               anyError = true;
               System.err.printf("[LogisticsThread] Belt-Fehler bei (%d,%d): %s%n",
                     belt.getX(), belt.getY(), e);
            }
         }
         if (anyError) {
            consecutiveErrorTicks++;
            if (consecutiveErrorTicks >= MAX_CONSECUTIVE_ERRORS) {
               System.err.printf(
                     "[LogisticsThread] %d aufeinanderfolgende Tick-Fehler — "
                           + "Thread beendet sich fuer Watchdog-Neustart.%n",
                     MAX_CONSECUTIVE_ERRORS);
               return;
            }
         } else {
            consecutiveErrorTicks = 0;
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
    * Pro Tick darf jedes Tile höchstens einmal ein Item empfangen.
    */
   private void processBelt(ConveyorBelt belt, Set<Tile> movedThisTick) {
      Tile source = belt.getTile();
      if (!source.hasItem())
         return;

      // Skip if this source tile already received an item this tick
      if (movedThisTick.contains(source))
         return;

      ConveyorBelt.Direction dir = belt.getDirection();
      int tx = belt.getX() + dir.dx;
      int ty = belt.getY() + dir.dy;
      if (map.inBounds(tx, ty)) {
         Tile dest = map.getTile(tx, ty);
         if (map.transferItem(source, dest)) {
            movedThisTick.add(dest);
         }
      }
   }
}
