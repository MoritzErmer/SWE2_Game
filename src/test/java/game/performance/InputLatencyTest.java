package game.performance;

import game.crafting.CraftingManager;
import game.crafting.CraftingRecipe;
import game.entity.ItemType;
import game.entity.PlayerCharacter;
import game.world.WorldMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Latenz-Tests für User-Input-Events.
 *
 * Jede Eingabeoperation muss innerhalb von LATENCY_THRESHOLD_MS (100 ms)
 * abgeschlossen sein. Die Tests laufen headless-kompatibel (kein GUI nötig)
 * und sind daher auch in der CI/CD-Pipeline ausführbar.
 *
 * Methodik: Worst-Case-Messung über SAMPLE_COUNT Iterationen.
 * Schlägt fehl, wenn eine einzelne Operation die Schwelle überschreitet.
 */
@Tag("latency")
class InputLatencyTest {

   /** Maximale erlaubte Latenz in Millisekunden pro Eingabeaktion. */
   private static final long LATENCY_THRESHOLD_MS = 100;

   /** Anzahl der Messwiederholungen pro Test. */
   private static final int SAMPLE_COUNT = 500;

   private WorldMap map;
   private PlayerCharacter player;

   @BeforeEach
   void setUp() {
      map = new WorldMap(50, 50);
      player = new PlayerCharacter(25, 25);
   }

   // ──────────────────────────────────────────────────────────────────────────
   // Bewegung (WASD)
   // ──────────────────────────────────────────────────────────────────────────

   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   @DisplayName("WASD-Bewegungs-Latenz < 100 ms")
   void playerMovementLatency() {
      char[] directions = { 'w', 'a', 's', 'd' };
      long worstNs = 0;

      for (int i = 0; i < SAMPLE_COUNT; i++) {
         char dir = directions[i % directions.length];

         long t0 = System.nanoTime();
         player.move(dir, map);
         long elapsed = System.nanoTime() - t0;

         if (elapsed > worstNs) {
            worstNs = elapsed;
         }
      }

      long worstMs = worstNs / 1_000_000;
      assertTrue(worstMs < LATENCY_THRESHOLD_MS,
            "Maximale Bewegungs-Latenz überschreitet 100 ms: " + worstMs + " ms");
   }

   // ──────────────────────────────────────────────────────────────────────────
   // Hotbar-Auswahl (Tasten 1–9)
   // ──────────────────────────────────────────────────────────────────────────

   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   @DisplayName("Hotbar-Auswahl-Latenz < 100 ms")
   void hotbarSelectionLatency() {
      long worstNs = 0;

      for (int i = 0; i < SAMPLE_COUNT; i++) {
         int slot = i % PlayerCharacter.HOTBAR_SLOTS;

         long t0 = System.nanoTime();
         player.setSelectedHotbarSlot(slot);
         long elapsed = System.nanoTime() - t0;

         if (elapsed > worstNs) {
            worstNs = elapsed;
         }
      }

      long worstMs = worstNs / 1_000_000;
      assertTrue(worstMs < LATENCY_THRESHOLD_MS,
            "Maximale Hotbar-Auswahl-Latenz überschreitet 100 ms: " + worstMs + " ms");
   }

   // ──────────────────────────────────────────────────────────────────────────
   // Inventar-Operationen (Item aufnehmen / ablegen)
   // ──────────────────────────────────────────────────────────────────────────

   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   @DisplayName("Inventar-Operationen-Latenz < 100 ms")
   void inventoryOperationLatency() {
      long worstAddNs = 0;
      long worstRemoveNs = 0;

      for (int i = 0; i < SAMPLE_COUNT; i++) {
         long t0 = System.nanoTime();
         player.addItem(ItemType.IRON_ORE, 1);
         long addElapsed = System.nanoTime() - t0;

         long t1 = System.nanoTime();
         player.removeItem(ItemType.IRON_ORE, 1);
         long removeElapsed = System.nanoTime() - t1;

         if (addElapsed > worstAddNs)
            worstAddNs = addElapsed;
         if (removeElapsed > worstRemoveNs)
            worstRemoveNs = removeElapsed;
      }

      long worstAddMs = worstAddNs / 1_000_000;
      long worstRemoveMs = worstRemoveNs / 1_000_000;

      assertTrue(worstAddMs < LATENCY_THRESHOLD_MS,
            "Maximale Inventar-Add-Latenz überschreitet 100 ms: " + worstAddMs + " ms");
      assertTrue(worstRemoveMs < LATENCY_THRESHOLD_MS,
            "Maximale Inventar-Remove-Latenz überschreitet 100 ms: " + worstRemoveMs + " ms");
   }

   // ──────────────────────────────────────────────────────────────────────────
   // Crafting (Taste C + Rezept bestätigen)
   // ──────────────────────────────────────────────────────────────────────────

   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   @DisplayName("Crafting-Latenz < 100 ms")
   void craftingLatency() {
      CraftingManager craftingManager = new CraftingManager();
      craftingManager.setCreativeMode(true); // Keine Items erforderlich

      List<CraftingRecipe> recipes = craftingManager.getRecipes();
      if (recipes.isEmpty()) {
         return; // Kein Rezept vorhanden – Test überspringen
      }

      CraftingRecipe recipe = recipes.get(0); // Iron Plate
      long worstNs = 0;

      for (int i = 0; i < SAMPLE_COUNT; i++) {
         long t0 = System.nanoTime();
         craftingManager.craft(recipe, player);
         long elapsed = System.nanoTime() - t0;

         if (elapsed > worstNs) {
            worstNs = elapsed;
         }
      }

      long worstMs = worstNs / 1_000_000;
      assertTrue(worstMs < LATENCY_THRESHOLD_MS,
            "Maximale Crafting-Latenz überschreitet 100 ms: " + worstMs + " ms");
   }

   // ──────────────────────────────────────────────────────────────────────────
   // Kombinierter Burst – simulierte schnelle Eingabenfolge
   // ──────────────────────────────────────────────────────────────────────────

   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   @DisplayName("Burst-Input-Latenz < 100 ms pro Aktion")
   void burstInputLatency() {
      char[] moves = { 'w', 'd', 's', 'a' };
      long worstNs = 0;

      for (int i = 0; i < SAMPLE_COUNT; i++) {
         // Bewegung
         long t0 = System.nanoTime();
         player.move(moves[i % moves.length], map);
         long moveElapsed = System.nanoTime() - t0;

         // Hotbar-Wechsel
         long t1 = System.nanoTime();
         player.setSelectedHotbarSlot(i % PlayerCharacter.HOTBAR_SLOTS);
         long hotbarElapsed = System.nanoTime() - t1;

         long maxThisRound = Math.max(moveElapsed, hotbarElapsed);
         if (maxThisRound > worstNs) {
            worstNs = maxThisRound;
         }
      }

      long worstMs = worstNs / 1_000_000;
      assertTrue(worstMs < LATENCY_THRESHOLD_MS,
            "Maximale Burst-Input-Latenz überschreitet 100 ms: " + worstMs + " ms");
   }

   // ──────────────────────────────────────────────────────────────────────────
   // Durchschnittliche Latenz (Regression Guard)
   // ──────────────────────────────────────────────────────────────────────────

   @Test
   @Timeout(value = 10, unit = TimeUnit.SECONDS)
   @DisplayName("Durchschnittliche Eingabe-Latenz < 10 ms")
   void averageInputLatency() {
      char[] directions = { 'w', 'a', 's', 'd' };
      long totalNs = 0;

      for (int i = 0; i < SAMPLE_COUNT; i++) {
         long t0 = System.nanoTime();
         player.move(directions[i % directions.length], map);
         totalNs += System.nanoTime() - t0;
      }

      long avgNs = totalNs / SAMPLE_COUNT;
      long avgMs = avgNs / 1_000_000;

      // Durchschnitt deutlich unter 100 ms als Regressionssicherung
      assertTrue(avgMs < 10,
            "Durchschnittliche Bewegungs-Latenz überschreitet 10 ms: " + avgMs + " ms"
                  + " (Einzelwert ns: " + avgNs + ")");
   }
}
