package com.dinosurvival.game;

import com.dinosurvival.util.StatsLoader;
import com.dinosurvival.model.NPCAnimal;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EggConsumptionTest {
    @Test
    public void testNpcEggClusterRemovedAfterEating() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Morrison");
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.getAnimals(x, y).clear();
                map.getEggs(x, y).clear();
            }
        }
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Ceratosaurus");
        npc.setEnergy(50.0);
        npc.setWeight(10.0);
        map.addAnimal(0, 0, npc);
        map.addEggs(0, 0, new EggCluster("Stegosaurus", 1, 1.0, 5));
        g.updateNpcs();
        Assertions.assertTrue(map.getEggs(0, 0).isEmpty());
        Assertions.assertEquals(1, npc.getEggClustersEaten());
    }

    @Test
    public void testPlayerCollectsEggsRemovesCluster() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Morrison");
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.getAnimals(x, y).clear();
                map.getEggs(x, y).clear();
            }
        }
        int px = g.getPlayerX();
        int py = g.getPlayerY();
        map.addEggs(px, py, new EggCluster("Stegosaurus", 2, 2.0, 5));
        g.collectEggs();
        Assertions.assertTrue(map.getEggs(px, py).isEmpty());
    }
}
