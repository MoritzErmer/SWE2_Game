package game.core;

import game.logistics.ConveyorBelt;
import game.logistics.LogisticsThread;
import game.machine.Direction;
import game.machine.BaseMachine;
import game.world.Tile;
import game.world.WorldMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
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
 */
public class GameSupervisor {
   private ScheduledExecutorService machineExecutor;
   private ExecutorService logisticsExecutor;
   private final List<BaseMachine> machines;
   private final List<ConveyorBelt> belts;
   private final WorldMap map;
   private final AtomicBoolean running = new AtomicBoolean(false);

   // Speichert ScheduledFuture pro Maschine für gezieltes Abmelden
   // (Dekonstruktion)
   private final Map<BaseMachine, ScheduledFuture<?>> machineFutures = new ConcurrentHashMap<>();
   private final Map<String, ConveyorBelt> beltIndex = new ConcurrentHashMap<>();

   public GameSupervisor(WorldMap map, List<BaseMachine> machines,
         List<ConveyorBelt> belts) {
      this.map = map;
      this.machines = new CopyOnWriteArrayList<>(machines);
      this.belts = new CopyOnWriteArrayList<>(belts);
      initializeExecutors();

      for (ConveyorBelt belt : this.belts) {
         beltIndex.put(beltKey(belt.getX(), belt.getY()), belt);
      }
   }

   public synchronized void start() {
      if (running.get()) {
         return;
      }

      ensureExecutorsRunning();

      machineFutures.clear();
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

      System.out.println("[GameSupervisor] Alle Threads gestartet. Maschinen: "
            + machines.size() + " Belts: " + belts.size());
   }

   /**
    * Registriert eine neue Maschine zur Laufzeit und startet ihren periodischen
    * Task.
    */
   public void registerMachine(BaseMachine machine) {
      if (!machines.contains(machine)) {
         machines.add(machine);
      }
      if (running.get()) {
         ensureExecutorsRunning();
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

   public void registerBelt(int x, int y, Direction machineDirection) {
      if (!map.inBounds(x, y)) {
         return;
      }

      String key = beltKey(x, y);
      if (beltIndex.containsKey(key)) {
         return;
      }

      Tile tile = map.getTile(x, y);
      ConveyorBelt.Direction direction = toBeltDirection(machineDirection);
      ConveyorBelt belt = new ConveyorBelt(tile, x, y, direction);
      belts.add(belt);
      beltIndex.put(key, belt);
      System.out.println("[GameSupervisor] Belt registriert bei (" + x + "," + y + ") " + direction);
   }

   public void deregisterBelt(int x, int y) {
      String key = beltKey(x, y);
      ConveyorBelt belt = beltIndex.remove(key);
      if (belt == null) {
         return;
      }

      belts.remove(belt);
      Tile tile = map.getTile(x, y);
      tile.getLock().lock();
      try {
         if (tile.getType() == game.world.TileType.CONVEYOR_BELT) {
            tile.setType(game.world.TileType.EMPTY);
         }
      } finally {
         tile.getLock().unlock();
      }
      System.out.println("[GameSupervisor] Belt abgemeldet bei (" + x + "," + y + ")");
   }

   public void rotateBelt(int x, int y) {
      ConveyorBelt belt = beltIndex.get(beltKey(x, y));
      if (belt == null) {
         return;
      }
      belt.rotateClockwise();
      System.out.println("[GameSupervisor] Belt rotiert bei (" + x + "," + y + ") -> " + belt.getDirection());
   }

   public void registerRobot(TransportRobot robot) {
      if (!robots.contains(robot)) {
         robots.add(robot);
      }
      if (running.get()) {
         robotExecutor.submit(robot);
      }
   }

   public void deregisterRobot(TransportRobot robot) {
      robots.remove(robot);
   }

   /**
    * Sicheres Herunterfahren aller Threads.
    */
   public synchronized void stop() {
      running.set(false);
      machineExecutor.shutdownNow();
      logisticsExecutor.shutdownNow();

      try {
         machineExecutor.awaitTermination(2, TimeUnit.SECONDS);
         logisticsExecutor.awaitTermination(2, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
      System.out.println("[GameSupervisor] Alle Threads gestoppt.");
   }

   private void initializeExecutors() {
      this.machineExecutor = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors());
      this.logisticsExecutor = Executors.newSingleThreadExecutor(r -> {
         Thread t = new Thread(r, "Logistics-Thread");
         t.setDaemon(true);
         return t;
      });
   }

   private void ensureExecutorsRunning() {
      if (machineExecutor == null || machineExecutor.isShutdown() || machineExecutor.isTerminated()
            || logisticsExecutor == null || logisticsExecutor.isShutdown() || logisticsExecutor.isTerminated()) {
         initializeExecutors();
      }
   }

   public AtomicBoolean getRunning() {
      return running;
   }

   public List<BaseMachine> getMachines() {
      return Collections.unmodifiableList(machines);
   }

   public List<ConveyorBelt> getBelts() {
      return Collections.unmodifiableList(belts);
   }

   private String beltKey(int x, int y) {
      return x + ":" + y;
   }

   private ConveyorBelt.Direction toBeltDirection(Direction direction) {
      switch (direction) {
         case UP:
            return ConveyorBelt.Direction.UP;
         case DOWN:
            return ConveyorBelt.Direction.DOWN;
         case LEFT:
            return ConveyorBelt.Direction.LEFT;
         case RIGHT:
         default:
            return ConveyorBelt.Direction.RIGHT;
      }
   }
}
