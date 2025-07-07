package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HuntsTest {

    @Test
    public void testLiveHuntCountsKill() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Tyrannosaurus", 0L);
        Map map = game.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Centipede");
        npc.setWeight(1.0);
        map.addAnimal(game.getPlayerX(), game.getPlayerY(), npc);
        game.huntNpc(npc.getId());
        Assertions.assertEquals(1, game.getHuntStats().get("Centipede")[1]);
    }

    @Test
    public void testCarcassEatNotCountedAsHunt() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Morrison");
        Game game = new Game();
        game.start("Morrison", "Allosaurus", 0L);
        Map map = game.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        NPCAnimal carcass = new NPCAnimal();
        carcass.setId(1);
        carcass.setName("Stegosaurus");
        carcass.setAlive(false);
        carcass.setWeight(1.0);
        map.addAnimal(game.getPlayerX(), game.getPlayerY(), carcass);
        game.huntNpc(carcass.getId());
        Assertions.assertFalse(game.getHuntStats().containsKey("Stegosaurus"));
    }

    @Test
    public void testPlayerTakesDamageWhenHuntingCritter() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Tyrannosaurus", 0L);
        Map map = game.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        game.getPlayer().setHealthRegen(0.0);
        NPCAnimal critter = new NPCAnimal();
        critter.setId(1);
        critter.setName("Didelphodon");
        critter.setWeight(5.0);
        map.addAnimal(game.getPlayerX(), game.getPlayerY(), critter);
        game.huntNpc(critter.getId());
        Assertions.assertTrue(game.getPlayer().getHp() < game.getPlayer().getMaxHp());
    }

    @Test
    public void testNpcTakesDamageWhenHuntingCritter() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Tyrannosaurus", 0L);
        Map map = game.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        NPCAnimal predator = new NPCAnimal();
        predator.setId(1);
        predator.setName("Acheroraptor");
        predator.setWeight(10.0);
        predator.setEnergy(50.0);
        NPCAnimal prey = new NPCAnimal();
        prey.setId(2);
        prey.setName("Didelphodon");
        prey.setWeight(5.0);
        map.addAnimal(0, 0, predator);
        map.addAnimal(0, 0, prey);
        game.updateNpcs();
        Assertions.assertTrue(predator.getHp() < predator.getMaxHp());
    }
}
