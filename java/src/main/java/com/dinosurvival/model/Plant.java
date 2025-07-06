package com.dinosurvival.model;

public class Plant {
    private String name;
    private PlantStats stats;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PlantStats getStats() {
        return stats;
    }

    public void setStats(PlantStats stats) {
        this.stats = stats;
    }
}
