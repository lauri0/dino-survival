package com.dinosurvival.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;

public class YamlLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    public static <T> T load(InputStream in, Class<T> clazz) throws IOException {
        return MAPPER.readValue(in, clazz);
    }

    /**
     * Convenience method for loading generic types such as maps or lists.
     */
    public static <T> T load(InputStream in, TypeReference<T> ref) throws IOException {
        return MAPPER.readValue(in, ref);
    }
}
