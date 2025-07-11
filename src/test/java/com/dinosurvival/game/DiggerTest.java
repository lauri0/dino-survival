package com.dinosurvival.game;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Map;
import com.dinosurvival.game.Burrow;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import com.dinosurvival.game.Settings;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DiggerTest {
    @Test
    public void testPectinodonDigBurrowOneTurn() throws Exception {
        StatsLoader.load(Path.of("conf"), "Hell Creek");
        Game g = new Game();
        g.start("Hell Creek", "Pectinodon");
        Map map = g.getMap();
        int x = g.getPlayerX();
        int y = g.getPlayerY();
        map.spawnBurrow(x, y, true);
        Burrow b = map.getBurrow(x, y);
        Assertions.assertTrue(b.isFull());
        g.digBurrow();
        b = map.getBurrow(x, y);
        Assertions.assertNotNull(b);
        Assertions.assertFalse(b.isFull());
        Assertions.assertEquals(0.0, b.getProgress());
        boolean found = false;
        for (NPCAnimal a : map.getAnimals(x, y)) {
            if ("Didelphodon".equals(a.getName())) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found);
    }

    @Test
    public void testNpcDiggerDigsBurrow() throws Exception {
        StatsLoader.load(Path.of("conf"), "Hell Creek");
        Game g = new Game();
        g.start("Hell Creek", "Tyrannosaurus");
        Map map = g.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        int x = g.getPlayerX();
        int y = g.getPlayerY();
        map.spawnBurrow(x, y, true);
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Pectinodon");
        npc.setEnergy(50.0);
        npc.setWeight(10.0);
        npc.setAbilities(List.of("digger"));
        npc.setLastAction("None");
        map.addAnimal(x, y, npc);
        g.updateNpcs();
        Burrow b = map.getBurrow(x, y);
        Assertions.assertNotNull(b);
        Assertions.assertFalse(b.isFull());
        boolean found = false;
        for (NPCAnimal a : map.getAnimals(x, y)) {
            if (a != npc && "Didelphodon".equals(a.getName())) {
                found = true;
                break;
            }
        }
        Assertions.assertTrue(found);
    }

    @Test
    public void testHellCreekPlayableList() {
        String[] list = Settings.HELL_CREEK.getPlayableDinos()
                .keySet().toArray(new String[0]);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(Arrays.asList(list).contains("Pectinodon"));
        Assertions.assertTrue(Arrays.asList(list).contains("Acheroraptor"));
    }
}
