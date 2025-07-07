package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BleedDamageTest {

    @Test
    public void testBleedDamageAndExpires() throws Exception {
        StatsLoader.load(Path.of("..", "conf"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Acheroraptor");
        Map map = game.getMap();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                map.getAnimals(tx, ty).clear();
            }
        }
        DinosaurStats stats = StatsLoader.getDinoStats().get("Acheroraptor");
        double weight = stats.getAdultWeight();
        double pct = stats.getAdultWeight() > 0 ? weight / stats.getAdultWeight() : 1.0;
        pct = Math.max(0.0, Math.min(1.0, pct));
        double maxHp = stats.getAdultHp() * pct;
        NPCAnimal target = new NPCAnimal();
        target.setId(1);
        target.setName("Acheroraptor");
        target.setWeight(weight);
        target.setMaxHp(maxHp);
        target.setHp(maxHp);
        map.addAnimal(game.getPlayerX(), game.getPlayerY(), target);

        target.setBleeding(5);
        double hpBefore = target.getHp();
        for (int i = 0; i < 5; i++) {
            game.updateNpcs();
        }
        Assertions.assertTrue(target.getHp() < hpBefore);
        Assertions.assertEquals(0, target.getBleeding());
    }

    @Test
    public void testPlayerMovingWhileBleedingTakesExtraDamage() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Hell Creek");
        Game game = new Game();
        game.start("Hell Creek", "Acheroraptor");
        DinosaurStats player = game.getPlayer();
        player.setWeight(player.getAdultWeight());
        player.setMaxHp(player.getAdultHp());
        player.setHp(player.getMaxHp());
        player.setBleeding(1);

        double hpBeforeMove = player.getHp();
        game.applyTurnCosts(true);
        double moveLoss = hpBeforeMove - player.getHp();

        player.setHp(hpBeforeMove);
        player.setBleeding(1);
        game.applyTurnCosts(false);
        double stayLoss = hpBeforeMove - player.getHp();

        Assertions.assertEquals(moveLoss, stayLoss * 2, 1e-9);
    }
}
