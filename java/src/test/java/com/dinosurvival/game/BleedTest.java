package com.dinosurvival.game;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Map;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BleedTest {
    @Test
    public void testPlayerBleedsTarget() throws Exception {
        StatsLoader.load(Path.of("..", "conf"), "Hell Creek");
        Game g = new Game();
        g.start("Hell Creek", "Acheroraptor");
        Map map = g.getMap();
        int x = g.getPlayerX();
        int y = g.getPlayerY();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        NPCAnimal target = new NPCAnimal();
        target.setId(1);
        target.setName("Acheroraptor");
        target.setWeight(100.0);
        target.setLastAction("spawned");
        map.addAnimal(x, y, target);
        g.getPlayer().setWeight(g.getPlayer().getAdultWeight());
        g.getPlayer().setAttack(1.0);
        g.getPlayer().setHp(g.getPlayer().getAdultHp());
        g.getPlayer().setSpeed(1000.0);
        g.huntNpc(target.getId());
        g.updateNpcs();
        Assertions.assertEquals(4, target.getBleeding());
    }

    @Test
    public void testNpcBleedsPlayer() throws Exception {
        StatsLoader.load(Path.of("..", "conf"), "Morrison");
        Game g = new Game();
        g.start("Morrison", "Ceratosaurus");
        Map map = g.getMap();
        int x = g.getPlayerX();
        int y = g.getPlayerY();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Allosaurus");
        npc.setWeight(100.0);
        npc.setAbilities(List.of("bleed"));
        npc.setLastAction("spawned");
        map.addAnimal(x, y, npc);
        g.getPlayer().setWeight(g.getPlayer().getAdultWeight());
        g.getPlayer().setAttack(g.getPlayer().getAdultAttack() * (g.getPlayer().getWeight() / g.getPlayer().getAdultWeight()));
        g.getPlayer().setHp(g.getPlayer().getAdultHp());
        g.getPlayer().setSpeed(1000.0);
        g.huntNpc(npc.getId());
        g.updateNpcs();
        Assertions.assertEquals(4, g.getPlayer().getBleeding());
    }
}
