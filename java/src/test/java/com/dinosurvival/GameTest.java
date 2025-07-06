package com.dinosurvival;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Map;
import com.dinosurvival.game.Terrain;
import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GameTest {
    @BeforeAll
    public static void setup() throws Exception {
        Path base = Path.of("..", "dinosurvival");
        if (Files.exists(base)) {
            StatsLoader.load(base, "Morrison");
        }
    }

    @Test
    public void testPlayerInitialization() {
        Game g = new Game();
        g.start();
        Map map = g.getMap();
        int x = g.getPlayerX();
        int y = g.getPlayerY();
        Assertions.assertTrue(map.isRevealed(x, y));
        Assertions.assertNotEquals(Terrain.LAKE, map.terrainAt(x, y));
        boolean nearLake = false;
        for (int dy = -2; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (nx >= 0 && ny >= 0 && nx < map.getWidth() && ny < map.getHeight()) {
                    if (map.terrainAt(nx, ny) == Terrain.LAKE) {
                        nearLake = true;
                    }
                }
            }
        }
        Assertions.assertTrue(nearLake, "Player should start near a lake");
        Assertions.assertEquals(100.0, g.getPlayer().getEnergy(), 0.001);
        Assertions.assertEquals(100.0, g.getPlayer().getHydration(), 0.001);
    }

    @Test
    public void testNpcSpawningCounts() {
        Game g = new Game();
        g.start();
        java.util.Map<String, Integer> pop = g.populationStats();
        for (java.util.Map.Entry<String, DinosaurStats> e : StatsLoader.getDinoStats().entrySet()) {
            int count = pop.getOrDefault(e.getKey(), 0);
            Assertions.assertTrue(count >= 1, "no spawn for " + e.getKey());
        }
        for (java.util.Map.Entry<String, java.util.Map<String, Object>> e : StatsLoader.getCritterStats().entrySet()) {
            Object m = e.getValue().get("maximum_individuals");
            if (m instanceof Number num) {
                int count = pop.getOrDefault(e.getKey(), 0);
                Assertions.assertTrue(count >= 0 && count <= num.intValue(), "spawn bounds for " + e.getKey());
            }
        }
    }

    @Test
    public void testTurnUpdates() {
        Game g = new Game();
        g.start();
        double energy = g.getPlayer().getEnergy();
        String firstWeather = g.getWeather().getName();
        g.rest();
        Assertions.assertTrue(g.getPlayer().getEnergy() < energy);
        for (int i = 0; i < 20; i++) {
            g.rest();
        }
        Assertions.assertTrue(g.getTurn() >= 21);
        Assertions.assertNotEquals(firstWeather, g.getWeather().getName());
    }

    @Test
    public void testBasicPlayerActions() {
        Game g = new Game();
        g.start();
        int startX = g.getPlayerX();
        g.moveEast();
        Assertions.assertEquals(startX + 1, g.getPlayerX());

        NPCAnimal npc = new NPCAnimal();
        npc.setId(1234);
        npc.setName("Nanosaurus");
        npc.setWeight(10.0);
        npc.setMaxHp(10.0);
        npc.setHp(10.0);
        g.getMap().addAnimal(g.getPlayerX(), g.getPlayerY(), npc);
        double beforeEnergy = g.getPlayer().getEnergy();
        g.huntNpc(1234);
        Assertions.assertTrue(g.getPlayer().getEnergy() < beforeEnergy);

        DinosaurStats p = g.getPlayer();
        p.setWeight(p.getAdultWeight());
        p.setHp(p.getMaxHp());
        p.setEnergy(100.0);
        g.layEggs();
        Assertions.assertFalse(g.getMap().getEggs(g.getPlayerX(), g.getPlayerY()).isEmpty());
    }
}
