package game.ui;

import game.machine.Direction;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates isometric pixel-art textures for all tile types, machines, items,
 * and the player.
 *
 * Texture sizes:
 *   - Ground tiles  : ISO_TILE_W x ISO_TILE_H  (64 x 32) via buildIsoTile()
 *   - Machine boxes : ISO_TILE_W x ISO_BOX_SPRITE_H (64 x 48) via buildIsoBox()
 *   - Item icons    : 32 x 32 flat squares via scaleNearest()
 *
 * All textures are rendered from 16x16 palette-indexed source maps and scaled
 * with nearest-neighbor interpolation to preserve the pixel-art look.
 *
 * Base direction for all directional sprites is RIGHT (0 deg rotation).
 * getRotated() applies Direction.getRotationAngle() and caches results.
 */
public class PixelTextures {

    /** Source resolution of every pixel-art map (16x16). */
    private static final int PX = 16;

    /** Cache keyed by texture name (and "name_DIRECTION" for rotations). */
    private final Map<String, BufferedImage> cache = new HashMap<>();

    public PixelTextures() {
        generateAll();
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** Direct cache lookup — returns null if the key is unknown. */
    public BufferedImage get(String key) {
        return cache.get(key);
    }

    /**
     * Returns a version of the texture rotated to match the given direction.
     * RIGHT = no rotation (returns the base sprite). Results are cached.
     * Uses nearest-neighbor interpolation for a sharp pixel-art look.
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

    // -----------------------------------------------------------------------
    // Texture generation — called once at construction
    // -----------------------------------------------------------------------

    private void generateAll() {
        // --- Ground tiles (64 x 32) ---
        cache.put("grass",          buildGrass());
        cache.put("iron_deposit",   buildIronDeposit());
        cache.put("copper_deposit", buildCopperDeposit());
        cache.put("coal_deposit",   buildCoalDeposit());
        cache.put("stone_floor",    buildStoneFloor());

        // Conveyor belt animation frames (base direction = RIGHT)
        for (int f = 0; f < 4; f++) {
            cache.put("conveyor_belt_frame" + f, buildConveyorFrame(f));
        }

        // --- Machine box sprites (64 x 48) ---
        cache.put("machine_bg", buildMachineBg());
        cache.put("miner",      buildMiner());

        // Smelter frames (fire brightness varies)
        for (int f = 0; f < 3; f++) {
            cache.put("smelter_frame" + f, buildSmelterFrame(f));
        }

        // Grabber frames (arm extension)
        for (int f = 0; f < 4; f++) {
            cache.put("grabber_frame" + f, buildGrabberFrame(f));
        }

        // --- Player (64 x 48) ---
        cache.put("player", buildPlayer());

        // --- Item icons (32 x 32) ---
        cache.put("iron_ore",         buildItemIcon(new Color(0xB0, 0x40, 0x20), "nugget"));
        cache.put("copper_ore",       buildItemIcon(new Color(0xCC, 0x77, 0x33), "nugget"));
        cache.put("coal",             buildItemIcon(new Color(0x33, 0x33, 0x33), "nugget"));
        cache.put("iron_plate",       buildItemIcon(new Color(0xAA, 0xAA, 0xAA), "plate"));
        cache.put("copper_plate",     buildItemIcon(new Color(0xCC, 0x88, 0x44), "plate"));
        cache.put("gear",             buildGearIcon());
        cache.put("circuit",          buildCircuitIcon());
        cache.put("construction_kit", buildConstructionKitIcon());
    }

    // -----------------------------------------------------------------------
    // Ground tile builders
    // -----------------------------------------------------------------------

    private BufferedImage buildGrass() {
        // Palette: 0=medium green base, 1=dark green patch, 2=light green highlight
        Color[] pal = {
            new Color(0x4a, 0x8c, 0x3f),
            new Color(0x35, 0x6b, 0x2e),
            new Color(0x60, 0xa8, 0x50)
        };
        int[][] map = {
            { 0,0,1,0,0,2,0,0,1,0,0,0,2,0,0,0 },
            { 0,2,0,0,1,0,0,0,0,1,0,2,0,0,1,0 },
            { 1,0,0,2,0,0,0,1,0,0,0,0,0,1,0,0 },
            { 0,0,1,0,0,0,2,0,0,0,1,0,0,0,0,1 },
            { 0,2,0,0,0,1,0,0,2,0,0,0,1,0,2,0 },
            { 1,0,0,1,0,0,0,0,0,1,0,0,0,0,0,1 },
            { 0,0,2,0,0,2,0,1,0,0,0,2,0,1,0,0 },
            { 0,1,0,0,1,0,0,0,0,2,1,0,0,0,0,2 },
            { 0,0,0,2,0,0,1,0,0,0,0,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,2,0,0,1,0,0,2,0 },
            { 0,0,0,0,1,0,2,0,0,0,1,0,0,0,0,1 },
            { 0,2,0,1,0,0,0,0,1,0,0,0,2,0,1,0 },
            { 1,0,0,0,0,1,0,2,0,0,0,1,0,0,0,0 },
            { 0,0,1,0,2,0,0,0,0,1,2,0,0,1,0,0 },
            { 0,2,0,0,0,0,1,0,2,0,0,0,0,0,2,1 },
            { 0,0,0,1,0,2,0,0,0,0,1,0,2,0,0,0 }
        };
        return buildIsoTile(map, pal);
    }

    private BufferedImage buildIronDeposit() {
        // Palette: 0=gray-green base, 1=medium gray, 2=rust-orange fleck, 3=light rust
        Color[] pal = {
            new Color(0x7a, 0x8c, 0x6e),
            new Color(0x90, 0x9e, 0x82),
            new Color(0xB0, 0x70, 0x38),
            new Color(0xC8, 0x88, 0x50)
        };
        int[][] map = {
            { 0,0,1,0,0,0,1,0,0,1,0,0,0,1,0,0 },
            { 0,1,0,0,2,3,0,0,1,0,0,2,0,0,0,1 },
            { 1,0,0,2,3,2,0,1,0,0,0,0,2,3,0,0 },
            { 0,0,1,3,2,0,0,0,1,0,2,3,2,0,1,0 },
            { 0,2,0,0,0,0,1,0,0,2,3,2,0,0,0,1 },
            { 1,3,2,0,0,1,0,0,0,1,2,0,0,1,0,0 },
            { 0,2,0,0,1,0,2,3,0,0,0,0,1,0,2,0 },
            { 0,0,0,1,0,0,3,2,0,1,0,0,0,2,3,0 },
            { 1,0,2,0,0,0,0,0,1,0,0,2,3,2,0,1 },
            { 0,0,3,2,0,1,0,0,0,0,1,3,2,0,0,0 },
            { 0,1,2,0,0,0,1,0,2,0,0,2,0,0,1,0 },
            { 1,0,0,0,2,3,0,1,0,0,0,0,0,2,0,1 },
            { 0,0,1,0,3,2,0,0,1,0,2,0,0,3,0,0 },
            { 0,1,0,0,0,0,0,1,0,0,3,2,0,0,1,0 },
            { 0,0,0,1,0,2,0,0,0,1,0,0,0,1,0,0 },
            { 1,0,0,0,1,0,0,0,1,0,0,1,0,0,0,0 }
        };
        return buildIsoTile(map, pal);
    }

    private BufferedImage buildCopperDeposit() {
        // Palette: 0=brown-orange base, 1=medium brown, 2=copper highlight, 3=dark shadow
        Color[] pal = {
            new Color(0x8c, 0x64, 0x40),
            new Color(0xA0, 0x78, 0x55),
            new Color(0xCC, 0x99, 0x55),
            new Color(0x60, 0x44, 0x28)
        };
        int[][] map = {
            { 0,0,1,0,0,1,0,0,0,1,0,0,1,0,0,0 },
            { 0,1,2,1,0,0,0,1,0,0,0,1,2,1,0,0 },
            { 1,2,2,1,0,0,1,0,0,0,1,2,2,0,1,0 },
            { 0,1,0,0,0,1,2,2,1,0,0,1,0,0,0,1 },
            { 0,0,0,1,0,2,2,1,0,0,1,0,0,1,2,0 },
            { 1,0,1,2,1,1,0,0,0,1,2,1,0,2,1,0 },
            { 0,0,2,2,0,0,1,0,1,2,2,0,0,1,0,1 },
            { 0,1,1,0,0,0,2,2,2,1,0,0,1,0,0,0 },
            { 1,2,0,0,1,0,1,2,0,0,0,1,2,0,1,0 },
            { 0,1,0,1,2,1,0,0,0,1,0,2,1,0,0,1 },
            { 0,0,1,2,2,0,0,1,0,2,2,1,0,0,1,0 },
            { 1,0,0,1,0,0,1,2,1,1,0,0,0,1,0,1 },
            { 0,1,0,0,0,1,2,1,0,0,1,0,1,2,1,0 },
            { 1,0,0,1,0,0,1,0,0,1,2,2,1,0,0,0 },
            { 0,0,1,2,1,0,0,0,1,1,2,0,0,0,1,0 },
            { 0,1,0,1,0,0,1,0,0,0,0,0,1,0,0,0 }
        };
        return buildIsoTile(map, pal);
    }

    private BufferedImage buildCoalDeposit() {
        // Palette: 0=very dark gray base, 1=slightly darker patch, 2=medium dark, 3=faint glint
        Color[] pal = {
            new Color(0x2a, 0x2a, 0x2a),
            new Color(0x1a, 0x1a, 0x1a),
            new Color(0x3a, 0x3a, 0x3a),
            new Color(0x50, 0x50, 0x55)
        };
        int[][] map = {
            { 0,1,0,0,1,0,0,1,0,0,0,1,0,0,1,0 },
            { 1,2,2,1,0,0,1,2,1,0,1,2,2,1,0,0 },
            { 0,2,3,2,0,1,2,3,2,0,2,3,2,0,1,0 },
            { 0,1,2,1,0,2,3,2,1,0,1,2,1,0,2,1 },
            { 1,0,0,0,1,1,2,1,0,1,0,0,0,1,2,2 },
            { 0,0,1,2,2,0,1,0,0,2,2,1,0,0,1,2 },
            { 0,1,2,3,2,0,0,1,0,3,2,0,1,2,3,2 },
            { 1,0,1,2,1,0,1,2,1,2,1,0,1,3,2,1 },
            { 0,0,0,1,0,1,2,3,2,1,0,0,0,2,1,0 },
            { 0,1,0,0,2,2,3,2,1,0,0,1,0,1,0,0 },
            { 1,2,1,0,1,2,1,0,0,0,1,2,1,0,0,1 },
            { 0,2,2,0,0,1,0,0,1,0,2,3,2,0,1,0 },
            { 0,1,0,0,1,2,2,1,2,0,1,2,0,0,0,1 },
            { 1,0,0,1,2,3,2,0,1,0,0,1,0,1,0,0 },
            { 0,0,1,0,1,2,1,0,0,1,0,0,1,2,1,0 },
            { 0,1,0,0,0,1,0,1,0,0,0,1,0,1,0,0 }
        };
        return buildIsoTile(map, pal);
    }

    private BufferedImage buildStoneFloor() {
        // Palette: 0=medium gray, 1=slightly darker, 2=slightly lighter, 3=joint line
        Color[] pal = {
            new Color(0x88, 0x88, 0x88),
            new Color(0x70, 0x70, 0x70),
            new Color(0xA0, 0xA0, 0xA0),
            new Color(0x55, 0x55, 0x55)
        };
        int[][] map = {
            { 3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3 },
            { 3,0,0,0,0,0,0,3,0,0,0,0,0,0,0,3 },
            { 3,0,2,0,0,0,0,3,0,2,0,0,0,0,0,3 },
            { 3,0,0,0,1,0,0,3,0,0,0,1,0,0,0,3 },
            { 3,0,0,0,0,0,1,3,0,0,0,0,0,1,0,3 },
            { 3,0,0,0,0,0,0,3,0,0,0,0,0,0,0,3 },
            { 3,0,0,1,0,0,0,3,0,0,1,0,0,0,0,3 },
            { 3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3 },
            { 3,0,0,0,0,0,0,3,0,0,0,0,0,0,0,3 },
            { 3,0,2,0,0,0,0,3,0,2,0,0,0,0,0,3 },
            { 3,0,0,0,0,1,0,3,0,0,0,0,1,0,0,3 },
            { 3,0,1,0,0,0,0,3,0,1,0,0,0,0,0,3 },
            { 3,0,0,0,0,0,1,3,0,0,0,0,0,1,0,3 },
            { 3,0,0,0,0,0,0,3,0,0,0,0,0,0,0,3 },
            { 3,0,0,1,0,0,0,3,0,0,1,0,0,0,0,3 },
            { 3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3 }
        };
        return buildIsoTile(map, pal);
    }

    /**
     * Conveyor belt frame (base direction = RIGHT).
     * frame 0-3: white arrow/stripe shifts 4 pixels rightward each frame to
     * simulate belt movement.
     */
    private BufferedImage buildConveyorFrame(int frame) {
        // Palette: 0=dark gray bg, 1=gray rail, 2=white arrow stripe, 3=medium gray
        Color[] pal = {
            new Color(0x44, 0x44, 0x44),
            new Color(0x88, 0x88, 0x88),
            new Color(0xFF, 0xFF, 0xFF),
            new Color(0x66, 0x66, 0x66)
        };
        // Arrow tip column moves with frame: columns 4,5,6,7 for frames 0,1,2,3
        // Rail runs along rows 0,1 and rows 14,15 (top and bottom edges)
        // Arrow is a right-pointing chevron shifted by frame*4/16 of the tile width
        int arrowOffset = frame * 4; // 0,4,8,12 in 16-pixel space
        int[][] map = new int[16][16];

        // Fill base color
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                map[y][x] = 0;
            }
        }
        // Top and bottom rail (rows 0-1 and 14-15)
        for (int x = 0; x < 16; x++) {
            map[0][x]  = 1;
            map[1][x]  = 1;
            map[14][x] = 1;
            map[15][x] = 1;
        }
        // Arrow pointing RIGHT — chevron stripes shifted by arrowOffset
        // Draw two stripes of the arrow at wrapping positions
        for (int stripe = 0; stripe < 2; stripe++) {
            // Each stripe is centered at x = arrowOffset + stripe*8, width 4
            int cx = (arrowOffset + stripe * 8) % 16;
            for (int row = 2; row <= 13; row++) {
                // Row distance from center (rows 2..13 = 12 rows, center at row 7.5)
                int dist = Math.abs(row - 7); // 0..5
                // The chevron tip shifts left by dist (pointing right)
                int tipX = (cx + dist) % 16;
                int tipX2 = (cx + dist + 1) % 16;
                map[row][tipX]  = 2;
                map[row][tipX2] = 2;
            }
        }
        return buildIsoTile(map, pal);
    }

    // -----------------------------------------------------------------------
    // Machine box builders
    // -----------------------------------------------------------------------

    private BufferedImage buildMachineBg() {
        // Neutral gray box — top and front both use the same palette
        Color[] topPal = {
            new Color(0x66, 0x66, 0x66),
            new Color(0x80, 0x80, 0x80),
            new Color(0x50, 0x50, 0x50),
            new Color(0x40, 0x40, 0x40)
        };
        int[][] topMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,3,0,0,0,0,0,0,0,0,3,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,3,0,0,0,0,0,0,0,0,3,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };
        Color[] frontPal = {
            new Color(0x55, 0x55, 0x55),
            new Color(0x6e, 0x6e, 0x6e),
            new Color(0x44, 0x44, 0x44),
            new Color(0x30, 0x30, 0x30)
        };
        int[][] frontMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,3,0,0,0,0,0,0,0,0,3,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,0,3,0,0,0,0,0,0,0,0,3,0,1,2 },
            { 2,1,0,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };
        return buildIsoBox(topMap, frontMap, topPal, frontPal);
    }

    private BufferedImage buildMiner() {
        // Top face: orange-brown box
        // Palette: 0=orange-brown, 1=lighter orange, 2=dark brown shadow, 3=pickaxe silver
        Color[] topPal = {
            new Color(0xDC, 0x8C, 0x1E),
            new Color(0xF0, 0xA8, 0x38),
            new Color(0x9A, 0x60, 0x10),
            new Color(0xC0, 0xC0, 0xC8)
        };
        int[][] topMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,0,1,1,1,1,1,1,1,1,1,1,1,1,0,2 },
            { 2,0,1,3,3,0,0,0,0,0,0,0,3,3,1,2 },
            { 2,0,1,3,0,0,0,0,0,0,0,0,0,3,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,3,3,3,3,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,3,0,0,3,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,3,3,3,3,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,3,0,0,0,0,0,0,0,0,0,3,1,2 },
            { 2,0,1,3,3,0,0,0,0,0,0,0,3,3,1,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };
        // Front face: darker brown, pickaxe symbol, opening on RIGHT side (columns 13-15)
        Color[] frontPal = {
            new Color(0x9A, 0x60, 0x10),
            new Color(0xC0, 0x80, 0x18),
            new Color(0x60, 0x3C, 0x08),
            new Color(0xC0, 0xC0, 0xC8)
        };
        int[][] frontMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,0,1,1,1,1,1,1,1,1,1,1,1,0,0,0 }, // opening right
            { 2,0,1,0,0,3,0,0,0,0,3,3,1,0,0,0 }, // pickaxe
            { 2,0,1,0,3,3,3,0,0,3,3,0,1,0,0,0 },
            { 2,0,1,0,0,3,0,0,0,0,3,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,1,0,0,0 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };
        return buildIsoBox(topMap, frontMap, topPal, frontPal);
    }

    /**
     * Smelter animation frame.
     * frame 0: dim fire (#FF8000), frame 1: medium (#FFA030), frame 2: bright (#FFD060)
     */
    private BufferedImage buildSmelterFrame(int frame) {
        Color fireColor;
        switch (frame) {
            case 0:  fireColor = new Color(0xFF, 0x80, 0x00); break;
            case 1:  fireColor = new Color(0xFF, 0xA0, 0x30); break;
            default: fireColor = new Color(0xFF, 0xD0, 0x60); break;
        }

        // Top palette: red-brick body
        Color[] topPal = {
            new Color(0xA0, 0x32, 0x28),
            new Color(0xC0, 0x48, 0x38),
            new Color(0x70, 0x22, 0x18),
            new Color(0xD8, 0x80, 0x50)
        };
        int[][] topMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,0,1,1,1,1,1,1,1,1,1,1,1,1,0,2 },
            { 2,0,1,3,3,0,0,0,0,0,0,0,3,3,1,2 },
            { 2,0,1,3,0,0,0,0,0,0,0,0,0,3,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,3,0,0,0,0,0,0,0,0,0,3,1,2 },
            { 2,0,1,3,3,0,0,0,0,0,0,0,3,3,1,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };

        // Front palette: dark brick with fire color at index 3
        Color[] frontPal = {
            new Color(0x78, 0x24, 0x1C),
            new Color(0x98, 0x38, 0x2C),
            new Color(0x50, 0x18, 0x10),
            fireColor
        };
        // Fire opening on the RIGHT side (columns 10-15)
        int[][] frontMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,0,1,1,1,1,1,1,1,1,0,0,0,0,0,2 }, // opening right
            { 2,0,1,0,0,0,0,0,1,3,3,3,0,0,0,2 },
            { 2,0,1,0,0,0,0,0,1,3,3,3,3,3,0,2 },
            { 2,0,1,0,0,0,0,0,1,3,3,3,3,3,3,2 },
            { 2,0,1,0,0,0,0,0,1,3,3,3,3,3,3,2 },
            { 2,0,1,0,0,0,0,0,1,3,3,3,3,3,3,2 },
            { 2,0,1,0,0,0,0,0,1,3,3,3,3,3,3,2 },
            { 2,0,1,0,0,0,0,0,1,3,3,3,3,3,3,2 },
            { 2,0,1,0,0,0,0,0,1,3,3,3,3,3,0,2 },
            { 2,0,1,0,0,0,0,0,1,3,3,3,0,0,0,2 },
            { 2,0,1,0,0,0,0,0,1,3,0,0,0,0,0,2 },
            { 2,0,1,1,1,1,1,1,1,0,0,0,0,0,0,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };
        return buildIsoBox(topMap, frontMap, topPal, frontPal);
    }

    /**
     * Grabber animation frame.
     * frame 0: arm retracted, frame 1: 33% extended, frame 2: 66%, frame 3: fully extended.
     * Arm extends toward RIGHT (base direction).
     */
    private BufferedImage buildGrabberFrame(int frame) {
        // Top palette: blue body
        Color[] topPal = {
            new Color(0x3C, 0x78, 0xD2),
            new Color(0x58, 0x98, 0xF0),
            new Color(0x28, 0x55, 0x9A),
            new Color(0xA0, 0xC8, 0xFF)
        };
        int[][] topMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,0,1,1,1,1,1,1,1,1,1,1,1,1,0,2 },
            { 2,0,1,3,3,0,0,0,0,0,0,0,3,3,1,2 },
            { 2,0,1,3,0,0,0,0,0,0,0,0,0,3,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,3,3,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,3,3,3,3,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,3,3,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,0,0,0,0,0,0,0,0,0,0,0,1,2 },
            { 2,0,1,3,0,0,0,0,0,0,0,0,0,3,1,2 },
            { 2,0,1,3,3,0,0,0,0,0,0,0,3,3,1,2 },
            { 2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };

        // Front face: blue body, arm tip extends rightward based on frame
        // frame 0: arm ends at col 8; frame 3: arm extends to col 15
        Color[] frontPal = {
            new Color(0x28, 0x55, 0x9A),
            new Color(0x3C, 0x78, 0xD2),
            new Color(0x1C, 0x3C, 0x70),
            new Color(0xFF, 0xCC, 0x44)   // yellow arm tip
        };
        // Arm tip rightmost column: frame 0=8, 1=10, 2=12, 3=15
        int armEnd = 8 + frame * (7 / 3 + (frame == 3 ? 1 : 0));
        // Simpler: fixed mapping
        int[] armEnds = {8, 10, 12, 15};
        armEnd = armEnds[frame];

        int[][] frontMap = new int[16][16];
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                frontMap[y][x] = 0; // blue body base
            }
        }
        // Border
        for (int x = 0; x < 16; x++) { frontMap[0][x] = 2; frontMap[15][x] = 2; }
        for (int y = 0; y < 16; y++) { frontMap[y][0] = 2; frontMap[y][15] = 2; }
        // Body interior border
        for (int x = 1; x < 15; x++) { frontMap[1][x] = 0; frontMap[14][x] = 0; }
        for (int y = 1; y < 15; y++) { frontMap[y][1] = 0; frontMap[y][14] = 1; }
        // Arm: middle 4 rows (rows 6-9), from col 2 to armEnd
        for (int y = 6; y <= 9; y++) {
            for (int x = 2; x <= armEnd; x++) {
                frontMap[y][x] = 1;
            }
        }
        // Arm tip highlight
        for (int y = 6; y <= 9; y++) {
            if (armEnd < 15) frontMap[y][armEnd] = 3;
        }

        return buildIsoBox(topMap, frontMap, topPal, frontPal);
    }

    private BufferedImage buildPlayer() {
        // Top: blue torso
        Color[] topPal = {
            new Color(0x22, 0x44, 0xCC),
            new Color(0x3A, 0x60, 0xE8),
            new Color(0x14, 0x2C, 0x90),
            new Color(0xAA, 0xBB, 0xFF)   // lighter head color
        };
        int[][] topMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,3,3,3,3,2,2,2,2,2,2 }, // head
            { 2,2,2,2,2,2,3,3,3,3,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,3,3,3,3,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,3,3,3,3,2,2,2,2,2,2 },
            { 2,2,2,2,1,1,1,1,1,1,1,1,2,2,2,2 }, // torso
            { 2,2,2,1,0,0,0,0,0,0,0,0,1,2,2,2 },
            { 2,2,2,1,0,0,0,0,0,0,0,0,1,2,2,2 },
            { 2,2,2,1,0,0,0,0,0,0,0,0,1,2,2,2 },
            { 2,2,2,1,0,0,0,0,0,0,0,0,1,2,2,2 },
            { 2,2,2,2,1,1,1,1,1,1,1,1,2,2,2,2 },
            { 2,2,2,2,2,0,0,2,2,0,0,2,2,2,2,2 }, // legs
            { 2,2,2,2,2,0,0,2,2,0,0,2,2,2,2,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };
        Color[] frontPal = {
            new Color(0x22, 0x44, 0xCC),
            new Color(0x3A, 0x60, 0xE8),
            new Color(0x14, 0x2C, 0x90),
            new Color(0xAA, 0xBB, 0xFF)
        };
        int[][] frontMap = {
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,3,3,3,3,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,3,3,3,3,2,2,2,2,2,2 },
            { 2,2,2,2,1,1,1,1,1,1,1,1,2,2,2,2 },
            { 2,2,2,1,0,0,0,0,0,0,0,0,1,2,2,2 },
            { 2,2,2,1,0,0,0,0,0,0,0,0,1,2,2,2 },
            { 2,2,2,1,0,0,0,0,0,0,0,0,1,2,2,2 },
            { 2,2,2,2,1,1,1,1,1,1,1,1,2,2,2,2 },
            { 2,2,2,2,2,0,0,2,2,0,0,2,2,2,2,2 },
            { 2,2,2,2,2,0,0,2,2,0,0,2,2,2,2,2 },
            { 2,2,2,2,2,0,0,2,2,0,0,2,2,2,2,2 },
            { 2,2,2,2,2,0,0,2,2,0,0,2,2,2,2,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 },
            { 2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2 }
        };
        return buildIsoBox(topMap, frontMap, topPal, frontPal);
    }

    // -----------------------------------------------------------------------
    // Item icon builders (32x32 flat squares)
    // -----------------------------------------------------------------------

    /** Builds a 32x32 item icon from a 16x16 map. */
    private BufferedImage buildItemIcon(Color baseColor, String shape) {
        Color bg   = new Color(0, 0, 0, 0);
        Color main = baseColor;
        Color hi   = darken(baseColor, 1.35f);  // brighter highlight
        Color shad = darken(baseColor, 0.65f);  // shadow

        int[][] map;
        if ("plate".equals(shape)) {
            map = new int[][] {
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
                { 0,0,2,2,2,2,2,2,2,2,2,2,2,2,0,0 },
                { 0,0,2,3,3,3,3,3,3,3,3,3,3,2,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,3,2,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,3,2,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,3,2,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,3,2,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,3,2,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,3,2,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,3,2,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,3,2,0,0 },
                { 0,0,2,3,3,3,3,3,3,3,3,3,3,2,0,0 },
                { 0,0,2,2,2,2,2,2,2,2,2,2,2,2,0,0 },
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 }
            };
        } else { // nugget
            map = new int[][] {
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
                { 0,0,0,0,2,2,2,2,2,2,0,0,0,0,0,0 },
                { 0,0,0,2,3,3,1,1,2,1,2,0,0,0,0,0 },
                { 0,0,2,3,3,1,1,1,1,1,1,2,0,0,0,0 },
                { 0,0,2,3,1,1,1,1,1,1,1,1,2,0,0,0 },
                { 0,0,0,2,1,1,1,1,1,1,1,1,2,0,0,0 },
                { 0,0,0,2,1,1,1,1,1,2,1,1,2,0,0,0 },
                { 0,0,0,0,2,1,1,1,1,2,2,1,2,0,0,0 },
                { 0,0,0,0,2,1,1,1,2,2,0,2,0,0,0,0 },
                { 0,0,0,0,0,2,1,1,2,0,0,0,0,0,0,0 },
                { 0,0,0,0,0,0,2,2,0,0,0,0,0,0,0,0 },
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
                { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 }
            };
        }
        // Build 16x16 then scale to 32x32
        BufferedImage small = new BufferedImage(PX, PX, BufferedImage.TYPE_INT_ARGB);
        Color[] pal = { bg, main, shad, hi };
        for (int y = 0; y < PX; y++) {
            for (int x = 0; x < PX; x++) {
                small.setRGB(x, y, pal[map[y][x]].getRGB());
            }
        }
        return scaleNearest(small, 32, 32);
    }

    private BufferedImage buildGearIcon() {
        Color bg   = new Color(0, 0, 0, 0);
        Color main = new Color(0x88, 0x88, 0x88);
        Color dark = new Color(0x60, 0x60, 0x60);
        Color hi   = new Color(0xB0, 0xB0, 0xB8);
        int[][] map = {
            { 0,0,0,2,0,2,2,2,2,0,2,0,0,0,0,0 },
            { 0,0,2,2,2,3,3,1,1,3,2,2,2,0,0,0 },
            { 0,2,2,3,3,3,1,1,1,1,3,3,2,2,0,0 },
            { 2,2,3,3,1,0,0,0,0,0,0,1,3,3,2,0 },
            { 0,2,3,1,0,0,0,0,0,0,0,0,1,3,2,0 },
            { 2,3,3,0,0,0,0,0,0,0,0,0,0,3,3,2 },
            { 2,1,1,0,0,0,0,0,0,0,0,0,0,1,1,2 },
            { 2,1,1,0,0,0,0,0,0,0,0,0,0,1,1,2 },
            { 2,1,1,0,0,0,0,0,0,0,0,0,0,1,1,2 },
            { 2,3,3,0,0,0,0,0,0,0,0,0,0,3,3,2 },
            { 0,2,3,1,0,0,0,0,0,0,0,0,1,3,2,0 },
            { 2,2,3,3,1,0,0,0,0,0,0,1,3,3,2,0 },
            { 0,2,2,3,3,3,1,1,1,1,3,3,2,2,0,0 },
            { 0,0,2,2,2,3,3,1,1,3,2,2,2,0,0,0 },
            { 0,0,0,2,0,2,2,2,2,0,2,0,0,0,0,0 },
            { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 }
        };
        BufferedImage small = new BufferedImage(PX, PX, BufferedImage.TYPE_INT_ARGB);
        Color[] pal = { bg, main, dark, hi };
        for (int y = 0; y < PX; y++) {
            for (int x = 0; x < PX; x++) {
                small.setRGB(x, y, pal[map[y][x]].getRGB());
            }
        }
        return scaleNearest(small, 32, 32);
    }

    private BufferedImage buildCircuitIcon() {
        Color bg    = new Color(0, 0, 0, 0);
        Color board = new Color(0x22, 0x88, 0x44);
        Color dark  = new Color(0x14, 0x55, 0x28);
        Color trace = new Color(0xDD, 0xCC, 0x44);
        int[][] map = {
            { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
            { 0,2,2,2,2,2,2,2,2,2,2,2,2,2,2,0 },
            { 0,2,1,1,1,1,1,1,1,1,1,1,1,1,2,0 },
            { 0,2,1,3,3,3,1,1,1,3,3,3,1,1,2,0 },
            { 0,2,1,3,0,3,1,1,1,3,0,3,1,1,2,0 },
            { 0,2,1,3,3,3,1,1,1,3,3,3,1,1,2,0 },
            { 0,2,1,1,1,1,3,3,3,1,1,1,1,1,2,0 },
            { 0,2,1,1,1,1,3,3,3,1,1,1,1,1,2,0 },
            { 0,2,1,3,3,3,1,1,1,3,3,3,1,1,2,0 },
            { 0,2,1,3,0,3,1,1,1,3,0,3,1,1,2,0 },
            { 0,2,1,3,3,3,1,1,1,3,3,3,1,1,2,0 },
            { 0,2,1,1,1,1,1,1,1,1,1,1,1,1,2,0 },
            { 0,2,1,3,3,3,3,3,3,3,3,3,1,1,2,0 },
            { 0,2,1,1,1,1,1,1,1,1,1,1,1,1,2,0 },
            { 0,2,2,2,2,2,2,2,2,2,2,2,2,2,2,0 },
            { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 }
        };
        BufferedImage small = new BufferedImage(PX, PX, BufferedImage.TYPE_INT_ARGB);
        Color[] pal = { bg, board, dark, trace };
        for (int y = 0; y < PX; y++) {
            for (int x = 0; x < PX; x++) {
                small.setRGB(x, y, pal[map[y][x]].getRGB());
            }
        }
        return scaleNearest(small, 32, 32);
    }

    private BufferedImage buildConstructionKitIcon() {
        Color bg    = new Color(0, 0, 0, 0);
        Color body  = new Color(0xFF, 0xCC, 0x00);
        Color dark  = new Color(0xCC, 0x99, 0x00);
        Color hi    = new Color(0xFF, 0xEE, 0x88);
        int[][] map = {
            { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
            { 0,0,0,0,2,2,2,2,2,2,2,2,0,0,0,0 },
            { 0,0,0,2,1,1,1,1,1,1,1,1,2,0,0,0 },
            { 0,0,0,2,3,3,3,3,3,3,3,1,2,0,0,0 },
            { 0,0,0,2,3,2,2,2,2,1,1,1,2,0,0,0 },
            { 0,0,0,2,1,2,0,0,2,1,1,1,2,0,0,0 }, // handle cutout
            { 0,0,0,2,1,2,0,0,2,1,1,1,2,0,0,0 },
            { 0,0,0,2,1,2,2,2,2,1,1,1,2,0,0,0 },
            { 0,2,2,2,1,1,1,1,1,1,1,1,2,2,2,0 }, // wider lower box
            { 0,2,1,1,1,1,1,1,1,1,1,1,1,1,2,0 },
            { 0,2,1,3,3,1,3,3,1,3,3,1,1,1,2,0 }, // tool slots
            { 0,2,1,3,3,1,3,3,1,3,3,1,1,1,2,0 },
            { 0,2,1,1,1,1,1,1,1,1,1,1,1,1,2,0 },
            { 0,2,2,2,2,2,2,2,2,2,2,2,2,2,2,0 },
            { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 },
            { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 }
        };
        BufferedImage small = new BufferedImage(PX, PX, BufferedImage.TYPE_INT_ARGB);
        Color[] pal = { bg, body, dark, hi };
        for (int y = 0; y < PX; y++) {
            for (int x = 0; x < PX; x++) {
                small.setRGB(x, y, pal[map[y][x]].getRGB());
            }
        }
        return scaleNearest(small, 32, 32);
    }

    // -----------------------------------------------------------------------
    // Core shape builders
    // -----------------------------------------------------------------------

    /**
     * Builds a 64x32 isometric tile sprite from a 16x16 palette-indexed map.
     *
     * Process:
     *  1. Scale map16 to 64x32 using nearest-neighbor (4x horizontal, 2x vertical).
     *  2. Apply diamond mask: pixels outside the isometric diamond become transparent.
     *
     * Thread safety: pure function, no shared state.
     */
    private BufferedImage buildIsoTile(int[][] map16, Color[] palette) {
        int outW = IsoRenderer.ISO_TILE_W;  // 64
        int outH = IsoRenderer.ISO_TILE_H;  // 32

        // Scale 16x16 -> 64x32 nearest-neighbor
        int[][] scaled = new int[outH][outW];
        for (int py = 0; py < outH; py++) {
            int srcY = py * PX / outH;  // 0..15
            for (int px = 0; px < outW; px++) {
                int srcX = px * PX / outW;  // 0..15
                scaled[py][px] = map16[srcY][srcX];
            }
        }

        // Write to ARGB image and apply diamond mask
        BufferedImage img = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < outH; py++) {
            for (int px = 0; px < outW; px++) {
                // Normalize to [-1, +1]
                double normX = (px - 32.0) / 32.0;
                double normY = (py - 16.0) / 16.0;
                if (Math.abs(normX) + Math.abs(normY) > 1.0) {
                    // Outside diamond — fully transparent
                    img.setRGB(px, py, 0x00000000);
                } else {
                    Color c = palette[scaled[py][px]];
                    img.setRGB(px, py, c.getRGB());
                }
            }
        }
        return img;
    }

    /**
     * Builds a 64x48 isometric box sprite.
     *
     * Layout:
     *   y = 0..31  : top face — drawn via buildIsoTile(topMap16, topPalette)
     *   y = 32..47 : front face — frontMap16 scaled to 64x16, with 75% brightness shadow
     *
     * Thread safety: pure function, no shared state.
     */
    private BufferedImage buildIsoBox(int[][] topMap16, int[][] frontMap16,
                                      Color[] topPalette, Color[] frontPalette) {
        int boxW = IsoRenderer.ISO_TILE_W;          // 64
        int boxH = IsoRenderer.ISO_BOX_SPRITE_H;    // 48
        int topH = IsoRenderer.ISO_TILE_H;          // 32
        int faceH = IsoRenderer.ISO_BOX_H;          // 16

        BufferedImage result = new BufferedImage(boxW, boxH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();

        // --- Top face (0..31) ---
        BufferedImage top = buildIsoTile(topMap16, topPalette);
        g.drawImage(top, 0, 0, null);

        // --- Front face (32..47): scale frontMap16 to 64x16, apply 75% shadow ---
        BufferedImage front = new BufferedImage(boxW, faceH, BufferedImage.TYPE_INT_ARGB);
        for (int py = 0; py < faceH; py++) {
            int srcY = py * PX / faceH;  // 0..15
            for (int px = 0; px < boxW; px++) {
                int srcX = px * PX / boxW;  // 0..15
                int palIdx = frontMap16[srcY][srcX];
                Color c = frontPalette[palIdx];
                if (c.getAlpha() == 0) {
                    front.setRGB(px, py, 0x00000000);
                } else {
                    // 75% brightness — simulate shadow on front face
                    Color shadowed = darken(c, 0.75f);
                    front.setRGB(px, py, shadowed.getRGB());
                }
            }
        }
        g.drawImage(front, 0, topH, null);

        g.dispose();
        return result;
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    /**
     * Scales a BufferedImage to exactly (w x h) using nearest-neighbor interpolation.
     * Preserves ARGB channels.
     */
    private BufferedImage scaleNearest(BufferedImage src, int w, int h) {
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }

    /**
     * Returns a new Color with each RGB channel multiplied by factor.
     * Alpha is preserved. factor > 1 brightens, factor < 1 darkens.
     */
    private Color darken(Color c, float factor) {
        int r = Math.min(255, Math.max(0, (int) (c.getRed()   * factor)));
        int gv = Math.min(255, Math.max(0, (int) (c.getGreen() * factor)));
        int b = Math.min(255, Math.max(0, (int) (c.getBlue()  * factor)));
        return new Color(r, gv, b, c.getAlpha());
    }

    /**
     * Rotates a BufferedImage by the angle specified by the Direction enum.
     * Uses nearest-neighbor interpolation to maintain pixel sharpness.
     * The image is rotated around its center point.
     */
    private BufferedImage rotateImage(BufferedImage src, Direction direction) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = rotated.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        // Rotate around image center
        g.rotate(direction.getRotationAngle(), w / 2.0, h / 2.0);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return rotated;
    }
}
