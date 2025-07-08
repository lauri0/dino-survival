package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FloodTest {
    @BeforeAll
    public static void loadStats() throws Exception {
        StatsLoader.load(Path.of("conf"), "Morrison");
    }

    private static Map prepareMap(DinosaurStats player) throws Exception {
        Map map = new Map(5, 5, 0L);
        Field gridF = Map.class.getDeclaredField("grid");
        Field floodF = Map.class.getDeclaredField("floodInfo");
        gridF.setAccessible(true);
        floodF.setAccessible(true);
        Terrain[][] grid = (Terrain[][]) gridF.get(map);
        Terrain[][] flood = (Terrain[][]) floodF.get(map);
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                grid[y][x] = Terrain.PLAINS;
                flood[y][x] = null;
            }
        }
        grid[2][2] = Terrain.LAKE;
        return map;
    }

    @Test
    public void testFloodSpreadAndRevert() throws Exception {
        DinosaurStats player = new DinosaurStats();
        player.setMaxHp(100.0);
        player.setHp(100.0);
        Map map = prepareMap(player);

        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Test");
        npc.setMaxHp(100.0);
        npc.setHp(100.0);
        map.getAnimals(1, 2).add(npc);

        Field activeF = Map.class.getDeclaredField("activeFlood");
        Field turnF = Map.class.getDeclaredField("floodTurn");
        activeF.setAccessible(true);
        turnF.setAccessible(true);
        activeF.setBoolean(map, true);
        turnF.setInt(map, 0);

        map.initiateFlood(player, 0, 0);
        Assertions.assertTrue((Boolean) activeF.get(map));
        Assertions.assertEquals(Terrain.PLAINS_FLOODED, map.terrainAt(1, 2));
        Assertions.assertEquals(50.0, npc.getHp(), 1e-9);

        map.updateFlood(0, 0, player, 0.0);
        Assertions.assertEquals(Terrain.PLAINS_FLOODED, map.terrainAt(0, 2));

        map.updateFlood(0, 0, player, 0.0);
        Assertions.assertTrue((Boolean) activeF.get(map));

        map.updateFlood(0, 0, player, 0.0);
        Assertions.assertFalse((Boolean) activeF.get(map));
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (x == 2 && y == 2) continue;
                Assertions.assertEquals(Terrain.PLAINS, map.terrainAt(x, y));
            }
        }
    }
}
