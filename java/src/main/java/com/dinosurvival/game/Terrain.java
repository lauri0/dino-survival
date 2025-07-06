package com.dinosurvival.game;

public enum Terrain {
    DESERT("desert"),
    DESERT_FLOODED("desert_flooded"),
    TOXIC_BADLANDS("toxic_badlands"),
    PLAINS("plains"),
    PLAINS_FLOODED("plains_flooded"),
    WOODLANDS("woodlands"),
    WOODLANDS_FLOODED("woodlands_flooded"),
    FOREST("forest"),
    FOREST_FLOODED("forest_flooded"),
    FOREST_FIRE("forest_fire"),
    FOREST_BURNT("forest_burnt"),
    HIGHLAND_FOREST("highland_forest"),
    HIGHLAND_FOREST_FIRE("highland_forest_fire"),
    HIGHLAND_FOREST_BURNT("highland_forest_burnt"),
    SWAMP("swamp"),
    SWAMP_FLOODED("swamp_flooded"),
    LAKE("lake"),
    MOUNTAIN("mountain"),
    VOLCANO("volcano"),
    VOLCANO_ERUPTING("volcano_erupting"),
    LAVA("lava"),
    SOLIDIFIED_LAVA_FIELD("solidified_lava_field");

    private final String name;

    Terrain(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
