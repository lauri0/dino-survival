package com.dinosurvival;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.PlantStats;
import com.dinosurvival.util.StatsLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StatsLoaderTest {
    @Test
    public void testLoadStats() throws Exception {
        Path base = Path.of("..", "conf");
        StatsLoader.load(base, "Morrison");
        DinosaurStats allo = StatsLoader.getDinoStats().get("Allosaurus");
        Assertions.assertNotNull(allo);
        Assertions.assertTrue(allo.getHatchlingWeight() >= 2.0);
        Assertions.assertFalse(allo.getPreferredBiomes().isEmpty());
        PlantStats ferns = StatsLoader.getPlantStats().get("Ferns");
        Assertions.assertNotNull(ferns);
        Assertions.assertEquals("Ferns", ferns.getName());
    }
}
