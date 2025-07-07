package com.dinosurvival;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Map;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreatenTest {
    @BeforeAll
    public static void loadStats() throws Exception {
        Path base = Path.of("..", "dinosurvival");
        if (Files.exists(base)) {
            StatsLoader.load(base, "Morrison");
        }
    }

    @Test
    public void testThreatenScatter() {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        java.util.Map<String, java.util.Map<String, Object>> saved =
                new java.util.HashMap<>(StatsLoader.getCritterStats());
        StatsLoader.getCritterStats().clear();
        Map map = new Map(6, 6);
        try {
            java.lang.reflect.Field mapField = Game.class.getDeclaredField("map");
            mapField.setAccessible(true);
            mapField.set(g, map);
            java.lang.reflect.Field xf = Game.class.getDeclaredField("x");
            java.lang.reflect.Field yf = Game.class.getDeclaredField("y");
            xf.setAccessible(true);
            yf.setAccessible(true);
            xf.setInt(g, 3);
            yf.setInt(g, 3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        int x = 3;
        int y = 3;
        map.getAnimals(x, y).clear();
        NPCAnimal npc1 = new NPCAnimal();
        npc1.setId(1);
        npc1.setName("Stegosaurus");
        npc1.setWeight(1.0);
        npc1.setLastAction("spawned");
        NPCAnimal npc2 = new NPCAnimal();
        npc2.setId(2);
        npc2.setName("Stegosaurus");
        npc2.setWeight(1.0);
        npc2.setLastAction("spawned");
        map.addAnimal(x, y, npc1);
        map.addAnimal(x, y, npc2);
        double base;
        try {
            java.lang.reflect.Method m = Game.class.getDeclaredMethod("baseEnergyDrain");
            m.setAccessible(true);
            base = (double) m.invoke(g);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        g.getPlayer().setEnergy(100.0);
        g.threaten();
        StatsLoader.getCritterStats().putAll(saved);
        double expected = 100.0 - base * 2 * g.getWeather().getPlayerEnergyMult();
        Assertions.assertEquals(expected, g.getPlayer().getEnergy(), 0.0001);
        Assertions.assertTrue(map.getAnimals(x, y).isEmpty());
    }

    @Test
    public void testThreatenKilledByStronger() {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        int x = g.getPlayerX();
        int y = g.getPlayerY();
        map.getAnimals(x, y).clear();
        NPCAnimal strong = new NPCAnimal();
        strong.setId(1);
        strong.setName("Allosaurus");
        strong.setWeight(3000.0);
        strong.setLastAction("spawned");
        map.addAnimal(x, y, strong);
        g.threaten();
        Assertions.assertEquals(0.0, g.getPlayer().getHp(), 0.0001);
    }
}
