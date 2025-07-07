package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import com.dinosurvival.game.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CritterBleedTest {

    private NPCAnimal spawnCritter(Game game, String name) {
        java.util.Map<String, Object> stats = StatsLoader.getCritterStats().get(name);
        double weight = 0.0;
        Object wObj = stats.get("adult_weight");
        if (wObj instanceof Number num) {
            weight = num.doubleValue();
        }
        NPCAnimal critter = new NPCAnimal();
        critter.setId(1);
        critter.setName(name);
        critter.setWeight(weight);
        double maxHp = game.npcMaxHp(critter);
        critter.setMaxHp(maxHp);
        critter.setHp(maxHp);
        int x = game.getPlayerX();
        int y = game.getPlayerY();
        game.getMap().addAnimal(x, y, critter);
        return critter;
    }

    @Test
    public void testCritterBleedDamageAndExpires() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Acheroraptor");
        Map map = game.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        NPCAnimal critter = spawnCritter(game, "Didelphodon");
        critter.setBleeding(5);
        double hpBefore = critter.getHp();
        for (int i = 0; i < 5; i++) {
            game.updateNpcs();
        }
        Assertions.assertTrue(critter.getHp() < hpBefore);
        Assertions.assertEquals(0, critter.getBleeding());
    }

    @Test
    public void testCritterHealthRegen() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Acheroraptor");
        Map map = game.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        NPCAnimal critter = spawnCritter(game, "Didelphodon");
        critter.setHp(critter.getHp() - 0.2);
        double hpBefore = critter.getHp();
        java.util.Map<String, Object> stats = StatsLoader.getCritterStats().get("Didelphodon");
        double regen = 0.0;
        Object rObj = stats.get("health_regen");
        if (rObj instanceof Number num) {
            regen = num.doubleValue();
        }
        game.updateNpcs();
        double expected = Math.min(critter.getMaxHp(), hpBefore + critter.getMaxHp() * regen / 100.0);
        Assertions.assertEquals(expected, critter.getHp(), 1e-9);
    }
}
