package game.entity;

import java.awt.Color;

/**
 * Enum für verschiedene Item-Typen im Spiel.
 * Jeder Typ hat eine Abbauzeit (in ms), eine Darstellungsfarbe und ob er
 * platzierbar ist.
 */
public enum ItemType {
   IRON_ORE("Iron Ore", 1500, new Color(139, 90, 43), false),
   COPPER_ORE("Copper Ore", 1200, new Color(184, 115, 51), false),
   IRON_PLATE("Iron Plate", 0, new Color(180, 180, 200), true),
   COPPER_PLATE("Copper Plate", 0, new Color(210, 140, 80), true),
   COAL("Coal", 800, new Color(40, 40, 40), false),
   IRON_GEAR("Iron Gear", 0, new Color(160, 160, 180), true),
   CONVEYOR_BELT_ITEM("Conveyor Belt", 0, new Color(100, 100, 110), true),
   MINER_KIT("Miner", 0, new Color(200, 180, 60), true),
   SMELTER_KIT("Smelter", 0, new Color(220, 100, 50), true),
   GRABBER_KIT("Grabber", 0, new Color(120, 200, 120), true);

   private final String displayName;
   private final int miningTimeMs; // Abbauzeit in Millisekunden (0 = nicht abbaubar)
   private final Color color;
   private final boolean placeable;

   ItemType(String displayName, int miningTimeMs, Color color, boolean placeable) {
      this.displayName = displayName;
      this.miningTimeMs = miningTimeMs;
      this.color = color;
      this.placeable = placeable;
   }

   public String getDisplayName() {
      return displayName;
   }

   public int getMiningTimeMs() {
      return miningTimeMs;
   }

   public Color getColor() {
      return color;
   }

   public boolean isPlaceable() {
      return placeable;
   }
}
