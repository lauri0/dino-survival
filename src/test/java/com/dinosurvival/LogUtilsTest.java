package com.dinosurvival;

import com.dinosurvival.util.LogUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class LogUtilsTest {
    @Test
    public void testLoggingAndStats(@TempDir Path dir) throws Exception {
        LogUtils.setBaseDir(dir);

        Map<String, int[]> hunts = new HashMap<>();
        hunts.put("Stegosaurus", new int[]{1, 1});
        hunts.put("Triceratops", new int[]{2, 0});
        LogUtils.updateHunterLog("Morrison", "Allosaurus", hunts);

        LogUtils.appendGameLog("Morrison", "Allosaurus", 10, 50.0, true);
        LogUtils.appendEventLog("Test event");

        int[] dino = LogUtils.getDinoGameStats("Morrison", "Allosaurus");
        Assertions.assertArrayEquals(new int[]{1, 0}, dino);

        int[] player = LogUtils.getPlayerStats();
        Assertions.assertArrayEquals(new int[]{1, 1, 1, 10}, player);

        Assertions.assertTrue(Files.exists(dir.resolve("game_log.txt")));
        Assertions.assertTrue(Files.exists(dir.resolve("hunter_stats.yaml")));
    }
}
