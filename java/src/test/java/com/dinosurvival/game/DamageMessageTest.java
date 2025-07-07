package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DamageMessageTest {
    @Test
    public void testHuntOnlyOneDamageMessage() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Tyrannosaurus");
        Map map = game.getMap();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.getAnimals(x, y).clear();
            }
        }
        java.util.Map<String, Object> stats = StatsLoader.getCritterStats().get("Didelphodon");
        double weight = 0.0;
        Object w = stats.get("adult_weight");
        if (w instanceof Number num) weight = num.doubleValue();
        double hp = 0.0;
        Object h = stats.get("hp");
        if (h instanceof Number num) hp = num.doubleValue();
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Didelphodon");
        npc.setWeight(weight);
        npc.setMaxHp(hp);
        npc.setHp(hp);
        map.addAnimal(game.getPlayerX(), game.getPlayerY(), npc);

        game.huntNpc(npc.getId());
        long count = game.getTurnMessages().stream()
                .filter(m -> m.startsWith("You deal"))
                .count();
        Assertions.assertEquals(1, count);
    }
}
