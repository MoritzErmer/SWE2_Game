package game.world;

import game.entity.ItemStack;
import game.entity.ItemType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldMapTest {

    @Test
    void transferItemMovesStackWhenTargetIsEmpty() {
        WorldMap map = new WorldMap(3, 3);
        Tile from = map.getTile(0, 0);
        Tile to = map.getTile(1, 0);

        from.getLock().lock();
        try {
            from.setItemOnGround(new ItemStack(ItemType.IRON_ORE, 2));
        } finally {
            from.getLock().unlock();
        }

        boolean moved = map.transferItem(from, to);

        assertTrue(moved);
        assertFalse(from.hasItem());
        assertTrue(to.hasItem());
        assertEquals(ItemType.IRON_ORE, to.getItemOnGround().getType());
        assertEquals(2, to.getItemOnGround().getAmount());
    }

    @Test
    void transferItemReturnsFalseWhenTargetAlreadyOccupied() {
        WorldMap map = new WorldMap(3, 3);
        Tile from = map.getTile(0, 0);
        Tile to = map.getTile(1, 0);

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
}
