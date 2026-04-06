package game.objective;

import game.entity.ItemType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RocketObjectiveTest {

   @Test
   void feedConsumesOnlyNeededAmount() {
      RocketObjective objective = new RocketObjective(10, 12);

      int consumed = objective.feed(ItemType.IRON_GEAR, 100);

      assertEquals(RocketObjective.REQUIRED_IRON_GEARS, consumed);
      assertEquals(RocketObjective.REQUIRED_IRON_GEARS, objective.getDeliveredIronGears());
      assertEquals(0, objective.getRemaining(ItemType.IRON_GEAR));
   }

   @Test
   void feedRejectsUnsupportedItems() {
      RocketObjective objective = new RocketObjective(4, 5);

      int consumed = objective.feed(ItemType.COAL, 12);

      assertEquals(0, consumed);
      assertEquals(0, objective.getDelivered(ItemType.IRON_GEAR));
      assertEquals(0, objective.getDelivered(ItemType.COPPER_PLATE));
      assertEquals(0, objective.getDelivered(ItemType.CONVEYOR_BELT_ITEM));
   }

   @Test
   void objectiveTransitionsToLaunchingOnlyWhenComplete() {
      RocketObjective objective = new RocketObjective(1, 1);

      objective.startLaunch(5000L);
      assertEquals(RocketObjective.Status.ACTIVE, objective.getStatus());

      objective.feed(ItemType.IRON_GEAR, RocketObjective.REQUIRED_IRON_GEARS);
      objective.feed(ItemType.COPPER_PLATE, RocketObjective.REQUIRED_COPPER_PLATES);
      objective.feed(ItemType.CONVEYOR_BELT_ITEM, RocketObjective.REQUIRED_CONVEYOR_BELTS);

      assertTrue(objective.isComplete());
      objective.startLaunch(7777L);
      assertEquals(RocketObjective.Status.LAUNCHING, objective.getStatus());
      assertEquals(7777L, objective.getLaunchStartedAtElapsedMs());

      objective.finishLaunch();
      assertEquals(RocketObjective.Status.ENDED, objective.getStatus());
   }

   @Test
   void occupiesMatchesFourByFourArea() {
      RocketObjective objective = new RocketObjective(20, 30);

      assertTrue(objective.occupies(20, 30));
      assertTrue(objective.occupies(23, 33));
      assertFalse(objective.occupies(24, 33));
      assertFalse(objective.occupies(23, 34));
      assertTrue(objective.isTopLeft(20, 30));
      assertFalse(objective.isTopLeft(21, 30));
   }
}
