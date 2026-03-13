package game.core;

import game.logistics.ConveyorBelt;
import game.logistics.LogisticsThread;
import game.logistics.TransportRobot;
import game.machine.BaseMachine;
import game.world.WorldMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * GameSupervisor steuert und überwacht alle Spiel-Threads.
 *
 * Thread-Modell:
 * 1. machineExecutor (ScheduledExecutorService): Jede Maschine hat einen
 * periodischen Task.
 * 2. logisticsExecutor (SingleThreadExecutor): Globaler Thread für
 * Fließband-Logik.
 * 3. robotExecutor (CachedThreadPool): Jeder TransportRobot ist ein eigener
 * Thread.
 * 4. collisionHandler (dedizierter Thread): Prüft auf Roboter-Kollisionen.
 */
public class GameSupervisor {
   private final ScheduledExecutorService machineExecutor;
   private final ExecutorService logisticsExecutor;
   private final ExecutorService robotExecutor;
   private final CollisionHandler collisionHandler;
   private final List<BaseMachine> machines;
   private final List<ConveyorBelt> belts;
   private final List<TransportRobot> robots;
   private final WorldMap map;
   private final AtomicBoolean running = new AtomicBoolean(false);

   // Speichert ScheduledFuture pro Maschine für gezieltes Abmelden
   // (Dekonstruktion)
   private final Map<BaseMachine, ScheduledFuture<?>> machineFutures = new ConcurrentHashMap<>();

   public GameSupervisor(WorldMap map, List<BaseMachine> machines,
         List<ConveyorBelt> belts, List<TransportRobot> robots) {
      this.map = map;
      this.machines = machines;
      this.belts = belts;
      this.robots = robots;
      this.machineExecutor = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());
      this.logisticsExecutor = Executors.newSingleThreadExecutor(r -> {
         Thread t = new Thread(r, "Logistics-Thread");
         t.setDaemon(true);
         return t;
      });
      this.robotExecutor = Executors.newCachedThreadPool(r -> {
         Thread t = new Thread(r, "Robot-" + System.nanoTime());
         t.setDaemon(true);
         return t;
      });
      this.collisionHandler = new CollisionHandler(robots);
   }

   public void start() {
      running.set(true);

      // Starte Maschinen (Producer) als periodische Tasks
      for (BaseMachine machine : machines) {
         ScheduledFuture<?> future = machineExecutor.scheduleAtFixedRate(() -> {
            try {
               machine.tick();
            } catch (Exception e) {
               System.err.println("Machine error: " + machine.getName() + " - " + e.getMessage());
            }
         }, 0, 500, TimeUnit.MILLISECONDS);
         machineFutures.put(machine, future);
      }

      // Starte Logistik-Thread für Fließbänder
      logisticsExecutor.submit(new LogisticsThread(map, belts, running));

      // Starte Transport-Roboter als eigene Threads
      for (TransportRobot robot : robots) {
         robotExecutor.submit(robot);
      }

      // Starte Kollisionsüberwachung
      collisionHandler.start(running);

      System.out.println("[GameSupervisor] Alle Threads gestartet. Maschinen: "
            + machines.size() + " Belts: " + belts.size() + " Robots: " + robots.size());
   }

   /**
    * Registriert eine neue Maschine zur Laufzeit und startet ihren periodischen
    * Task.
    */
   public void registerMachine(BaseMachine machine) {
      machines.add(machine);
      if (running.get()) {
         ScheduledFuture<?> future = machineExecutor.scheduleAtFixedRate(() -> {
            try {
               machine.tick();
            } catch (Exception e) {
               System.err.println("Machine error: " + machine.getName() + " - " + e.getMessage());
            }
         }, 0, 500, TimeUnit.MILLISECONDS);
         machineFutures.put(machine, future);
      }
   }

   /**
    * Meldet eine Maschine ab und stoppt ihren periodischen Task.
    * Wird beim Dekonstruieren (Abbauen) einer Maschine aufgerufen.
    */
   public void deregisterMachine(BaseMachine machine) {
      machines.remove(machine);
      ScheduledFuture<?> future = machineFutures.remove(machine);
      if (future != null) {
         future.cancel(false);
      }
      System.out.println("[GameSupervisor] Maschine abgemeldet: " + machine.getName());
   }

   /**
    * Sicheres Herunterfahren aller Threads.
    */
   public void stop() {
      running.set(false);
      machineExecutor.shutdownNow();
      logisticsExecutor.shutdownNow();
      robotExecutor.shutdownNow();
      collisionHandler.stop();

      try {
         machineExecutor.awaitTermination(2, TimeUnit.SECONDS);
         logisticsExecutor.awaitTermination(2, TimeUnit.SECONDS);
         robotExecutor.awaitTermination(2, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
      System.out.println("[GameSupervisor] Alle Threads gestoppt.");
   }

   public AtomicBoolean getRunning() {
      return running;
   }

   public List<BaseMachine> getMachines() {
      return machines;
   }

   public List<TransportRobot> getRobots() {
      return robots;
   }
}
