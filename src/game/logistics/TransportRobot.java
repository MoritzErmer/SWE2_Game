package game.logistics;

import game.entity.ItemStack;
import game.world.Tile;
import game.world.WorldMap;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * TransportRobot ist ein autonomer Agent, der als eigener Thread läuft
 * und Items zwischen zwei Positionen auf der WorldMap transportiert.
 *
 * Jeder Roboter ist ein eigenständiger Thread → echtes Multithreading.
 * Die Kollisionsprüfung erfolgt durch den CollisionHandler.
 */
public class TransportRobot implements Runnable {
   private volatile int x, y;
   private final int targetX, targetY;
   private final int sourceX, sourceY;
   private final WorldMap map;
   private final AtomicBoolean running;
   private final String id;

   public TransportRobot(String id, int sourceX, int sourceY,
         int targetX, int targetY, WorldMap map, AtomicBoolean running) {
      this.id = id;
      this.sourceX = sourceX;
      this.sourceY = sourceY;
      this.x = sourceX;
      this.y = sourceY;
      this.targetX = targetX;
      this.targetY = targetY;
      this.map = map;
      this.running = running;
   }

   @Override
   public void run() {
      while (running.get() && !Thread.currentThread().isInterrupted()) {
         try {
            // Phase 1: Zum Quell-Tile gehen und Item aufnehmen
            moveTowards(sourceX, sourceY);
            if (x == sourceX && y == sourceY) {
               Tile src = map.getTile(sourceX, sourceY);
               src.getLock().lock();
               try {
                  ItemStack picked = src.pickupItem();
                  if (picked != null) {
                     // Phase 2: Zum Ziel-Tile gehen und Item ablegen
                     while (running.get() && (x != targetX || y != targetY)) {
                        moveTowards(targetX, targetY);
                        Thread.sleep(300); // Bewegungsgeschwindigkeit
                     }
                     Tile dst = map.getTile(targetX, targetY);
                     dst.getLock().lock();
                     try {
                        dst.setItemOnGround(picked);
                     } finally {
                        dst.getLock().unlock();
                     }
                  }
               } finally {
                  src.getLock().unlock();
               }
            }
            Thread.sleep(500); // Warte bevor nächster Zyklus
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
         }
      }
   }

   /** Bewegt den Roboter einen Schritt Richtung Ziel. */
   private void moveTowards(int tx, int ty) {
      if (x < tx)
         x++;
      else if (x > tx)
         x--;
      if (y < ty)
         y++;
      else if (y > ty)
         y--;
   }

   public int getX() {
      return x;
   }

   public int getY() {
      return y;
   }

   public String getId() {
      return id;
   }
}
