package game.objective;

import game.entity.ItemType;
import game.save.GameSaveState;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RocketObjectiveTest {

    @Test
    void feedAcceptsOnlyRequiredItemsAndCapsAtRequirements() {
        WorldMap map = new WorldMap(24, 24);
        GameSaveState.RocketData data = new GameSaveState.RocketData();
        data.x = 5;
        data.y = 6;

        RocketObjective objective = RocketObjective.fromSaveData(data, map);

        assertEquals(0, objective.feed(ItemType.IRON_PLATE, 10));
        assertEquals(80, objective.feed(ItemType.IRON_GEAR, 120));
        assertEquals(0, objective.feed(ItemType.IRON_GEAR, 1));

        assertEquals(75, objective.feed(ItemType.COPPER_PLATE, 75));
        assertFalse(objective.isFullyRepaired());

        assertEquals(25, objective.feed(ItemType.COPPER_PLATE, 40));
        assertEquals(10, objective.feed(ItemType.CONVEYOR_BELT_ITEM, 10));
        assertTrue(objective.isFullyRepaired());
    }

    @Test
    void randomSpawnStaysWithinBounds() {
        WorldMap map = new WorldMap(13, 9);
        RocketObjective objective = RocketObjective.createRandom(map, new Random(42));

        assertTrue(objective.getX() >= 0);
        assertTrue(objective.getY() >= 0);
        assertTrue(objective.getX() + RocketObjective.WIDTH <= map.getWidth());
        assertTrue(objective.getY() + RocketObjective.HEIGHT <= map.getHeight());
    }

    @Test
    void applyToMapMarksCompleteFootprintAsRocketPad() {
        WorldMap map = new WorldMap(12, 12);
        GameSaveState.RocketData data = new GameSaveState.RocketData();
        data.x = 3;
        data.y = 4;

        RocketObjective objective = RocketObjective.fromSaveData(data, map);

        for (int x = data.x; x < data.x + RocketObjective.WIDTH; x++) {
            for (int y = data.y; y < data.y + RocketObjective.HEIGHT; y++) {
                Tile t = map.getTile(x, y);
                t.setType(TileType.IRON_DEPOSIT);
            }
        }

        objective.applyToMap(map);

        for (int x = data.x; x < data.x + RocketObjective.WIDTH; x++) {
            for (int y = data.y; y < data.y + RocketObjective.HEIGHT; y++) {
                assertEquals(TileType.ROCKET_PAD, map.getTile(x, y).getType());
            }
        }
    }
}
