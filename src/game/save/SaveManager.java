package game.save;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import game.GameMode;
import game.core.GameSupervisor;
import game.entity.ItemStack;
import game.entity.PlayerCharacter;
import game.logistics.ConveyorBelt;
import game.machine.BaseMachine;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Saves and loads game state to/from a JSON file in %APPDATA%/SWE2-Game/save.json.
 *
 * Thread safety: save() stops the supervisor before serializing and restarts it
 * in the finally block, so no concurrent mutation occurs during snapshot.
 */
public class SaveManager {
    private static final Logger LOG = Logger.getLogger(SaveManager.class.getName());
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private SaveManager() {}

    private static Path getSavePath() {
        String appData = System.getenv("APPDATA");
        String base = (appData != null) ? appData : System.getProperty("user.home");
        return Paths.get(base, "SWE2-Game", "save.json");
    }

    public static void save(GameSupervisor supervisor, WorldMap world,
                            PlayerCharacter player, List<BaseMachine> machines,
                            List<ConveyorBelt> belts, GameMode mode) {
        boolean restartAfterSave = supervisor.getRunning().get();
        supervisor.stop();
        try {
            GameSaveState state = buildState(world, player, machines, belts, mode);
            Path savePath = getSavePath();
            Files.createDirectories(savePath.getParent());
            Files.writeString(savePath, GSON.toJson(state));
            LOG.info("Game saved to " + savePath);
        } catch (IOException e) {
            LOG.warning("Failed to save game: " + e.getMessage());
        } finally {
            if (restartAfterSave) {
                try {
                    supervisor.start();
                } catch (RuntimeException e) {
                    LOG.severe("Failed to restart supervisor after save: " + e.getMessage());
                }
            }
        }
    }

    public static Optional<GameSaveState> load() {
        Path savePath = getSavePath();
        if (!Files.exists(savePath)) return Optional.empty();
        try {
            String json = Files.readString(savePath);
            return Optional.of(GSON.fromJson(json, GameSaveState.class));
        } catch (Exception e) {
            LOG.warning("Failed to load save: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static GameSaveState buildState(WorldMap world, PlayerCharacter player,
                                             List<BaseMachine> machines,
                                             List<ConveyorBelt> belts, GameMode mode) {
        GameSaveState state = new GameSaveState();
        state.gameMode = mode.name();

        // Player
        state.player = new GameSaveState.PlayerData();
        state.player.x = player.getX();
        state.player.y = player.getY();
        state.player.health = player.getHealth();
        state.player.maxHealth = player.getMaxHealth();
        state.player.selectedSlot = player.getSelectedHotbarSlot();
        state.player.inventory = new ArrayList<>();
        for (ItemStack is : player.getInventory()) {
            state.player.inventory.add(toItemStackData(is));
        }

        // Modified tiles (non-GRASS / non-EMPTY with items)
        state.modifiedTiles = new ArrayList<>();
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                Tile tile = world.getTile(x, y);
                TileType type = tile.getType();
                boolean isDeposit = type == TileType.IRON_DEPOSIT
                        || type == TileType.COPPER_DEPOSIT
                        || type == TileType.COAL_DEPOSIT;
                boolean hasGroundItem = tile.getItemOnGround() != null;
                // Save anything that isn't a plain empty/grass tile
                if (type != TileType.EMPTY || hasGroundItem || isDeposit) {
                    GameSaveState.TileData td = new GameSaveState.TileData();
                    td.x = x;
                    td.y = y;
                    td.tileType = type.name();
                    if (hasGroundItem) {
                        td.itemOnTile = toItemStackData(tile.getItemOnGround());
                    }
                    state.modifiedTiles.add(td);
                }
            }
        }

        // Machines — position is derived by scanning the world for the machine's tile
        state.machines = new ArrayList<>();
        List<BaseMachine> machinesToSave = (machines != null && !machines.isEmpty())
            ? machines
            : collectMachinesFromWorld(world);
        for (BaseMachine m : machinesToSave) {
            int[] pos = findTilePosition(world, m.getTile());
            if (pos == null) continue; // Orphaned machine, skip
            GameSaveState.MachineData md = new GameSaveState.MachineData();
            md.machineType = m.getClass().getSimpleName();
            md.x = pos[0];
            md.y = pos[1];
            md.direction = m.getDirection().name();
            md.baseTileType = m.getTile().getOriginalType().name();
            if (m.getInputBuffer() != null) {
                md.inputBuffer = toItemStackData(m.getInputBuffer());
            }
            if (m.getOutputBuffer() != null) {
                md.outputBuffer = toItemStackData(m.getOutputBuffer());
            }
            state.machines.add(md);
        }

        // Belts
        state.belts = new ArrayList<>();
        if (belts != null && !belts.isEmpty()) {
            for (ConveyorBelt b : belts) {
                GameSaveState.BeltData bd = new GameSaveState.BeltData();
                bd.x = b.getX();
                bd.y = b.getY();
                bd.direction = b.getDirection().name();
                state.belts.add(bd);
            }
        } else {
            // Fallback for callers that do not track belt objects: store conveyor tile positions.
            for (int x = 0; x < world.getWidth(); x++) {
                for (int y = 0; y < world.getHeight(); y++) {
                    Tile tile = world.getTile(x, y);
                    if (tile.getType() == TileType.CONVEYOR_BELT) {
                        GameSaveState.BeltData bd = new GameSaveState.BeltData();
                        bd.x = x;
                        bd.y = y;
                        bd.direction = ConveyorBelt.Direction.RIGHT.name();
                        state.belts.add(bd);
                    }
                }
            }
        }

        return state;
    }

    private static List<BaseMachine> collectMachinesFromWorld(WorldMap world) {
        List<BaseMachine> fallbackMachines = new ArrayList<>();
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                Tile tile = world.getTile(x, y);
                if (tile.hasMachine()) {
                    fallbackMachines.add(tile.getMachine());
                }
            }
        }
        return fallbackMachines;
    }

    /** Scans the world grid for a tile by identity and returns its [x, y] or null. */
    private static int[] findTilePosition(WorldMap world, Tile target) {
        for (int x = 0; x < world.getWidth(); x++) {
            for (int y = 0; y < world.getHeight(); y++) {
                if (world.getTile(x, y) == target) {
                    return new int[]{x, y};
                }
            }
        }
        return null;
    }

    private static GameSaveState.ItemStackData toItemStackData(ItemStack is) {
        GameSaveState.ItemStackData isd = new GameSaveState.ItemStackData();
        isd.itemType = is.getType().name();
        isd.amount = is.getAmount();
        return isd;
    }
}
