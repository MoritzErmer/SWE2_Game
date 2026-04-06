package game.integration;

import game.core.GameSupervisor;
import game.entity.ItemStack;
import game.entity.ItemType;
import game.logistics.ConveyorBelt;
import game.logistics.LogisticsThread;
import game.machine.BaseMachine;
import game.machine.Miner;
import game.machine.ProductionStrategy;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Robustheitstests für das Threading-Modell des Spiels.
 *
 * Prüft:
 * - Maschinen-Exceptions isolieren sich (kein Thread-Tod durch Einzelfehler)
 * - LogisticsThread überlauscht Belt-NPE ohne Absturz
 * - Watchdog im GameSupervisor startet toten Logistics-Thread automatisch neu
 * - stop() funktioniert sauber auch bei dauerhaft fehlerhaften Maschinen
 * - Stop + Neustart (Recovery-Zyklus) funktioniert fehlerfrei
 */
class ThreadRobustnessTest {

   // ─── Hilfsmethoden ────────────────────────────────────────────────────────

   /**
    * Erstellt eine Maschine, deren Strategy bei jedem tick() eine
    * RuntimeException wirft.
    */
   private static BaseMachine crashingMachine(Tile tile) {
      return new BaseMachine("CrashingMachine", tile,
            (ProductionStrategy) machine -> {
               throw new RuntimeException("Intentional crash for test");
            }) {
      };
   }

   // ─── Tests ────────────────────────────────────────────────────────────────

   /**
    * Eine crashende Maschine darf nicht verhindern, dass eine gesunde Maschine
    * produziert.
    * Erwartet: Miner hat nach 1.5 s Output, obwohl Nachbar-Maschine jedes Mal
    * wirft.
    */
   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   void crashingMachineDoesNotKillOtherMachines() throws InterruptedException {
      WorldMap map = new WorldMap(10, 10);

      // Gesunder Miner auf Eisenvorkommen
      Tile minerTile = map.getTile(2, 2);
      minerTile.setType(TileType.IRON_DEPOSIT);
      Miner miner = new Miner(minerTile);
      minerTile.setMachine(miner);
      miner.tryInsertInput(new ItemStack(ItemType.COAL, 32));

      // Maschine, die immer crasht
      Tile crashTile = map.getTile(5, 5);
      BaseMachine crasher = crashingMachine(crashTile);

      List<BaseMachine> machines = new CopyOnWriteArrayList<>();
      machines.add(miner);
      machines.add(crasher);

      GameSupervisor supervisor = new GameSupervisor(map, machines, new CopyOnWriteArrayList<>());
      supervisor.start();
      Thread.sleep(1500);
      supervisor.stop();

      assertTrue(miner.hasOutput(),
            "Gesunder Miner muss Output produziert haben, obwohl Nachbar-Maschine crasht.");
   }

   /**
    * Eine Belt-Verarbeitungsausnahme (NPE durch null-Tile) darf den
    * LogisticsThread nicht töten. Der Thread muss weiterlaufen und andere
    * Belts weiterhin verarbeiten.
    */
   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   void logisticsThreadContinuesAfterBeltException() throws InterruptedException {
      WorldMap map = new WorldMap(10, 10);
      AtomicInteger goodBeltTicks = new AtomicInteger(0);

      // Bad belt: getTile() liefert null → NPE in processBelt
      Tile badTile = map.getTile(1, 1);
      ConveyorBelt badBelt = new ConveyorBelt(badTile, 1, 1, ConveyorBelt.Direction.RIGHT) {
         @Override
         public Tile getTile() {
            return null; // Verursacht NPE in LogisticsThread.processBelt
         }
      };

      // Good belt: zählt Aufrufe
      Tile goodTile = map.getTile(5, 5);
      ConveyorBelt goodBelt = new ConveyorBelt(goodTile, 5, 5, ConveyorBelt.Direction.RIGHT) {
         @Override
         public Tile getTile() {
            goodBeltTicks.incrementAndGet();
            return super.getTile();
         }
      };

      List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();
      belts.add(badBelt);
      belts.add(goodBelt);

      GameSupervisor supervisor = new GameSupervisor(map, new CopyOnWriteArrayList<>(), belts);
      supervisor.start();
      Thread.sleep(800); // 4 Ticks à 200 ms

      // Good belt sollte trotz bad belt verarbeitet worden sein
      assertTrue(goodBeltTicks.get() > 0,
            "Guter Belt muss verarbeitet worden sein, auch wenn Nachbar-Belt NPE wirft.");

      supervisor.stop();
   }

   /**
    * Der Watchdog im GameSupervisor startet den Logistics-Thread automatisch neu,
    * wenn er sich durch anhaltende Fehler selbst beendet hat.
    *
    * Ablauf:
    * 1. Ein "flaky" Belt produziert für die ersten N Ticks eine NPE.
    * 2. Nach MAX_CONSECUTIVE_ERRORS Ticks (= 5 * 200 ms = 1 s) stirbt der Thread.
    * 3. Der Watchdog erkennt isDone() und startet neu (spätestens nach 1 s).
    * 4. Ab Aufruf > N gibt der flaky Belt sein echtes Tile zurück.
    * 5. processingCount muss danach > 0 sein.
    */
   @Test
   @Timeout(value = 12, unit = TimeUnit.SECONDS)
   void watchdogRestartsDeadLogisticsThread() throws InterruptedException {
      WorldMap map = new WorldMap(10, 10);
      AtomicInteger callCount = new AtomicInteger(0);
      AtomicInteger processingCount = new AtomicInteger(0);

      // Anzahl der Ticks, die der Thread crashen soll:
      // MAX_CONSECUTIVE_ERRORS + 3 → zwei volle Thread-Lebenszeiten crashen, dann
      // Erfolg
      final int failUntilCall = LogisticsThread.MAX_CONSECUTIVE_ERRORS + 3;

      Tile beltTile = map.getTile(3, 3);
      ConveyorBelt flakyBelt = new ConveyorBelt(beltTile, 3, 3, ConveyorBelt.Direction.RIGHT) {
         @Override
         public Tile getTile() {
            int call = callCount.incrementAndGet();
            if (call <= failUntilCall) {
               return null; // NPE → consecutive errors
            }
            processingCount.incrementAndGet();
            return super.getTile();
         }
      };

      List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();
      belts.add(flakyBelt);

      GameSupervisor supervisor = new GameSupervisor(map, new CopyOnWriteArrayList<>(), belts);
      supervisor.start();

      // Warten: erster Thread stirbt (~1 s) + Watchdog startet (~1 s) + einige gute
      // Ticks
      Thread.sleep(5000);
      supervisor.stop();

      assertTrue(processingCount.get() > 0,
            "Belt muss nach Watchdog-Neustart erfolgreich verarbeitet worden sein.");
   }

   /**
    * stop() darf niemals werfen, selbst wenn alle laufenden Maschinen dauerhaft
    * crashen.
    */
   @Test
   @Timeout(value = 8, unit = TimeUnit.SECONDS)
   void stopIsCleanWithPermanentlyCrashingMachines() throws InterruptedException {
      WorldMap map = new WorldMap(10, 10);
      List<BaseMachine> machines = new CopyOnWriteArrayList<>();
      for (int i = 0; i < 5; i++) {
         machines.add(crashingMachine(map.getTile(i, i)));
      }

      GameSupervisor supervisor = new GameSupervisor(map, machines, new CopyOnWriteArrayList<>());
      supervisor.start();
      Thread.sleep(300);

      assertDoesNotThrow(supervisor::stop, "stop() darf keine Exception werfen.");
      assertFalse(supervisor.getRunning().get(), "running muss nach stop() false sein.");
   }

   /**
    * Nach stop() kann der Supervisor wieder gestartet werden und produziert
    * weiterhin korrekt (Recovery-Zyklus).
    */
   @Test
   @Timeout(value = 12, unit = TimeUnit.SECONDS)
   void supervisorRecoversThroughStopAndRestart() throws InterruptedException {
      WorldMap map = new WorldMap(10, 10);

      Tile minerTile = map.getTile(1, 1);
      minerTile.setType(TileType.IRON_DEPOSIT);
      Miner miner = new Miner(minerTile);
      minerTile.setMachine(miner);
      miner.tryInsertInput(new ItemStack(ItemType.COAL, 64));

      List<BaseMachine> machines = new CopyOnWriteArrayList<>();
      machines.add(miner);

      GameSupervisor supervisor = new GameSupervisor(map, machines, new CopyOnWriteArrayList<>());

      // Erster Zyklus
      supervisor.start();
      assertTrue(supervisor.getRunning().get());
      Thread.sleep(600);
      supervisor.stop();
      assertFalse(supervisor.getRunning().get(), "Nach erstem stop() muss running=false sein.");

      // Output zwischen den Zyklen leeren
      miner.extractOutput();

      // Zweiter Zyklus
      supervisor.start();
      assertTrue(supervisor.getRunning().get(), "Supervisor muss nach Neustart wieder running sein.");
      Thread.sleep(600);
      supervisor.stop();
      assertFalse(supervisor.getRunning().get(), "Nach zweitem stop() muss running=false sein.");
   }

   /**
    * Concurrent register/deregister unter Last: Keine DeadLocks, keine
    * ConcurrentModificationExceptions, running bleibt stabil.
    */
   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   void concurrentRegisterDeregisterDoesNotCorruptSupervisor() throws InterruptedException {
      WorldMap map = new WorldMap(20, 20);
      GameSupervisor supervisor = new GameSupervisor(map, new CopyOnWriteArrayList<>(),
            new CopyOnWriteArrayList<>());
      supervisor.start();

      AtomicInteger errors = new AtomicInteger(0);
      Thread[] workers = new Thread[4];

      for (int w = 0; w < workers.length; w++) {
         final int workerId = w;
         workers[w] = new Thread(() -> {
            for (int i = 0; i < 15; i++) {
               try {
                  int x = 1 + workerId * 4;
                  int y = 1 + (i % 15);
                  Tile tile = map.getTile(x, y);

                  tile.getLock().lock();
                  try {
                     if (!tile.hasMachine()) {
                        tile.setType(TileType.IRON_DEPOSIT);
                        tile.setMachine(new Miner(tile));
                     }
                  } finally {
                     tile.getLock().unlock();
                  }

                  BaseMachine machine = tile.getMachine();
                  if (machine != null) {
                     supervisor.registerMachine(machine);
                     Thread.sleep(10);
                     supervisor.deregisterMachine(machine);
                  }
               } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
               } catch (Exception e) {
                  errors.incrementAndGet();
               }
            }
         }, "robustness-worker-" + w);
         workers[w].start();
      }

      for (Thread worker : workers) {
         worker.join(8000);
      }

      supervisor.stop();

      assertEquals(0, errors.get(),
            "Keine Exceptions bei concurrent register/deregister erwartet.");
      assertFalse(supervisor.getRunning().get());
   }
}
