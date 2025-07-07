package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

public class ForestFireTest {

    private static Map prepareForestMap() throws Exception {
        Map map = new Map(5, 5, 0L);
        Field gridF = Map.class.getDeclaredField("grid");
        Field fireF = Map.class.getDeclaredField("fireTurns");
        Field burntF = Map.class.getDeclaredField("burntTurns");
        gridF.setAccessible(true);
        fireF.setAccessible(true);
        burntF.setAccessible(true);
        Terrain[][] grid = (Terrain[][]) gridF.get(map);
        int[][] fire = (int[][]) fireF.get(map);
        int[][] burnt = (int[][]) burntF.get(map);
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                grid[y][x] = Terrain.FOREST;
                fire[y][x] = 0;
                burnt[y][x] = 0;
            }
        }
        return map;
    }

    @Test
    public void testForestFireRevertsToOriginalTile() throws Exception {
        Map map = prepareForestMap();
        int x = 2;
        int y = 2;
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Test");
        npc.setWeight(10.0);
        map.addAnimal(x, y, npc);

        map.startForestFire(x, y);
        Assertions.assertEquals(Terrain.FOREST_FIRE, map.terrainAt(x, y));
        Assertions.assertTrue(map.getAnimals(x, y).isEmpty());

        for (int i = 0; i < 5; i++) {
            map.updateForestFire();
        }
        Assertions.assertEquals(Terrain.FOREST_BURNT, map.terrainAt(x, y));

        for (int i = 0; i < 50; i++) {
            map.updateForestFire();
        }
        Assertions.assertEquals(Terrain.FOREST, map.terrainAt(x, y));
    }
}
