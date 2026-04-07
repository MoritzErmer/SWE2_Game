package game.ui;

import game.GameMode;
import game.core.GameSupervisor;
import game.core.PollutionManager;
import game.crafting.CraftingManager;
import game.crafting.CraftingRecipe;
import game.entity.ItemStack;
import game.entity.ItemType;
import game.entity.PlayerCharacter;
import game.logistics.ConveyorBelt;
import game.machine.BaseMachine;
import game.machine.Forge;
import game.machine.Grabber;
import game.machine.Miner;
import game.machine.Smelter;
import game.objective.RocketObjective;
import game.save.SaveManager;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Haupt-UI des Spiels mit Grid-Rendering, Inventar-Overlay, Hotbar,
 * Mining-Mechanik (Enter halten) und Infrastruktur-Platzierung (Linksklick).
 *
 * Rendering ist FPS-begrenzt über einen Swing-Timer, der repaint() triggert.
 * Mining läuft als separater SwingWorker-Thread, um den EDT nicht zu
 * blockieren.
 */
@SuppressWarnings("serial")
public class GameUI extends JFrame {
   private static final int TILE_SIZE = 32;
   private static final int HOTBAR_HEIGHT = 48;
   private static final int HUD_HEIGHT = 20;
   private static final int TARGET_FPS = 30;
   private static final long ROCKET_LAUNCH_DURATION_MS = 3500L;
   private static final int ROCKET_ASCENT_PIXELS = TILE_SIZE * 6;

   private final WorldMap map;
   private final PlayerCharacter player;
   private final GamePanel gamePanel;
   private Consumer<BaseMachine> onMachinePlaced;
   private Consumer<BeltPlacement> onBeltPlaced;

   // Save/load support
   private GameSupervisor supervisor;
   private List<BaseMachine> machineList;
   private List<ConveyorBelt> beltList;
   private GameMode gameMode = GameMode.NORMAL;
   private RocketObjective rocketObjective;

   // Persisted runtime tracking (sum of previous sessions + current session)
   private long elapsedPlayTimeBeforeSessionMs = 0L;
   private long sessionStartedAtMs = System.currentTimeMillis();
   private long finalElapsedPlayTimeMs = -1L;
   private boolean showEndScreen = false;

   // HUD notification message (save/load feedback)
   private String hudMessage = null;
   private long hudMessageTime = 0;

   // Pollution
   private PollutionManager pollutionManager;
   private boolean showGameOverScreen = false;

   // Pixel-Art Texturen (8x8 → skaliert auf TILE_SIZE)
   private final PixelTextures textures;

   // Crafting
   private final CraftingManager craftingManager = new CraftingManager();

   // Overlay state
   private boolean inventoryOpen = false;
   private boolean craftingOpen = false;
   private int craftingSelectedIndex = 0;
   private String craftingMessage = null;
   private long craftingMessageTime = 0;

   // Animation frame counter (incremented at TARGET_FPS)
   private int animFrame = 0;

   // Spieler-Blickrichtung (für Greifer-Platzierung und Vorschau)
   private int lastDx = 1; // Standard: nach rechts
   private int lastDy = 0;
   private game.machine.Direction lastDirection = game.machine.Direction.RIGHT;

   // Callback für Maschinen-Deconstruction (deregistrieren)
   private java.util.function.Consumer<BaseMachine> onMachineRemoved;
   private BiConsumer<Integer, Integer> onBeltRemoved;

   public static final class BeltPlacement {
      private final int x;
      private final int y;
      private final game.machine.Direction direction;

      public BeltPlacement(int x, int y, game.machine.Direction direction) {
         this.x = x;
         this.y = y;
         this.direction = direction;
      }

      public int getX() {
         return x;
      }

      public int getY() {
         return y;
      }

      public game.machine.Direction getDirection() {
         return direction;
      }
   }

   // ==================== ROTATION & DECONSTRUCTION ====================

   /**
    * Rotiere Maschine auf Spieler-Tile (falls vorhanden und rotierbar).
    */
   private void rotateMachineOnPlayerTile() {
      Tile tile = map.getTile(player.getX(), player.getY());
      tile.getLock().lock();
      try {
         if (tile.isRocketTile()) {
            return;
         }
         if (!tile.hasMachine()) {
            if (tile.getType() == TileType.CONVEYOR_BELT && supervisor != null) {
               supervisor.rotateBelt(player.getX(), player.getY());
            }
            return;
         }
         BaseMachine machine = tile.getMachine();
         // Nur Greifer besitzt eine Sonderrotation; alle anderen Maschinen drehen
         // ihre Richtung normal im Uhrzeigersinn.
         if (machine instanceof Grabber) {
            ((Grabber) machine).rotate();
            machine.setDirection(
                  game.machine.Direction.fromDxDy(((Grabber) machine).getDestDx(), ((Grabber) machine).getDestDy()));
         } else {
            // Miner/Smelter/Forge: Richtung rotieren
            machine.setDirection(machine.getDirection().rotateClockwise());
         }
      } finally {
         tile.getLock().unlock();
      }
   }

   /**
    * Dekonstruiert (baut ab) die Maschine auf dem Spieler-Tile, gibt das Kit-Item
    * zurück.
    */
   private void deconstructMachineOnPlayerTile() {
      Tile tile = map.getTile(player.getX(), player.getY());
      tile.getLock().lock();
      try {
         if (tile.isRocketTile()) {
            showHudMessage("Auf der Rakete kann nichts platziert werden");
            return;
         }
         if (!tile.hasMachine()) {
            if (tile.getType() == TileType.CONVEYOR_BELT) {
               List<ItemStack> beltDrops = new ArrayList<>();
               if (tile.hasItem()) {
                  ItemStack ground = tile.getItemOnGround();
                  beltDrops.add(new ItemStack(ground.getType(), ground.getAmount()));
               }
               beltDrops.add(new ItemStack(ItemType.CONVEYOR_BELT_ITEM, 1));
               if (!canAcceptAllStacks(beltDrops)) {
                  showHudMessage("Inventar voll");
                  return;
               }

               transferItemStackToInventory(tile.pickupItem());
               tile.setType(TileType.EMPTY);
               player.addItem(ItemType.CONVEYOR_BELT_ITEM, 1);
               if (onBeltRemoved != null) {
                  onBeltRemoved.accept(player.getX(), player.getY());
               }
            }
            return;
         }
         BaseMachine machine = tile.getMachine();
         ItemType kitType;
         if (machine instanceof Miner) {
            kitType = ItemType.MINER_KIT;
         } else if (machine instanceof Smelter) {
            kitType = ItemType.SMELTER_KIT;
         } else if (machine instanceof Grabber) {
            kitType = ItemType.GRABBER_KIT;
         } else if (machine instanceof Forge) {
            kitType = ItemType.FORGE_KIT;
         } else {
            return;
         }

         List<ItemStack> machineDrops = getMachineStoredItemsSnapshot(machine);
         machineDrops.add(new ItemStack(kitType, 1));
         if (!canAcceptAllStacks(machineDrops)) {
            showHudMessage("Inventar voll");
            return;
         }

         transferMachineStoredItemsToInventory(machine);
         // Fuege das passende Kit-Item ins Inventar zurueck
         player.addItem(kitType, 1);

         tile.removeMachine();
         if (onMachineRemoved != null)
            onMachineRemoved.accept(machine);
      } finally {
         tile.getLock().unlock();
      }
   }

   /**
    * Setzt Callback für Maschinen-Deconstruction (deregistrieren im Supervisor).
    */
   public void setOnMachineRemoved(java.util.function.Consumer<BaseMachine> callback) {
      this.onMachineRemoved = callback;
   }

   public void setOnBeltPlaced(Consumer<BeltPlacement> callback) {
      this.onBeltPlaced = callback;
   }

   public void setOnBeltRemoved(BiConsumer<Integer, Integer> callback) {
      this.onBeltRemoved = callback;
   }

   private void transferItemStackToInventory(ItemStack stack) {
      if (stack == null || stack.getAmount() <= 0) {
         return;
      }
      player.addItem(stack.getType(), stack.getAmount());
   }

   private void transferMachineStoredItemsToInventory(BaseMachine machine) {
      if (machine == null) {
         return;
      }
      for (ItemStack stored : machine.drainStoredItems()) {
         transferItemStackToInventory(stored);
      }
   }

   private List<ItemStack> getMachineStoredItemsSnapshot(BaseMachine machine) {
      List<ItemStack> stored = new ArrayList<>();
      if (machine == null) {
         return stored;
      }

      if (machine.hasInput()) {
         ItemStack input = machine.getInputBuffer();
         stored.add(new ItemStack(input.getType(), input.getAmount()));
      }
      if (machine.hasOutput()) {
         ItemStack output = machine.getOutputBuffer();
         stored.add(new ItemStack(output.getType(), output.getAmount()));
      }
      if (machine instanceof Forge) {
         int coalUnits = ((Forge) machine).getCoalUnits();
         if (coalUnits > 0) {
            stored.add(new ItemStack(ItemType.COAL, coalUnits));
         }
      }

      return stored;
   }

   private boolean canAcceptAllStacks(List<ItemStack> stacks) {
      if (stacks == null || stacks.isEmpty()) {
         return true;
      }

      Set<ItemType> existingTypes = new HashSet<>();
      for (ItemStack invStack : player.getInventory()) {
         existingTypes.add(invStack.getType());
      }

      Set<ItemType> newTypes = new HashSet<>();
      for (ItemStack stack : stacks) {
         if (stack == null || stack.getAmount() <= 0) {
            continue;
         }
         if (!existingTypes.contains(stack.getType())) {
            newTypes.add(stack.getType());
         }
      }

      int freeSlots = player.getInventorySize() - player.getInventory().size();
      return newTypes.size() <= freeSlots;
   }

   // ==================== SAVE / LOAD ====================

   private void showHudMessage(String msg) {
      hudMessage = msg;
      hudMessageTime = System.currentTimeMillis();
   }

   private void saveGame() {
      if (supervisor == null || machineList == null || beltList == null) {
         showHudMessage("Speichern nicht verfuegbar");
         return;
      }

      // Run off EDT so we don't block the UI thread during file I/O
      new Thread(() -> {
         try {
            boolean gameEnded = rocketObjective != null
                  && rocketObjective.getStatus() == RocketObjective.Status.ENDED;
            SaveManager.save(supervisor, map, player, machineList, beltList, gameMode,
                  rocketObjective, getElapsedPlayTimeMs(), gameEnded);
            SwingUtilities.invokeLater(() -> showHudMessage("Gespeichert!"));
         } catch (RuntimeException e) {
            SwingUtilities.invokeLater(() -> showHudMessage("Speichern fehlgeschlagen"));
            e.printStackTrace();
         }
      }, "save-thread").start();
   }

   private void loadGame() {
      // Loading mid-game requires a restart to fully restore machines/belts.
      // Show a message directing the user to use the main menu load option.
      showHudMessage("Laden: Spiel neu starten und 'Laden' waehlen");
   }

   // Mining state
   private boolean enterHeld = false;
   private long miningStartTime = 0;
   private int miningRequiredMs = 0;
   private boolean miningInProgress = false;

   // Kamera-Offset (Pixel): Wird jedes Frame auf Spielerposition zentriert
   private int cameraX = 0;
   private int cameraY = 0;

   /**
    * Sets the supervisor and lists needed for save/load. Call before the game
    * starts.
    */
   public void setSaveContext(GameSupervisor supervisor, List<BaseMachine> machines,
         List<ConveyorBelt> belts) {
      this.supervisor = supervisor;
      this.machineList = machines;
      this.beltList = belts;
   }

   public void setRocketContext(RocketObjective objective, long elapsedPlayTimeMs, boolean gameAlreadyEnded) {
      this.rocketObjective = objective;
      this.elapsedPlayTimeBeforeSessionMs = Math.max(0L, elapsedPlayTimeMs);
      this.sessionStartedAtMs = System.currentTimeMillis();
      this.finalElapsedPlayTimeMs = -1L;
      this.showEndScreen = gameAlreadyEnded;

      if (gameAlreadyEnded && this.rocketObjective != null
            && this.rocketObjective.getStatus() != RocketObjective.Status.ENDED) {
         this.rocketObjective.finishLaunch();
      }

      if (gameAlreadyEnded) {
         this.finalElapsedPlayTimeMs = this.elapsedPlayTimeBeforeSessionMs;
      }
   }

   public void setPollutionManager(PollutionManager pollutionManager) {
      this.pollutionManager = pollutionManager;
   }

   private long getElapsedPlayTimeMs() {
      if (finalElapsedPlayTimeMs >= 0L) {
         return finalElapsedPlayTimeMs;
      }
      long sessionElapsed = Math.max(0L, System.currentTimeMillis() - sessionStartedAtMs);
      return elapsedPlayTimeBeforeSessionMs + sessionElapsed;
   }

   private boolean isLaunchOrEndedState() {
      return showEndScreen
            || (rocketObjective != null
                  && (rocketObjective.getStatus() == RocketObjective.Status.LAUNCHING
                        || rocketObjective.getStatus() == RocketObjective.Status.ENDED));
   }

   private void updateRocketObjectiveState() {
      if (rocketObjective == null) {
         return;
      }

      if (rocketObjective.getStatus() == RocketObjective.Status.ACTIVE && rocketObjective.isComplete()) {
         rocketObjective.startLaunch(getElapsedPlayTimeMs());
         showHudMessage("Rakete repariert! Startsequenz...");
      }

      if (rocketObjective.getStatus() == RocketObjective.Status.LAUNCHING) {
         long elapsedSinceLaunch = getElapsedPlayTimeMs() - rocketObjective.getLaunchStartedAtElapsedMs();
         if (elapsedSinceLaunch >= ROCKET_LAUNCH_DURATION_MS) {
            rocketObjective.finishLaunch();
            finalElapsedPlayTimeMs = getElapsedPlayTimeMs();
            showEndScreen = true;
         }
      }

      if (rocketObjective.getStatus() == RocketObjective.Status.ENDED && finalElapsedPlayTimeMs < 0L) {
         finalElapsedPlayTimeMs = getElapsedPlayTimeMs();
         showEndScreen = true;
      }
   }

   private String formatDuration(long millis) {
      long totalSeconds = Math.max(0L, millis / 1000L);
      long hours = totalSeconds / 3600L;
      long minutes = (totalSeconds % 3600L) / 60L;
      long seconds = totalSeconds % 60L;
      if (hours > 0L) {
         return String.format("%02d:%02d:%02d", hours, minutes, seconds);
      }
      return String.format("%02d:%02d", minutes, seconds);
   }

   public void setGameMode(GameMode mode) {
      this.gameMode = mode;
      craftingManager.setCreativeMode(mode == GameMode.CREATIVE);
   }

   public game.crafting.CraftingManager getCraftingManager() {
      return craftingManager;
   }

   public GameUI(WorldMap map, PlayerCharacter player) {
      this(map, player, GameMode.NORMAL);
   }

   public GameUI(WorldMap map, PlayerCharacter player, GameMode mode) {
      this.map = map;
      this.player = player;
      this.gameMode = mode;
      this.craftingManager.setCreativeMode(mode == GameMode.CREATIVE);

      this.textures = new PixelTextures(TILE_SIZE);

      setTitle("2D Automation Game - SE2 DHBW 2026");
      setDefaultCloseOperation(EXIT_ON_CLOSE);
      setUndecorated(true);

      gamePanel = new GamePanel();
      add(gamePanel);
      setExtendedState(JFrame.MAXIMIZED_BOTH);

      // --- Keyboard ---
      gamePanel.setFocusable(true);
      gamePanel.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            if (isLaunchOrEndedState()) {
               if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                  dispose();
                  System.exit(0);
               }
               return;
            }

            // Game-Over-Screen: nur ESC reagiert
            if (showGameOverScreen) {
               if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                  dispose();
                  System.exit(0);
               }
               return;
            }

            // --- Ctrl+S: Speichern ---
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
               saveGame();
               return;
            }
            // --- Ctrl+L: Laden (nur Hinweis, vollständige Wiederherstellung erfordert
            // Neustart) ---
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_L) {
               loadGame();
               return;
            }

            // --- Crafting-Menü offen ---
            if (craftingOpen) {
               handleCraftingKey(e);
               return;
            }

            // --- Inventar offen ---
            if (inventoryOpen) {
               if (e.getKeyCode() == KeyEvent.VK_E) {
                  inventoryOpen = false;
               }
               return;
            }

            // Hotbar-Slot per Zahlenreihe 1-9
            if (e.getKeyChar() >= '1' && e.getKeyChar() <= '9') {
               player.setSelectedHotbarSlot(e.getKeyChar() - '1');
               return;
            }

            switch (e.getKeyCode()) {
               case KeyEvent.VK_ESCAPE:
                  dispose();
                  System.exit(0);
                  break;
               case KeyEvent.VK_W:
                  player.move('w', map);
                  lastDx = 0;
                  lastDy = -1;
                  lastDirection = game.machine.Direction.UP;
                  break;
               case KeyEvent.VK_A:
                  player.move('a', map);
                  lastDx = -1;
                  lastDy = 0;
                  lastDirection = game.machine.Direction.LEFT;
                  break;
               case KeyEvent.VK_S:
                  player.move('s', map);
                  lastDx = 0;
                  lastDy = 1;
                  lastDirection = game.machine.Direction.DOWN;
                  break;
               case KeyEvent.VK_D:
                  player.move('d', map);
                  lastDx = 1;
                  lastDy = 0;
                  lastDirection = game.machine.Direction.RIGHT;
                  break;
               case KeyEvent.VK_E:
                  inventoryOpen = true;
                  break;
               case KeyEvent.VK_C:
                  craftingOpen = true;
                  craftingSelectedIndex = 0;
                  break;
               case KeyEvent.VK_ENTER:
                  if (!enterHeld) {
                     startMining();
                  }
                  break;
               case KeyEvent.VK_R:
                  // R rotiert Maschine auf aktuellem Tile (falls vorhanden)
                  rotateMachineOnPlayerTile();
                  break;
               case KeyEvent.VK_Q:
                  // Q dekonstruiert Maschine auf aktuellem Tile
                  deconstructMachineOnPlayerTile();
                  break;
            }
         }

         @Override
         public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
               cancelMining();
            }
         }
      });

      gamePanel.addMouseListener(new MouseAdapter() {
         @Override
         public void mousePressed(MouseEvent e) {
            if (isLaunchOrEndedState()) {
               return;
            }
            int tx = (e.getX() + cameraX) / TILE_SIZE;
            int ty = (e.getY() + cameraY) / TILE_SIZE;
            if (!map.inBounds(tx, ty))
               return;
            if (SwingUtilities.isLeftMouseButton(e)) {
               placeItemOnTile(tx, ty);
            } else if (SwingUtilities.isRightMouseButton(e)) {
               interactWithMachine(tx, ty);
            }
         }
      });

      // FPS-Timer: repaint + Mining-Check + Kamera-Update
      Timer timer = new Timer(1000 / TARGET_FPS, evt -> {
         animFrame++;
         updateCamera();
         checkMiningComplete();
         updateRocketObjectiveState();
         // Game-Over-Prüfung: Spieler tot durch Pollution-Schaden
         if (!showGameOverScreen && !showEndScreen && !player.isAlive()) {
            showGameOverScreen = true;
            if (finalElapsedPlayTimeMs < 0L) {
               finalElapsedPlayTimeMs = getElapsedPlayTimeMs();
            }
         }
         gamePanel.repaint();
      });
      timer.start();

      // Frame sichtbar machen und Fokus fuer Tastatursteuerung setzen.
      setVisible(true);
      SwingUtilities.invokeLater(() -> gamePanel.requestFocusInWindow());
   }

   /** Zentriert die Kamera auf den Spieler, clampt an Kartenränder. */
   private void updateCamera() {
      int screenW = gamePanel.getWidth();
      int screenH = gamePanel.getHeight() - HOTBAR_HEIGHT - HUD_HEIGHT;
      int worldW = map.getWidth() * TILE_SIZE;
      int worldH = map.getHeight() * TILE_SIZE;

      // Kamera zentriert auf Spieler
      cameraX = player.getX() * TILE_SIZE + TILE_SIZE / 2 - screenW / 2;
      cameraY = player.getY() * TILE_SIZE + TILE_SIZE / 2 - screenH / 2;

      // Clamp: wenn Welt kleiner als Bildschirm → zentrieren, sonst an Ränder clampen
      if (worldW <= screenW) {
         cameraX = -(screenW - worldW) / 2;
      } else {
         cameraX = Math.max(0, Math.min(cameraX, worldW - screenW));
      }
      if (worldH <= screenH) {
         cameraY = -(screenH - worldH) / 2;
      } else {
         cameraY = Math.max(0, Math.min(cameraY, worldH - screenH));
      }
   }

   public void setOnMachinePlaced(Consumer<BaseMachine> callback) {
      this.onMachinePlaced = callback;
   }

   // ==================== CRAFTING INPUT ====================

   /** Verarbeitet Tasteneingaben im Crafting-Menü. */
   private void handleCraftingKey(KeyEvent e) {
      java.util.List<CraftingRecipe> recipes = craftingManager.getRecipes();
      switch (e.getKeyCode()) {
         case KeyEvent.VK_C:
         case KeyEvent.VK_ESCAPE:
            craftingOpen = false;
            break;
         case KeyEvent.VK_W:
         case KeyEvent.VK_UP:
            craftingSelectedIndex = Math.max(0, craftingSelectedIndex - 1);
            break;
         case KeyEvent.VK_S:
         case KeyEvent.VK_DOWN:
            craftingSelectedIndex = Math.min(recipes.size() - 1, craftingSelectedIndex + 1);
            break;
         case KeyEvent.VK_ENTER:
            if (!recipes.isEmpty() && craftingSelectedIndex < recipes.size()) {
               CraftingRecipe recipe = recipes.get(craftingSelectedIndex);
               if (craftingManager.craft(recipe, player)) {
                  craftingMessage = "Crafted: " + recipe.getResult().getDisplayName()
                        + " x" + recipe.getResultAmount();
               } else {
                  craftingMessage = "Nicht genug Materialien!";
               }
               craftingMessageTime = System.currentTimeMillis();
            }
            break;
      }
   }

   // ==================== MINING ====================

   /**
    * Startet den Mining-Vorgang wenn der Spieler auf einem Ressourcen-Tile steht.
    */
   private void startMining() {
      if (isLaunchOrEndedState()) {
         return;
      }
      Tile tile = map.getTile(player.getX(), player.getY());
      if (!tile.isResourceDeposit())
         return;

      int timeMs = tile.getType().getMiningTimeMs();
      if (timeMs <= 0)
         return;

      enterHeld = true;
      miningInProgress = true;
      miningStartTime = System.currentTimeMillis();
      miningRequiredMs = timeMs;
   }

   /** Prüft ob der Mining-Vorgang abgeschlossen ist. */
   private void checkMiningComplete() {
      if (!miningInProgress || !enterHeld)
         return;

      long elapsed = System.currentTimeMillis() - miningStartTime;
      if (elapsed >= miningRequiredMs) {
         // Mining abgeschlossen
         Tile tile = map.getTile(player.getX(), player.getY());
         tile.getLock().lock();
         try {
            ItemType mined = tile.getType().getMinedItem();
            if (mined != null) {
               player.addItem(mined, 1);
            }
         } finally {
            tile.getLock().unlock();
         }
         miningInProgress = false;
         enterHeld = false;
      }
   }

   /** Bricht den Mining-Vorgang ab. */
   private void cancelMining() {
      enterHeld = false;
      miningInProgress = false;
   }

   // ==================== ITEM PLACEMENT ====================

   /** Kompatibler Maschinen-Placement-Check. */
   private boolean canPlaceMachineOnTile(Tile tile) {
      try {
         java.lang.reflect.Method method = tile.getClass().getMethod("canPlaceMachine");
         Object result = method.invoke(tile);
         if (result instanceof Boolean) {
            return (Boolean) result;
         }
      } catch (NoSuchMethodException ignored) {
         // Fallback unten
      } catch (ReflectiveOperationException e) {
         return false;
      }

      return !tile.hasItem() && (tile.getType() == TileType.EMPTY || tile.isResourceDeposit());
   }

   /** Platziert das ausgewaehlte platzierbare Hotbar-Item per Linksklick. */
   private void placeItemOnTile(int tx, int ty) {
      ItemStack selected = player.getSelectedItem();
      if (selected == null)
         return;
      if (!selected.getType().isPlaceable())
         return;

      Tile tile = map.getTile(tx, ty);
      tile.getLock().lock();
      try {
         if (tile.isRocketTile()) {
            showHudMessage("Auf der Rakete kann nichts platziert werden");
            return;
         }
         if (tile.hasMachine())
            return;

         ItemType type = selected.getType();
         boolean placingMachine = type == ItemType.MINER_KIT
               || type == ItemType.SMELTER_KIT
               || type == ItemType.GRABBER_KIT
               || type == ItemType.FORGE_KIT;
         if (placingMachine && !canPlaceMachineOnTile(tile))
            return;

         // Miner platzieren (nur auf Ressourcen-Tile)
         if (type == ItemType.MINER_KIT) {
            if (!tile.isResourceDeposit())
               return;
            if (player.consumeSelectedItem(1)) {
               Miner machine = new Miner(tile);
               machine.setDirection(lastDirection);
               tile.setMachine(machine);
               if (onMachinePlaced != null)
                  onMachinePlaced.accept(machine);
            }
            return;
         }

         // Smelter platzieren
         if (type == ItemType.SMELTER_KIT) {
            if (player.consumeSelectedItem(1)) {
               Smelter machine = new Smelter(tile);
               machine.setDirection(lastDirection);
               tile.setMachine(machine);
               if (onMachinePlaced != null)
                  onMachinePlaced.accept(machine);
            }
            return;
         }

         // Forge platzieren
         if (type == ItemType.FORGE_KIT) {
            if (player.consumeSelectedItem(1)) {
               Forge machine = new Forge(tile);
               machine.setDirection(lastDirection);
               tile.setMachine(machine);
               if (onMachinePlaced != null)
                  onMachinePlaced.accept(machine);
            }
            return;
         }

         // Förderband platzieren
         if (type == ItemType.CONVEYOR_BELT_ITEM) {
            if (tile.getType() != TileType.EMPTY)
               return;
            if (player.consumeSelectedItem(1)) {
               tile.setType(TileType.CONVEYOR_BELT);
               if (onBeltPlaced != null) {
                  onBeltPlaced.accept(new BeltPlacement(tx, ty, lastDirection));
               }
            }
            return;
         }

         // Greifer platzieren (Richtung = letzte Blickrichtung)
         if (type == ItemType.GRABBER_KIT) {
            if (tile.getType() != TileType.EMPTY && !tile.isResourceDeposit())
               return;
            if (tile.hasMachine())
               return;
            if (player.consumeSelectedItem(1)) {
               // Greifer: Quelle = hinter uns, Ziel = vor uns (in Blickrichtung)
               Grabber grabber = new Grabber(tile, map, tx, ty,
                     -lastDx, -lastDy, lastDx, lastDy);
               grabber.setDirection(lastDirection);
               tile.setMachine(grabber);
               if (onMachinePlaced != null)
                  onMachinePlaced.accept(grabber);
            }
            return;
         }
      } finally {
         tile.getLock().unlock();
      }
   }

   // ==================== MACHINE INTERACTION ====================

   private void feedRocketFromSelectedItem() {
      if (rocketObjective == null || rocketObjective.getStatus() != RocketObjective.Status.ACTIVE) {
         return;
      }

      ItemStack selected = player.getSelectedItem();
      if (selected == null || selected.getAmount() <= 0) {
         showHudMessage("Waehle ein Item fuer die Rakete");
         return;
      }

      int consumed = rocketObjective.feed(selected.getType(), selected.getAmount());
      if (consumed <= 0) {
         showHudMessage("Rakete braucht Iron Gear, Copper Plate, Conveyor Belt");
         return;
      }

      player.consumeSelectedItem(consumed);
      showHudMessage(
            "Rakete: G " + rocketObjective.getDeliveredIronGears() + "/" + RocketObjective.REQUIRED_IRON_GEARS
                  + "  C " + rocketObjective.getDeliveredCopperPlates() + "/"
                  + RocketObjective.REQUIRED_COPPER_PLATES
                  + "  B " + rocketObjective.getDeliveredConveyorBelts() + "/"
                  + RocketObjective.REQUIRED_CONVEYOR_BELTS);
   }

   /**
    * Rechtsklick auf ein Tile:
    * - Foerderband: Boden-Item ins Inventar nehmen
    * - Miner: Kohle aus Inventar als Brennstoff einfuellen
    * - Greifer: Kohle aus Inventar als Brennstoff einfüllen
    * - Smelter: Erz aus Inventar in Input-Buffer legen
    * - Jede Maschine: Output-Buffer ins Inventar nehmen
    */
   private void interactWithMachine(int tx, int ty) {
      Tile tile = map.getTile(tx, ty);
      tile.getLock().lock();
      try {
         if (tile.isRocketTile()) {
            feedRocketFromSelectedItem();
            return;
         }

         if (!tile.hasMachine()) {
            if (tile.isConveyorBelt() && tile.hasItem()) {
               if (!canAcceptAllStacks(Collections.singletonList(tile.getItemOnGround()))) {
                  showHudMessage("Inventar voll");
                  return;
               }
               transferItemStackToInventory(tile.pickupItem());
            }
            return;
         }

         BaseMachine machine = tile.getMachine();

         // Output abholen (höchste Priorität)
         if (machine.hasOutput()) {
            if (!canAcceptAllStacks(Collections.singletonList(machine.getOutputBuffer()))) {
               showHudMessage("Inventar voll");
               return;
            }
            ItemStack out = machine.extractOutput();
            transferItemStackToInventory(out);
            return;
         }

         // Miner: Kohle einfuellen
         if (machine instanceof Miner) {
            if (player.getItemCount(ItemType.COAL) > 0) {
               int toAdd = Math.min(8, player.getItemCount(ItemType.COAL));
               if (machine.tryInsertInput(new ItemStack(ItemType.COAL, toAdd))) {
                  player.removeItem(ItemType.COAL, toAdd);
               }
            }
            return;
         }

         // Greifer: Kohle einfüllen
         if (machine instanceof Grabber) {
            if (player.getItemCount(ItemType.COAL) > 0) {
               int toAdd = Math.min(8, player.getItemCount(ItemType.COAL));
               if (machine.tryInsertInput(new ItemStack(ItemType.COAL, toAdd))) {
                  player.removeItem(ItemType.COAL, toAdd);
               }
            }
            return;
         }

         // Smelter: Erz einfüllen aus Hotbar
         if (machine instanceof Smelter) {
            ItemStack selected = player.getSelectedItem();
            if (selected != null) {
               ItemType type = selected.getType();
               if (type == ItemType.IRON_ORE || type == ItemType.COPPER_ORE) {
                  int toAdd = Math.min(5, selected.getAmount());
                  if (machine.tryInsertInput(new ItemStack(type, toAdd))) {
                     player.consumeSelectedItem(toAdd);
                  }
               }
            }
            return;
         }

         // Forge: Eisenplatten oder Kohle einfüllen aus Hotbar
         if (machine instanceof Forge) {
            ItemStack selected = player.getSelectedItem();
            if (selected != null) {
               ItemType type = selected.getType();
               if (type == ItemType.IRON_PLATE || type == ItemType.COAL) {
                  int toAdd = (type == ItemType.COAL)
                        ? Math.min(8, selected.getAmount())
                        : Math.min(5, selected.getAmount());
                  if (machine.tryInsertInput(new ItemStack(type, toAdd))) {
                     player.consumeSelectedItem(toAdd);
                  }
               }
            }
            return;
         }
      } finally {
         tile.getLock().unlock();
      }
   }

   // ==================== MACHINE PLACEMENT ====================

   /**
    * Platziert eine Maschine auf dem Spieler-Tile (via Hotbar-Kontext, zukünftig
    * erweiterbar).
    */
   private void placeMachine(String type) {
      Tile tile = map.getTile(player.getX(), player.getY());
      tile.getLock().lock();
      try {
         if (tile.isRocketTile()) {
            return;
         }
         if (!tile.canPlaceMachine())
            return;
         BaseMachine machine;
         if ("miner".equals(type) && tile.isResourceDeposit()) {
            machine = new Miner(tile);
         } else if ("smelter".equals(type)) {
            machine = new Smelter(tile);
         } else if ("forge".equals(type)) {
            machine = new Forge(tile);
         } else {
            return;
         }
         tile.setMachine(machine);
         if (onMachinePlaced != null) {
            onMachinePlaced.accept(machine);
         }
      } finally {
         tile.getLock().unlock();
      }
   }

   // ==================== RENDERING ====================

   /**
    * Inneres JPanel für doppelt gepuffertes Grid-Rendering.
    */
   private class GamePanel extends JPanel {
      GamePanel() {
         setDoubleBuffered(true);
         setBackground(Color.BLACK);
      }

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);
         Graphics2D g2 = (Graphics2D) g;
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

         drawGrid(g2);
         drawPlayer(g2);
         drawMiningBar(g2);
         drawHotbar(g2);
         drawHUD(g2);

         if (showGameOverScreen) {
            drawGameOverScreen(g2);
            return;
         }

         if (showEndScreen) {
            drawEndScreen(g2);
            return;
         }

         if (inventoryOpen) {
            drawInventoryOverlay(g2);
         }
         if (craftingOpen) {
            drawCraftingOverlay(g2);
         }
      }

      // --- Grid (Pixel-Art Texturen) mit Kamera-Offset ---
      private void drawGrid(Graphics2D g2) {
         // Nearest-Neighbor für scharfe Pixel
         g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
               RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

         int screenW = getWidth();
         int screenH = getHeight();

         // Nur sichtbare Tiles rendern (Culling)
         int startTX = Math.max(0, cameraX / TILE_SIZE);
         int startTY = Math.max(0, cameraY / TILE_SIZE);
         int endTX = Math.min(map.getWidth(), (cameraX + screenW) / TILE_SIZE + 1);
         int endTY = Math.min(map.getHeight(), (cameraY + screenH) / TILE_SIZE + 1);

         for (int x = startTX; x < endTX; x++) {
            for (int y = startTY; y < endTY; y++) {
               Tile tile = map.getTile(x, y);
               int px = x * TILE_SIZE - cameraX;
               int py = y * TILE_SIZE - cameraY;

               // Tile-Textur (conveyor belts get a direction-aware texture)
               BufferedImage tileTex;
               if (tile.getType() == TileType.CONVEYOR_BELT && beltList != null) {
                  // Find the ConveyorBelt at this position for direction info
                  final int fx = x, fy = y;
                  ConveyorBelt beltAtTile = beltList.stream()
                        .filter(b -> b.getX() == fx && b.getY() == fy)
                        .findFirst()
                        .orElse(null);
                  int beltFrame = Math.floorMod(animFrame / 3, 4);
                  tileTex = (beltAtTile != null)
                        ? getConveyorBeltTexture(beltAtTile, beltFrame)
                        : textures.get("conveyor_belt_f" + beltFrame);
                  if (tileTex == null) {
                     tileTex = textures.get("conveyor_belt");
                  }
               } else {
                  tileTex = getTileTexture(tile);
               }
               g2.drawImage(tileTex, px, py, null);

               // Maschine als Textur
               if (!tile.isRocketTile() && tile.hasMachine()) {
                  BaseMachine machine = tile.getMachine();
                  BufferedImage machineTex = getMachineTexture(machine, animFrame);
                  if (machineTex != null) {
                     g2.drawImage(machineTex, px, py, null);
                  }

                  // Buffer-Indikatoren (kleine Zahlen)
                  g2.setFont(new Font("Monospaced", Font.BOLD, 9));
                  if (machine.hasInput()) {
                     g2.setColor(new Color(100, 200, 255));
                     g2.drawString("I:" + machine.getInputBuffer().getAmount(), px + 1, py + 9);
                  }
                  if (machine.hasOutput()) {
                     g2.setColor(new Color(255, 220, 80));
                     g2.drawString("O:" + machine.getOutputBuffer().getAmount(), px + 1, py + TILE_SIZE - 2);
                  }

                  // Greifer: Richtungspfeil zeichnen
                  if (machine instanceof Grabber) {
                     Grabber gr = (Grabber) machine;
                     int cx = px + TILE_SIZE / 2;
                     int cy = py + TILE_SIZE / 2;
                     int ax = cx + gr.getDestDx() * 10;
                     int ay = cy + gr.getDestDy() * 10;
                     g2.setColor(new Color(255, 255, 100, 200));
                     g2.setStroke(new BasicStroke(2));
                     g2.drawLine(cx - gr.getDestDx() * 8, cy - gr.getDestDy() * 8, ax, ay);
                     g2.setStroke(new BasicStroke(1));
                  }
               }

               // Item auf dem Boden als Textur
               if (!tile.isRocketTile() && tile.hasItem()) {
                  BufferedImage itemTex = getItemTexture(tile.getItemOnGround().getType());
                  if (itemTex != null) {
                     g2.drawImage(itemTex, px, py, null);
                  }
               }
            }
         }

         // Draw the rocket after all tiles so the pad cannot overpaint hull parts.
         drawRocketObjective(g2);
      }

      // --- Player (Pixel-Art Textur) mit Kamera-Offset ---
      private void drawPlayer(Graphics2D g2) {
         int px = player.getX() * TILE_SIZE - cameraX;
         int py = player.getY() * TILE_SIZE - cameraY;
         BufferedImage playerTex = textures.get("player");
         if (playerTex != null) {
            g2.drawImage(playerTex, px, py, null);
         }
      }

      // --- Mining Progress Bar (über dem Spieler, Kamera-Offset) ---
      private void drawMiningBar(Graphics2D g2) {
         if (!miningInProgress || !enterHeld)
            return;

         long elapsed = System.currentTimeMillis() - miningStartTime;
         float progress = Math.min(1.0f, (float) elapsed / miningRequiredMs);

         int barWidth = 80;
         int barHeight = 10;
         int bx = player.getX() * TILE_SIZE + TILE_SIZE / 2 - barWidth / 2 - cameraX;
         int by = player.getY() * TILE_SIZE - 16 - cameraY;

         // Background
         g2.setColor(new Color(50, 50, 50, 200));
         g2.fillRect(bx, by, barWidth, barHeight);
         // Progress
         g2.setColor(new Color(0, 200, 80));
         g2.fillRect(bx, by, (int) (barWidth * progress), barHeight);
         // Border
         g2.setColor(Color.WHITE);
         g2.drawRect(bx, by, barWidth, barHeight);
      }

      // --- Hotbar (am unteren Bildschirmrand) ---
      private void drawHotbar(Graphics2D g2) {
         int hotbarY = getHeight() - HOTBAR_HEIGHT - HUD_HEIGHT;
         int slotSize = 40;
         int totalWidth = PlayerCharacter.HOTBAR_SLOTS * slotSize;
         int startX = (getWidth() - totalWidth) / 2;

         // Background
         g2.setColor(new Color(40, 40, 40, 220));
         g2.fillRect(0, hotbarY, getWidth(), HOTBAR_HEIGHT);

         List<ItemStack> inv = player.getInventory();
         for (int i = 0; i < PlayerCharacter.HOTBAR_SLOTS; i++) {
            int sx = startX + i * slotSize;
            int sy = hotbarY + 4;

            // Slot background
            boolean selected = (i == player.getSelectedHotbarSlot());
            g2.setColor(selected ? new Color(255, 255, 255, 60) : new Color(80, 80, 80, 180));
            g2.fillRect(sx, sy, slotSize - 2, slotSize - 2);

            // Border
            g2.setColor(selected ? Color.YELLOW : Color.GRAY);
            g2.drawRect(sx, sy, slotSize - 2, slotSize - 2);

            // Item in slot
            if (i < inv.size()) {
               ItemStack stack = inv.get(i);
               BufferedImage itemTex = getItemTexture(stack.getType());
               if (itemTex != null) {
                  g2.drawImage(itemTex, sx + 3, sy + 3, slotSize - 8, slotSize - 8, null);
               } else {
                  g2.setColor(stack.getType().getColor());
                  g2.fillRect(sx + 6, sy + 6, 26, 26);
               }

               // Amount
               g2.setColor(Color.WHITE);
               g2.setFont(new Font("Monospaced", Font.BOLD, 10));
               g2.drawString(String.valueOf(stack.getAmount()), sx + 24, sy + 36);
            }

            // Slot number
            g2.setColor(new Color(200, 200, 200));
            g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
            g2.drawString(String.valueOf(i + 1), sx + 2, sy + 10);
         }
      }

      // --- HUD (am unteren Bildschirmrand, unter Hotbar) ---
      private void drawHUD(Graphics2D g2) {
         int hudY = getHeight() - HUD_HEIGHT;

         g2.setColor(new Color(20, 20, 20));
         g2.fillRect(0, hudY, getWidth(), HUD_HEIGHT);

         g2.setColor(Color.WHITE);
         g2.setFont(new Font("Monospaced", Font.PLAIN, 11));

         // Health bar
         int hp = player.getHealth();
         int maxHp = player.getMaxHealth();
         g2.drawString("HP:" + hp + "/" + maxHp, 6, hudY + 14);

         // Creative mode badge
         int modeOffset = 70;
         if (gameMode == GameMode.CREATIVE) {
            g2.setColor(new Color(80, 140, 255));
            g2.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2.drawString("[KREATIV]", 6 + modeOffset, hudY + 14);
            modeOffset += 72;
         }

         g2.setColor(Color.WHITE);
         g2.setFont(new Font("Monospaced", Font.PLAIN, 11));

         // Position
         g2.drawString("(" + player.getX() + "," + player.getY() + ")", 6 + modeOffset, hudY + 14);

         // Tile info
         Tile current = map.getTile(player.getX(), player.getY());
         g2.drawString(current.getType().name(), 76 + modeOffset, hudY + 14);

         String timeText = "Zeit " + formatDuration(getElapsedPlayTimeMs());
         FontMetrics timeMetrics = g2.getFontMetrics();
         g2.setColor(new Color(240, 220, 150));
         g2.drawString(timeText, getWidth() - timeMetrics.stringWidth(timeText) - 10, hudY + 14);

         if (rocketObjective != null) {
            g2.setColor(new Color(180, 220, 255));
            String objectiveText = "Rakete G " + rocketObjective.getDeliveredIronGears() + "/"
                  + RocketObjective.REQUIRED_IRON_GEARS
                  + "  C " + rocketObjective.getDeliveredCopperPlates() + "/"
                  + RocketObjective.REQUIRED_COPPER_PLATES
                  + "  B " + rocketObjective.getDeliveredConveyorBelts() + "/"
                  + RocketObjective.REQUIRED_CONVEYOR_BELTS;
            g2.drawString(objectiveText, 6, hudY - 8);
         }

         // Pollution bar (above rocket info)
         if (pollutionManager != null) {
            int pollution = pollutionManager.getPollutionLevel();
            int barX = 82;
            int barW = 100;
            int barH = 9;
            int barY = hudY - 42;
            // Background
            g2.setColor(new Color(50, 50, 50));
            g2.fillRect(barX, barY, barW, barH);
            // Fill: green → yellow → red based on level
            float ratio = pollution / 100.0f;
            Color barColor;
            if (ratio < 0.5f) {
               int r = (int) (ratio * 2 * 255);
               barColor = new Color(r, 200, 30);
            } else {
               int gc = (int) ((1f - ratio) * 2 * 200);
               barColor = new Color(220, gc, 20);
            }
            g2.setColor(barColor);
            g2.fillRect(barX, barY, (int) (barW * ratio), barH);
            // Border
            g2.setColor(new Color(150, 150, 150));
            g2.drawRect(barX, barY, barW, barH);
            // Label + value
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g2.drawString("Pollution:", 6, hudY - 34);
            g2.drawString(pollution + "%", barX + barW + 4, hudY - 34);
         }

         // Controls hint
         g2.setColor(new Color(150, 150, 150));
         g2.drawString(
               "[WASD]Move [E]Inv [C]Craft [Ctrl+S]Save [Enter]Mine [1-9]Hotbar [R]Rotate [Q]Deconstruct [Left]Place [Right]Interact/Feed",
               176 + modeOffset, hudY + 14);

         // HUD notification (save/load feedback, fades after 3 seconds)
         if (hudMessage != null) {
            long age = System.currentTimeMillis() - hudMessageTime;
            if (age < 3000) {
               int alpha = (int) (255 * Math.max(0.0, 1.0 - age / 3000.0));
               g2.setFont(new Font("Monospaced", Font.BOLD, 13));
               g2.setColor(new Color(100, 255, 120, alpha));
               FontMetrics fm = g2.getFontMetrics();
               int msgX = (getWidth() - fm.stringWidth(hudMessage)) / 2;
               g2.drawString(hudMessage, msgX, hudY - 60);
            } else {
               hudMessage = null;
            }
         }
      }

      // --- Inventory Overlay ---
      private void drawInventoryOverlay(Graphics2D g2) {
         int panelW = 320;
         int panelH = 280;
         int px = (getWidth() - panelW) / 2;
         int py = (getHeight() - panelH) / 2;

         // Dark overlay
         g2.setColor(new Color(0, 0, 0, 150));
         g2.fillRect(0, 0, getWidth(), getHeight());

         // Inventory panel
         g2.setColor(new Color(50, 50, 60, 240));
         g2.fillRoundRect(px, py, panelW, panelH, 12, 12);
         g2.setColor(Color.WHITE);
         g2.drawRoundRect(px, py, panelW, panelH, 12, 12);

         // Title
         g2.setFont(new Font("SansSerif", Font.BOLD, 16));
         g2.drawString("Inventar", px + panelW / 2 - 35, py + 24);

         // Items as grid
         g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
         List<ItemStack> inv = player.getInventory();
         int cols = 5;
         int cellSize = 50;
         int gridX = px + 20;
         int gridY = py + 40;

         for (int i = 0; i < player.getInventorySize(); i++) {
            int cx = gridX + (i % cols) * cellSize;
            int cy = gridY + (i / cols) * cellSize;

            // Slot
            g2.setColor(new Color(80, 80, 90));
            g2.fillRect(cx, cy, cellSize - 4, cellSize - 4);
            g2.setColor(Color.GRAY);
            g2.drawRect(cx, cy, cellSize - 4, cellSize - 4);

            if (i < inv.size()) {
               ItemStack stack = inv.get(i);
               BufferedImage itemTex = getItemTexture(stack.getType());
               if (itemTex != null) {
                  g2.drawImage(itemTex, cx + 5, cy + 5, 34, 34, null);
               } else {
                  g2.setColor(stack.getType().getColor());
                  g2.fillRect(cx + 6, cy + 6, 32, 32);
               }
               g2.setColor(Color.WHITE);
               g2.setFont(new Font("Monospaced", Font.BOLD, 10));
               g2.drawString(stack.getType().getDisplayName(), cx + 2, cy + cellSize - 8);
               g2.drawString("x" + stack.getAmount(), cx + 30, cy + 14);
            }
         }

         // Close hint
         g2.setColor(new Color(180, 180, 180));
         g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
         g2.drawString("Drücke [E] zum Schließen", px + panelW / 2 - 75, py + panelH - 10);
      }

      // --- Crafting Overlay ---
      private void drawCraftingOverlay(Graphics2D g2) {
         java.util.List<CraftingRecipe> recipes = craftingManager.getRecipes();
         int panelW = 380;
         int rowH = 56;
         int panelH = 60 + recipes.size() * rowH + 30;
         int px = (getWidth() - panelW) / 2;
         int py = (getHeight() - panelH) / 2;

         // Dark overlay
         g2.setColor(new Color(0, 0, 0, 150));
         g2.fillRect(0, 0, getWidth(), getHeight());

         // Panel
         g2.setColor(new Color(45, 45, 55, 240));
         g2.fillRoundRect(px, py, panelW, panelH, 12, 12);
         g2.setColor(new Color(120, 180, 255));
         g2.drawRoundRect(px, py, panelW, panelH, 12, 12);

         // Title
         g2.setFont(new Font("SansSerif", Font.BOLD, 16));
         g2.setColor(Color.WHITE);
         g2.drawString("Crafting", px + panelW / 2 - 30, py + 24);

         // Recipe list
         int listY = py + 40;
         for (int i = 0; i < recipes.size(); i++) {
            CraftingRecipe recipe = recipes.get(i);
            boolean selected = (i == craftingSelectedIndex);
            boolean canCraft = craftingManager.canCraft(recipe, player);
            int ry = listY + i * rowH;

            // Row background
            if (selected) {
               g2.setColor(new Color(80, 120, 200, 80));
               g2.fillRoundRect(px + 8, ry, panelW - 16, rowH - 4, 6, 6);
            }

            // Selection indicator
            if (selected) {
               g2.setColor(new Color(120, 180, 255));
               g2.fillRect(px + 8, ry + 4, 3, rowH - 12);
            }

            // Result icon
            BufferedImage resultTex = getItemTexture(recipe.getResult());
            if (resultTex != null) {
               g2.drawImage(resultTex, px + 18, ry + 4, 32, 32, null);
            }

            // Recipe name + result amount
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(canCraft ? Color.WHITE : new Color(150, 150, 150));
            g2.drawString(recipe.getName() + "  \u2192  x" + recipe.getResultAmount(),
                  px + 56, ry + 18);

            // Ingredients
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            StringBuilder ingStr = new StringBuilder();
            for (java.util.Map.Entry<ItemType, Integer> entry : recipe.getIngredients().entrySet()) {
               int have = player.getItemCount(entry.getKey());
               int need = entry.getValue();
               boolean enough = have >= need;
               if (ingStr.length() > 0)
                  ingStr.append("  ");
               ingStr.append(entry.getKey().getDisplayName())
                     .append(" ")
                     .append(enough ? "" : "!")
                     .append(have).append("/").append(need);
            }
            g2.setColor(canCraft ? new Color(180, 220, 180) : new Color(220, 140, 140));
            g2.drawString(ingStr.toString(), px + 56, ry + 36);

            // Craftability badge
            if (canCraft) {
               g2.setColor(new Color(60, 200, 80));
               g2.setFont(new Font("SansSerif", Font.BOLD, 10));
               g2.drawString("\u2714", px + panelW - 28, ry + 20);
            } else {
               g2.setColor(new Color(200, 80, 60));
               g2.setFont(new Font("SansSerif", Font.BOLD, 10));
               g2.drawString("\u2718", px + panelW - 28, ry + 20);
            }
         }

         // Crafting message (fade out after 2s)
         if (craftingMessage != null) {
            long age = System.currentTimeMillis() - craftingMessageTime;
            if (age < 2000) {
               int alpha = (int) (255 * (1.0 - age / 2000.0));
               g2.setFont(new Font("SansSerif", Font.BOLD, 13));
               g2.setColor(new Color(255, 255, 100, alpha));
               g2.drawString(craftingMessage, px + 20, py + panelH - 12);
            } else {
               craftingMessage = null;
            }
         }

         // Controls hint
         g2.setColor(new Color(160, 160, 170));
         g2.setFont(new Font("SansSerif", Font.ITALIC, 11));
         g2.drawString("[W/S] Auswahl  [Enter] Craften  [C] Schlie\u00DFen",
               px + 40, py + panelH - 28);
      }

      private BufferedImage getTileTexture(Tile tile) {
         switch (tile.getType()) {
            case IRON_DEPOSIT:
               return textures.get("iron_deposit");
            case COPPER_DEPOSIT:
               return textures.get("copper_deposit");
            case COAL_DEPOSIT:
               return textures.get("coal_deposit");
            case CONVEYOR_BELT:
               // Direction-aware belt texture: fall back to non-rotated if no belt data found
               return textures.get("conveyor_belt");
            case MACHINE:
               return textures.get("machine_bg");
            case ROCKET:
               return textures.get("rocket_pad");
            default:
               return textures.get("grass");
         }
      }

      private BufferedImage getRocketTexture() {
         if (rocketObjective == null) {
            return null;
         }
         if (rocketObjective.getStatus() == RocketObjective.Status.LAUNCHING) {
            int frame = Math.floorMod(animFrame / 5, 4);
            return textures.get("rocket_f" + frame);
         }
         return textures.get("rocket_f0");
      }

      private void drawRocketObjective(Graphics2D g2) {
         if (rocketObjective == null || rocketObjective.getStatus() == RocketObjective.Status.ENDED) {
            return;
         }

         BufferedImage rocketTex = getRocketTexture();
         if (rocketTex == null) {
            return;
         }

         int px = rocketObjective.getOriginX() * TILE_SIZE - cameraX;
         int py = rocketObjective.getOriginY() * TILE_SIZE - cameraY;
         int width = TILE_SIZE * RocketObjective.WIDTH;
         int height = TILE_SIZE * RocketObjective.HEIGHT;

         if (px > getWidth() || py > getHeight() || px + width < 0 || py + height < 0) {
            return;
         }

         int launchOffset = getRocketLaunchOffsetPixels();
         g2.drawImage(rocketTex, px, py - launchOffset, width, height, null);
      }

      private int getRocketLaunchOffsetPixels() {
         if (rocketObjective == null || rocketObjective.getStatus() != RocketObjective.Status.LAUNCHING) {
            return 0;
         }
         long elapsedSinceLaunch = getElapsedPlayTimeMs() - rocketObjective.getLaunchStartedAtElapsedMs();
         float progress = Math.max(0.0f,
               Math.min(1.0f, (float) elapsedSinceLaunch / (float) ROCKET_LAUNCH_DURATION_MS));
         return (int) (ROCKET_ASCENT_PIXELS * progress);
      }

      private void drawGameOverScreen(Graphics2D g2) {
         g2.setColor(new Color(0, 0, 0, 210));
         g2.fillRect(0, 0, getWidth(), getHeight());

         int panelW = 560;
         int panelH = 260;
         int px = (getWidth() - panelW) / 2;
         int py = (getHeight() - panelH) / 2;

         g2.setColor(new Color(46, 14, 14, 245));
         g2.fillRoundRect(px, py, panelW, panelH, 18, 18);
         g2.setColor(new Color(200, 80, 60));
         g2.setStroke(new BasicStroke(2f));
         g2.drawRoundRect(px, py, panelW, panelH, 18, 18);
         g2.setStroke(new BasicStroke(1f));

         g2.setColor(new Color(255, 100, 80));
         g2.setFont(new Font("SansSerif", Font.BOLD, 32));
         String title = "Zu viel Luftverschmutzung!";
         FontMetrics titleFm = g2.getFontMetrics();
         g2.drawString(title, px + (panelW - titleFm.stringWidth(title)) / 2, py + 70);

         g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
         g2.setColor(new Color(230, 190, 180));
         String subtitle = "Der Spieler ist an der Pollution gestorben.";
         FontMetrics subFm = g2.getFontMetrics();
         g2.drawString(subtitle, px + (panelW - subFm.stringWidth(subtitle)) / 2, py + 100);

         long endTime = finalElapsedPlayTimeMs >= 0L ? finalElapsedPlayTimeMs : getElapsedPlayTimeMs();
         g2.setFont(new Font("Monospaced", Font.BOLD, 30));
         g2.setColor(new Color(255, 180, 80));
         String timeStr = formatDuration(endTime);
         FontMetrics timeFm = g2.getFontMetrics();
         g2.drawString(timeStr, px + (panelW - timeFm.stringWidth(timeStr)) / 2, py + 150);

         g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
         g2.setColor(new Color(210, 180, 170));
         g2.drawString("Überlebenszeit", px + (panelW - 100) / 2, py + 122);

         g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
         g2.setColor(new Color(180, 160, 155));
         String hint = "Drücke ESC zum Beenden";
         FontMetrics hintFm = g2.getFontMetrics();
         g2.drawString(hint, px + (panelW - hintFm.stringWidth(hint)) / 2, py + 210);
      }

      private void drawEndScreen(Graphics2D g2) {
         g2.setColor(new Color(0, 0, 0, 205));
         g2.fillRect(0, 0, getWidth(), getHeight());

         int panelW = 560;
         int panelH = 240;
         int px = (getWidth() - panelW) / 2;
         int py = (getHeight() - panelH) / 2;

         g2.setColor(new Color(26, 34, 46, 245));
         g2.fillRoundRect(px, py, panelW, panelH, 18, 18);
         g2.setColor(new Color(150, 205, 255));
         g2.setStroke(new BasicStroke(2f));
         g2.drawRoundRect(px, py, panelW, panelH, 18, 18);
         g2.setStroke(new BasicStroke(1f));

         g2.setColor(new Color(235, 245, 255));
         g2.setFont(new Font("SansSerif", Font.BOLD, 34));
         g2.drawString("Rakete gestartet", px + 130, py + 70);

         long endTime = finalElapsedPlayTimeMs >= 0L ? finalElapsedPlayTimeMs : getElapsedPlayTimeMs();
         g2.setFont(new Font("Monospaced", Font.BOLD, 30));
         g2.setColor(new Color(255, 220, 120));
         g2.drawString(formatDuration(endTime), px + 195, py + 130);

         g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
         g2.setColor(new Color(210, 225, 245));
         g2.drawString("Benötigte Zeit", px + 205, py + 102);

         g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
         g2.setColor(new Color(170, 190, 210));
         g2.drawString("Drücke ESC zum Beenden", px + 192, py + 188);
      }

      /**
       * Returns the directional conveyor belt texture for the given ConveyorBelt.
       * Converts ConveyorBelt.Direction to game.machine.Direction for getRotated().
       */
      private BufferedImage getConveyorBeltTexture(ConveyorBelt belt, int frame) {
         game.machine.Direction machineDir;
         switch (belt.getDirection()) {
            case UP:
               machineDir = game.machine.Direction.UP;
               break;
            case RIGHT:
               machineDir = game.machine.Direction.RIGHT;
               break;
            case DOWN:
               machineDir = game.machine.Direction.DOWN;
               break;
            case LEFT:
            default:
               machineDir = game.machine.Direction.LEFT;
               break;
         }
         String key = "conveyor_belt_f" + frame;
         BufferedImage tex = textures.getRotated(key, machineDir);
         return (tex != null) ? tex : textures.getRotated("conveyor_belt", machineDir);
      }

      private BufferedImage getMachineTexture(BaseMachine machine, int frame) {
         if (machine instanceof Grabber) {
            Grabber grabber = (Grabber) machine;
            if (!grabber.isActiveForAnimation()) {
               BufferedImage t = textures.getRotated("grabber_f0", machine.getDirection());
               return (t != null) ? t : textures.getRotated("grabber", machine.getDirection());
            }
            int f = Math.floorMod(frame / 8, 4);
            BufferedImage t = textures.getRotated("grabber_f" + f, machine.getDirection());
            return (t != null) ? t : textures.getRotated("grabber", machine.getDirection());
         }
         if (machine instanceof Miner) {
            Miner miner = (Miner) machine;
            if (!miner.isActiveForAnimation()) {
               BufferedImage t = textures.getRotated("miner_f0", machine.getDirection());
               return (t != null) ? t : textures.getRotated("miner", machine.getDirection());
            }
            int f = Math.floorMod(frame / 15, 2);
            BufferedImage t = textures.getRotated("miner_f" + f, machine.getDirection());
            return (t != null) ? t : textures.getRotated("miner", machine.getDirection());
         }
         if (machine instanceof Smelter) {
            Smelter smelter = (Smelter) machine;
            if (!smelter.isActiveForAnimation()) {
               BufferedImage t = textures.getRotated("smelter_f0", machine.getDirection());
               return (t != null) ? t : textures.getRotated("smelter", machine.getDirection());
            }
            int f = Math.floorMod(frame / 10, 3);
            BufferedImage t = textures.getRotated("smelter_f" + f, machine.getDirection());
            return (t != null) ? t : textures.getRotated("smelter", machine.getDirection());
         }
         if (machine instanceof Forge) {
            Forge forge = (Forge) machine;
            if (forge.isActiveForAnimation()) {
               int f = Math.floorMod(frame / 10, 2);
               BufferedImage t = textures.getRotated("forge_f" + f, machine.getDirection());
               return (t != null) ? t : textures.getRotated("forge_idle", machine.getDirection());
            }

            BufferedImage t = textures.getRotated("forge_idle", machine.getDirection());
            return (t != null) ? t : textures.getRotated("forge", machine.getDirection());
         }
         return null;
      }

      private BufferedImage getItemTexture(ItemType type) {
         switch (type) {
            case IRON_ORE:
               return textures.get("item_iron_ore");
            case COPPER_ORE:
               return textures.get("item_copper_ore");
            case COAL:
               return textures.get("item_coal");
            case IRON_PLATE:
               return textures.get("item_iron_plate");
            case COPPER_PLATE:
               return textures.get("item_copper_plate");
            case IRON_GEAR:
               return textures.get("item_iron_gear");
            case CONVEYOR_BELT_ITEM:
               return textures.get("item_conveyor_belt");
            case MINER_KIT:
               return textures.get("item_miner_kit");
            case SMELTER_KIT:
               return textures.get("item_smelter_kit");
            case GRABBER_KIT:
               return textures.get("item_grabber_kit");
            case FORGE_KIT:
               return textures.get("item_forge_kit");
            default:
               return null;
         }
      }
   }
}
