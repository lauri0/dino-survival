package com.dinosurvival;

import com.dinosurvival.game.EggCluster;
import com.dinosurvival.game.Game;
import com.dinosurvival.game.Map;
import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.lang.reflect.Method;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EggsTest {
    @BeforeAll
    public static void loadStats() throws Exception {
        Path base = Path.of("..", "conf");
        StatsLoader.load(base, "Morrison");
    }

    private void clearMap(Game g) {
        Map map = g.getMap();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.getAnimals(x, y).clear();
                map.getEggs(x, y).clear();
            }
        }
    }

    @Test
    public void testCarnivoreIgnoresOwnEggs() throws Exception {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        clearMap(g);
        Map map = g.getMap();
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Allosaurus");
        npc.setEnergy(50.0);
        npc.setWeight(10.0);
        map.addAnimal(0, 0, npc);
        EggCluster ec = new EggCluster("Allosaurus", 1, 1.0, 5);
        map.addEggs(0, 0, ec);
        Method m = Game.class.getDeclaredMethod("updateNpcs");
        m.setAccessible(true);
        m.invoke(g);
        Assertions.assertEquals(1.0, map.getEggs(0, 0).get(0).getWeight(), 1e-9);
    }

    @Test
    public void testEggWeightEqualsHatchlingWeightTimesNumber() {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        Map map = g.getMap();
        clearMap(g);
        DinosaurStats player = g.getPlayer();
        player.setWeight(player.getAdultWeight());
        player.setEnergy(100.0);
        player.setHp(player.getMaxHp());
        player.setTurnsUntilLayEggs(0);
        g.layEggs();
        EggCluster egg = map.getEggs(g.getPlayerX(), g.getPlayerY()).get(0);
        DinosaurStats base = StatsLoader.getDinoStats().get("Allosaurus");
        double hatch = base.getHatchlingWeight();
        if (hatch <= 0) {
            hatch = Math.max(1.0, base.getAdultWeight() * 0.001);
        }
        double expected = hatch * base.getNumEggs();
        Assertions.assertEquals(expected, egg.getWeight(), 1e-9);
    }
}
