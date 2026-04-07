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
 * - Jede Sekunde: + Summe getPollutionPerTick() aktiver Maschinen
 * (Greifer: immer 0; inaktive Maschinen ohne Brennstoff/Input: 0)
 * - Alle 5 Sekunden: -3 Punkte Decay
 * - Alle 2 Sekunden: HP-Schaden je nach Pollution-Level:
 * >= 75: 4 HP | >= 50: 2 HP | >= 25: 1 HP | < 25: kein Schaden
 */
public class PollutionThread extends Thread {

   private static final int DECAY_INTERVAL_S = 5;
   private static final int DECAY_AMOUNT = 3;
   private static final int ACCUMULATION_INTERVAL_S = 1;

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

         // Decay: -3 Punkte alle 5 Sekunden
         if (tickCount % DECAY_INTERVAL_S == 0) {
            pollutionManager.decreasePollution(DECAY_AMOUNT);
         }

         // Accumulation: jede Sekunde Summe der aktiven Maschinen-Pollution.
         // Inaktive Maschinen (kein Brennstoff / kein Input / voller Output) und
         // Greifer (getPollutionPerTick() == 0) tragen nichts bei.
         if (tickCount % ACCUMULATION_INTERVAL_S == 0) {
            int totalPollution = machines.stream()
                  .mapToInt(BaseMachine::getPollutionPerTick)
                  .sum();
            if (totalPollution > 0) {
               pollutionManager.addPollution(totalPollution);
               System.out.printf("[PollutionThread] +%d Pollution%n", totalPollution);
            }
         }

         // HP-Schaden alle 2 Sekunden
         if (tickCount % 2 == 0 && player != null && player.isAlive()) {
            int level = pollutionManager.getPollutionLevel();
            int damage;
            if (level >= 75) {
               damage = 4;
            } else if (level >= 50) {
               damage = 2;
            } else if (level >= 25) {
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
