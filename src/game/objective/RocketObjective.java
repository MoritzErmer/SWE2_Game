package game.objective;

import game.entity.ItemType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Mission objective for repairing and launching the rocket.
 */
public class RocketObjective {
   public static final int WIDTH = 4;
   public static final int HEIGHT = 4;

   public static final int REQUIRED_IRON_GEARS = 80;
   public static final int REQUIRED_COPPER_PLATES = 100;
   public static final int REQUIRED_CONVEYOR_BELTS = 10;

   public enum Status {
      ACTIVE,
      LAUNCHING,
      ENDED;

      public static Status fromRaw(String raw) {
         if (raw == null || raw.isBlank()) {
            return ACTIVE;
         }
         try {
            return Status.valueOf(raw);
         } catch (IllegalArgumentException ignored) {
            return ACTIVE;
         }
      }
   }

   private static final Map<ItemType, Integer> REQUIRED_BY_TYPE;

   static {
      EnumMap<ItemType, Integer> required = new EnumMap<>(ItemType.class);
      required.put(ItemType.IRON_GEAR, REQUIRED_IRON_GEARS);
      required.put(ItemType.COPPER_PLATE, REQUIRED_COPPER_PLATES);
      required.put(ItemType.CONVEYOR_BELT_ITEM, REQUIRED_CONVEYOR_BELTS);
      REQUIRED_BY_TYPE = Collections.unmodifiableMap(required);
   }

   private final int originX;
   private final int originY;

   private int deliveredIronGears;
   private int deliveredCopperPlates;
   private int deliveredConveyorBelts;

   private Status status;
   private long launchStartedAtElapsedMs;

   public RocketObjective(int originX, int originY) {
      this(originX, originY, 0, 0, 0, Status.ACTIVE, -1L);
   }

   public RocketObjective(int originX, int originY,
         int deliveredIronGears,
         int deliveredCopperPlates,
         int deliveredConveyorBelts,
         Status status,
         long launchStartedAtElapsedMs) {
      this.originX = originX;
      this.originY = originY;
      this.deliveredIronGears = clampProgress(deliveredIronGears, REQUIRED_IRON_GEARS);
      this.deliveredCopperPlates = clampProgress(deliveredCopperPlates, REQUIRED_COPPER_PLATES);
      this.deliveredConveyorBelts = clampProgress(deliveredConveyorBelts, REQUIRED_CONVEYOR_BELTS);
      this.status = status == null ? Status.ACTIVE : status;
      this.launchStartedAtElapsedMs = launchStartedAtElapsedMs;
   }

   private int clampProgress(int value, int max) {
      return Math.max(0, Math.min(value, max));
   }

   public int getOriginX() {
      return originX;
   }

   public int getOriginY() {
      return originY;
   }

   public int getWidth() {
      return WIDTH;
   }

   public int getHeight() {
      return HEIGHT;
   }

   public boolean occupies(int x, int y) {
      return x >= originX && x < originX + WIDTH
            && y >= originY && y < originY + HEIGHT;
   }

   public boolean isTopLeft(int x, int y) {
      return x == originX && y == originY;
   }

   public Status getStatus() {
      return status;
   }

   public long getLaunchStartedAtElapsedMs() {
      return launchStartedAtElapsedMs;
   }

   public int getDelivered(ItemType type) {
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

   public int getRequired(ItemType type) {
      Integer required = REQUIRED_BY_TYPE.get(type);
      return required == null ? 0 : required;
   }

   public int getRemaining(ItemType type) {
      int required = getRequired(type);
      if (required <= 0) {
         return 0;
      }
      return Math.max(0, required - getDelivered(type));
   }

   public int feed(ItemType type, int availableAmount) {
      if (status != Status.ACTIVE || availableAmount <= 0) {
         return 0;
      }

      switch (type) {
         case IRON_GEAR:
            return feedIronGears(availableAmount);
         case COPPER_PLATE:
            return feedCopperPlates(availableAmount);
         case CONVEYOR_BELT_ITEM:
            return feedConveyorBelts(availableAmount);
         default:
            return 0;
      }
   }

   private int feedIronGears(int availableAmount) {
      int consumed = consumeByNeed(deliveredIronGears, REQUIRED_IRON_GEARS, availableAmount);
      deliveredIronGears += consumed;
      return consumed;
   }

   private int feedCopperPlates(int availableAmount) {
      int consumed = consumeByNeed(deliveredCopperPlates, REQUIRED_COPPER_PLATES, availableAmount);
      deliveredCopperPlates += consumed;
      return consumed;
   }

   private int feedConveyorBelts(int availableAmount) {
      int consumed = consumeByNeed(deliveredConveyorBelts, REQUIRED_CONVEYOR_BELTS, availableAmount);
      deliveredConveyorBelts += consumed;
      return consumed;
   }

   private int consumeByNeed(int delivered, int required, int availableAmount) {
      int remaining = Math.max(0, required - delivered);
      return Math.min(remaining, availableAmount);
   }

   public boolean isComplete() {
      return deliveredIronGears >= REQUIRED_IRON_GEARS
            && deliveredCopperPlates >= REQUIRED_COPPER_PLATES
            && deliveredConveyorBelts >= REQUIRED_CONVEYOR_BELTS;
   }

   public void startLaunch(long elapsedPlayTimeMs) {
      if (status == Status.ACTIVE && isComplete()) {
         status = Status.LAUNCHING;
         launchStartedAtElapsedMs = Math.max(0L, elapsedPlayTimeMs);
      }
   }

   public void finishLaunch() {
      status = Status.ENDED;
   }

   public Map<ItemType, Integer> getRequiredByType() {
      return REQUIRED_BY_TYPE;
   }

   public int getDeliveredIronGears() {
      return deliveredIronGears;
   }

   public int getDeliveredCopperPlates() {
      return deliveredCopperPlates;
   }

   public int getDeliveredConveyorBelts() {
      return deliveredConveyorBelts;
   }
}
