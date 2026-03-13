package game.core;

import game.logistics.TransportRobot;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Überwacht autonome Transport-Roboter auf Kollisionen.
 * Läuft als eigener dedizierter Thread und prüft regelmäßig,
 * ob sich zwei Roboter auf demselben Tile befinden.
 *
 * Thread-Sicherheit: Liest nur volatile Positionen der Roboter,
 * daher ist kein Lock nötig (read-only Zugriff).
 */
public class CollisionHandler {
   private final List<TransportRobot> robots;
   private Thread thread;
   private volatile boolean running = false;

   public CollisionHandler(List<TransportRobot> robots) {
      this.robots = robots;
   }

   public void start(AtomicBoolean supervisorRunning) {
      running = true;
      thread = new Thread(() -> {
         while (running && supervisorRunning.get()) {
            checkCollisions();
            try {
               Thread.sleep(50); // Prüfintervall: 50ms
            } catch (InterruptedException e) {
               Thread.currentThread().interrupt();
               return;
            }
         }
      }, "CollisionHandler");
      thread.setDaemon(true);
      thread.start();
   }

   /**
    * Prüft paarweise alle Roboter auf Positionsüberschneidungen.
    * Bei Kollision wird ein Warnsignal ausgegeben (erweiterbar für
    * Gameplay-Logik).
    */
   private void checkCollisions() {
      for (int i = 0; i < robots.size(); i++) {
         for (int j = i + 1; j < robots.size(); j++) {
            TransportRobot a = robots.get(i);
            TransportRobot b = robots.get(j);
            if (a.getX() == b.getX() && a.getY() == b.getY()) {
               System.out.println("[COLLISION] Robot " + a.getId()
                     + " and Robot " + b.getId()
                     + " at (" + a.getX() + "," + a.getY() + ")");
               // Hier könnte man Roboter stoppen, umleiten, oder Schaden verursachen
            }
         }
      }
   }

   public void stop() {
      running = false;
      if (thread != null)
         thread.interrupt();
   }
}
