package com.dinosurvival.game;

import com.dinosurvival.util.StatsLoader;
import com.dinosurvival.model.NPCAnimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class InitialCritterSpawnTest {

    @Test
    public void testInitialCritterSpawnAndPlacement() throws Exception {
        StatsLoader.load(Path.of("conf"), "Morrison");
        Game game = new Game();
        game.start("Morrison", "Allosaurus");
        java.util.Map<String, java.util.Map<String, Object>> critters = StatsLoader.getCritterStats();
        com.dinosurvival.game.Map map = game.getMap();
        for (java.util.Map.Entry<String, java.util.Map<String, Object>> entry : critters.entrySet()) {
            String name = entry.getKey();
            java.util.Map<String, Object> stats = entry.getValue();
            int count = 0;
            for (int y = 0; y < map.getHeight(); y++) {
                for (int x = 0; x < map.getWidth(); x++) {
                    List<NPCAnimal> cell = map.getAnimals(x, y);
                    int perTile = 0;
                    for (NPCAnimal npc : cell) {
                        if (name.equals(npc.getName())) {
                            perTile++;
                        }
                    }
                    if (perTile > 0) {
                        Terrain t = map.terrainAt(x, y);
                        boolean canWalk = !Boolean.FALSE.equals(stats.get("can_walk"));
                        if (canWalk) {
                            Assertions.assertNotEquals(Terrain.LAKE, t);
                        } else {
                            Assertions.assertEquals(Terrain.LAKE, t);
                        }
                        Assertions.assertEquals(1, perTile);
                        count += perTile;
                    }
                }
            }
            int maxInd = 0;
            Object val = stats.get("maximum_individuals");
            if (val instanceof Number n) {
                maxInd = n.intValue();
            }
            Assertions.assertTrue(count <= maxInd / 2);
        }
    }
}
