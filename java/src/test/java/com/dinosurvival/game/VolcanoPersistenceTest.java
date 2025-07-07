package com.dinosurvival.game;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VolcanoPersistenceTest {
    @Test
    public void testVolcanoTileRemainsAfterEruption() throws Exception {
        Map map = new Map(6, 6, 0L);

        Field gridField = Map.class.getDeclaredField("grid");
        gridField.setAccessible(true);
        Field lavaInfoField = Map.class.getDeclaredField("lavaInfo");
        lavaInfoField.setAccessible(true);
        Field eruptField = Map.class.getDeclaredField("erupting");
        eruptField.setAccessible(true);

        Terrain[][] grid = (Terrain[][]) gridField.get(map);
        LavaInfo[][] lavaInfo = (LavaInfo[][]) lavaInfoField.get(map);
        boolean[][] erupting = (boolean[][]) eruptField.get(map);
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                grid[y][x] = Terrain.PLAINS;
                lavaInfo[y][x] = null;
                erupting[y][x] = false;
            }
        }
        grid[3][3] = Terrain.VOLCANO;

        map.startVolcanoEruption(3, 3, "medium");
        Assertions.assertEquals(Terrain.VOLCANO_ERUPTING, map.terrainAt(3, 3));

        for (int i = 0; i < 3; i++) {
            map.updateVolcanicActivity();
        }

        Assertions.assertEquals(Terrain.VOLCANO, map.terrainAt(3, 3));
    }
}
