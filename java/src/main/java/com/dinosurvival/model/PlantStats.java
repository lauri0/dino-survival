package com.dinosurvival.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mirrors the Python {@code PlantStats} dataclass.
 */
public class PlantStats {

    private String name;
    private List<String> formations = new ArrayList<>();
    private String image;
    private double weight;
    private Map<String, Double> growthChance = new HashMap<>();

    public PlantStats() {
        // default constructor
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getFormations() {
        return formations;
    }

    public void setFormations(List<String> formations) {
        this.formations = formations;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Map<String, Double> getGrowthChance() {
        return growthChance;
    }

    public void setGrowthChance(Map<String, Double> growthChance) {
        this.growthChance = growthChance;
    }
}
