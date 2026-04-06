package game;

import game.core.GameSupervisor;
import game.entity.ItemStack;
import game.entity.ItemType;
import game.entity.PlayerCharacter;
import game.logistics.ConveyorBelt;
import game.machine.BaseMachine;
import game.machine.Grabber;
import game.machine.Miner;
import game.machine.Smelter;
import game.save.GameSaveState;
import game.ui.GameUI;
import game.ui.MainMenuUI;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Hauptklasse des Spiels. Initialisiert alle Komponenten und startet die
 * Threads.
 *
 * Thread-Architektur:
 * - UI-Thread (EDT): Java Swing Rendering, FPS-begrenzt.
 * - Producer-Thread-Pool: Jede Maschine hat einen periodischen Task
 * (ScheduledExecutorService).
 * - Logistics-Thread: Globaler asynchroner Thread für Fließband-Transport.
 */
public class Main {

   public static void main(String[] args) {
      installGlobalErrorHandler();
      try {
         runGame();
      } catch (Throwable t) {
         handleFatalError("Fatal error during startup", t);
         System.exit(1);
      }
   }

   private static void runGame() throws Exception {
      // --- Show main menu and block until user makes a choice ---
      MainMenuUI.MenuResult menuResult = showMainMenu();
      GameMode gameMode = menuResult.mode();
      Optional<GameSaveState> saveState = menuResult.saveState();

      // --- Welt erstellen ---
      final int MAP_WIDTH = 120;
      final int MAP_HEIGHT = 80;
      WorldMap map = new WorldMap(MAP_WIDTH, MAP_HEIGHT);
      PlayerCharacter player = new PlayerCharacter(MAP_WIDTH / 2, MAP_HEIGHT / 2);

      // --- Maschinen, Belts, Roboter (initial leer, werden im Spiel platziert) ---
      List<BaseMachine> machines = new CopyOnWriteArrayList<>();
      List<ConveyorBelt> belts = new CopyOnWriteArrayList<>();

      if (saveState.isPresent()) {
         restoreFromSave(saveState.get(), map, player, machines, belts);
         System.out.println("[Main] Spielstand geladen.");
      } else {
         map.generateResources(0.15); // 15% der Tiles sind Ressourcen
         addStarterInventory(player);
      }

      // --- Game Supervisor erstellen ---
      GameSupervisor supervisor = new GameSupervisor(map, machines, belts);

      // --- UI im EDT starten ---
      SwingUtilities.invokeLater(() -> {
         GameUI ui = new GameUI(map, player, gameMode);
         // Use supervisor-managed lists so UI rendering and save snapshots see live updates.
         ui.setSaveContext(supervisor, supervisor.getMachines(), supervisor.getBelts());

         // Creative mode is applied to CraftingManager in the GameUI constructor.

         // Callback: Wenn eine Maschine im UI platziert wird, beim Supervisor
         // registrieren
         ui.setOnMachinePlaced(machine -> {
            supervisor.registerMachine(machine);
            System.out.println("[Main] Maschine registriert: " + machine.getName());
         });

         // Callback: Wenn eine Maschine de-konstruiert wird, beim Supervisor abmelden
         ui.setOnMachineRemoved(machine -> {
            supervisor.deregisterMachine(machine);
            System.out.println("[Main] Maschine abgemeldet: " + machine.getName());
         });

         ui.setOnBeltPlaced(placement -> {
            supervisor.registerBelt(placement.getX(), placement.getY(), placement.getDirection());
            System.out.println("[Main] Belt registriert bei (" + placement.getX() + "," + placement.getY() + ")");
         });

         ui.setOnBeltRemoved((x, y) -> {
            supervisor.deregisterBelt(x, y);
            System.out.println("[Main] Belt abgemeldet bei (" + x + "," + y + ")");
         });

         // Shutdown-Hook: Sicheres Beenden bei Fenster-Schließen
         ui.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
               supervisor.stop();
            }
         });
      });

      // --- Alle Threads starten ---
      supervisor.start();

      System.out.println("=== 2D Automation Game gestartet ===");
      System.out.println("Steuerung: WASD=Bewegen, 1=Miner, 2=Smelter, E=Item aufheben");
      System.out.println("Karte: " + MAP_WIDTH + "x" + MAP_HEIGHT + " Tiles");
   }

   private static void addStarterInventory(PlayerCharacter player) {
      player.addItem(ItemType.MINER_KIT, 10);
      player.addItem(ItemType.SMELTER_KIT, 10);
      player.addItem(ItemType.GRABBER_KIT, 10);
      player.addItem(ItemType.CONVEYOR_BELT_ITEM, 50);
   }

   private static void restoreFromSave(GameSaveState state, WorldMap map, PlayerCharacter player,
         List<BaseMachine> machines, List<ConveyorBelt> belts) {
      if (state == null) {
         return;
      }

      if ((state.machines == null || state.machines.isEmpty()) && hasMachineTileMarkers(state.modifiedTiles)) {
         System.out.println("[Main] Hinweis: Spielstand enthaelt keine Maschinen-Daten (legacy save). "
               + "Maschinen muessen ggf. neu gesetzt werden.");
      }

      restorePlayer(state.player, player, map);
      restoreModifiedTiles(state.modifiedTiles, map);
      restoreBelts(state, map, belts);
      restoreMachines(state.machines, map, machines);
   }

   private static void restorePlayer(GameSaveState.PlayerData data, PlayerCharacter player, WorldMap map) {
      if (data == null) {
         return;
      }

      int clampedX = Math.max(0, Math.min(map.getWidth() - 1, data.x));
      int clampedY = Math.max(0, Math.min(map.getHeight() - 1, data.y));
      player.setPosition(clampedX, clampedY);

      player.getInventory().clear();
      if (data.inventory != null) {
         for (GameSaveState.ItemStackData stackData : data.inventory) {
            ItemStack stack = toItemStack(stackData);
            if (stack != null && stack.getAmount() > 0) {
               player.addItem(stack.getType(), stack.getAmount());
            }
         }
      }

      player.setSelectedHotbarSlot(data.selectedSlot);

      int currentHealth = player.getHealth();
      int targetHealth = Math.max(0, Math.min(player.getMaxHealth(), data.health));
      if (targetHealth < currentHealth) {
         player.damage(currentHealth - targetHealth);
      } else if (targetHealth > currentHealth) {
         player.heal(targetHealth - currentHealth);
      }
   }

   private static void restoreModifiedTiles(List<GameSaveState.TileData> modifiedTiles, WorldMap map) {
      if (modifiedTiles == null) {
         return;
      }

      for (GameSaveState.TileData tileData : modifiedTiles) {
         if (tileData == null || !map.inBounds(tileData.x, tileData.y)) {
            continue;
         }

         Tile tile = map.getTile(tileData.x, tileData.y);
         TileType tileType = parseTileType(tileData.tileType);
         if (tileType != null && tileType != TileType.MACHINE) {
            tile.setType(tileType);
         }

         ItemStack itemOnTile = toItemStack(tileData.itemOnTile);
         if (itemOnTile != null) {
            tile.setItemOnGround(itemOnTile);
         }
      }
   }

   private static void restoreBelts(GameSaveState state, WorldMap map, List<ConveyorBelt> belts) {
      if (state.belts != null) {
         for (GameSaveState.BeltData beltData : state.belts) {
            if (beltData == null || !map.inBounds(beltData.x, beltData.y) || hasBeltAt(belts, beltData.x, beltData.y)) {
               continue;
            }

            Tile tile = map.getTile(beltData.x, beltData.y);
            ConveyorBelt.Direction direction = parseBeltDirection(beltData.direction);
            belts.add(new ConveyorBelt(tile, beltData.x, beltData.y, direction));
         }
      }

      // Backward compatibility: older saves may contain conveyor tiles but no belt list.
      if (state.modifiedTiles != null) {
         for (GameSaveState.TileData tileData : state.modifiedTiles) {
            if (tileData == null || !map.inBounds(tileData.x, tileData.y) || hasBeltAt(belts, tileData.x, tileData.y)) {
               continue;
            }

            if ("CONVEYOR_BELT".equals(tileData.tileType)) {
               Tile tile = map.getTile(tileData.x, tileData.y);
               belts.add(new ConveyorBelt(tile, tileData.x, tileData.y, ConveyorBelt.Direction.RIGHT));
            }
         }
      }
   }

   private static void restoreMachines(List<GameSaveState.MachineData> machineDataList, WorldMap map,
         List<BaseMachine> machines) {
      if (machineDataList == null) {
         return;
      }

      for (GameSaveState.MachineData machineData : machineDataList) {
         if (machineData == null || !map.inBounds(machineData.x, machineData.y)) {
            continue;
         }

         Tile tile = map.getTile(machineData.x, machineData.y);
         game.machine.Direction direction = parseMachineDirection(machineData.direction);

         TileType baseTileType = parseTileType(machineData.baseTileType);
         if (baseTileType == null || baseTileType == TileType.MACHINE) {
            baseTileType = inferMachineBaseTileType(machineData);
         }
         tile.setType(baseTileType);

         BaseMachine machine;
         switch (machineData.machineType) {
            case "Miner":
               machine = new Miner(tile);
               break;
            case "Smelter":
               machine = new Smelter(tile);
               break;
            case "Grabber":
               machine = new Grabber(tile, map, machineData.x, machineData.y,
                     -direction.getDx(), -direction.getDy(), direction.getDx(), direction.getDy());
               break;
            default:
               continue;
         }

         machine.setDirection(direction);

         ItemStack input = toItemStack(machineData.inputBuffer);
         if (input != null) {
            machine.setInputBuffer(input);
         }
         ItemStack output = toItemStack(machineData.outputBuffer);
         if (output != null) {
            machine.setOutputBuffer(output);
         }

         tile.setMachine(machine);
         machines.add(machine);
      }
   }

   private static TileType inferMachineBaseTileType(GameSaveState.MachineData machineData) {
      if ("Miner".equals(machineData.machineType)) {
         ItemType fromOutput = parseItemType(machineData.outputBuffer != null ? machineData.outputBuffer.itemType : null);
         TileType inferred = mapMinedItemToDeposit(fromOutput);
         return inferred != null ? inferred : TileType.IRON_DEPOSIT;
      }
      return TileType.EMPTY;
   }

   private static TileType mapMinedItemToDeposit(ItemType itemType) {
      if (itemType == null) {
         return null;
      }
      switch (itemType) {
         case IRON_ORE:
            return TileType.IRON_DEPOSIT;
         case COPPER_ORE:
            return TileType.COPPER_DEPOSIT;
         case COAL:
            return TileType.COAL_DEPOSIT;
         default:
            return null;
      }
   }

   private static boolean hasBeltAt(List<ConveyorBelt> belts, int x, int y) {
      for (ConveyorBelt belt : belts) {
         if (belt.getX() == x && belt.getY() == y) {
            return true;
         }
      }
      return false;
   }

   private static boolean hasMachineTileMarkers(List<GameSaveState.TileData> modifiedTiles) {
      if (modifiedTiles == null) {
         return false;
      }
      for (GameSaveState.TileData tileData : modifiedTiles) {
         if (tileData != null && "MACHINE".equals(tileData.tileType)) {
            return true;
         }
      }
      return false;
   }

   private static TileType parseTileType(String raw) {
      if (raw == null || raw.isBlank()) {
         return null;
      }
      try {
         return TileType.valueOf(raw);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private static game.machine.Direction parseMachineDirection(String raw) {
      if (raw == null || raw.isBlank()) {
         return game.machine.Direction.RIGHT;
      }
      try {
         return game.machine.Direction.valueOf(raw);
      } catch (IllegalArgumentException ignored) {
         return game.machine.Direction.RIGHT;
      }
   }

   private static ConveyorBelt.Direction parseBeltDirection(String raw) {
      if (raw == null || raw.isBlank()) {
         return ConveyorBelt.Direction.RIGHT;
      }
      try {
         return ConveyorBelt.Direction.valueOf(raw);
      } catch (IllegalArgumentException ignored) {
         return ConveyorBelt.Direction.RIGHT;
      }
   }

   private static ItemType parseItemType(String raw) {
      if (raw == null || raw.isBlank()) {
         return null;
      }
      try {
         return ItemType.valueOf(raw);
      } catch (IllegalArgumentException ignored) {
         return null;
      }
   }

   private static ItemStack toItemStack(GameSaveState.ItemStackData data) {
      if (data == null || data.amount <= 0) {
         return null;
      }

      ItemType type = parseItemType(data.itemType);
      if (type == null) {
         return null;
      }

      return new ItemStack(type, data.amount);
   }

   /**
    * Displays the main menu on the EDT and blocks the calling thread until
    * the user makes a selection. Safe to call from the main thread.
    */
   private static MainMenuUI.MenuResult showMainMenu() throws Exception {
      java.util.concurrent.CompletableFuture<MainMenuUI.MenuResult> future = new java.util.concurrent.CompletableFuture<>();
      SwingUtilities.invokeLater(() -> {
         MainMenuUI menu = new MainMenuUI();
         menu.showMenu().thenAccept(future::complete);
      });
      return future.get(); // blocks until user clicks a button
   }

   private static void installGlobalErrorHandler() {
      Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
         String source = "Uncaught exception in thread " + thread.getName();
         handleFatalError(source, throwable);
      });
   }

   private static void handleFatalError(String source, Throwable throwable) {
      String appData = System.getenv("APPDATA");
      Path logDir = (appData == null || appData.isBlank())
            ? Path.of("logs")
            : Path.of(appData, "SWE2-Game");
      Path logFile = logDir.resolve("error.log");

      try {
         Files.createDirectories(logDir);
         String entry = java.time.Instant.now() + " | " + source + System.lineSeparator()
               + throwable + System.lineSeparator();

         for (StackTraceElement element : throwable.getStackTrace()) {
            entry += "    at " + element + System.lineSeparator();
         }
         entry += System.lineSeparator();

         Files.writeString(
               logFile,
               entry,
               StandardOpenOption.CREATE,
               StandardOpenOption.APPEND);
      } catch (IOException ioEx) {
         System.err.println("Failed to write error log: " + ioEx.getMessage());
      }

      System.err.println(source + ": " + throwable.getMessage());
      throwable.printStackTrace(System.err);

      SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
            null,
            "A fatal error occurred. See log: " + logFile.toAbsolutePath(),
            "SWE2 Game Error",
            JOptionPane.ERROR_MESSAGE));
   }
}
