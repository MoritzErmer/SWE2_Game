package game.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import game.machine.Direction;

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
      cache.put("machine_bg", generateMachineBg());
      cache.put("miner", generateMinerDirectional());
      cache.put("smelter", generateSmelterDirectional());
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
      cache.put("grabber", generateGrabberTexture());
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
      BufferedImage scaled = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = scaled.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.drawImage(src, 0, 0, tileSize, tileSize, null);
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
