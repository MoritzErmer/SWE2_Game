package game.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import game.machine.Direction;
import game.objective.RocketObjective;

/**
 * Generiert 8x8 Pixel-Art-Texturen für alle Tile-Typen, Maschinen, Items und
 * den Spieler.
 * Jede Textur wird als BufferedImage vorberechnet und beim Rendern auf
 * TILE_SIZE skaliert.
 * Nearest-Neighbor-Interpolation bewahrt den Pixel-Art-Stil.
 */
public class PixelTextures {

   private static final int PX = 8; // 8x8 Pixel-Textur

   private final Map<String, BufferedImage> cache = new HashMap<>();
   private final int tileSize;

   public PixelTextures(int tileSize) {
      this.tileSize = tileSize;
      generateAll();
   }

   public BufferedImage get(String key) {
      return cache.get(key);
   }

   // ==================== Textur-Generierung ====================

   private void generateAll() {
      cache.put("grass", generateGrass());
      cache.put("iron_deposit", generateIronDeposit());
      cache.put("copper_deposit", generateCopperDeposit());
      cache.put("coal_deposit", generateCoalDeposit());
      cache.put("conveyor_belt", generateConveyorBelt());
      cache.put("rocket_pad", generateRocketPad());
      cache.put("machine_bg", generateMachineBg());
      cache.put("miner", generateMinerDirectional());
      cache.put("smelter", generateSmelterDirectional());
      cache.put("forge", generateForgeIdleDirectional());
      cache.put("forge_idle", generateForgeIdleDirectional());
      cache.put("player", generatePlayer());
      cache.put("item_iron_ore", generateItemTexture(new Color(139, 90, 43), new Color(100, 65, 30)));
      cache.put("item_copper_ore", generateItemTexture(new Color(184, 115, 51), new Color(140, 85, 35)));
      cache.put("item_coal", generateItemTexture(new Color(40, 40, 40), new Color(25, 25, 25)));
      cache.put("item_iron_plate", generatePlateTexture(new Color(180, 180, 200), new Color(140, 140, 160)));
      cache.put("item_copper_plate", generatePlateTexture(new Color(210, 140, 80), new Color(170, 110, 60)));
      cache.put("item_iron_gear", generateGearTexture());
      cache.put("item_conveyor_belt", generateItemTexture(new Color(100, 100, 110), new Color(70, 70, 80)));
      cache.put("item_miner_kit", generateItemTexture(new Color(200, 180, 60), new Color(160, 140, 40)));
      cache.put("item_smelter_kit", generateItemTexture(new Color(220, 100, 50), new Color(180, 70, 30)));
      cache.put("item_grabber_kit", generateItemTexture(new Color(120, 200, 120), new Color(80, 160, 80)));
      cache.put("item_forge_kit", generateItemTexture(new Color(145, 120, 85), new Color(105, 85, 60)));
      cache.put("grabber", generateGrabberTexture());
      generateConveyorBeltFrames();
      generateMinerFrames();
      generateSmelterFrames();
      generateForgeFrames();
      generateGrabberFrames();
      generateRocketFrames();
   }

   private void generateConveyorBeltFrames() {
      Color bg    = new Color(0x44, 0x44, 0x44);
      Color rail  = new Color(0x88, 0x88, 0x88);
      Color arrow = new Color(0xFF, 0xFF, 0xFF);
      Color spare = new Color(0xAA, 0xAA, 0xAA);
      Color[] pal = { bg, rail, arrow, spare };
      // Frame 0: chevron tip at col 3 (same as static base)
      int[][] f0 = {
            { 1, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 2, 0, 0, 0, 1 },
            { 1, 0, 0, 2, 2, 0, 0, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 0, 0, 2, 2, 0, 0, 1 },
            { 1, 0, 0, 2, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 1 },
      };
      // Frame 1: chevron tip shifted +2 to col 5
      int[][] f1 = {
            { 1, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 2, 0, 1 },
            { 1, 0, 0, 0, 0, 2, 2, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 0, 0, 0, 0, 2, 2, 1 },
            { 1, 0, 0, 0, 0, 2, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 1 },
      };
      // Frame 2: chevron wraps — tip at col 1 (just entered from left)
      int[][] f2 = {
            { 1, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 2, 0, 0, 0, 0, 0, 1 },
            { 1, 2, 2, 0, 0, 0, 0, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 2, 2, 0, 0, 0, 0, 1 },
            { 1, 2, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 1 },
      };
      // Frame 3: chevron tip at col 2
      int[][] f3 = {
            { 1, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 2, 0, 0, 0, 0, 1 },
            { 1, 0, 2, 2, 0, 0, 0, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 0, 2, 2, 0, 0, 0, 1 },
            { 1, 0, 2, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 1 },
      };
      cache.put("conveyor_belt_f0", buildFromPalette(f0, pal));
      cache.put("conveyor_belt_f1", buildFromPalette(f1, pal));
      cache.put("conveyor_belt_f2", buildFromPalette(f2, pal));
      cache.put("conveyor_belt_f3", buildFromPalette(f3, pal));
   }

   private void generateMinerFrames() {
      Color bg   = new Color(0, 0, 0, 0);
      Color body = new Color(220, 140, 30);
      Color dark = new Color(170, 100, 20);
      Color pick = new Color(180, 180, 190);
      // Frame 0: identical to generateMinerDirectional
      int[][] f0 = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 1, 1, 0, 3, 3, 0 },
            { 0, 1, 2, 1, 1, 0, 3, 0 },
            { 0, 1, 1, 2, 1, 1, 3, 0 },
            { 0, 1, 1, 2, 1, 1, 3, 0 },
            { 0, 1, 2, 1, 1, 0, 3, 0 },
            { 0, 0, 1, 1, 0, 3, 3, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      // Frame 1: pick tip shifted 1px right (drill impact)
      int[][] f1 = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 1, 1, 0, 0, 3, 3 },
            { 0, 1, 2, 1, 1, 0, 0, 3 },
            { 0, 1, 1, 2, 1, 1, 0, 3 },
            { 0, 1, 1, 2, 1, 1, 0, 3 },
            { 0, 1, 2, 1, 1, 0, 0, 3 },
            { 0, 0, 1, 1, 0, 0, 3, 3 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      Color[] pal = { bg, body, dark, pick };
      cache.put("miner_f0", buildFromPalette(f0, pal));
      cache.put("miner_f1", buildFromPalette(f1, pal));
   }

   private void generateSmelterFrames() {
      Color bg   = new Color(0, 0, 0, 0);
      Color body = new Color(160, 50, 40);
      Color dark = new Color(120, 30, 25);
      // Same pixel layout as generateSmelterDirectional, only fire color cycles
      int[][] map = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 1, 1, 1, 2, 0, 0 },
            { 0, 1, 1, 1, 1, 1, 0, 0 },
            { 0, 1, 2, 2, 1, 1, 3, 3 },
            { 0, 1, 2, 2, 1, 1, 3, 3 },
            { 0, 1, 1, 1, 1, 1, 0, 0 },
            { 0, 2, 1, 1, 1, 2, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      cache.put("smelter_f0", buildFromPalette(map, new Color[]{ bg, body, dark, new Color(255, 180, 30) }));
      cache.put("smelter_f1", buildFromPalette(map, new Color[]{ bg, body, dark, new Color(230, 100, 20) }));
      cache.put("smelter_f2", buildFromPalette(map, new Color[]{ bg, body, dark, new Color(255, 140, 10) }));
   }

      private void generateForgeFrames() {
      Color bg = new Color(0, 0, 0, 0);
      Color body = new Color(105, 85, 70);
      Color dark = new Color(70, 55, 45);
      Color chimney = new Color(95, 95, 95);

      int[][] idle = {
         { 0, 0, 4, 0, 0, 0, 0, 0 },
         { 0, 2, 1, 1, 1, 2, 0, 0 },
         { 0, 1, 1, 1, 1, 1, 0, 0 },
         { 0, 1, 2, 2, 1, 1, 3, 3 },
         { 0, 1, 2, 2, 1, 1, 3, 3 },
         { 0, 1, 1, 1, 1, 1, 0, 0 },
         { 0, 2, 1, 1, 1, 2, 0, 0 },
         { 0, 0, 0, 0, 0, 0, 0, 0 },
      };

      int[][] activePulse = {
         { 0, 0, 4, 0, 0, 0, 0, 0 },
         { 0, 2, 1, 1, 1, 2, 0, 0 },
         { 0, 1, 1, 1, 1, 1, 3, 0 },
         { 0, 1, 2, 2, 1, 1, 3, 3 },
         { 0, 1, 2, 2, 1, 1, 3, 3 },
         { 0, 1, 1, 1, 1, 1, 3, 0 },
         { 0, 2, 1, 1, 1, 2, 0, 0 },
         { 0, 0, 0, 0, 0, 0, 0, 0 },
      };

      cache.put("forge_idle", buildFromPalette(idle,
         new Color[]{ bg, body, dark, new Color(150, 60, 30), chimney }));
      cache.put("forge_f0", buildFromPalette(idle,
         new Color[]{ bg, body, dark, new Color(245, 135, 45), chimney }));
      cache.put("forge_f1", buildFromPalette(activePulse,
         new Color[]{ bg, body, dark, new Color(255, 185, 60), chimney }));
      }

   private void generateGrabberFrames() {
      Color bg   = new Color(0, 0, 0, 0);
      Color body = new Color(60, 120, 210);
      Color dark = new Color(40, 80, 160);
      Color tip  = new Color(230, 80, 30);
      Color[] pal = { bg, body, dark, tip };
      // Frame 0: arm retracted (tip only at col 4)
      int[][] f0 = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 3, 0, 0, 0 },
            { 0, 1, 2, 1, 3, 0, 0, 0 },
            { 0, 1, 2, 1, 3, 0, 0, 0 },
            { 0, 1, 1, 1, 3, 0, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      // Frame 1: arm half extended (cols 4-5)
      int[][] f1 = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 3, 3, 0, 0 },
            { 0, 1, 2, 1, 3, 3, 0, 0 },
            { 0, 1, 2, 1, 3, 3, 0, 0 },
            { 0, 1, 1, 1, 3, 3, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      // Frame 2: arm fully extended (cols 4-7, same as static base)
      int[][] f2 = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 3, 0, 0, 0 },
            { 0, 1, 2, 1, 3, 3, 3, 3 },
            { 0, 1, 2, 1, 3, 3, 3, 3 },
            { 0, 1, 1, 1, 3, 0, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      // Frame 3: arm retracting (cols 4-6)
      int[][] f3 = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 3, 3, 3, 0 },
            { 0, 1, 2, 1, 3, 3, 3, 0 },
            { 0, 1, 2, 1, 3, 3, 3, 0 },
            { 0, 1, 1, 1, 3, 3, 3, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      cache.put("grabber_f0", buildFromPalette(f0, pal));
      cache.put("grabber_f1", buildFromPalette(f1, pal));
      cache.put("grabber_f2", buildFromPalette(f2, pal));
      cache.put("grabber_f3", buildFromPalette(f3, pal));
   }

   private BufferedImage generateRocketPad() {
      Color center = new Color(64, 70, 80);
      Color asphalt = new Color(43, 48, 56);
      Color panel = new Color(96, 106, 118);
      Color hazard = new Color(216, 170, 74);
      int[][] map = {
         { 1, 1, 1, 1, 1, 1, 1, 1 },
         { 1, 2, 3, 2, 2, 3, 2, 1 },
         { 1, 3, 0, 0, 0, 0, 3, 1 },
         { 1, 2, 0, 2, 2, 0, 2, 1 },
         { 1, 2, 0, 2, 2, 0, 2, 1 },
         { 1, 3, 0, 0, 0, 0, 3, 1 },
         { 1, 2, 3, 2, 2, 3, 2, 1 },
         { 1, 1, 1, 1, 1, 1, 1, 1 },
      };
      return buildFromPalette(map, new Color[] { center, asphalt, panel, hazard });
   }

   private void generateRocketFrames() {
      for (int frame = 0; frame < 4; frame++) {
         cache.put("rocket_f" + frame, generateRocketFrame(frame));
      }
   }

   private BufferedImage generateRocketFrame(int frame) {
      int size = PX * RocketObjective.WIDTH;
      BufferedImage small = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = small.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

      Color outline = new Color(58, 64, 78);
      Color hullLight = new Color(234, 239, 246);
      Color hullMid = new Color(186, 197, 214);
      Color hullDark = new Color(121, 135, 156);
      Color nose = new Color(224, 90, 78);
      Color noseShade = new Color(177, 63, 55);
      Color stripe = new Color(228, 178, 74);
      Color window = new Color(108, 198, 245);
      Color windowRing = new Color(44, 122, 174);
      Color booster = new Color(199, 207, 218);
      Color boosterDark = new Color(120, 127, 140);
      Color fin = new Color(192, 74, 66);
      Color engine = new Color(82, 90, 104);
      Color flameOuter = new Color(255, 194, 76);
      Color flameMid = new Color(255, 145, 42);
      Color flameInner = new Color(255, 96, 28);

      int centerX = size / 2;
      int bodyTop = 7;
      int bodyBottom = 24;

      // Side boosters for a stronger rocket silhouette.
      g.setColor(boosterDark);
      g.fillRect(centerX - 9, 12, 3, 11);
      g.fillRect(centerX + 6, 12, 3, 11);
      g.setColor(booster);
      g.fillRect(centerX - 8, 12, 2, 10);
      g.fillRect(centerX + 6, 12, 2, 10);
      g.setColor(engine);
      g.fillRect(centerX - 8, 22, 2, 2);
      g.fillRect(centerX + 6, 22, 2, 2);

      // Main body.
      g.setColor(hullDark);
      g.fillRect(centerX - 5, bodyTop, 10, bodyBottom - bodyTop);
      g.setColor(hullLight);
      g.fillRect(centerX - 4, bodyTop + 1, 8, bodyBottom - bodyTop - 2);
      g.setColor(hullMid);
      g.fillRect(centerX + 1, bodyTop + 1, 3, bodyBottom - bodyTop - 2);

      // Nose cone and right-side shading.
      Polygon noseCone = new Polygon(
         new int[] { centerX, centerX - 6, centerX + 6 },
         new int[] { 1, bodyTop + 2, bodyTop + 2 },
         3);
      g.setColor(nose);
      g.fillPolygon(noseCone);

      Polygon noseRightShade = new Polygon(
         new int[] { centerX, centerX + 6, centerX + 2 },
         new int[] { 2, bodyTop + 2, bodyTop + 2 },
         3);
      g.setColor(noseShade);
      g.fillPolygon(noseRightShade);

      g.setColor(outline);
      g.drawPolygon(noseCone);

      // Stripes and portholes.
      g.setColor(stripe);
      g.fillRect(centerX - 3, 13, 6, 2);
      g.fillRect(centerX - 2, 18, 4, 1);

      g.setColor(window);
      g.fillOval(centerX - 2, 10, 4, 4);
      g.fillOval(centerX - 2, 15, 4, 4);
      g.setColor(windowRing);
      g.drawOval(centerX - 2, 10, 4, 4);
      g.drawOval(centerX - 2, 15, 4, 4);

      // Stabilizer fins.
      Polygon leftFin = new Polygon(
         new int[] { centerX - 5, centerX - 10, centerX - 5 },
         new int[] { 20, 25, 24 },
         3);
      Polygon rightFin = new Polygon(
         new int[] { centerX + 5, centerX + 10, centerX + 5 },
         new int[] { 20, 25, 24 },
         3);
      g.setColor(fin);
      g.fillPolygon(leftFin);
      g.fillPolygon(rightFin);
      g.setColor(outline);
      g.drawPolygon(leftFin);
      g.drawPolygon(rightFin);

      // Main engine nozzle.
      g.setColor(engine);
      g.fillRect(centerX - 3, bodyBottom - 1, 6, 3);
      g.setColor(outline);
      g.drawRect(centerX - 3, bodyBottom - 1, 5, 2);

      int[] outerLength = { 4, 8, 12, 9 };
      int[] midLength = { 3, 6, 9, 7 };
      int[] innerLength = { 2, 4, 6, 5 };
      int flameTopY = bodyBottom + 2;
      int f = Math.floorMod(frame, 4);

      Polygon outerFlame = new Polygon(
         new int[] { centerX - 4, centerX + 4, centerX },
         new int[] { flameTopY, flameTopY, flameTopY + outerLength[f] },
         3);
      g.setColor(flameOuter);
      g.fillPolygon(outerFlame);

      Polygon midFlame = new Polygon(
         new int[] { centerX - 3, centerX + 3, centerX },
         new int[] { flameTopY + 1, flameTopY + 1, flameTopY + midLength[f] },
         3);
      g.setColor(flameMid);
      g.fillPolygon(midFlame);

      Polygon innerFlame = new Polygon(
         new int[] { centerX - 2, centerX + 2, centerX },
         new int[] { flameTopY + 1, flameTopY + 1, flameTopY + innerLength[f] },
         3);
      g.setColor(flameInner);
      g.fillPolygon(innerFlame);

      g.dispose();
      return scaleNearest(small, tileSize * RocketObjective.WIDTH, tileSize * RocketObjective.HEIGHT);
   }

   // --- Grass: Grüntöne mit zufälligen dunkleren Grashalmen ---
   private BufferedImage generateGrass() {
      int[][] px = {
            { 0, 1, 0, 0, 1, 0, 0, 1 },
            { 0, 0, 0, 1, 0, 0, 1, 0 },
            { 1, 0, 0, 0, 0, 1, 0, 0 },
            { 0, 0, 1, 0, 0, 0, 0, 1 },
            { 0, 1, 0, 0, 1, 0, 0, 0 },
            { 0, 0, 0, 1, 0, 0, 1, 0 },
            { 1, 0, 0, 0, 0, 0, 0, 1 },
            { 0, 0, 1, 0, 1, 0, 0, 0 },
      };
      Color base = new Color(42, 130, 42);
      Color dark = new Color(30, 100, 30);
      Color light = new Color(55, 150, 50);
      return buildTexture(px, new Color[] { base, dark, light }, new int[][] {
            { 0, 1, 0, 0, 2, 0, 0, 1 },
            { 0, 0, 0, 1, 0, 0, 2, 0 },
            { 2, 0, 0, 0, 0, 1, 0, 0 },
            { 0, 0, 1, 0, 0, 0, 0, 2 },
            { 0, 2, 0, 0, 1, 0, 0, 0 },
            { 0, 0, 0, 2, 0, 0, 1, 0 },
            { 1, 0, 0, 0, 0, 0, 0, 2 },
            { 0, 0, 2, 0, 1, 0, 0, 0 },
      });
   }

   // --- Iron Deposit: Braun-Grau mit hellen Erzbrocken ---
   private BufferedImage generateIronDeposit() {
      Color dirt = new Color(100, 75, 50);
      Color rock = new Color(120, 110, 100);
      Color ore = new Color(180, 170, 160);
      Color shine = new Color(210, 200, 190);
      int[][] map = {
            { 0, 0, 1, 1, 0, 0, 1, 0 },
            { 0, 1, 2, 3, 1, 0, 0, 0 },
            { 1, 2, 3, 2, 1, 0, 0, 1 },
            { 0, 1, 2, 1, 0, 0, 1, 2 },
            { 0, 0, 1, 0, 0, 1, 2, 3 },
            { 0, 0, 0, 0, 1, 2, 3, 2 },
            { 1, 0, 0, 0, 0, 1, 2, 1 },
            { 0, 0, 1, 0, 0, 0, 1, 0 },
      };
      return buildFromPalette(map, new Color[] { dirt, rock, ore, shine });
   }

   // --- Copper Deposit: Orange-Braun mit Kupferflecken ---
   private BufferedImage generateCopperDeposit() {
      Color dirt = new Color(90, 60, 35);
      Color rock = new Color(110, 80, 50);
      Color copper = new Color(195, 120, 55);
      Color shine = new Color(230, 160, 80);
      int[][] map = {
            { 0, 1, 0, 0, 0, 1, 0, 0 },
            { 1, 2, 2, 1, 0, 0, 0, 1 },
            { 0, 2, 3, 2, 0, 0, 1, 2 },
            { 0, 1, 2, 1, 0, 0, 2, 3 },
            { 0, 0, 0, 0, 1, 2, 3, 2 },
            { 0, 1, 0, 1, 2, 3, 2, 1 },
            { 1, 0, 0, 0, 1, 2, 1, 0 },
            { 0, 0, 1, 0, 0, 1, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { dirt, rock, copper, shine });
   }

   // --- Coal Deposit: Dunkle Kohle mit Glanzpunkten ---
   private BufferedImage generateCoalDeposit() {
      Color base = new Color(35, 30, 30);
      Color dark = new Color(20, 18, 18);
      Color coal = new Color(55, 50, 50);
      Color glint = new Color(80, 75, 75);
      int[][] map = {
            { 0, 1, 0, 0, 1, 0, 0, 1 },
            { 1, 2, 2, 1, 0, 0, 1, 0 },
            { 0, 2, 3, 2, 0, 1, 2, 2 },
            { 0, 1, 2, 1, 0, 0, 2, 3 },
            { 1, 0, 0, 0, 1, 0, 1, 2 },
            { 0, 0, 1, 2, 2, 1, 0, 1 },
            { 0, 1, 2, 3, 2, 0, 0, 0 },
            { 1, 0, 1, 2, 1, 0, 1, 0 },
      };
      return buildFromPalette(map, new Color[] { base, dark, coal, glint });
   }

   // --- Conveyor Belt: Klarer Richtungspfeil (Basis-Richtung = RIGHT, Pfeil nach rechts) ---
   // Palette: 0=#444444 (Hintergrund), 1=#888888 (Schiene), 2=#FFFFFF (Pfeil), 3=#AAAAAA (unused)
   // Das Sprite zeigt nach RECHTS (Standard). getRotated() dreht es für andere Richtungen.
   private BufferedImage generateConveyorBelt() {
      Color bg    = new Color(0x44, 0x44, 0x44);
      Color rail  = new Color(0x88, 0x88, 0x88);
      Color arrow = new Color(0xFF, 0xFF, 0xFF);
      Color spare = new Color(0xAA, 0xAA, 0xAA);
      // Arrow pointing RIGHT (base direction = RIGHT, matches getRotated convention)
      int[][] map = {
            { 1, 0, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 2, 0, 0, 0, 1 },
            { 1, 0, 0, 2, 2, 0, 0, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 0, 0, 2, 2, 0, 0, 1 },
            { 1, 0, 0, 2, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 0, 1 },
      };
      return buildFromPalette(map, new Color[] { bg, rail, arrow, spare });
   }

   // --- Machine background: Metallplatten-Look ---
   private BufferedImage generateMachineBg() {
      Color dark = new Color(70, 70, 75);
      Color med = new Color(95, 95, 100);
      Color light = new Color(110, 110, 115);
      Color bolt = new Color(140, 140, 50);
      int[][] map = {
            { 3, 1, 1, 1, 1, 1, 1, 3 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 1, 2, 1, 1, 1, 1, 2, 1 },
            { 1, 2, 1, 0, 0, 1, 2, 1 },
            { 1, 2, 1, 0, 0, 1, 2, 1 },
            { 1, 2, 1, 1, 1, 1, 2, 1 },
            { 1, 2, 2, 2, 2, 2, 2, 1 },
            { 3, 1, 1, 1, 1, 1, 1, 3 },
      };
      return buildFromPalette(map, new Color[] { dark, med, light, bolt });
   }

   // --- Miner: Orange mit Spitzhacke ---
   private BufferedImage generateMiner() {
      Color bg = new Color(0, 0, 0, 0);
      Color body = new Color(220, 140, 30);
      Color dark = new Color(170, 100, 20);
      Color pick = new Color(180, 180, 190);
      int[][] map = {
            { 0, 0, 0, 3, 3, 0, 0, 0 },
            { 0, 0, 3, 3, 0, 0, 0, 0 },
            { 0, 1, 1, 2, 1, 1, 0, 0 },
            { 0, 1, 2, 2, 2, 1, 0, 0 },
            { 0, 1, 1, 2, 1, 1, 0, 0 },
            { 0, 0, 1, 1, 1, 0, 0, 0 },
            { 0, 0, 2, 0, 2, 0, 0, 0 },
            { 0, 0, 2, 0, 2, 0, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, body, dark, pick });
   }

   // --- Smelter: Roter Ofen mit Feuer ---
   private BufferedImage generateSmelter() {
      Color bg = new Color(0, 0, 0, 0);
      Color body = new Color(160, 50, 40);
      Color dark = new Color(120, 30, 25);
      Color fire = new Color(255, 180, 30);
      int[][] map = {
            { 0, 0, 2, 2, 2, 2, 0, 0 },
            { 0, 2, 1, 1, 1, 1, 2, 0 },
            { 0, 1, 1, 1, 1, 1, 1, 0 },
            { 0, 1, 2, 3, 3, 2, 1, 0 },
            { 0, 1, 3, 3, 3, 3, 1, 0 },
            { 0, 1, 2, 3, 3, 2, 1, 0 },
            { 0, 2, 1, 1, 1, 1, 2, 0 },
            { 0, 0, 2, 2, 2, 2, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, body, dark, fire });
   }

   // --- Player: Blaues Männchen ---
   private BufferedImage generatePlayer() {
      Color bg = new Color(0, 0, 0, 0);
      Color body = new Color(30, 100, 220);
      Color dark = new Color(20, 70, 170);
      Color skin = new Color(240, 200, 160);
      int[][] map = {
            { 0, 0, 3, 3, 3, 3, 0, 0 },
            { 0, 0, 3, 3, 3, 3, 0, 0 },
            { 0, 0, 1, 1, 1, 1, 0, 0 },
            { 0, 1, 1, 2, 2, 1, 1, 0 },
            { 0, 0, 1, 1, 1, 1, 0, 0 },
            { 0, 0, 1, 1, 1, 1, 0, 0 },
            { 0, 0, 2, 0, 0, 2, 0, 0 },
            { 0, 0, 2, 0, 0, 2, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, body, dark, skin });
   }

   // --- Generische Erz-Item-Textur ---
   private BufferedImage generateItemTexture(Color main, Color shadow) {
      Color bg = new Color(0, 0, 0, 0);
      Color hi = brighter(main, 40);
      int[][] map = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 1, 1, 1, 0, 0, 0 },
            { 0, 1, 3, 1, 1, 1, 0, 0 },
            { 0, 1, 1, 1, 2, 1, 0, 0 },
            { 0, 1, 2, 1, 1, 1, 0, 0 },
            { 0, 0, 1, 1, 1, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, main, shadow, hi });
   }

   // --- Generische Platten-Item-Textur ---
   private BufferedImage generatePlateTexture(Color main, Color shadow) {
      Color bg = new Color(0, 0, 0, 0);
      Color hi = brighter(main, 30);
      int[][] map = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 1, 1, 1, 0 },
            { 0, 1, 3, 3, 1, 1, 1, 0 },
            { 0, 1, 3, 1, 1, 1, 1, 0 },
            { 0, 1, 1, 1, 1, 2, 1, 0 },
            { 0, 1, 1, 1, 2, 2, 1, 0 },
            { 0, 1, 1, 1, 1, 1, 1, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, main, shadow, hi });
   }

   // --- Gear: Zahnrad-Form ---
   private BufferedImage generateGearTexture() {
      Color bg = new Color(0, 0, 0, 0);
      Color main = new Color(160, 160, 180);
      Color dark = new Color(120, 120, 140);
      Color hi = new Color(200, 200, 220);
      int[][] map = {
            { 0, 1, 0, 1, 1, 0, 1, 0 },
            { 1, 1, 1, 2, 2, 1, 1, 1 },
            { 0, 1, 2, 3, 3, 2, 1, 0 },
            { 1, 2, 3, 0, 0, 3, 2, 1 },
            { 1, 2, 3, 0, 0, 3, 2, 1 },
            { 0, 1, 2, 3, 3, 2, 1, 0 },
            { 1, 1, 1, 2, 2, 1, 1, 1 },
            { 0, 1, 0, 1, 1, 0, 1, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, main, dark, hi });
   }

   // --- Grabber: Blaue Basis (Eingang), roter/oranger Greiferarm (Ausgang nach RECHTS) ---
   // Palette: 0=transparent, 1=blue body, 2=dark blue, 3=red/orange tip
   private BufferedImage generateGrabberTexture() {
      Color bg   = new Color(0, 0, 0, 0);
      Color body = new Color(60, 120, 210);   // blue input side
      Color dark = new Color(40, 80, 160);    // dark blue shading
      Color tip  = new Color(230, 80, 30);    // red-orange output arm/tip
      // Body on left (input), arm extends right (output). Base direction = RIGHT.
      int[][] map = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 1, 1, 1, 3, 0, 0, 0 },
            { 0, 1, 2, 1, 3, 3, 3, 3 },
            { 0, 1, 2, 1, 3, 3, 3, 3 },
            { 0, 1, 1, 1, 3, 0, 0, 0 },
            { 0, 2, 1, 1, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, body, dark, tip });
   }

   // --- Miner: Nach RECHTS orientiert (Spitzhacke zeigt rechts) ---
   private BufferedImage generateMinerDirectional() {
      Color bg = new Color(0, 0, 0, 0);
      Color body = new Color(220, 140, 30);
      Color dark = new Color(170, 100, 20);
      Color pick = new Color(180, 180, 190);
      int[][] map = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 1, 1, 0, 3, 3, 0 },
            { 0, 1, 2, 1, 1, 0, 3, 0 },
            { 0, 1, 1, 2, 1, 1, 3, 0 },
            { 0, 1, 1, 2, 1, 1, 3, 0 },
            { 0, 1, 2, 1, 1, 0, 3, 0 },
            { 0, 0, 1, 1, 0, 3, 3, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, body, dark, pick });
   }

   // --- Smelter: Nach RECHTS orientiert (Feueröffnung rechts) ---
   private BufferedImage generateSmelterDirectional() {
      Color bg = new Color(0, 0, 0, 0);
      Color body = new Color(160, 50, 40);
      Color dark = new Color(120, 30, 25);
      Color fire = new Color(255, 180, 30);
      int[][] map = {
            { 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 2, 1, 1, 1, 2, 0, 0 },
            { 0, 1, 1, 1, 1, 1, 0, 0 },
            { 0, 1, 2, 2, 1, 1, 3, 3 },
            { 0, 1, 2, 2, 1, 1, 3, 3 },
            { 0, 1, 1, 1, 1, 1, 0, 0 },
            { 0, 2, 1, 1, 1, 2, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, body, dark, fire });
   }

   // --- Forge: Nach RECHTS orientiert (Auslass vorne/rechts) ---
   private BufferedImage generateForgeIdleDirectional() {
      Color bg = new Color(0, 0, 0, 0);
      Color body = new Color(105, 85, 70);
      Color dark = new Color(70, 55, 45);
      Color glow = new Color(150, 60, 30);
      Color chimney = new Color(95, 95, 95);

      int[][] map = {
            { 0, 0, 4, 0, 0, 0, 0, 0 },
            { 0, 2, 1, 1, 1, 2, 0, 0 },
            { 0, 1, 1, 1, 1, 1, 0, 0 },
            { 0, 1, 2, 2, 1, 1, 3, 3 },
            { 0, 1, 2, 2, 1, 1, 3, 3 },
            { 0, 1, 1, 1, 1, 1, 0, 0 },
            { 0, 2, 1, 1, 1, 2, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 0, 0 },
      };
      return buildFromPalette(map, new Color[] { bg, body, dark, glow, chimney });
   }

   // ==================== Rotation ====================

   /**
    * Gibt eine rotierte Version einer Textur zurück, basierend auf der Direction.
    * Standard-Richtung (RIGHT) gibt das Original zurück.
    * Rotierte Versionen werden gecached.
    */
   public BufferedImage getRotated(String key, Direction direction) {
      if (direction == null || direction == Direction.RIGHT) {
         return cache.get(key);
      }
      String rotKey = key + "_" + direction.name();
      BufferedImage cached = cache.get(rotKey);
      if (cached != null) {
         return cached;
      }
      BufferedImage src = cache.get(key);
      if (src == null) {
         return null;
      }
      BufferedImage rotated = rotateImage(src, direction);
      cache.put(rotKey, rotated);
      return rotated;
   }

   /**
    * Rotiert ein BufferedImage basierend auf der Direction.
    * Verwendet Nearest-Neighbor für scharfe Pixel-Art.
    */
   private BufferedImage rotateImage(BufferedImage src, Direction direction) {
      int w = src.getWidth();
      int h = src.getHeight();
      BufferedImage rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = rotated.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.rotate(direction.getRotationAngle(), w / 2.0, h / 2.0);
      g.drawImage(src, 0, 0, null);
      g.dispose();
      return rotated;
   }

   // ==================== Helpers ====================

   /**
    * Baut ein 8x8 BufferedImage aus einer Palettenindex-Map. Skaliert auf
    * tileSize.
    */
   private BufferedImage buildFromPalette(int[][] pixelMap, Color[] palette) {
      BufferedImage small = new BufferedImage(PX, PX, BufferedImage.TYPE_INT_ARGB);
      for (int y = 0; y < PX; y++) {
         for (int x = 0; x < PX; x++) {
            small.setRGB(x, y, palette[pixelMap[y][x]].getRGB());
         }
      }
      return scaleNearest(small);
   }

   /** Überladung mit separatem Farbindex-Array (für Grass mit 3 Farben). */
   private BufferedImage buildTexture(int[][] unused, Color[] palette, int[][] colorMap) {
      return buildFromPalette(colorMap, palette);
   }

   /** Skaliert nearest-neighbor (kein Weichzeichner) auf TILE_SIZE. */
   private BufferedImage scaleNearest(BufferedImage src) {
      return scaleNearest(src, tileSize, tileSize);
   }

   private BufferedImage scaleNearest(BufferedImage src, int width, int height) {
      BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = scaled.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.drawImage(src, 0, 0, width, height, null);
      g.dispose();
      return scaled;
   }

   private Color brighter(Color c, int amount) {
      return new Color(
            Math.min(255, c.getRed() + amount),
            Math.min(255, c.getGreen() + amount),
            Math.min(255, c.getBlue() + amount));
   }
}
