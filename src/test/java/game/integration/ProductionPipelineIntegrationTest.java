package game.integration;

import game.core.GameSupervisor;
import game.logistics.ConveyorBelt;
import game.logistics.TransportRobot;
import game.machine.BaseMachine;
import game.machine.Grabber;
import game.machine.Miner;
import game.machine.Smelter;
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

        Tile grabberTile = map.getTile(2, 1);
        Grabber grabber = new Grabber(grabberTile, map, 2, 1, -1, 0, 1, 0);
        grabberTile.setMachine(grabber);

        Tile smelterTile = map.getTile(3, 1);
        Smelter smelter = new Smelter(smelterTile);
        smelterTile.setMachine(smelter);

        List<BaseMachine> machines = new CopyOnWriteArrayList<>();
        machines.add(miner);
        machines.add(grabber);
        machines.add(smelter);

        List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();
        List<TransportRobot> robots = new CopyOnWriteArrayList<>();

        GameSupervisor supervisor = new GameSupervisor(map, machines, belts, robots);
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
}
