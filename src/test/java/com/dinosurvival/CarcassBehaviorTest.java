package com.dinosurvival;

import com.dinosurvival.game.EncounterEntry;
import com.dinosurvival.game.Game;
import com.dinosurvival.game.Map;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CarcassBehaviorTest {
    @BeforeAll
    public static void loadStats() throws Exception {
        Path base = Path.of("conf");
        StatsLoader.load(base, "Morrison");
    }

    private void clearAnimals(Map map) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.getAnimals(x, y).clear();
            }
        }
    }

    @Test
    public void testCarcassCannotAttack() throws Exception {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        clearAnimals(map);
        NPCAnimal carcass = new NPCAnimal();
        carcass.setId(1);
        carcass.setName("Allosaurus");
        carcass.setAlive(false);
        carcass.setWeight(100.0);
        map.addAnimal(g.getPlayerX(), g.getPlayerY(), carcass);
        Method gen = Game.class.getDeclaredMethod("generateEncounters");
        gen.setAccessible(true);
        gen.invoke(g);
        Method atk = Game.class.getDeclaredMethod("aggressiveAttackCheck");
        atk.setAccessible(true);
        Object result = atk.invoke(g);
        Assertions.assertNull(result);
    }

    @Test
    public void testNpcStarvesAndCarcassDecays() throws Exception {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        clearAnimals(map);
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Allosaurus");
        npc.setEnergy(1.0);
        npc.setWeight(10.0);
        map.addAnimal(0, 0, npc);
        g.getNpcController().updateNpcs();
        Assertions.assertFalse(npc.isAlive());
        Assertions.assertEquals(0.0, npc.getEnergy(), 1e-9);
        double before = npc.getWeight();
        g.spoilCarcasses();
        Assertions.assertTrue(npc.getWeight() < before);
    }

    private boolean encounterContains(Game g, NPCAnimal npc) {
        for (EncounterEntry e : g.getCurrentEncounters()) {
            if (e.getNpc() == npc) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void testCarcassRemovedAfterSpoilage() throws Exception {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        clearAnimals(map);
        NPCAnimal carcass = new NPCAnimal();
        carcass.setId(1);
        carcass.setName("Stegosaurus");
        carcass.setAlive(false);
        carcass.setWeight(1.0);
        map.addAnimal(g.getPlayerX(), g.getPlayerY(), carcass);
        Method gen = Game.class.getDeclaredMethod("generateEncounters");
        gen.setAccessible(true);
        gen.invoke(g);
        Assertions.assertTrue(encounterContains(g, carcass));
        g.rest();
        Assertions.assertFalse(encounterContains(g, carcass));
        Assertions.assertFalse(map.getAnimals(g.getPlayerX(), g.getPlayerY()).contains(carcass));
    }

    @Test
    public void testZeroWeightRemovedOnGenerate() throws Exception {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        clearAnimals(map);
        NPCAnimal carcass = new NPCAnimal();
        carcass.setId(2);
        carcass.setName("Stegosaurus");
        carcass.setAlive(false);
        carcass.setWeight(0.0);
        map.addAnimal(g.getPlayerX(), g.getPlayerY(), carcass);
        Method gen = Game.class.getDeclaredMethod("generateEncounters");
        gen.setAccessible(true);
        gen.invoke(g);
        Assertions.assertFalse(map.getAnimals(g.getPlayerX(), g.getPlayerY()).contains(carcass));
        Assertions.assertFalse(encounterContains(g, carcass));
    }

    @Test
    public void testSpoilageOccursAfterTurn() throws Exception {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        clearAnimals(map);
        NPCAnimal carcass = new NPCAnimal();
        carcass.setId(3);
        carcass.setName("Lizard");
        carcass.setAlive(false);
        carcass.setWeight(1.0);
        map.addAnimal(g.getPlayerX(), g.getPlayerY(), carcass);
        g.getNpcController().updateNpcs();
        Assertions.assertEquals(1.0, carcass.getWeight(), 1e-9);
        g.rest();
        Assertions.assertFalse(map.getAnimals(g.getPlayerX(), g.getPlayerY()).contains(carcass));
    }

    @Test
    public void testSpoilageMessageClampedToRemainingWeight() {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        clearAnimals(map);
        NPCAnimal carcass = new NPCAnimal();
        carcass.setId(4);
        carcass.setName("Stegosaurus");
        carcass.setAlive(false);
        carcass.setWeight(1.0);
        map.addAnimal(g.getPlayerX(), g.getPlayerY(), carcass);
        List<String> messages = g.spoilCarcasses();
        String expected = "The Stegosaurus (4) carcass lost 1.0kg to spoilage.";
        Assertions.assertEquals(List.of(expected), messages);
        Assertions.assertFalse(map.getAnimals(g.getPlayerX(), g.getPlayerY()).contains(carcass));
    }
}
