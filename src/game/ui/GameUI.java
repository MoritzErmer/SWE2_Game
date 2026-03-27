package game.ui;

import game.GameMode;
import game.core.GameSupervisor;
import game.crafting.CraftingManager;
import game.crafting.CraftingRecipe;
import game.entity.ItemStack;
import game.entity.ItemType;
import game.entity.PlayerCharacter;
import game.logistics.ConveyorBelt;
import game.machine.BaseMachine;
import game.machine.Grabber;
import game.machine.Miner;
import game.machine.Smelter;
import game.save.SaveManager;
import game.world.Tile;
import game.world.TileType;
import game.world.WorldMap;
import game.ui.IsoRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Haupt-UI des Spiels mit isometrischem Grid-Rendering, Inventar-Overlay,
 * Hotbar, Mining-Mechanik (Enter halten) und Item-Platzierung (Linksklick).
 *
 * Rendering ist FPS-begrenzt über einen Swing-Timer, der repaint() triggert.
 * Mining läuft als separater SwingWorker-Thread, um den EDT nicht zu
 * blockieren.
 */
@SuppressWarnings("serial")
public class GameUI extends JFrame {
   private static final int TILE_SIZE = 32;   // Used only for UI elements (hotbar, inventory)
   private static final int HOTBAR_HEIGHT = 48;
   private static final int HUD_HEIGHT = 20;
   private static final int TARGET_FPS = 30;

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

   // HUD notification message (save/load feedback)
   private String hudMessage = null;
   private long hudMessageTime = 0;

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

   // Spieler-Blickrichtung (für Greifer-Platzierung und Vorschau)
   private int lastDx = 1; // Standard: nach rechts
   private int lastDy = 0;
   private game.machine.Direction lastDirection = game.machine.Direction.RIGHT;

   // Callback für Maschinen-Deconstruction (deregistrieren)
   private java.util.function.Consumer<BaseMachine> onMachineRemoved;
   private BiConsumer<Integer, Integer> onBeltRemoved;

   // Isometric rendering
   private final IsoRenderer isoRenderer = new IsoRenderer();
   private int isoOriginX = 0;
   private int isoOriginY = 0;
   private long animFrame = 0;

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
         if (!tile.hasMachine())
            return;
         BaseMachine machine = tile.getMachine();
         // Nur Greifer, Miner, Smelter rotierbar
         if (machine instanceof Grabber) {
            ((Grabber) machine).rotate();
            machine.setDirection(
                  game.machine.Direction.fromDxDy(((Grabber) machine).getDestDx(), ((Grabber) machine).getDestDy()));
         } else {
            // Miner/Smelter: Richtung rotieren
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
         if (!tile.hasMachine()) {
            if (tile.getType() == TileType.CONVEYOR_BELT) {
               tile.setType(TileType.EMPTY);
               player.addItem(ItemType.CONVEYOR_BELT_ITEM, 1);
               if (onBeltRemoved != null) {
                  onBeltRemoved.accept(player.getX(), player.getY());
               }
            }
            return;
         }
         BaseMachine machine = tile.getMachine();
         // Füge das passende Kit-Item ins Inventar zurück
         if (machine instanceof Miner) {
            player.addItem(ItemType.MINER_KIT, 1);
         } else if (machine instanceof Smelter) {
            player.addItem(ItemType.SMELTER_KIT, 1);
         } else if (machine instanceof Grabber) {
            player.addItem(ItemType.GRABBER_KIT, 1);
         } else {
            return;
         }
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
         SaveManager.save(supervisor, map, player, machineList, beltList, gameMode);
         SwingUtilities.invokeLater(() -> showHudMessage("Gespeichert!"));
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

   /**
    * Sets the supervisor and lists needed for save/load. Call before the game starts.
    */
   public void setSaveContext(GameSupervisor supervisor, List<BaseMachine> machines,
                               List<ConveyorBelt> belts) {
      this.supervisor = supervisor;
      this.machineList = machines;
      this.beltList = belts;
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

      this.textures = new PixelTextures();

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
            // --- Ctrl+S: Speichern ---
            if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) {
               saveGame();
               return;
            }
            // --- Ctrl+L: Laden (nur Hinweis, vollständige Wiederherstellung erfordert Neustart) ---
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
            Point worldPt = isoRenderer.screenToWorld(e.getX(), e.getY());
            int tx = worldPt.x;
            int ty = worldPt.y;
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
         gamePanel.repaint();
      });
      timer.start();

      // Frame sichtbar machen und Fokus fuer Tastatursteuerung setzen.
      setVisible(true);
      SwingUtilities.invokeLater(() -> gamePanel.requestFocusInWindow());
   }

   /** Centers the camera on the player in isometric space. */
   private void updateCamera() {
      int screenW = gamePanel.getWidth();
      int screenH = gamePanel.getHeight() - HOTBAR_HEIGHT - HUD_HEIGHT;

      int px = player.getX();
      int py = player.getY();

      // Place player at screen center in isometric space
      isoOriginX = screenW / 2 - (px - py) * (IsoRenderer.ISO_TILE_W / 2);
      isoOriginY = screenH / 2 - (px + py) * (IsoRenderer.ISO_TILE_H / 2);

      isoRenderer.updateOrigin(isoOriginX, isoOriginY);
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

   /** Platziert das ausgewählte Hotbar-Item auf einem Tile per Linksklick. */
   private void placeItemOnTile(int tx, int ty) {
      ItemStack selected = player.getSelectedItem();
      if (selected == null)
         return;
      if (!selected.getType().isPlaceable())
         return;

      Tile tile = map.getTile(tx, ty);
      tile.getLock().lock();
      try {
         if (tile.hasMachine())
            return;

         ItemType type = selected.getType();

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

         // Normales Item ablegen
         if (!tile.hasItem()) {
            if (player.consumeSelectedItem(1)) {
               tile.setItemOnGround(new ItemStack(type, 1));
            }
         }
      } finally {
         tile.getLock().unlock();
      }
   }

   // ==================== MACHINE INTERACTION ====================

   /**
    * Rechtsklick auf eine Maschine:
    * - Greifer: Kohle aus Inventar als Brennstoff einfüllen
    * - Smelter: Erz aus Inventar in Input-Buffer legen
    * - Jede Maschine: Output-Buffer ins Inventar nehmen
    */
   private void interactWithMachine(int tx, int ty) {
      Tile tile = map.getTile(tx, ty);
      tile.getLock().lock();
      try {
         if (!tile.hasMachine()) {
            // Kein Maschine → Item vom Boden aufheben
            if (tile.hasItem()) {
               ItemStack ground = tile.pickupItem();
               player.addItem(ground.getType(), ground.getAmount());
            }
            return;
         }

         BaseMachine machine = tile.getMachine();

         // Output abholen (höchste Priorität)
         if (machine.hasOutput()) {
            ItemStack out = machine.extractOutput();
            player.addItem(out.getType(), out.getAmount());
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
            ItemStack sel = player.getSelectedItem();
            if (sel != null) {
               ItemType type = sel.getType();
               if (type == ItemType.IRON_ORE || type == ItemType.COPPER_ORE) {
                  int toAdd = Math.min(5, sel.getAmount());
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
         if (tile.hasMachine())
            return;
         BaseMachine machine;
         if ("miner".equals(type) && tile.isResourceDeposit()) {
            machine = new Miner(tile);
         } else if ("smelter".equals(type)) {
            machine = new Smelter(tile);
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
    * Inneres JPanel für doppelt gepuffertes isometrisches Grid-Rendering.
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

         drawIsoGrid(g2);
         drawPlayer(g2);
         drawMiningBar(g2);
         drawHotbar(g2);
         drawHUD(g2);

         if (inventoryOpen) {
            drawInventoryOverlay(g2);
         }
         if (craftingOpen) {
            drawCraftingOverlay(g2);
         }
      }

      // --- Isometric Grid ---
      private void drawIsoGrid(Graphics2D g2) {
         g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
               RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_OFF);

         int screenW = gamePanel.getWidth();
         int screenH = gamePanel.getHeight() - HOTBAR_HEIGHT - HUD_HEIGHT;
         int mapW = map.getWidth();
         int mapH = map.getHeight();

         // Frustum culling with 3-tile padding (box sprites can poke into adjacent cells)
         int txMin = isoRenderer.visibleTxMin(screenW, screenH, mapW, mapH, 3);
         int txMax = isoRenderer.visibleTxMax(screenW, screenH, mapW, mapH, 3);
         int tyMin = isoRenderer.visibleTyMin(screenW, screenH, mapW, mapH, 3);
         int tyMax = isoRenderer.visibleTyMax(screenW, screenH, mapW, mapH, 3);

         isoRenderer.visitDepthSorted(txMin, txMax, tyMin, tyMax, (tx, ty) -> {
            if (!map.inBounds(tx, ty)) return;
            Tile tile = map.getTile(tx, ty);

            int sx = isoRenderer.screenX(tx, ty);
            int sy = isoRenderer.screenY(tx, ty);

            // --- 1. Ground tile (64x32 iso diamond) ---
            BufferedImage groundTex = getIsoGroundTexture(tile, tx, ty);
            if (groundTex != null) {
               g2.drawImage(groundTex, sx, sy, null);
            }

            // --- 2. Conveyor belt (renders as ground-level iso tile) ---
            if (tile.getType() == TileType.CONVEYOR_BELT && beltList != null) {
               final int fx = tx, fy = ty;
               ConveyorBelt belt = beltList.stream()
                     .filter(b -> b.getX() == fx && b.getY() == fy)
                     .findFirst().orElse(null);
               if (belt != null) {
                  int frame = (int) ((animFrame / 3) % 4);
                  String key = "conveyor_belt_frame" + frame;
                  game.machine.Direction dir = switch (belt.getDirection()) {
                     case UP    -> game.machine.Direction.UP;
                     case DOWN  -> game.machine.Direction.DOWN;
                     case LEFT  -> game.machine.Direction.LEFT;
                     case RIGHT -> game.machine.Direction.RIGHT;
                  };
                  BufferedImage beltTex = textures.getRotated(key, dir);
                  if (beltTex != null) g2.drawImage(beltTex, sx, sy, null);
               }
            }

            // --- 3. Machine box sprite (64x48, drawn ISO_BOX_H above ground) ---
            BaseMachine machine = getMachineAt(tx, ty);
            if (machine != null) {
               BufferedImage machineTex = getIsoMachineTexture(machine);
               if (machineTex != null) {
                  // Box sprite origin: tile diamond top minus front-face height
                  g2.drawImage(machineTex, sx, sy - IsoRenderer.ISO_BOX_H, null);
               }

               // Buffer indicators (text overlay, centred on diamond face)
               int cx = sx + IsoRenderer.ISO_TILE_W / 2;
               int cy = sy + IsoRenderer.ISO_TILE_H / 2 - IsoRenderer.ISO_BOX_H;
               g2.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 9));
               int inCount = machine.getInputBuffer() != null ? machine.getInputBuffer().getAmount() : 0;
               int outCount = machine.getOutputBuffer() != null ? machine.getOutputBuffer().getAmount() : 0;
               if (inCount > 0) {
                  g2.setColor(new java.awt.Color(150, 220, 255));
                  g2.drawString("I:" + inCount, cx - 14, cy - 4);
               }
               if (outCount > 0) {
                  g2.setColor(new java.awt.Color(255, 220, 80));
                  g2.drawString("O:" + outCount, cx - 14, cy + 8);
               }

               // Grabber direction arrow
               if (machine instanceof Grabber) {
                  Grabber gr = (Grabber) machine;
                  int ax = cx + gr.getDestDx() * 12;
                  int ay = cy + IsoRenderer.ISO_BOX_H / 2 + gr.getDestDy() * 6;
                  g2.setColor(new java.awt.Color(255, 255, 100, 200));
                  g2.setStroke(new java.awt.BasicStroke(2));
                  g2.drawLine(cx - gr.getDestDx() * 8, cy + IsoRenderer.ISO_BOX_H / 2 - gr.getDestDy() * 6, ax, ay);
                  g2.setStroke(new java.awt.BasicStroke(1));
               }
            }

            // --- 4. Item on ground (small icon centred in diamond) ---
            if (tile.hasItem()) {
               ItemStack item = tile.getItemOnGround();
               if (item != null) {
                  BufferedImage icon = getItemTexture(item.getType());
                  if (icon != null) {
                     int iconSize = 20;
                     g2.drawImage(icon,
                           sx + IsoRenderer.ISO_TILE_W / 2 - iconSize / 2,
                           sy + IsoRenderer.ISO_TILE_H / 2 - iconSize / 2,
                           iconSize, iconSize, null);
                  }
               }
            }
         });
      }

      /** Returns the ground texture for the tile. Conveyor belt is handled separately. */
      private BufferedImage getIsoGroundTexture(Tile tile, int tx, int ty) {
         return switch (tile.getType()) {
            case IRON_DEPOSIT   -> textures.get("iron_deposit");
            case COPPER_DEPOSIT -> textures.get("copper_deposit");
            case COAL_DEPOSIT   -> textures.get("coal_deposit");
            case MACHINE        -> textures.get("machine_bg");
            case CONVEYOR_BELT  -> textures.get("stone_floor"); // belt drawn on top separately
            default             -> textures.get("grass");       // EMPTY and any future types
         };
      }

      /** Returns the animated box sprite for a machine. */
      private BufferedImage getIsoMachineTexture(BaseMachine machine) {
         if (machine instanceof Smelter) {
            int frame = (int) ((animFrame / 5) % 3);
            return textures.getRotated("smelter_frame" + frame, machine.getDirection());
         }
         if (machine instanceof Grabber) {
            int frame = (int) ((animFrame / 4) % 4);
            return textures.getRotated("grabber_frame" + frame, machine.getDirection());
         }
         if (machine instanceof Miner) {
            return textures.getRotated("miner", machine.getDirection());
         }
         return textures.get("machine_bg");
      }

      /** Returns the machine located at world tile (tx, ty), or null. */
      private BaseMachine getMachineAt(int tx, int ty) {
         if (!map.inBounds(tx, ty)) return null;
         Tile tile = map.getTile(tx, ty);
         if (!tile.hasMachine()) return null;
         return tile.getMachine();
      }

      // --- Player (Pixel-Art Textur) with isometric offset ---
      private void drawPlayer(Graphics2D g2) {
         int sx = isoRenderer.screenX(player.getX(), player.getY());
         int sy = isoRenderer.screenY(player.getX(), player.getY()) - IsoRenderer.ISO_BOX_H;
         BufferedImage playerTex = textures.get("player");
         if (playerTex != null) {
            g2.drawImage(playerTex, sx, sy, IsoRenderer.ISO_TILE_W, IsoRenderer.ISO_BOX_SPRITE_H, null);
         }
      }

      // --- Mining Progress Bar (over player, isometric) ---
      private void drawMiningBar(Graphics2D g2) {
         if (!miningInProgress || !enterHeld)
            return;

         long elapsed = System.currentTimeMillis() - miningStartTime;
         float progress = Math.min(1.0f, (float) elapsed / miningRequiredMs);

         int barWidth = 80;
         int barHeight = 10;
         int sx = isoRenderer.screenX(player.getX(), player.getY());
         int sy = isoRenderer.screenY(player.getX(), player.getY());
         int bx = sx + IsoRenderer.ISO_TILE_W / 2 - barWidth / 2;
         int by = sy - IsoRenderer.ISO_BOX_H - 20;

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

         // Controls hint
         g2.setColor(new Color(150, 150, 150));
         g2.drawString("[WASD]Move [E]Inv [C]Craft [Ctrl+S]Save [Enter]Mine [1-9]Hotbar",
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
               g2.drawString(hudMessage, msgX, hudY - 8);
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
            default:
               return null;
         }
      }
   }
}
