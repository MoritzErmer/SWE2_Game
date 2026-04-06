package game.save;

import java.util.List;

public class GameSaveState {
    public int saveVersion = 2;
    public String gameMode = "NORMAL";
    public PlayerData player;
    public List<TileData> modifiedTiles;
    public List<MachineData> machines;
    public List<BeltData> belts;
    public Integer rocketX;
    public Integer rocketY;
    public int rocketIronGearsDelivered;
    public int rocketCopperPlatesDelivered;
    public int rocketConveyorBeltsDelivered;
    public String rocketStatus = "ACTIVE";
    public long rocketLaunchStartedAtElapsedMs = -1L;
    public long elapsedPlayTimeMs = 0L;
    public boolean gameEnded = false;

    public static class PlayerData {
        public int x, y, health, maxHealth, selectedSlot;
        public List<ItemStackData> inventory;
    }

    public static class ItemStackData {
        public String itemType;
        public int amount;
    }

    public static class TileData {
        public int x, y;
        public String tileType;
        public ItemStackData itemOnTile;
    }

    public static class MachineData {
        public String machineType;
        public int x, y;
        public String direction;
        public String baseTileType;
        public ItemStackData inputBuffer;
        public ItemStackData outputBuffer;
    }

    public static class BeltData {
        public int x, y;
        public String direction;
    }
}
