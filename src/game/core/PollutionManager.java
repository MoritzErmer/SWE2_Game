package game.core;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * PollutionManager verfolgt den globalen Luftverschmutzungs-Level (0–100).
 *
 * Ist thread-sicher: alle Operationen nutzen AtomicInteger.
 * Dieser Manager wird von Maschinen-Ticks beschrieben und vom PollutionThread
 * gelesen und reduziert.
 */
public class PollutionManager {

   public static final int MAX_POLLUTION = 100;

   private final AtomicInteger pollutionLevel = new AtomicInteger(0);

   /**
    * Erhöht den Verschmutzungs-Level um den angegebenen Betrag.
    * Der Wert wird auf MAX_POLLUTION gekappt.
    *
    * @param amount Betrag, um den erhöht wird (negativ wird ignoriert)
    */
   public void addPollution(int amount) {
      if (amount <= 0)
         return;
      pollutionLevel.updateAndGet(v -> Math.min(MAX_POLLUTION, v + amount));
   }

   /**
    * Verringert den Verschmutzungs-Level um den angegebenen Betrag.
    * Der Wert wird auf 0 gekappt.
    *
    * @param amount Betrag, um den verringert wird (negativ wird ignoriert)
    */
   public void decreasePollution(int amount) {
      if (amount <= 0)
         return;
      pollutionLevel.updateAndGet(v -> Math.max(0, v - amount));
   }

   /**
    * Gibt den aktuellen Verschmutzungs-Level zurück (0–100).
    */
   public int getPollutionLevel() {
      return pollutionLevel.get();
   }
}
