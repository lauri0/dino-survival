package com.dinosurvival.game;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Map;
import com.dinosurvival.game.Terrain;
import com.dinosurvival.util.StatsLoader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VolcanoEruptionTest {
    private static void setPlayerPos(Game g, int x, int y) throws Exception {
        Field xf = Game.class.getDeclaredField("x");
        Field yf = Game.class.getDeclaredField("y");
        xf.setAccessible(true);
        yf.setAccessible(true);
        xf.setInt(g, x);
        yf.setInt(g, y);
    }


    private static Map prepareMap(Game g, int px, int py) throws Exception {
        Map map = new Map(6, 6);
        Field mapField = Game.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(g, map);
        setPlayerPos(g, px, py);

        Field gridField = Map.class.getDeclaredField("grid");
        gridField.setAccessible(true);
        Terrain[][] grid = (Terrain[][]) gridField.get(map);
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                grid[y][x] = Terrain.PLAINS;
            }
        }
        grid[3][3] = Terrain.VOLCANO;
        return map;
    }

    @Test
    public void testPlayerDiesOnVolcanoEruption() throws Exception {
        StatsLoader.load(Path.of("conf"), "Morrison");
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = prepareMap(g, 3, 3);
        map.startVolcanoEruption(3, 3, "medium", 3, 3);
        map.updateVolcanicActivity(3, 3, g.getPlayer());
        Assertions.assertEquals(Terrain.VOLCANO_ERUPTING, map.terrainAt(3, 3));
        Assertions.assertEquals(0.0, g.getPlayer().getHp(), 1e-9);
    }

    @Test
    public void testPlayerDiesAdjacentVolcanoEruption() throws Exception {
        StatsLoader.load(Path.of("conf"), "Morrison");
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = prepareMap(g, 3, 2);
        map.startVolcanoEruption(3, 3, "medium", 3, 2);
        map.updateVolcanicActivity(3, 2, g.getPlayer());
        Assertions.assertEquals(Terrain.VOLCANO_ERUPTING, map.terrainAt(3, 3));
        Assertions.assertEquals(Terrain.LAVA, map.terrainAt(3, 2));
        Assertions.assertEquals(0.0, g.getPlayer().getHp(), 1e-9);
    }
}
