package game.machine;

import game.entity.ItemStack;
import game.world.Tile;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstrakte Basisklasse für alle Maschinen.
 * Nutzt das Strategy-Pattern für verschiedene Produktionstypen.
 * Jede Maschine ist einem Tile zugeordnet und wird vom Producer-Thread-Pool
 * getriggert.
 *
 * Maschinen haben einen internen Input- und Output-Buffer (je 1 ItemStack).
 * Greifer/Inserter bewegen Items zwischen diesen Buffern und der Welt.
 *
 * Jede Maschine hat eine Ausrichtung (Direction), die mit R rotiert werden
 * kann.
 */
public abstract class BaseMachine {
   protected static final int DEFAULT_MAX_OUTPUT = 5;

   protected final Tile tile;
   protected ProductionStrategy strategy;
   protected final String name;

   // Ausrichtung der Maschine (Standard: nach rechts)
   protected Direction direction = Direction.RIGHT;

   // Interne Buffer für Produktion
   protected ItemStack inputBuffer; // Eingangs-Slot (z.B. Erz für Smelter)
   protected ItemStack outputBuffer; // Ausgangs-Slot (z.B. produziertes Erz / Platten)

   public BaseMachine(String name, Tile tile, ProductionStrategy strategy) {
      this.name = name;
      this.tile = tile;
      this.strategy = strategy;
   }

   /**
    * Wird vom ScheduledExecutorService periodisch aufgerufen.
    * Jeder tick() ist ein eigenständiger Task im Thread-Pool.
    */
   public void tick() {
      tile.getLock().lock();
      try {
         if (isOutputFull()) {
            return;
         }
         strategy.produce(this);
      } finally {
         tile.getLock().unlock();
      }
   }

   public Tile getTile() {
      return tile;
   }

   public String getName() {
      return name;
   }

   public void setStrategy(ProductionStrategy strategy) {
      this.strategy = strategy;
   }

   public ProductionStrategy getStrategy() {
      return strategy;
   }

   // ==================== Richtung ====================

   public Direction getDirection() {
      return direction;
   }

   public void setDirection(Direction direction) {
      this.direction = direction;
   }

   // ==================== Input/Output Buffer ====================

   public ItemStack getInputBuffer() {
      return inputBuffer;
   }

   public void setInputBuffer(ItemStack item) {
      this.inputBuffer = item;
   }

   /**
    * Versucht ein Item in den Input-Buffer zu legen. Gibt false zurück wenn voll
    * oder falscher Typ.
    */
   public boolean tryInsertInput(ItemStack item) {
      if (isOutputFull()) {
         return false;
      }

      if (inputBuffer == null) {
         inputBuffer = item;
         return true;
      }
      if (inputBuffer.getType() == item.getType()) {
         inputBuffer.add(item.getAmount());
         return true;
      }
      return false;
   }

   /**
    * Automation-Hook: versucht Input von einer bestimmten Seite einzulegen.
    * Standardverhalten bleibt abwärtskompatibel und ignoriert die Seite.
    */
   public boolean tryInsertInputFromSide(ItemStack item, Direction incomingSide) {
      return tryInsertInput(item);
   }

   public ItemStack getOutputBuffer() {
      return outputBuffer;
   }

   public void setOutputBuffer(ItemStack item) {
      this.outputBuffer = item;
   }

   /**
    * Entnimmt alle in der Maschine gespeicherten Items (Input + Output) und
    * leert die Buffer.
    */
   public List<ItemStack> drainStoredItems() {
      List<ItemStack> drained = new ArrayList<>();

      if (hasInput()) {
         drained.add(inputBuffer);
      }
      if (hasOutput()) {
         drained.add(outputBuffer);
      }

      inputBuffer = null;
      outputBuffer = null;
      return drained;
   }

   /** Entnimmt den gesamten Output-Buffer und gibt ihn zurück. */
   public ItemStack extractOutput() {
      ItemStack out = outputBuffer;
      outputBuffer = null;
      return out;
   }

   /**
    * Automation-Hook: ob Output von einer bestimmten Seite entnommen werden darf.
    * Standard: von allen Seiten erlaubt.
    */
   public boolean canExtractOutputFromSide(Direction extractionSide) {
      return true;
   }

   /** Prüft ob der Output-Buffer leer ist. */
   public boolean hasOutput() {
      return outputBuffer != null && outputBuffer.getAmount() > 0;
   }

   /** Prüft ob der Output-Buffer sein Standard-Limit erreicht hat. */
   public boolean isOutputFull() {
      return hasOutput() && outputBuffer.getAmount() >= DEFAULT_MAX_OUTPUT;
   }

   /** Prüft ob der Input-Buffer leer ist. */
   public boolean hasInput() {
      return inputBuffer != null && inputBuffer.getAmount() > 0;
   }

   /**
    * Gibt an, wie viele Pollution-Punkte diese Maschine pro Tick erzeugt.
    * Subklassen überschreiben diese Methode, um Pollution zu erzeugen.
    *
    * @return Pollution-Punkte pro Tick (Standard: 0)
    */
   public int getPollutionPerTick() {
      return 1;
   }

   @Override
   public String toString() {
      return name + " @ Tile(" + System.identityHashCode(tile) + ")";
   }
}
