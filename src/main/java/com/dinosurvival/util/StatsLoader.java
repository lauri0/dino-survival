package com.dinosurvival.util;

import com.dinosurvival.model.Diet;
import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.PlantStats;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import com.dinosurvival.util.Constants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loader for dinosaur, plant and critter statistics stored in the YAML files
 * under the {@code dinosurvival} directory.
 */
public class StatsLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);


    private static Map<String, DinosaurStats> dinoStats = new HashMap<>();
    private static Map<String, PlantStats> plantStats = new HashMap<>();
    private static Map<String, Map<String, Object>> critterStats = new HashMap<>();
    private static String currentFormation = null;

    private StatsLoader() {
        // utility class
    }

    public static Map<String, DinosaurStats> getDinoStats() {
        return dinoStats;
    }

    public static Map<String, PlantStats> getPlantStats() {
        return plantStats;
    }

    public static Map<String, Map<String, Object>> getCritterStats() {
        return critterStats;
    }

    /**
     * Load statistics for the given formation from the provided base directory.
     *
     * @param baseDir   directory containing the YAML files
     * @param formation geologic formation name (e.g. "Morrison")
     */
    public static void load(Path baseDir, String formation) throws IOException {
        if (formation.equals(currentFormation)) {
            return;
        }
        String suffix = formation.toLowerCase().replace(" ", "_");
        Path dinoFile = baseDir.resolve("dino_stats_" + suffix + ".yaml");
        Path plantFile = baseDir.resolve("plant_stats_" + suffix + ".yaml");
        Path critterFile = baseDir.resolve("critter_stats_" + suffix + ".yaml");

        // Dinosaurs
        try (InputStream in = Files.newInputStream(dinoFile)) {
            TypeReference<Map<String, Map<String, Object>>> ref =
                    new TypeReference<Map<String, Map<String, Object>>>() {};
            Map<String, Map<String, Object>> raw = YamlLoader.load(in, ref);
            dinoStats = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> e : raw.entrySet()) {
                Map<String, Object> map = new HashMap<>(e.getValue());
                applyDinoDefaults(map);
                DinosaurStats stats = MAPPER.convertValue(map, DinosaurStats.class);
                dinoStats.put(e.getKey(), stats);
            }
        }

        // Plants
        try (InputStream in = Files.newInputStream(plantFile)) {
            TypeReference<Map<String, Map<String, Object>>> ref =
                    new TypeReference<Map<String, Map<String, Object>>>() {};
            Map<String, Map<String, Object>> raw = YamlLoader.load(in, ref);
            plantStats = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> e : raw.entrySet()) {
                PlantStats ps = MAPPER.convertValue(e.getValue(), PlantStats.class);
                plantStats.put(e.getKey(), ps);
            }
        }

        // Critters
        critterStats = new HashMap<>();
        if (Files.exists(critterFile)) {
            try (InputStream in = Files.newInputStream(critterFile)) {
                TypeReference<Map<String, Map<String, Object>>> ref =
                        new TypeReference<Map<String, Map<String, Object>>>() {};
                Map<String, Map<String, Object>> raw = YamlLoader.load(in, ref);
                critterStats.putAll(raw);
            }
        }

        currentFormation = formation;
    }

    private static void applyDinoDefaults(Map<String, Object> stats) {
        Object atk = stats.remove("attack");
        if (atk != null && !stats.containsKey("adult_attack")) {
            stats.put("adult_attack", getDouble(atk));
        }
        Object hp = stats.remove("hp");
        if (hp != null && !stats.containsKey("adult_hp")) {
            stats.put("adult_hp", getDouble(hp));
        }

        Object diet = stats.get("diet");
        if (diet instanceof List<?> list) {
            List<Diet> diets = new ArrayList<>();
            for (Object item : list) {
                diets.add(Diet.valueOf(item.toString().toUpperCase()));
            }
            stats.put("diet", diets);
        }

        Object abilities = stats.get("abilities");
        if (abilities instanceof List<?> list) {
            List<String> abil = new ArrayList<>();
            for (Object a : list) {
                abil.add(a.toString());
            }
            stats.put("abilities", abil);
        }

        Object pref = stats.get("preferred_biomes");
        if (pref instanceof List<?> list) {
            List<String> biomes = new ArrayList<>();
            for (Object b : list) {
                biomes.add(b.toString());
            }
            stats.put("preferred_biomes", biomes);
        }

        double adultWeight = getDouble(stats.get("adult_weight"));
        double hatchWeight;
        if (stats.containsKey("hatchling_weight")) {
            hatchWeight = Math.max(getDouble(stats.get("hatchling_weight")), Constants.MIN_HATCHING_WEIGHT);
        } else {
            hatchWeight = Math.max(adultWeight / Constants.HATCHLING_WEIGHT_DIVISOR, Constants.MIN_HATCHING_WEIGHT);
        }
        stats.put("hatchling_weight", hatchWeight);

        double adultSpeed = getDouble(stats.get("adult_speed"));
        if (!stats.containsKey("hatchling_speed")) {
            stats.put("hatchling_speed", adultSpeed * Constants.HATCHLING_SPEED_MULTIPLIER);
        }

        double adultDrain = getDouble(stats.get("adult_energy_drain"));
        if (!stats.containsKey("hatchling_energy_drain")) {
            stats.put(
                    "hatchling_energy_drain",
                    adultDrain / Constants.HATCHLING_ENERGY_DRAIN_DIVISOR);
        }
    }

    private static double getDouble(Object val) {
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        if (val != null) {
            try {
                return Double.parseDouble(val.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }
}
