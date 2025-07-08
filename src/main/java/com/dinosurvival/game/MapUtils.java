package com.dinosurvival.game;

/** Utility methods for {@link Map} objects. */
public final class MapUtils {

    private MapUtils() {
        // utility class
    }

    /** Reveal the four orthogonally adjacent tiles to the given position. */
    public static void revealCardinals(Map map, int x, int y) {
        int[][] dirs = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
        for (int[] d : dirs) {
            int nx = x + d[0];
            int ny = y + d[1];
            if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                map.reveal(nx, ny);
            }
        }
    }

    /** Reveal all eight tiles surrounding the given position. */
    public static void revealSurrounding(Map map, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                    map.reveal(nx, ny);
                }
            }
        }
    }

    /**
     * Reveal any adjacent mountain or volcano tiles around the provided
     * position.
     */
    public static void revealAdjacentMountains(Map map, int x, int y) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && nx < map.getWidth() && ny >= 0 && ny < map.getHeight()) {
                    Terrain t = map.terrainAt(nx, ny);
                    if (t == Terrain.MOUNTAIN ||
                            t == Terrain.VOLCANO ||
                            t == Terrain.VOLCANO_ERUPTING) {
                        map.reveal(nx, ny);
                    }
                }
            }
        }
    }
}

