package com.dinosurvival.model;

public class DinosaurData {
    private String name;
    private DinosaurStats stats;
    private Diet diet;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DinosaurStats getStats() {
        return stats;
    }

    public void setStats(DinosaurStats stats) {
        this.stats = stats;
    }

    public Diet getDiet() {
        return diet;
    }

    public void setDiet(Diet diet) {
        this.diet = diet;
    }
}
