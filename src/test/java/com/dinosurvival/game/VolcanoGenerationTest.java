package com.dinosurvival.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class VolcanoGenerationTest {
    @Test
    public void testVolcanoGenerationRate() {
        Map map = new Map(40, 40, 0L);
        int volcano = 0;
        int mountain = 0;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                Terrain t = map.terrainAt(x, y);
                if (t == Terrain.VOLCANO) {
                    volcano++;
                } else if (t == Terrain.MOUNTAIN) {
                    mountain++;
                }
            }
        }
        double ratio = volcano / (double) (volcano + mountain);
        Assertions.assertTrue(ratio >= 0.4 && ratio <= 0.6,
                "Volcano to mountain ratio out of expected range: " + ratio);
    }
}
