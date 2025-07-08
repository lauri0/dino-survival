package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.model.Plant;
import com.dinosurvival.util.StatsLoader;
import com.dinosurvival.game.EggCluster;
import com.dinosurvival.game.Map;
import com.dinosurvival.game.Terrain;
import com.dinosurvival.game.LavaInfo;
import java.lang.reflect.Field;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class LavaReversionTest {
    @BeforeAll
    public static void loadStats() throws Exception {
        StatsLoader.load(Path.of("conf"), "Morrison");
    }

    @Test
    public void testPlayerDiesOnLavaTile() throws Exception {
        Game game = new Game();
        game.start("Morrison", "Allosaurus");
        Map map = game.getMap();
        Field gridField = Map.class.getDeclaredField("grid");
        gridField.setAccessible(true);
        Terrain[][] grid = (Terrain[][]) gridField.get(map);
        int x = game.getPlayerX();
        int y = game.getPlayerY();
        grid[y][x] = Terrain.LAVA;
        game._apply_terrain_effects();
        Assertions.assertEquals(0.0, game.getPlayer().getHp(), 1e-9);
    }

    @Test
    public void testLavaClearsCellContents() throws Exception {
        Game game = new Game();
        game.start("Morrison", "Allosaurus");
        Map map = game.getMap();
        Field gridField = Map.class.getDeclaredField("grid");
        gridField.setAccessible(true);
        Terrain[][] grid = (Terrain[][]) gridField.get(map);
        int x = 1;
        int y = 1;
        grid[y][x] = Terrain.VOLCANO_ERUPTING;
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Stegosaurus");
        npc.setWeight(10.0);
        map.getAnimals(x, y).add(npc);
        map.getEggs(x, y).add(new EggCluster("Stegosaurus", 1, 1.0, 5));
        map.spawnBurrow(x, y, true);
        Plant p = new Plant();
        p.setName("Ferns");
        p.setWeight(5.0);
        map.getPlants(x, y).add(p);
        game._apply_terrain_effects();
        Assertions.assertFalse(npc.isAlive());
        Assertions.assertTrue(map.getEggs(x, y).isEmpty());
        Assertions.assertFalse(map.hasBurrow(x, y));
        Assertions.assertTrue(map.getPlants(x, y).isEmpty());
    }

    @Test
    public void testSolidifiedLavaRevertsToOriginalTile() throws Exception {
        Map map = new Map(6, 6, 0L);
        Field gridF = Map.class.getDeclaredField("grid");
        Field infoF = Map.class.getDeclaredField("lavaInfo");
        Field eruptF = Map.class.getDeclaredField("erupting");
        Field origF = Map.class.getDeclaredField("lavaOrig");
        Field turnsF = Map.class.getDeclaredField("solidifiedTurns");
        gridF.setAccessible(true);
        infoF.setAccessible(true);
        eruptF.setAccessible(true);
        origF.setAccessible(true);
        turnsF.setAccessible(true);
        Terrain[][] grid = (Terrain[][]) gridF.get(map);
        LavaInfo[][] info = (LavaInfo[][]) infoF.get(map);
        boolean[][] erupt = (boolean[][]) eruptF.get(map);
        Terrain[][] orig = (Terrain[][]) origF.get(map);
        int[][] turns = (int[][]) turnsF.get(map);
        for (int j = 0; j < map.getHeight(); j++) {
            for (int i = 0; i < map.getWidth(); i++) {
                grid[j][i] = Terrain.PLAINS;
                info[j][i] = null;
                erupt[j][i] = false;
                orig[j][i] = null;
                turns[j][i] = 0;
            }
        }
        grid[3][3] = Terrain.VOLCANO;
        map.startVolcanoEruption(3, 3, "medium");
        for (int i = 0; i < 3; i++) {
            map.updateVolcanicActivity();
        }
        int tx = 3;
        int ty = 2; // north of volcano
        Assertions.assertEquals(Terrain.SOLIDIFIED_LAVA_FIELD, map.terrainAt(tx, ty));
        for (int i = 0; i < 100; i++) {
            map.updateSolidifiedLava();
        }
        Assertions.assertEquals(Terrain.PLAINS, map.terrainAt(tx, ty));
    }
}
