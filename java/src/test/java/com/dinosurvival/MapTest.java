package com.dinosurvival;

import com.dinosurvival.game.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MapTest {
    @Test
    public void testMapGeneration() {
        Map map = new Map(5, 5);
        Assertions.assertEquals(5, map.getWidth());
        Assertions.assertEquals(5, map.getHeight());
        Assertions.assertNotNull(map.getTerrain(0,0));
    }
}
