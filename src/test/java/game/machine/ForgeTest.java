package game.machine;

import game.entity.ItemStack;
import game.entity.ItemType;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgeTest {

    @Test
    void drainStoredItemsReturnsInputOutputAndCoalFuel() {
        Tile tile = new Tile();
        Forge forge = new Forge(tile);
        tile.setMachine(forge);

        assertTrue(forge.tryInsertInput(new ItemStack(ItemType.IRON_PLATE, 4)));
        assertTrue(forge.tryInsertInput(new ItemStack(ItemType.COAL, 2)));
        forge.setOutputBuffer(new ItemStack(ItemType.IRON_GEAR, 1));

        List<ItemStack> drained = forge.drainStoredItems();

        assertEquals(4, amountForType(drained, ItemType.IRON_PLATE));
        assertEquals(1, amountForType(drained, ItemType.IRON_GEAR));
        assertEquals(2, amountForType(drained, ItemType.COAL));
        assertFalse(forge.hasInput());
        assertFalse(forge.hasOutput());
        assertEquals(0, forge.getCoalUnits());
        assertEquals(0, forge.getProducedGearsSinceCoal());
    }

    @Test
    void forgeDoesNotProduceWithoutCoal() {
        Tile tile = new Tile();
        Forge forge = new Forge(tile);
        tile.setMachine(forge);
        forge.tryInsertInput(new ItemStack(ItemType.IRON_PLATE, 2));

        forge.tick();

        assertFalse(forge.hasOutput());
    }

    @Test
    void forgeProducesGearFromTwoIronPlatesWhenFueled() {
        Tile tile = new Tile();
        Forge forge = new Forge(tile);
        tile.setMachine(forge);

        assertTrue(forge.tryInsertInput(new ItemStack(ItemType.COAL, 1)));
        assertTrue(forge.tryInsertInput(new ItemStack(ItemType.IRON_PLATE, 2)));

        forge.tick();

        assertTrue(forge.hasOutput());
        assertEquals(ItemType.IRON_GEAR, forge.getOutputBuffer().getType());
        assertEquals(1, forge.getOutputBuffer().getAmount());
        assertEquals(1, forge.getCoalUnits());
        assertEquals(1, forge.getProducedGearsSinceCoal());
    }

    @Test
    void forgeConsumesOneCoalAfterThirdProducedGear() {
        Tile tile = new Tile();
        Forge forge = new Forge(tile);
        tile.setMachine(forge);

        assertTrue(forge.tryInsertInput(new ItemStack(ItemType.COAL, 1)));
        assertTrue(forge.tryInsertInput(new ItemStack(ItemType.IRON_PLATE, 6)));

        forge.tick();
        forge.tick();
        forge.tick();

        assertTrue(forge.hasOutput());
        assertEquals(3, forge.getOutputBuffer().getAmount());
        assertEquals(0, forge.getCoalUnits());
        assertEquals(0, forge.getProducedGearsSinceCoal());
    }

    @Test
    void forgeAutomationInputRespectsConfiguredSides() {
        Tile tile = new Tile();
        Forge forge = new Forge(tile);
        tile.setMachine(forge);
        forge.setDirection(Direction.RIGHT);

        // Front=RIGHT, Back=LEFT, Left=UP, Right=DOWN
        assertTrue(forge.tryInsertInputFromSide(new ItemStack(ItemType.IRON_PLATE, 2), Direction.LEFT));
        assertFalse(forge.tryInsertInputFromSide(new ItemStack(ItemType.IRON_PLATE, 1), Direction.RIGHT));

        assertTrue(forge.tryInsertInputFromSide(new ItemStack(ItemType.COAL, 1), Direction.UP));
        assertTrue(forge.tryInsertInputFromSide(new ItemStack(ItemType.COAL, 1), Direction.DOWN));
        assertFalse(forge.tryInsertInputFromSide(new ItemStack(ItemType.COAL, 1), Direction.LEFT));
        assertFalse(forge.tryInsertInputFromSide(new ItemStack(ItemType.COAL, 1), Direction.RIGHT));
    }

    @Test
    void forgeOutputCanOnlyBeExtractedFromFrontSide() {
        Tile tile = new Tile();
        Forge forge = new Forge(tile);
        tile.setMachine(forge);
        forge.setDirection(Direction.DOWN);

        assertTrue(forge.canExtractOutputFromSide(Direction.DOWN));
        assertFalse(forge.canExtractOutputFromSide(Direction.UP));
        assertFalse(forge.canExtractOutputFromSide(Direction.LEFT));
        assertFalse(forge.canExtractOutputFromSide(Direction.RIGHT));
    }

    @Test
    void grabberCanExtractFromForgeFrontButNotFromBack() {
        WorldMap map = new WorldMap(8, 5);
        Tile forgeTile = map.getTile(3, 2);
        Forge forge = new Forge(forgeTile);
        forgeTile.setMachine(forge);
        forge.setDirection(Direction.RIGHT);

        // Grabber on front side (x+1) should extract.
        Tile frontGrabberTile = map.getTile(4, 2);
        Grabber frontGrabber = new Grabber(frontGrabberTile, map, 4, 2, -1, 0, 1, 0);
        frontGrabberTile.setMachine(frontGrabber);
        assertTrue(frontGrabber.tryInsertInput(new ItemStack(ItemType.COAL, 1)));

        map.getTile(5, 2).setType(TileType.CONVEYOR_BELT);

        forge.setOutputBuffer(new ItemStack(ItemType.IRON_GEAR, 1));
        frontGrabber.tick();

        assertFalse(forge.hasOutput());
        assertTrue(map.getTile(5, 2).hasItem());
        assertEquals(ItemType.IRON_GEAR, map.getTile(5, 2).getItemOnGround().getType());

        // Grabber on back side (x-1) must not extract.
        Tile backGrabberTile = map.getTile(2, 2);
        Grabber backGrabber = new Grabber(backGrabberTile, map, 2, 2, 1, 0, -1, 0);
        backGrabberTile.setMachine(backGrabber);
        assertTrue(backGrabber.tryInsertInput(new ItemStack(ItemType.COAL, 1)));

        forge.setOutputBuffer(new ItemStack(ItemType.IRON_GEAR, 1));
        backGrabber.tick();

        assertTrue(forge.hasOutput());
        assertFalse(map.getTile(1, 2).hasItem());
    }

    private int amountForType(List<ItemStack> stacks, ItemType type) {
        return stacks.stream()
                .filter(stack -> stack.getType() == type)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }
}