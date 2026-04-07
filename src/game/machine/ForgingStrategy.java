package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;

/**
 * ForgingStrategy: 8x Iron Plate -> 1x Iron Gear.
 * Brennstofflogik wird über Forge verwaltet (1 Kohle pro 3 Zahnräder).
 */
public class ForgingStrategy implements ProductionStrategy {

   @Override
   public void produce(BaseMachine machine) {
      if (!(machine instanceof Forge)) {
         return;
      }

      Forge forge = (Forge) machine;
      if (!forge.hasFuel() || !machine.hasInput()) {
         return;
      }

      ItemStack input = machine.getInputBuffer();
      if (input.getType() != ItemType.IRON_PLATE || input.getAmount() < 8) {
         return;
      }

      if (machine.hasOutput() && machine.getOutputBuffer().getType() != ItemType.IRON_GEAR) {
         return;
      }

      input.remove(8);
      if (input.getAmount() <= 0) {
         machine.setInputBuffer(null);
      }

      if (machine.getOutputBuffer() == null) {
         machine.setOutputBuffer(new ItemStack(ItemType.IRON_GEAR, 1));
      } else {
         machine.getOutputBuffer().add(1);
      }

      forge.onGearProduced();
   }
}