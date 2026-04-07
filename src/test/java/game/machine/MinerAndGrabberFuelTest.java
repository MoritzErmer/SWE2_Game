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
   void grabberTransfersWithoutFuel() {
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

      assertFalse(src.hasItem(), "Grabber should transfer without requiring fuel.");
      assertTrue(dst.hasItem());
      assertEquals(ItemType.IRON_ORE, dst.getItemOnGround().getType());
   }

   @Test
   void smelterRequiresCoalToProduce() {
      Tile tile = new Tile(TileType.IRON_DEPOSIT);
      Smelter smelter = new Smelter(tile);
      tile.setMachine(smelter);
      smelter.setInputBuffer(new ItemStack(ItemType.IRON_ORE, 1));

      smelter.tick();
      assertFalse(smelter.hasOutput(), "Smelter must not produce without coal.");

      assertTrue(smelter.tryInsertInput(new ItemStack(ItemType.COAL, 1)));
      smelter.tick();

      assertTrue(smelter.hasOutput());
      assertEquals(ItemType.IRON_PLATE, smelter.getOutputBuffer().getType());
   }
}
