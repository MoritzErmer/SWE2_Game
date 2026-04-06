package game.integration;

import game.core.GameSupervisor;
import game.logistics.ConveyorBelt;
import game.machine.BaseMachine;
import game.machine.Forge;
import game.machine.Grabber;
import game.machine.Miner;
import game.machine.Smelter;
import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductionPipelineIntegrationTest {

   @Test
   void minerGrabberSmelterPipelineProducesPlates() throws InterruptedException {
      WorldMap map = new WorldMap(6, 4);

        Tile minerTile = map.getTile(1, 1);
        minerTile.setType(TileType.IRON_DEPOSIT);
        Miner miner = new Miner(minerTile);
        minerTile.setMachine(miner);
        assertTrue(miner.tryInsertInput(new ItemStack(ItemType.COAL, 2)));

        Tile grabberTile = map.getTile(2, 1);
        Grabber grabber = new Grabber(grabberTile, map, 2, 1, -1, 0, 1, 0);
        grabberTile.setMachine(grabber);
        assertTrue(grabber.tryInsertInput(new ItemStack(ItemType.COAL, 2)));

      Tile smelterTile = map.getTile(3, 1);
      Smelter smelter = new Smelter(smelterTile);
      smelterTile.setMachine(smelter);

      List<BaseMachine> machines = new CopyOnWriteArrayList<>();
      machines.add(miner);
      machines.add(grabber);
      machines.add(smelter);

      List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();

      GameSupervisor supervisor = new GameSupervisor(map, machines, belts);
      supervisor.start();

      try {
         long deadline = System.currentTimeMillis() + 6000;
         boolean produced = false;
         while (System.currentTimeMillis() < deadline) {
            smelterTile.getLock().lock();
            try {
               produced = smelter.hasOutput() && smelter.getOutputBuffer().getAmount() > 0;
               if (produced) {
                  break;
               }
            } finally {
               smelterTile.getLock().unlock();
            }
            Thread.sleep(150);
         }

            assertTrue(produced, "Pipeline should eventually produce iron plates in smelter output.");
        } finally {
            supervisor.stop();
        }
    }

    @Test
    void minerSmelterForgePipelineProducesIronGears() throws InterruptedException {
        WorldMap map = new WorldMap(9, 4);

        Tile minerTile = map.getTile(1, 1);
        minerTile.setType(TileType.IRON_DEPOSIT);
        Miner miner = new Miner(minerTile);
        minerTile.setMachine(miner);
        assertTrue(miner.tryInsertInput(new ItemStack(ItemType.COAL, 4)));

        // Miner output -> Smelter input
        Tile grabber1Tile = map.getTile(2, 1);
        Grabber grabber1 = new Grabber(grabber1Tile, map, 2, 1, -1, 0, 1, 0);
        grabber1Tile.setMachine(grabber1);
        assertTrue(grabber1.tryInsertInput(new ItemStack(ItemType.COAL, 4)));

        Tile smelterTile = map.getTile(3, 1);
        Smelter smelter = new Smelter(smelterTile);
        smelterTile.setMachine(smelter);

        // Smelter output -> Forge input (incoming from Forge back side)
        Tile grabber2Tile = map.getTile(4, 1);
        Grabber grabber2 = new Grabber(grabber2Tile, map, 4, 1, -1, 0, 1, 0);
        grabber2Tile.setMachine(grabber2);
        assertTrue(grabber2.tryInsertInput(new ItemStack(ItemType.COAL, 4)));

        Tile forgeTile = map.getTile(5, 1);
        Forge forge = new Forge(forgeTile);
        forge.setDirection(game.machine.Direction.RIGHT);
        forge.tryInsertInput(new ItemStack(ItemType.COAL, 2));
        forgeTile.setMachine(forge);

        // Forge output (front/right) -> ground
        Tile grabber3Tile = map.getTile(6, 1);
        Grabber grabber3 = new Grabber(grabber3Tile, map, 6, 1, -1, 0, 1, 0);
        grabber3Tile.setMachine(grabber3);
        assertTrue(grabber3.tryInsertInput(new ItemStack(ItemType.COAL, 4)));

        Tile outputTile = map.getTile(7, 1);
        outputTile.setType(TileType.CONVEYOR_BELT);

        List<BaseMachine> machines = new CopyOnWriteArrayList<>();
        machines.add(miner);
        machines.add(grabber1);
        machines.add(smelter);
        machines.add(grabber2);
        machines.add(forge);
        machines.add(grabber3);

        List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();
        List<TransportRobot> robots = new CopyOnWriteArrayList<>();

        GameSupervisor supervisor = new GameSupervisor(map, machines, belts, robots);
        supervisor.start();

        try {
            long deadline = System.currentTimeMillis() + 12000;
            boolean produced = false;

            while (System.currentTimeMillis() < deadline) {
                outputTile.getLock().lock();
                try {
                    produced = outputTile.hasItem() && outputTile.getItemOnGround().getType() == ItemType.IRON_GEAR;
                    if (produced) {
                        break;
                    }
                } finally {
                    outputTile.getLock().unlock();
                }
                Thread.sleep(150);
            }

            assertTrue(produced, "Pipeline should eventually place iron gears on the output tile.");
        } finally {
            supervisor.stop();
        }
    }
}
