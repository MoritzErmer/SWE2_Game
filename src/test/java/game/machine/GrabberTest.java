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

class GrabberTest {

    @Test
    void grabberIgnoresGroundItemOnNonConveyorSource() {
        WorldMap map = new WorldMap(5, 5);
        Grabber grabber = placeGrabber(map, 2, 2, -1, 0, 1, 0);

        Tile source = map.getTile(1, 2);
        Tile destination = map.getTile(3, 2);
        source.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));

        grabber.tick();

        assertTrue(source.hasItem(), "Ground item on non-conveyor source must not be grabbed.");
        assertFalse(destination.hasItem(), "Destination should stay empty when source is invalid.");
    }

    @Test
    void grabberCanTakeGroundItemFromConveyorSource() {
        WorldMap map = new WorldMap(5, 5);
        Grabber grabber = placeGrabber(map, 2, 2, -1, 0, 1, 0);

        Tile source = map.getTile(1, 2);
        Tile destination = map.getTile(3, 2);
        source.setType(TileType.CONVEYOR_BELT);
        source.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));

        grabber.tick();

        assertFalse(source.hasItem(), "Conveyor source should be emptied by a successful grab.");
        assertTrue(destination.hasItem(), "Destination should receive item from conveyor source.");
        assertEquals(ItemType.IRON_ORE, destination.getItemOnGround().getType());
        assertEquals(1, destination.getItemOnGround().getAmount());
    }

    @Test
    void grabberCanStillTakeMachineOutput() {
        WorldMap map = new WorldMap(5, 5);
        Grabber grabber = placeGrabber(map, 2, 2, -1, 0, 1, 0);

        Tile source = map.getTile(1, 2);
        Tile destination = map.getTile(3, 2);

        Smelter sourceMachine = new Smelter(source);
        source.setMachine(sourceMachine);
        sourceMachine.setOutputBuffer(new ItemStack(ItemType.IRON_ORE, 1));

        grabber.tick();

        assertFalse(sourceMachine.hasOutput(), "Machine output should be consumed by grabber.");
        assertTrue(destination.hasItem(), "Destination should receive machine output.");
        assertEquals(ItemType.IRON_ORE, destination.getItemOnGround().getType());
        assertEquals(1, destination.getItemOnGround().getAmount());
    }

    @Test
    void grabberDoesNotMergeWithOccupiedGroundDestination() {
        WorldMap map = new WorldMap(5, 5);
        Grabber grabber = placeGrabber(map, 2, 2, -1, 0, 1, 0);

        Tile source = map.getTile(1, 2);
        Tile destination = map.getTile(3, 2);
        source.setType(TileType.CONVEYOR_BELT);
        source.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));
        destination.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));

        grabber.tick();

        assertTrue(source.hasItem(), "Failed delivery must keep item at source.");
        assertTrue(destination.hasItem(), "Destination keeps its original item.");
        assertEquals(1, source.getItemOnGround().getAmount());
        assertEquals(1, destination.getItemOnGround().getAmount());
    }

    private Grabber placeGrabber(WorldMap map, int x, int y, int sourceDx, int sourceDy, int destDx, int destDy) {
        Tile grabberTile = map.getTile(x, y);
        Grabber grabber = new Grabber(grabberTile, map, x, y, sourceDx, sourceDy, destDx, destDy);
        grabberTile.setMachine(grabber);
        return grabber;
    }
}