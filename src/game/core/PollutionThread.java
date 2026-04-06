package game.core;

import game.entity.PlayerCharacter;
import game.machine.BaseMachine;

import java.util.List;

/**
 * PollutionThread: Verwaltet Pollution-Aufbau, Abbau und HP-Schaden zentral.
 *
 * Threading-Ansatz 3 (raw Thread):
 * Direktes Erben von Thread, manuelles Lifecycle-Management via
 * start()/interrupt(), kein Executor-Wrapping.
 *
 * Takt: jede Sekunde
 * - Alle 2 Sekunden: + Anzahl aktiver Maschinen (0.5 Punkte/s pro Maschine)
 * - Alle 5 Sekunden: -5 Punkte Decay
 * - Alle 2 Sekunden: HP-Schaden je nach Pollution-Level:
 * >= 100: 4 HP | >= 75: 2 HP | >= 50: 1 HP | < 50: kein Schaden
 */
public class PollutionThread extends Thread {

   private static final int DECAY_INTERVAL_S = 5;
   private static final int DECAY_AMOUNT = 5;
   private static final int ACCUMULATION_INTERVAL_S = 2;

   private final PollutionManager pollutionManager;
   private final PlayerCharacter player;
   private final List<BaseMachine> machines;

   public PollutionThread(PollutionManager pollutionManager, PlayerCharacter player,
         List<BaseMachine> machines) {
      super("Pollution-Thread");
      this.pollutionManager = pollutionManager;
      this.player = player;
      this.machines = machines;
      setDaemon(true);
   }

   @Override
   public void run() {
      int tickCount = 0;
      while (!isInterrupted()) {
         try {
            Thread.sleep(1000);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
         }

         tickCount++;

         // Decay: -5 Punkte alle 5 Sekunden
         if (tickCount % DECAY_INTERVAL_S == 0) {
            pollutionManager.decreasePollution(DECAY_AMOUNT);
         }

         // Accumulation: alle 2 Sekunden +1 pro aktiver Maschine (= 0.5/s pro Maschine)
         if (tickCount % ACCUMULATION_INTERVAL_S == 0) {
            int machineCount = machines.size();
            if (machineCount > 0) {
               pollutionManager.addPollution(machineCount);
               System.out.printf("[PollutionThread] +%d Pollution (%d Maschinen)%n",
                     machineCount, machineCount);
            }
         }

         // HP-Schaden alle 2 Sekunden
         if (tickCount % 2 == 0 && player != null && player.isAlive()) {
            int level = pollutionManager.getPollutionLevel();
            int damage;
            if (level >= 100) {
               damage = 4;
            } else if (level >= 75) {
               damage = 2;
            } else if (level >= 50) {
               damage = 1;
            } else {
               damage = 0;
            }
            if (damage > 0) {
               player.damage(damage);
               System.out.printf("[PollutionThread] HP-Schaden: -%d (Pollution: %d)%n",
                     damage, level);
            }
         }
      }
      System.out.println("[PollutionThread] Beendet.");
   }
}
