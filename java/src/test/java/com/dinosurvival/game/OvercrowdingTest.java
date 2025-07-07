package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class OvercrowdingTest {
    @Test
    public void testOvercrowdedLayingMoves() throws Exception {
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
        int x = 2;
        int y = 2;
        for (int i = 0; i < 4; i++) {
            NPCAnimal npc = new NPCAnimal();
            npc.setId(i);
            npc.setName("Stegosaurus");
            npc.setSex("F");
            npc.setWeight(10.0);
            map.addAnimal(x, y, npc);
        }
        DinosaurStats stats = StatsLoader.getDinoStats().get("Stegosaurus");
        NPCAnimal ready = new NPCAnimal();
        ready.setId(99);
        ready.setName("Stegosaurus");
        ready.setSex("F");
        ready.setWeight(stats.getAdultWeight());
        ready.setEnergy(100.0);
        ready.setHp(100.0);
        ready.setMaxHp(100.0);
        ready.setTurnsUntilLayEggs(0);
        map.addAnimal(x, y, ready);
        g.updateNpcs();
        Assertions.assertTrue(map.getEggs(x, y).isEmpty());
        Assertions.assertFalse(map.getAnimals(x, y).contains(ready));
    }
}
