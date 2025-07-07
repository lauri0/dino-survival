package com.dinosurvival;

import com.dinosurvival.game.Game;
import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AttackScalingTest {
    @BeforeAll
    public static void setup() throws Exception {
        Path base = Path.of("..", "dinosurvival");
        StatsLoader.load(base, "Morrison");
    }

    @Test
    public void testPlayerAttackScalesWithHp() {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        DinosaurStats player = g.getPlayer();
        player.setWeight(player.getAdultWeight());
        double pct = player.getAdultWeight() > 0 ? player.getWeight() / player.getAdultWeight() : 1.0;
        pct = Math.max(0.0, Math.min(pct, 1.0));
        player.setAttack(player.getAdultAttack() * pct);
        player.setHp(player.getMaxHp() / 2.0);
        double expected = player.getAdultAttack() * 0.5;
        Assertions.assertEquals(expected, g.playerEffectiveAttack(), 1e-9);
    }

    @Test
    public void testNpcAttackScalesWithHp() {
        Game g = new Game();
        g.start("Morrison", "Allosaurus");
        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Stegosaurus");
        npc.setWeight(1.0);
        npc.setHp(50.0);
        npc.setMaxHp(100.0);
        DinosaurStats stats = StatsLoader.getDinoStats().get("Stegosaurus");
        double pct = stats.getAdultWeight() > 0 ? npc.getWeight() / stats.getAdultWeight() : 1.0;
        pct = Math.max(0.0, Math.min(pct, 1.0));
        double baseAtk = stats.getAdultAttack() * pct;
        double expected = baseAtk * 0.5;
        double actual = g.npcEffectiveAttack(npc);
        Assertions.assertEquals(expected, actual, 1e-9);
    }

    @Test
    public void testDamageAfterArmorFunction() throws Exception {
        Game g = new Game();
        Method m = Game.class.getDeclaredMethod("damageAfterArmor", double.class, Object.class, Object.class);
        m.setAccessible(true);
        double dmg = (double) m.invoke(g, 100.0,
                Map.of("abilities", List.of("bone_break")),
                Map.of("abilities", List.of("heavy_armor")));
        Assertions.assertEquals(80.0, dmg, 1e-9);
    }
}
