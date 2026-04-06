package game.world;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.machine.Smelter;
import org.junit.jupiter.api.Test;

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
        assertTrue(from.hasItem());
        assertEquals(1, from.getItemOnGround().getAmount());
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
    void tileRejectsGroundStacks() {
        Tile tile = new Tile();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> tile.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 2)));

        assertTrue(ex.getMessage().contains("may not stack"));
    }

    @Test
    void tileRejectsItemPlacementOnMachineTile() {
        Tile tile = new Tile();
        tile.setMachine(new Smelter(tile));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> tile.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1)));

        assertTrue(ex.getMessage().contains("machine tile"));
    }

    @Test
    void tileRejectsMachinePlacementOnOccupiedTile() {
        Tile tile = new Tile();
        tile.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 1));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> tile.setMachine(new Smelter(tile)));

        assertTrue(ex.getMessage().contains("contains an item"));
    }
}
