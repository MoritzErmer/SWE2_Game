package game.ui;

import java.awt.Point;

/**
 * Owns all isometric coordinate math for the game's rendering layer.
 * No other class should compute screen positions directly.
 *
 * Coordinate system (2:1 isometric):
 *   sx = isoOriginX + (tx - ty) * (ISO_TILE_W / 2)
 *   sy = isoOriginY + (tx + ty) * (ISO_TILE_H / 2)
 *
 * The origin (isoOriginX / isoOriginY) is the screen pixel where world tile (0,0)
 * would be drawn. It is updated every frame by GameUI.updateCamera().
 *
 * Thread safety: all methods are called exclusively on the Swing EDT.
 * No synchronisation required.
 */
public final class IsoRenderer {

    // -----------------------------------------------------------------------
    // Tile geometry constants
    // -----------------------------------------------------------------------

    /** Full pixel width of one isometric diamond tile. */
    public static final int ISO_TILE_W = 64;

    /** Full pixel height of one isometric diamond tile (face only, no depth). */
    public static final int ISO_TILE_H = 32;

    /**
     * Height of the visible front face added below the diamond for box-shaped
     * sprites (machines, player).  A box sprite is ISO_TILE_H + ISO_BOX_H tall.
     */
    public static final int ISO_BOX_H = 16;

    /** Total pixel height of a box sprite (top diamond + front face). */
    public static final int ISO_BOX_SPRITE_H = ISO_TILE_H + ISO_BOX_H; // 48

    // -----------------------------------------------------------------------
    // Mutable camera state — updated once per frame
    // -----------------------------------------------------------------------

    private int isoOriginX;
    private int isoOriginY;

    // -----------------------------------------------------------------------
    // Camera update
    // -----------------------------------------------------------------------

    /**
     * Sets the screen position of world tile (0, 0).
     * Must be called before any worldToScreen calls for the current frame.
     */
    public void updateOrigin(int originX, int originY) {
        this.isoOriginX = originX;
        this.isoOriginY = originY;
    }

    public int getOriginX() { return isoOriginX; }
    public int getOriginY() { return isoOriginY; }

    // -----------------------------------------------------------------------
    // World → Screen
    // -----------------------------------------------------------------------

    /**
     * Returns the screen X of the top-left corner of the isometric diamond
     * for world tile (tx, ty).
     */
    public int screenX(int tx, int ty) {
        return isoOriginX + (tx - ty) * (ISO_TILE_W / 2);
    }

    /**
     * Returns the screen Y of the top corner of the isometric diamond
     * for world tile (tx, ty).
     */
    public int screenY(int tx, int ty) {
        return isoOriginY + (tx + ty) * (ISO_TILE_H / 2);
    }

    // -----------------------------------------------------------------------
    // Screen → World (inverse transform for mouse input)
    // -----------------------------------------------------------------------

    /**
     * Maps a screen pixel (sx, sy) back to the world tile grid.
     * Uses the exact inverse of the isometric projection.
     *
     * Note: the returned tile may be out of world bounds — callers must
     * validate with map.inBounds() before use.
     */
    public Point screenToWorld(int sx, int sy) {
        double dx = sx - isoOriginX;
        double dy = sy - isoOriginY;

        // Derived by solving the 2x2 linear system:
        //   dx = (tx - ty) * (W/2)
        //   dy = (tx + ty) * (H/2)
        double hw = ISO_TILE_W / 2.0;
        double hh = ISO_TILE_H / 2.0;

        int tx = (int) Math.floor(dx / hw / 2.0 + dy / hh / 2.0);
        int ty = (int) Math.floor(dy / hh / 2.0 - dx / hw / 2.0);
        return new Point(tx, ty);
    }

    // -----------------------------------------------------------------------
    // Depth-sorted tile iteration (Painter's Algorithm, zero allocations)
    // -----------------------------------------------------------------------

    /**
     * Visits all tiles within the given world-coordinate bounds in ascending
     * (tx + ty) order — the correct draw order for isometric rendering so that
     * tiles closer to the viewer are always drawn on top of tiles further away.
     *
     * Uses only loop counters; no List or array allocation per frame.
     *
     * @param txMin  inclusive lower bound for tile x
     * @param txMax  inclusive upper bound for tile x
     * @param tyMin  inclusive lower bound for tile y
     * @param tyMax  inclusive upper bound for tile y
     * @param visitor called for each tile in painter's order
     */
    public void visitDepthSorted(int txMin, int txMax,
                                  int tyMin, int tyMax,
                                  TileVisitor visitor) {
        int dMin = txMin + tyMin;
        int dMax = txMax + tyMax;
        for (int d = dMin; d <= dMax; d++) {
            for (int tx = txMin; tx <= txMax; tx++) {
                int ty = d - tx;
                if (ty < tyMin || ty > tyMax) continue;
                visitor.visit(tx, ty);
            }
        }
    }

    /** Callback interface for depth-sorted tile iteration. */
    @FunctionalInterface
    public interface TileVisitor {
        void visit(int tx, int ty);
    }

    // -----------------------------------------------------------------------
    // Frustum culling helpers
    // -----------------------------------------------------------------------

    /**
     * Computes the minimum world-tile x visible on screen given the current
     * origin and screen width.  Adds the requested padding.
     */
    public int visibleTxMin(int screenW, int screenH, int mapW, int mapH, int pad) {
        // Transform all four screen corners and take the minimum tx
        int min = Integer.MAX_VALUE;
        for (int cx : new int[]{0, screenW}) {
            for (int cy : new int[]{0, screenH}) {
                min = Math.min(min, screenToWorld(cx, cy).x);
            }
        }
        return Math.max(0, min - pad);
    }

    public int visibleTxMax(int screenW, int screenH, int mapW, int mapH, int pad) {
        int max = Integer.MIN_VALUE;
        for (int cx : new int[]{0, screenW}) {
            for (int cy : new int[]{0, screenH}) {
                max = Math.max(max, screenToWorld(cx, cy).x);
            }
        }
        return Math.min(mapW - 1, max + pad);
    }

    public int visibleTyMin(int screenW, int screenH, int mapW, int mapH, int pad) {
        int min = Integer.MAX_VALUE;
        for (int cx : new int[]{0, screenW}) {
            for (int cy : new int[]{0, screenH}) {
                min = Math.min(min, screenToWorld(cx, cy).y);
            }
        }
        return Math.max(0, min - pad);
    }

    public int visibleTyMax(int screenW, int screenH, int mapW, int mapH, int pad) {
        int max = Integer.MIN_VALUE;
        for (int cx : new int[]{0, screenW}) {
            for (int cy : new int[]{0, screenH}) {
                max = Math.max(max, screenToWorld(cx, cy).y);
            }
        }
        return Math.min(mapH - 1, max + pad);
    }
}
