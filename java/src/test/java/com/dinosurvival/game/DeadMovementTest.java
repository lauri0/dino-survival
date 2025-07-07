package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeadMovementTest {

    @Test
    public void testDeadNpcDoesNotMove() throws Exception {
        StatsLoader.load(Path.of("..", "dinosurvival"), "Morrison");
        Game game = new Game();
        game.start("Morrison", "Allosaurus");

        Map map = new Map(6, 6);
        Field mapField = Game.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(game, map);

        Field xField = Game.class.getDeclaredField("x");
        Field yField = Game.class.getDeclaredField("y");
        xField.setAccessible(true);
        yField.setAccessible(true);
        xField.setInt(game, 0);
        yField.setInt(game, 0);

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                map.getAnimals(x, y).clear();
            }
        }

        NPCAnimal npc = new NPCAnimal();
        npc.setId(1);
        npc.setName("Stegosaurus");
        npc.setWeight(10.0);
        npc.setNextMove("Right");
        npc.setAlive(false);
        map.addAnimal(0, 0, npc);

        Method m = Game.class.getDeclaredMethod("moveNpcs");
        m.setAccessible(true);
        m.invoke(game);

        Assertions.assertTrue(map.getAnimals(0, 0).contains(npc));
        Assertions.assertEquals("None", npc.getNextMove());
    }
}
