package game.integration;

import game.core.GameSupervisor;
import game.logistics.ConveyorBelt;
import game.machine.BaseMachine;
import game.world.WorldMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupervisorLifecycleE2ETest {

   @Test
   void startAndStopSupervisorWithoutDeadlock() throws InterruptedException {
      WorldMap map = new WorldMap(20, 20);
      List<BaseMachine> machines = new CopyOnWriteArrayList<>();
      List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();

      GameSupervisor supervisor = new GameSupervisor(map, machines, belts);

      supervisor.start();
      Thread.sleep(250);
      assertTrue(supervisor.getRunning().get());

      supervisor.stop();
      assertFalse(supervisor.getRunning().get());
   }
}
