package com.dinosurvival.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;

public class YamlLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    public static <T> T load(InputStream in, Class<T> clazz) throws IOException {
        return MAPPER.readValue(in, clazz);
    }
}
