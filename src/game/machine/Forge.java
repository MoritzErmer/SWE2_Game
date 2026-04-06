package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;

import java.util.List;

/**
 * Forge: Produziert Zahnräder aus Eisenplatten und verbraucht Kohle als Brennstoff.
 *
 * IO-Mapping relativ zur Front (direction):
 * - Output: Front
 * - Eisenplatten-Input: Back
 * - Kohle-Input: Left oder Right
 */
public class Forge extends BaseMachine {

   private static final int GEARS_PER_COAL = 3;
   private static final int MAX_COAL_BUFFER = 32;
   private static final int MAX_OUTPUT = 5;

   private int coalUnits = 0;
   private int producedGearsSinceCoal = 0;

   public Forge(Tile tile) {
      super("Forge", tile, new ForgingStrategy());
   }

   @Override
   public boolean tryInsertInput(ItemStack item) {
      if (item == null || item.getAmount() <= 0)
         return false;

      if (item.getType() == ItemType.COAL) {
         return tryInsertCoal(item.getAmount());
      }

      if (item.getType() == ItemType.IRON_PLATE) {
         return super.tryInsertInput(item);
      }

      return false;
   }

   // Side-aware insertion hook (kept without @Override for compatibility).
   @Override
   public boolean tryInsertInputFromSide(ItemStack item, Direction incomingSide) {
      if (item == null || incomingSide == null)
         return false;

      if (item.getType() == ItemType.IRON_PLATE) {
         if (incomingSide != getBackSide())
            return false;
         return tryInsertInput(item);
      }

      if (item.getType() == ItemType.COAL) {
         if (incomingSide != getLeftSide() && incomingSide != getRightSide())
            return false;
         return tryInsertInput(item);
      }

      return false;
   }

   // Side-aware extraction hook (kept without @Override for compatibility).
   public boolean canExtractOutputFromSide(Direction extractionSide) {
      return extractionSide == getFrontSide();
   }

   public boolean hasFuel() {
      return coalUnits > 0;
   }

   public int getCoalUnits() {
      return coalUnits;
   }

   public int getProducedGearsSinceCoal() {
      return producedGearsSinceCoal;
   }

   @Override
   public List<ItemStack> drainStoredItems() {
      List<ItemStack> drained = super.drainStoredItems();
      if (coalUnits > 0) {
         drained.add(new ItemStack(ItemType.COAL, coalUnits));
      }
      coalUnits = 0;
      producedGearsSinceCoal = 0;
      return drained;
   }

   public void onGearProduced() {
      if (!hasFuel())
         return;

      producedGearsSinceCoal++;
      if (producedGearsSinceCoal >= GEARS_PER_COAL) {
         coalUnits = Math.max(0, coalUnits - 1);
         producedGearsSinceCoal = 0;
      }
   }

   public boolean isActiveForAnimation() {
      if (!hasFuel())
         return false;

      if (!hasInput() || inputBuffer.getType() != ItemType.IRON_PLATE || inputBuffer.getAmount() < 2)
         return false;

      return outputBuffer == null
         || (outputBuffer.getType() == ItemType.IRON_GEAR && outputBuffer.getAmount() < MAX_OUTPUT);
   }

   private boolean tryInsertCoal(int amount) {
      if (amount <= 0)
         return false;
      if (coalUnits + amount > MAX_COAL_BUFFER)
         return false;
      coalUnits += amount;
      return true;
   }

   private Direction getFrontSide() {
      return getDirection();
   }

   private Direction getBackSide() {
      return getFrontSide().opposite();
   }

   private Direction getRightSide() {
      return getFrontSide().rotateClockwise();
   }

   private Direction getLeftSide() {
      return getRightSide().opposite();
   }
}