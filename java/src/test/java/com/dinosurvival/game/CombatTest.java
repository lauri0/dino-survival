package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CombatTest {

    @Test
    public void test_player_attack_damage() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Tyrannosaurus");
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
        Assertions.assertTrue(npc.getHp() < npc.getMaxHp());
    }

    @Test
    public void test_npc_target_selection() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Morrison");
        Game game = new Game();
        game.start("Morrison", "Allosaurus");
        Map map = game.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        NPCAnimal predator = new NPCAnimal();
        predator.setId(1);
        predator.setName("Allosaurus");
        predator.setWeight(3000.0);
        predator.setEnergy(50.0);
        NPCAnimal strong = new NPCAnimal();
        strong.setId(2);
        strong.setName("Allosaurus");
        strong.setWeight(3000.0);
        strong.setEnergy(50.0);
        map.addAnimal(0, 0, predator);
        map.addAnimal(0, 0, strong);
        game.updateNpcs();
        Assertions.assertEquals(predator.getMaxHp(), predator.getHp());
    }
}

