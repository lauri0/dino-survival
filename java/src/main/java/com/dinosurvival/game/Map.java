package com.dinosurvival.game;

import java.util.Random;

public class Map {
    private final Terrain[][] grid;
    private final int width;
    private final int height;

    public Map(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Terrain[height][width];
        generate();
    }

    private void generate() {
        Random r = new Random();
        Terrain[] values = Terrain.values();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = values[r.nextInt(values.length)];
            }
        }
    }

    public Terrain getTerrain(int x, int y) {
        return grid[y][x];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
