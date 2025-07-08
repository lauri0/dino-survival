package com.dinosurvival.game;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Map;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BoneBreakTest {
    @Test
    public void testPlayerBreaksBonesTarget() throws Exception {
        StatsLoader.load(Path.of("conf"), "Hell Creek");
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
        target.setWeight(40.0);
        target.setLastAction("spawned");
        map.addAnimal(x, y, target);
        g.getPlayer().getAbilities().add("bone_break");
        g.getPlayer().setWeight(60.0);
        g.getPlayer().setAttack(1.0);
        g.getPlayer().setHp(g.getPlayer().getAdultHp());
        g.getPlayer().setSpeed(1000.0);
        g.huntNpc(target.getId());
        g.updateNpcs();
        Assertions.assertEquals(9, target.getBrokenBone());
    }

    @Test
    public void testNpcBreaksPlayerBones() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
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
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Acheroraptor");
        npc.setWeight(60.0);
        npc.setAbilities(List.of("bone_break"));
        npc.setLastAction("spawned");
        map.addAnimal(x, y, npc);
        g.getPlayer().setWeight(40.0);
        g.getPlayer().setAttack(1.0);
        g.getPlayer().setHp(g.getPlayer().getAdultHp());
        g.getPlayer().setSpeed(1000.0);
        g.huntNpc(npc.getId());
        g.updateNpcs();
        Assertions.assertEquals(9, g.getPlayer().getBrokenBone());
    }

    @Test
    public void testBrokenBoneHalvesSpeed() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game g = new Game();
        g.start("Hell Creek", "Acheroraptor");
        g.getPlayer().setSpeed(10.0);
        g.getPlayer().setBrokenBone(5);
        Assertions.assertEquals(5.0, g.playerEffectiveSpeed(), 1e-9);
    }
}
