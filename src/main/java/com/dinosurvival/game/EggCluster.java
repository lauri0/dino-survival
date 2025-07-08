package com.dinosurvival.game;

/**
 * Mirrors the Python {@code EggCluster} dataclass. Represents a group of eggs
 * laid by a particular species.
 */
public class EggCluster {

    private String species;
    private int number;
    private double weight;
    private int turnsUntilHatch;
    private boolean descendant;

    public EggCluster() {
        // default constructor
    }

    public EggCluster(String species, int number, double weight, int turnsUntilHatch) {
        this(species, number, weight, turnsUntilHatch, false);
    }

    public EggCluster(
            String species,
            int number,
            double weight,
            int turnsUntilHatch,
            boolean descendant) {
        this.species = species;
        this.number = number;
        this.weight = weight;
        this.turnsUntilHatch = turnsUntilHatch;
        this.descendant = descendant;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getTurnsUntilHatch() {
        return turnsUntilHatch;
    }

    public void setTurnsUntilHatch(int turnsUntilHatch) {
        this.turnsUntilHatch = turnsUntilHatch;
    }

    public boolean isDescendant() {
        return descendant;
    }

    public void setDescendant(boolean descendant) {
        this.descendant = descendant;
    }
}
