package com.dinosurvival.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads constant values from the {@code constants.ini} file.
 */
public final class Constants {
    public static final double WALKING_ENERGY_DRAIN_MULTIPLIER;
    public static final int HATCHLING_WEIGHT_DIVISOR;
    public static final int HATCHLING_SPEED_MULTIPLIER;
    public static final int HATCHLING_ENERGY_DRAIN_DIVISOR;
    public static final double MIN_HATCHING_WEIGHT;
    public static final int DESCENDANTS_TO_WIN;

    private Constants() {
        // utility class
    }

    static {
        Map<String, String> props = new HashMap<>();
        Path file = Path.of("conf", "constants.ini");
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";") || line.startsWith("[")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    props.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }

        WALKING_ENERGY_DRAIN_MULTIPLIER = Double.parseDouble(
                props.getOrDefault("walking_energy_drain_multiplier", "1.3"));
        HATCHLING_WEIGHT_DIVISOR = Integer.parseInt(
                props.getOrDefault("hatchling_weight_divisor", "1000"));
        HATCHLING_SPEED_MULTIPLIER = Integer.parseInt(
                props.getOrDefault("hatchling_speed_multiplier", "3"));
        HATCHLING_ENERGY_DRAIN_DIVISOR = Integer.parseInt(
                props.getOrDefault("hatchling_energy_drain_divisor", "2"));
        MIN_HATCHING_WEIGHT = Double.parseDouble(
                props.getOrDefault("min_hatching_weight", "2.0"));
        DESCENDANTS_TO_WIN = Integer.parseInt(
                props.getOrDefault("descendants_to_win", "5"));
    }
}
