package com.dinosurvival.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Java equivalent of the Python settings module. Provides pre-defined
 * {@link Setting} instances for each geologic formation.
 */
public final class Settings {

    /** Configuration for the Morrison Formation. */
    public static final Setting MORRISON;
    /** Configuration for the Hell Creek formation. */
    public static final Setting HELL_CREEK;

    static {
        MORRISON = buildMorrison();
        HELL_CREEK = buildHellCreek();
    }

    private Settings() {
        // utility class
    }

    /**
     * Return the {@link Setting} for the given formation name. Defaults to
     * {@link #MORRISON} if the name is unknown.
     */
    public static Setting forFormation(String formation) {
        if (formation == null) {
            return MORRISON;
        }
        String f = formation.trim().toLowerCase();
        if (f.equals("hell creek")) {
            return HELL_CREEK;
        }
        if (f.equals("morrison") || f.equals("morrison formation")) {
            return MORRISON;
        }
        return MORRISON;
    }

    private static Setting buildMorrison() {
        Map<String, Map<String, Object>> dinos = Map.of(
            "Allosaurus", Map.of("energy_threshold", 0, "growth_stages", 3),
            "Ceratosaurus", Map.of("energy_threshold", 0, "growth_stages", 3),
            "Torvosaurus", Map.of("energy_threshold", 0, "growth_stages", 3),
            "Ornitholestes", Map.of("energy_threshold", 0, "growth_stages", 3)
        );

        Map<String, Terrain> terrains = Map.ofEntries(
            Map.entry("desert", Terrain.DESERT),
            Map.entry("desert_flooded", Terrain.DESERT_FLOODED),
            Map.entry("toxic_badlands", Terrain.TOXIC_BADLANDS),
            Map.entry("plains", Terrain.PLAINS),
            Map.entry("plains_flooded", Terrain.PLAINS_FLOODED),
            Map.entry("woodlands", Terrain.WOODLANDS),
            Map.entry("woodlands_flooded", Terrain.WOODLANDS_FLOODED),
            Map.entry("forest", Terrain.FOREST),
            Map.entry("forest_flooded", Terrain.FOREST_FLOODED),
            Map.entry("forest_fire", Terrain.FOREST_FIRE),
            Map.entry("forest_burnt", Terrain.FOREST_BURNT),
            Map.entry("highland_forest", Terrain.HIGHLAND_FOREST),
            Map.entry("highland_forest_fire", Terrain.HIGHLAND_FOREST_FIRE),
            Map.entry("highland_forest_burnt", Terrain.HIGHLAND_FOREST_BURNT),
            Map.entry("swamp", Terrain.SWAMP),
            Map.entry("swamp_flooded", Terrain.SWAMP_FLOODED),
            Map.entry("lake", Terrain.LAKE),
            Map.entry("mountain", Terrain.MOUNTAIN),
            Map.entry("volcano", Terrain.VOLCANO),
            Map.entry("volcano_erupting", Terrain.VOLCANO_ERUPTING),
            Map.entry("lava", Terrain.LAVA),
            Map.entry("solidified_lava_field", Terrain.SOLIDIFIED_LAVA_FIELD)
        );

        Map<String, String> images = Map.ofEntries(
            Map.entry("desert", "desert.png"),
            Map.entry("desert_flooded", "desert_flooded.png"),
            Map.entry("toxic_badlands", "toxic_badlands.png"),
            Map.entry("plains", "plains.png"),
            Map.entry("plains_flooded", "plains_flooded.png"),
            Map.entry("woodlands", "woodlands.png"),
            Map.entry("woodlands_flooded", "woodlands_flooded.png"),
            Map.entry("forest", "forest.png"),
            Map.entry("forest_flooded", "forest_flooded.png"),
            Map.entry("forest_fire", "forest_fire.png"),
            Map.entry("forest_burnt", "forest_burnt.png"),
            Map.entry("highland_forest", "highland_forest.png"),
            Map.entry("highland_forest_fire", "highland_forest_fire.png"),
            Map.entry("highland_forest_burnt", "highland_forest_burnt.png"),
            Map.entry("swamp", "swamp.png"),
            Map.entry("swamp_flooded", "swamp_flooded.png"),
            Map.entry("lake", "lake.png"),
            Map.entry("mountain", "mountain.png"),
            Map.entry("volcano", "volcano.png"),
            Map.entry("volcano_erupting", "volcano_erupting.png"),
            Map.entry("lava", "lava.png"),
            Map.entry("solidified_lava_field", "solidified_lava_field.png")
        );

        Map<String, Double> heights = Map.of(
            "low", 0.3,
            "normal", 0.4,
            "hilly", 0.2,
            "mountain", 0.1
        );

        Map<String, Double> humidity = Map.of(
            "arid", 0.35,
            "normal", 0.4,
            "humid", 0.25
        );

        return new Setting(
            "Morrison Formation",
            "Morrison",
            new HashMap<>(dinos),
            new HashMap<>(terrains),
            new HashMap<>(images),
            new HashMap<>(heights),
            new HashMap<>(humidity),
            0
        );
    }

    private static Setting buildHellCreek() {
        Map<String, Map<String, Object>> dinos = Map.of(
            "Tyrannosaurus", Map.of("energy_threshold", 0, "growth_stages", 4),
            "Acheroraptor", Map.of("energy_threshold", 0, "growth_stages", 3),
            "Pectinodon", Map.of("energy_threshold", 0, "growth_stages", 3)
        );

        Map<String, Terrain> terrains = Map.ofEntries(
            Map.entry("desert", Terrain.DESERT),
            Map.entry("desert_flooded", Terrain.DESERT_FLOODED),
            Map.entry("toxic_badlands", Terrain.TOXIC_BADLANDS),
            Map.entry("plains", Terrain.PLAINS),
            Map.entry("plains_flooded", Terrain.PLAINS_FLOODED),
            Map.entry("woodlands", Terrain.WOODLANDS),
            Map.entry("woodlands_flooded", Terrain.WOODLANDS_FLOODED),
            Map.entry("forest", Terrain.FOREST),
            Map.entry("forest_flooded", Terrain.FOREST_FLOODED),
            Map.entry("forest_fire", Terrain.FOREST_FIRE),
            Map.entry("forest_burnt", Terrain.FOREST_BURNT),
            Map.entry("highland_forest", Terrain.HIGHLAND_FOREST),
            Map.entry("highland_forest_fire", Terrain.HIGHLAND_FOREST_FIRE),
            Map.entry("highland_forest_burnt", Terrain.HIGHLAND_FOREST_BURNT),
            Map.entry("swamp", Terrain.SWAMP),
            Map.entry("swamp_flooded", Terrain.SWAMP_FLOODED),
            Map.entry("lake", Terrain.LAKE),
            Map.entry("mountain", Terrain.MOUNTAIN),
            Map.entry("volcano", Terrain.VOLCANO),
            Map.entry("volcano_erupting", Terrain.VOLCANO_ERUPTING),
            Map.entry("lava", Terrain.LAVA),
            Map.entry("solidified_lava_field", Terrain.SOLIDIFIED_LAVA_FIELD)
        );

        Map<String, String> images = Map.ofEntries(
            Map.entry("desert", "desert.png"),
            Map.entry("desert_flooded", "desert_flooded.png"),
            Map.entry("toxic_badlands", "toxic_badlands.png"),
            Map.entry("plains", "plains.png"),
            Map.entry("plains_flooded", "plains_flooded.png"),
            Map.entry("woodlands", "woodlands.png"),
            Map.entry("woodlands_flooded", "woodlands_flooded.png"),
            Map.entry("forest", "forest.png"),
            Map.entry("forest_flooded", "forest_flooded.png"),
            Map.entry("forest_fire", "forest_fire.png"),
            Map.entry("forest_burnt", "forest_burnt.png"),
            Map.entry("highland_forest", "highland_forest.png"),
            Map.entry("highland_forest_fire", "highland_forest_fire.png"),
            Map.entry("highland_forest_burnt", "highland_forest_burnt.png"),
            Map.entry("swamp", "swamp.png"),
            Map.entry("swamp_flooded", "swamp_flooded.png"),
            Map.entry("lake", "lake.png"),
            Map.entry("mountain", "mountain.png"),
            Map.entry("volcano", "volcano.png"),
            Map.entry("volcano_erupting", "volcano_erupting.png"),
            Map.entry("lava", "lava.png"),
            Map.entry("solidified_lava_field", "solidified_lava_field.png")
        );

        Map<String, Double> heights = Map.of(
            "low", 0.3,
            "normal", 0.45,
            "hilly", 0.15,
            "mountain", 0.1
        );

        Map<String, Double> humidity = Map.of(
            "arid", 0.2,
            "normal", 0.5,
            "humid", 0.3
        );

        return new Setting(
            "Hell Creek",
            "Hell Creek",
            new HashMap<>(dinos),
            new HashMap<>(terrains),
            new HashMap<>(images),
            new HashMap<>(heights),
            new HashMap<>(humidity),
            5
        );
    }
}
