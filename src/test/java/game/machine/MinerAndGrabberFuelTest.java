package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinerAndGrabberFuelTest {

    @Test
    void minerOnIronDepositRequiresCoalToProduce() {
        Tile tile = new Tile(TileType.IRON_DEPOSIT);
        Miner miner = new Miner(tile);
        tile.setMachine(miner);

        miner.tick();
        assertFalse(miner.hasOutput());

        assertTrue(miner.tryInsertInput(new ItemStack(ItemType.COAL, 1)));
        miner.tick();

        assertTrue(miner.hasOutput());
        assertEquals(ItemType.IRON_ORE, miner.getOutputBuffer().getType());
    }

    @Test
    void coalMinerUsesMinedCoalAsFuel() {
        Tile tile = new Tile(TileType.COAL_DEPOSIT);
        Miner miner = new Miner(tile);
        tile.setMachine(miner);

        // First cycle is used to bootstrap fuel from mined coal.
        miner.tick();
        assertFalse(miner.hasOutput());

        miner.tick();
        assertTrue(miner.hasOutput());
        assertEquals(ItemType.COAL, miner.getOutputBuffer().getType());
    }

    @Test
    void grabberTransfersOnlyWhenFueled() {
        WorldMap map = new WorldMap(5, 3);

        Tile src = map.getTile(1, 1);
        src.setType(TileType.CONVEYOR_BELT);
        src.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));

        Tile grabberTile = map.getTile(2, 1);
        Grabber grabber = new Grabber(grabberTile, map, 2, 1, -1, 0, 1, 0);
        grabberTile.setMachine(grabber);

        Tile dst = map.getTile(3, 1);
        dst.setType(TileType.CONVEYOR_BELT);

        grabber.tick();
        assertTrue(src.hasItem());
        assertFalse(dst.hasItem());

        assertTrue(grabber.tryInsertInput(new ItemStack(ItemType.COAL, 1)));
        grabber.tick();

        assertFalse(src.hasItem());
        assertTrue(dst.hasItem());
        assertEquals(ItemType.IRON_ORE, dst.getItemOnGround().getType());
    }
}
