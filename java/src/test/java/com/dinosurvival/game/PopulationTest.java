package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PopulationTest {
    @Test
    public void testHellCreekPopulationScaling() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Tyrannosaurus");
        Map map = game.getMap();
        java.util.Map<String, Integer> counts = new HashMap<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                for (NPCAnimal npc : map.getAnimals(x, y)) {
                    counts.merge(npc.getName(), 1, Integer::sum);
                }
            }
        }
        java.util.Map<String, Double> multipliers = new HashMap<>();
        for (java.util.Map.Entry<String, DinosaurStats> e : StatsLoader.getDinoStats().entrySet()) {
            multipliers.put(e.getKey(), e.getValue().getInitialSpawnMultiplier());
        }
        int total = 0;
        for (String name : multipliers.keySet()) {
            total += counts.getOrDefault(name, 0);
        }
        Assertions.assertEquals(100, total);
        boolean different = false;
        for (java.util.Map.Entry<String, Double> e : multipliers.entrySet()) {
            int expected = (int) Math.round(e.getValue());
            if (counts.getOrDefault(e.getKey(), 0) != expected) {
                different = true;
                break;
            }
        }
        Assertions.assertTrue(different);
    }
}
