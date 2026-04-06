package game.objective;

import game.entity.ItemType;
import game.save.GameSaveState;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;

import java.util.Random;

/**
 * Holds the end-goal state for the 4x4 rocket and validates manual item feeding.
 */
public final class RocketObjective {
   public static final int WIDTH = 4;
   public static final int HEIGHT = 4;

   public static final int REQUIRED_IRON_GEARS = 80;
   public static final int REQUIRED_COPPER_PLATES = 100;
   public static final int REQUIRED_CONVEYOR_BELTS = 10;

   private final int x;
   private final int y;

   private int deliveredIronGears;
   private int deliveredCopperPlates;
   private int deliveredConveyorBelts;
   private boolean launched;

   private RocketObjective(int x, int y) {
      this.x = x;
      this.y = y;
   }

   public static RocketObjective createRandom(WorldMap map, Random random) {
      if (map.getWidth() < WIDTH || map.getHeight() < HEIGHT) {
         throw new IllegalStateException("Map is too small for a 4x4 rocket objective.");
      }

      int attempts = Math.max(64, map.getWidth() * map.getHeight());
      for (int i = 0; i < attempts; i++) {
         int candidateX = random.nextInt(map.getWidth() - WIDTH + 1);
         int candidateY = random.nextInt(map.getHeight() - HEIGHT + 1);
         if (canPlace(map, candidateX, candidateY)) {
            return new RocketObjective(candidateX, candidateY);
         }
      }

      // Fallback scan: deterministic search if random attempts do not find a free area.
      for (int tx = 0; tx <= map.getWidth() - WIDTH; tx++) {
         for (int ty = 0; ty <= map.getHeight() - HEIGHT; ty++) {
            if (canPlace(map, tx, ty)) {
               return new RocketObjective(tx, ty);
            }
         }
      }

      // Last resort: place at (0,0) by overwriting whatever is there.
      return new RocketObjective(0, 0);
   }

   public static RocketObjective fromSaveData(GameSaveState.RocketData data, WorldMap map) {
      if (data == null) {
         return createRandom(map, new Random());
      }

      int rx = data.x;
      int ry = data.y;
      if (!isInBounds(map, rx, ry)) {
         return createRandom(map, new Random());
      }

      RocketObjective objective = new RocketObjective(rx, ry);
      objective.deliveredIronGears = clamp(data.deliveredIronGears, 0, REQUIRED_IRON_GEARS);
      objective.deliveredCopperPlates = clamp(data.deliveredCopperPlates, 0, REQUIRED_COPPER_PLATES);
      objective.deliveredConveyorBelts = clamp(data.deliveredConveyorBelts, 0, REQUIRED_CONVEYOR_BELTS);
      objective.launched = data.launched;
      return objective;
   }

   public synchronized GameSaveState.RocketData toSaveData() {
      GameSaveState.RocketData data = new GameSaveState.RocketData();
      data.x = x;
      data.y = y;
      data.width = WIDTH;
      data.height = HEIGHT;
      data.deliveredIronGears = deliveredIronGears;
      data.deliveredCopperPlates = deliveredCopperPlates;
      data.deliveredConveyorBelts = deliveredConveyorBelts;
      data.launched = launched;
      return data;
   }

   public void applyToMap(WorldMap map) {
      for (int tx = x; tx < x + WIDTH; tx++) {
         for (int ty = y; ty < y + HEIGHT; ty++) {
            if (!map.inBounds(tx, ty)) {
               continue;
            }
            Tile tile = map.getTile(tx, ty);
            tile.getLock().lock();
            try {
               if (tile.hasMachine()) {
                  tile.removeMachine();
               }
               tile.setItemOnGround(null);
               tile.setType(TileType.ROCKET_PAD);
            } finally {
               tile.getLock().unlock();
            }
         }
      }
   }

   public boolean contains(int tx, int ty) {
      return tx >= x && tx < x + WIDTH && ty >= y && ty < y + HEIGHT;
   }

   public synchronized int feed(ItemType type, int requestedAmount) {
      if (requestedAmount <= 0 || launched || !isRequired(type)) {
         return 0;
      }

      int accepted = Math.min(requestedAmount, getRemaining(type));
      if (accepted <= 0) {
         return 0;
      }

      switch (type) {
         case IRON_GEAR:
            deliveredIronGears += accepted;
            break;
         case COPPER_PLATE:
            deliveredCopperPlates += accepted;
            break;
         case CONVEYOR_BELT_ITEM:
            deliveredConveyorBelts += accepted;
            break;
         default:
            return 0;
      }

      return accepted;
   }

   public synchronized boolean isFullyRepaired() {
      return deliveredIronGears >= REQUIRED_IRON_GEARS
            && deliveredCopperPlates >= REQUIRED_COPPER_PLATES
            && deliveredConveyorBelts >= REQUIRED_CONVEYOR_BELTS;
   }

   public synchronized boolean isLaunched() {
      return launched;
   }

   public synchronized void markLaunched() {
      this.launched = true;
   }

   public synchronized int getDelivered(ItemType type) {
      switch (type) {
         case IRON_GEAR:
            return deliveredIronGears;
         case COPPER_PLATE:
            return deliveredCopperPlates;
         case CONVEYOR_BELT_ITEM:
            return deliveredConveyorBelts;
         default:
            return 0;
      }
   }

   public synchronized int getRequired(ItemType type) {
      switch (type) {
         case IRON_GEAR:
            return REQUIRED_IRON_GEARS;
         case COPPER_PLATE:
            return REQUIRED_COPPER_PLATES;
         case CONVEYOR_BELT_ITEM:
            return REQUIRED_CONVEYOR_BELTS;
         default:
            return 0;
      }
   }

   public synchronized int getRemaining(ItemType type) {
      return Math.max(0, getRequired(type) - getDelivered(type));
   }

   public synchronized double getCompletionRatio() {
      double delivered = deliveredIronGears + deliveredCopperPlates + deliveredConveyorBelts;
      double required = REQUIRED_IRON_GEARS + REQUIRED_COPPER_PLATES + REQUIRED_CONVEYOR_BELTS;
      if (required <= 0.0) {
         return 0.0;
      }
      return delivered / required;
   }

   public static boolean isRequired(ItemType type) {
      return type == ItemType.IRON_GEAR
            || type == ItemType.COPPER_PLATE
            || type == ItemType.CONVEYOR_BELT_ITEM;
   }

   public int getX() {
      return x;
   }

   public int getY() {
      return y;
   }

   private static boolean canPlace(WorldMap map, int x, int y) {
      if (!isInBounds(map, x, y)) {
         return false;
      }

      for (int tx = x; tx < x + WIDTH; tx++) {
         for (int ty = y; ty < y + HEIGHT; ty++) {
            Tile tile = map.getTile(tx, ty);
            if (tile.hasMachine() || tile.hasItem() || tile.getType() == TileType.CONVEYOR_BELT
                  || tile.getType() == TileType.MACHINE || tile.getType() == TileType.ROCKET_PAD) {
               return false;
            }
         }
      }

      return true;
   }

   private static boolean isInBounds(WorldMap map, int x, int y) {
      return x >= 0 && y >= 0 && x + WIDTH <= map.getWidth() && y + HEIGHT <= map.getHeight();
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }
}
