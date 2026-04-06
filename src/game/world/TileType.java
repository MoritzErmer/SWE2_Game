package game.world;

import game.entity.ItemType;

/**
 * Definiert den Typ eines Tiles: Leer, Ressourcen-Vorkommen, oder
 * Maschine/Belt.
 */
public enum TileType {
   EMPTY,
   IRON_DEPOSIT,
   COPPER_DEPOSIT,
   COAL_DEPOSIT,
   MACHINE,
   CONVEYOR_BELT,
   ROCKET_PAD;

   /**
    * Gibt den ItemType zurück, den dieses Vorkommen beim Abbauen liefert.
    * 
    * @return ItemType oder null wenn kein Ressourcen-Vorkommen.
    */
   public ItemType getMinedItem() {
      switch (this) {
         case IRON_DEPOSIT:
            return ItemType.IRON_ORE;
         case COPPER_DEPOSIT:
            return ItemType.COPPER_ORE;
         case COAL_DEPOSIT:
            return ItemType.COAL;
         default:
            return null;
      }
   }

   /**
    * Gibt die Abbauzeit in ms zurück (aus dem zugehörigen ItemType).
    * 
    * @return Abbauzeit in ms, oder 0 wenn nicht abbaubar.
    */
   public int getMiningTimeMs() {
      ItemType item = getMinedItem();
      return item != null ? item.getMiningTimeMs() : 0;
   }
}
