package game.world;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.machine.Smelter;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class WorldMapTest {

    @Test
    void transferItemMovesSingleItemWhenTargetIsEmpty() {
        WorldMap map = new WorldMap(3, 3);
        Tile from = map.getTile(0, 0);
        Tile to = map.getTile(1, 0);

        from.setType(TileType.CONVEYOR_BELT);
        to.setType(TileType.CONVEYOR_BELT);

        from.getLock().lock();
        try {
            from.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));
        } finally {
            from.getLock().unlock();
        }

        boolean moved = map.transferItem(from, to);

        assertTrue(moved);
        assertFalse(from.hasItem());
        assertTrue(to.hasItem());
        assertEquals(ItemType.IRON_ORE, to.getItemOnGround().getType());
        assertEquals(1, to.getItemOnGround().getAmount());
    }

    @Test
    void transferItemReturnsFalseWhenTargetAlreadyOccupied() {
        WorldMap map = new WorldMap(3, 3);
        Tile from = map.getTile(0, 0);
        Tile to = map.getTile(1, 0);

        from.setType(TileType.CONVEYOR_BELT);
        to.setType(TileType.CONVEYOR_BELT);

        from.getLock().lock();
        to.getLock().lock();
        try {
            from.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));
            to.setItemOnGround(new ItemStack(ItemType.COAL, 1));
        } finally {
            to.getLock().unlock();
            from.getLock().unlock();
        }

        boolean moved = map.transferItem(from, to);

        assertFalse(moved);
        assertEquals(ItemType.IRON_ORE, from.getItemOnGround().getType());
        assertEquals(ItemType.COAL, to.getItemOnGround().getType());
    }

    @Test
    void transferItemReturnsFalseWhenTargetHasMachine() {
        WorldMap map = new WorldMap(3, 3);
        Tile from = map.getTile(0, 0);
        Tile to = map.getTile(1, 0);
        from.setType(TileType.CONVEYOR_BELT);

        from.getLock().lock();
        to.getLock().lock();
        try {
            from.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));
            to.setMachine(new Smelter(to));
        } finally {
            to.getLock().unlock();
            from.getLock().unlock();
        }

        boolean moved = map.transferItem(from, to);

        assertFalse(moved);
        assertTrue(from.hasItem());
        assertFalse(to.hasItem());
        assertTrue(to.hasMachine());
    }

    @Test
    void transferItemReturnsFalseWhenSourceIsNotConveyor() {
        WorldMap map = new WorldMap(3, 3);
        Tile from = map.getTile(0, 0);
        Tile to = map.getTile(1, 0);
        to.setType(TileType.CONVEYOR_BELT);

        from.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));
        assertFalse(from.hasItem(), "Non-conveyor source must reject ground items.");

        boolean moved = map.transferItem(from, to);

        assertFalse(moved);
        assertFalse(to.hasItem());
    }

    @Test
    void setTypeClearsGroundItemWhenLeavingConveyor() {
        Tile tile = new Tile();
        tile.setType(TileType.CONVEYOR_BELT);
        tile.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));
        assertTrue(tile.hasItem());

        tile.setType(TileType.EMPTY);

        assertFalse(tile.hasItem());
    }

    @Test
    void tileIgnoresGroundStacksOnNonConveyorTile() {
        Tile tile = new Tile();

        tile.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 2));

        assertFalse(tile.hasItem());
    }

    @Test
    void tileIgnoresItemPlacementOnMachineTile() {
        Tile tile = new Tile();
        tile.setMachine(new Smelter(tile));

        tile.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));

        assertFalse(tile.hasItem());
    }

    @Test
    void tileAllowsMachinePlacementWhenNonConveyorGroundItemWasIgnored() {
        Tile tile = new Tile();
        tile.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));

        assertFalse(tile.hasItem());
        assertDoesNotThrow(() -> tile.setMachine(new Smelter(tile)));
        assertTrue(tile.hasMachine());
    }

    @Test
    void transferItemReturnsFalseForRocketDestination() {
        WorldMap map = new WorldMap(4, 4);
        Tile from = map.getTile(0, 0);
        Tile to = map.getTile(1, 0);

        from.setType(TileType.CONVEYOR_BELT);
        to.setType(TileType.ROCKET);

        from.getLock().lock();
        try {
            from.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));
        } finally {
            from.getLock().unlock();
        }

        boolean moved = map.transferItem(from, to);

        assertFalse(moved);
        assertTrue(from.hasItem());
        assertFalse(to.hasItem());
    }

    @Test
    void findRandomAreaOriginReturnsInBoundsOrigin() {
        WorldMap map = new WorldMap(12, 10);
        int[] origin = map.findRandomAreaOrigin(4, 4, new Random(42));

        assertTrue(origin[0] >= 0);
        assertTrue(origin[1] >= 0);
        assertTrue(origin[0] <= map.getWidth() - 4);
        assertTrue(origin[1] <= map.getHeight() - 4);
    }
}
