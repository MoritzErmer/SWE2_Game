package game.entity;

import game.world.WorldMap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repräsentiert den Spielercharakter mit Inventar, Health und Position.
 * Health ist thread-sicher durch AtomicInteger, da mehrere Threads
 * (z.B. Kollisionen, Umweltschaden) gleichzeitig darauf zugreifen können.
 */
public class PlayerCharacter {
   private final AtomicInteger health = new AtomicInteger(100);
   private final AtomicInteger maxHealth = new AtomicInteger(100);
   private final List<ItemStack> inventory = new ArrayList<>();
   private final int inventorySize;
   private volatile int x, y; // volatile für Thread-Sichtbarkeit

   // Hotbar: Die ersten HOTBAR_SLOTS Einträge im Inventar
   public static final int HOTBAR_SLOTS = 9;
   private volatile int selectedHotbarSlot = 0;

   public PlayerCharacter(int startX, int startY, int inventorySize) {
      this.x = startX;
      this.y = startY;
      this.inventorySize = inventorySize;
   }

   public PlayerCharacter(int startX, int startY) {
      this(startX, startY, 20);
   }

   // --- Health ---

   public int getHealth() {
      return health.get();
   }

   public int getMaxHealth() {
      return maxHealth.get();
   }

   public boolean isAlive() {
      return health.get() > 0;
   }

   public void damage(int amount) {
      health.updateAndGet(h -> Math.max(0, h - amount));
   }

   public void heal(int amount) {
      health.updateAndGet(h -> Math.min(maxHealth.get(), h + amount));
   }

   // --- Inventory ---

   public List<ItemStack> getInventory() {
      return inventory;
   }

   public int getInventorySize() {
      return inventorySize;
   }

   /**
    * Fügt ein Item zum Inventar hinzu. Stapelt auf bestehende ItemStacks gleichen
    * Typs.
    * 
    * @return true wenn erfolgreich, false wenn Inventar voll.
    */
   public boolean addItem(ItemType type, int amount) {
      // Versuche auf bestehenden Stack zu stapeln
      Optional<ItemStack> existing = inventory.stream()
            .filter(s -> s.getType() == type)
            .findFirst();
      if (existing.isPresent()) {
         existing.get().add(amount);
         return true;
      }
      // Neuen Stack anlegen
      if (inventory.size() < inventorySize) {
         inventory.add(new ItemStack(type, amount));
         return true;
      }
      return false; // Inventar voll
   }

   /**
    * Entfernt eine bestimmte Menge eines Items aus dem Inventar.
    * 
    * @return true wenn genug vorhanden war, false sonst.
    */
   public boolean removeItem(ItemType type, int amount) {
      Optional<ItemStack> existing = inventory.stream()
            .filter(s -> s.getType() == type && s.getAmount() >= amount)
            .findFirst();
      if (existing.isPresent()) {
         existing.get().remove(amount);
         if (existing.get().getAmount() <= 0) {
            inventory.remove(existing.get());
         }
         return true;
      }
      return false;
   }

   public int getItemCount(ItemType type) {
      return inventory.stream()
            .filter(s -> s.getType() == type)
            .mapToInt(ItemStack::getAmount)
            .sum();
   }

   // --- Hotbar ---

   public int getSelectedHotbarSlot() {
      return selectedHotbarSlot;
   }

   public void setSelectedHotbarSlot(int slot) {
      if (slot >= 0 && slot < HOTBAR_SLOTS) {
         this.selectedHotbarSlot = slot;
      }
   }

   /**
    * Gibt den ItemStack im aktuell gewählten Hotbar-Slot zurück, oder null.
    */
   public ItemStack getSelectedItem() {
      if (selectedHotbarSlot < inventory.size()) {
         return inventory.get(selectedHotbarSlot);
      }
      return null;
   }

   /**
    * Entfernt ein Item aus dem aktuell gewählten Hotbar-Slot (für Platzierung).
    * 
    * @return true wenn erfolgreich.
    */
   public boolean consumeSelectedItem(int amount) {
      ItemStack selected = getSelectedItem();
      if (selected != null && selected.getAmount() >= amount) {
         selected.remove(amount);
         if (selected.getAmount() <= 0) {
            inventory.remove(selected);
         }
         return true;
      }
      return false;
   }

   // --- Movement ---

   public int getX() {
      return x;
   }

   public int getY() {
      return y;
   }

   public void setPosition(int x, int y) {
      this.x = x;
      this.y = y;
   }

   /**
    * Bewegt den Charakter um eine Zelle in die angegebene Richtung (WASD).
    * Prüft Grenzen anhand der WorldMap.
    */
   public void move(char direction, WorldMap map) {
      switch (Character.toLowerCase(direction)) {
         case 'w':
            y = Math.max(0, y - 1);
            break;
         case 'a':
            x = Math.max(0, x - 1);
            break;
         case 's':
            y = Math.min(map.getHeight() - 1, y + 1);
            break;
         case 'd':
            x = Math.min(map.getWidth() - 1, x + 1);
            break;
         default:
            break;
      }
   }

   /**
    * Bewegt den Charakter ohne WorldMap-Referenz (Rückwärtskompatibilität).
    */
   public void move(char direction) {
      switch (Character.toLowerCase(direction)) {
         case 'w':
            y = Math.max(0, y - 1);
            break;
         case 'a':
            x = Math.max(0, x - 1);
            break;
         case 's':
            y = y + 1;
            break;
         case 'd':
            x = x + 1;
            break;
         default:
            break;
      }
   }
}
