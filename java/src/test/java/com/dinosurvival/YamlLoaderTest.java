package com.dinosurvival;

import com.dinosurvival.model.DinosaurData;
import com.dinosurvival.util.YamlLoader;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class YamlLoaderTest {
    @Test
    public void testLoadYaml() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/sample_dino.yaml")) {
            DinosaurData data = YamlLoader.load(in, DinosaurData.class);
            Assertions.assertEquals("Testosaurus", data.getName());
            Assertions.assertEquals(10, data.getStats().getHealth());
        }
    }
}
