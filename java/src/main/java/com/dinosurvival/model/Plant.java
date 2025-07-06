package com.dinosurvival.model;

/**
 * A plant instance present on the map. Mirrors the Python {@code Plant} dataclass.
 */
public class Plant {

    private String name;
    private double weight;

    public Plant() {
        // default constructor
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
