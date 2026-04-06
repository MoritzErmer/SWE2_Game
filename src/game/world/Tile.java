package game.world;

import game.entity.ItemStack;
import game.machine.BaseMachine;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Tile repräsentiert eine einzelne Zelle im WorldMap-Grid.
 * Jedes Tile besitzt ein eigenes ReentrantLock für parallele Zugriffe.
 *
 * Warum ReentrantLock statt synchronized?
 * - Granulare Locks: Jedes Tile hat sein eigenes Lock, so können verschiedene
 * Threads parallel auf verschiedene Tiles zugreifen.
 * - Mit synchronized müsste man entweder auf dem gesamten Grid oder dem
 * Tile-Objekt
 * selbst synchronisieren, was die Parallelität deutlich einschränkt.
 * - ReentrantLock erlaubt tryLock(), timed locks und faire Scheduling-Optionen,
 * die mit synchronized nicht möglich sind.
 */
public class Tile {
   private final ReentrantLock lock = new ReentrantLock();
   private TileType type;
   private TileType originalType; // Bewahrt den ursprünglichen Typ (z.B. Deposit) wenn eine Maschine platziert
                                  // wird
   private ItemStack itemOnGround; // Item das auf dem Tile liegt (z.B. durch Fließband)
   private BaseMachine machine; // Maschine die auf dem Tile platziert ist

   public Tile() {
      this.type = TileType.EMPTY;
      this.originalType = TileType.EMPTY;
   }

   public Tile(TileType type) {
      this.type = type;
      this.originalType = type;
   }

   public ReentrantLock getLock() {
      return lock;
   }

   public TileType getType() {
      return type;
   }

   public void setType(TileType type) {
      this.type = type;
      this.originalType = type;
   }

   /** Gibt den ursprünglichen Tile-Typ zurück (vor Maschinen-Platzierung). */
   public TileType getOriginalType() {
      return originalType;
   }

   /** Gibt das auf dem Boden liegende Item zurück (kann null sein). */
   public ItemStack getItemOnGround() {
      return itemOnGround;
   }

   /**
    * True if this tile may hold ground items.
    * Rocket pads reject automated ground-item delivery and manual drops.
    */
   public boolean canAcceptGroundItem() {
      return machine == null && type != TileType.ROCKET_PAD && type != TileType.MACHINE;
   }

   /**
    * Legt ein Item auf das Tile. Thread-sicher nur innerhalb eines lock()-Blocks
    * aufrufen!
    */
   public void setItemOnGround(ItemStack item) {
      if (item != null && !canAcceptGroundItem()) {
         return;
      }
      this.itemOnGround = item;
   }

   /** Entfernt das Item vom Tile und gibt es zurück. */
   public ItemStack pickupItem() {
      ItemStack item = this.itemOnGround;
      this.itemOnGround = null;
      return item;
   }

   public BaseMachine getMachine() {
      return machine;
   }

   public void setMachine(BaseMachine machine) {
      this.machine = machine;
      if (machine != null) {
         this.type = TileType.MACHINE;
      }
   }

   /**
    * Entfernt die Maschine vom Tile und stellt den ursprünglichen Tile-Typ
    * wieder her (z.B. IRON_DEPOSIT wenn ein Miner entfernt wird).
    */
   public void removeMachine() {
      this.machine = null;
      this.type = this.originalType;
   }

   public boolean hasMachine() {
      return machine != null;
   }

   public boolean hasItem() {
      return itemOnGround != null;
   }

   public boolean isEmpty() {
      return type == TileType.EMPTY && machine == null && itemOnGround == null;
   }

   public boolean isResourceDeposit() {
      return type == TileType.IRON_DEPOSIT
            || type == TileType.COPPER_DEPOSIT
            || type == TileType.COAL_DEPOSIT;
   }
}
