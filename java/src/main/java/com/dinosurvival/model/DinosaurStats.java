package com.dinosurvival.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors the fields of the Python {@code DinosaurStats} dataclass so that
 * Java code can manipulate the same data easily.
 */
public class DinosaurStats {

    private String name;
    private int growthStages;

    private double hatchlingWeight = 0.0;
    private double adultWeight = 0.0;
    private double hatchlingAttack = 0.0;
    private double adultAttack = 0.0;
    private double hatchlingHp = 0.0;
    private double adultHp = 0.0;
    private double hatchlingSpeed = 0.0;
    private double adultSpeed = 0.0;
    private double hatchlingEnergyDrain = 0.0;
    private double adultEnergyDrain = 0.0;
    private double growthRate = 0.35;
    private double walkingEnergyDrainMultiplier = 1.0;
    private boolean canWalk = true;
    private boolean canBeJuvenile = true;
    private double initialSpawnMultiplier = 0.0;

    private double attack = 0.0;
    private double maxHp = 100.0;
    private double hp = 100.0;
    private double speed = 0.0;
    private double energy = 100.0;
    private double weight = 0.0;
    private double healthRegen = 0.0;
    private double hydration = 100.0;
    private double hydrationDrain = 0.0;
    private double aquaticBoost = 0.0;
    private boolean mated = false;
    private int turnsUntilLayEggs = 0;
    private List<Diet> diet = new ArrayList<>();
    private List<String> abilities = new ArrayList<>();
    private int ambushStreak = 0;
    private int bleeding = 0;
    private int brokenBone = 0;

    public DinosaurStats() {
        // default constructor
    }

    /* Helper methods matching the Python dataclass */
    public boolean isExhausted() {
        return energy <= 0;
    }

    public boolean isDehydrated() {
        return hydration <= 0;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGrowthStages() {
        return growthStages;
    }

    public void setGrowthStages(int growthStages) {
        this.growthStages = growthStages;
    }

    public double getHatchlingWeight() {
        return hatchlingWeight;
    }

    public void setHatchlingWeight(double hatchlingWeight) {
        this.hatchlingWeight = hatchlingWeight;
    }

    public double getAdultWeight() {
        return adultWeight;
    }

    public void setAdultWeight(double adultWeight) {
        this.adultWeight = adultWeight;
    }

    public double getHatchlingAttack() {
        return hatchlingAttack;
    }

    public void setHatchlingAttack(double hatchlingAttack) {
        this.hatchlingAttack = hatchlingAttack;
    }

    public double getAdultAttack() {
        return adultAttack;
    }

    public void setAdultAttack(double adultAttack) {
        this.adultAttack = adultAttack;
    }

    public double getHatchlingHp() {
        return hatchlingHp;
    }

    public void setHatchlingHp(double hatchlingHp) {
        this.hatchlingHp = hatchlingHp;
    }

    public double getAdultHp() {
        return adultHp;
    }

    public void setAdultHp(double adultHp) {
        this.adultHp = adultHp;
    }

    public double getHatchlingSpeed() {
        return hatchlingSpeed;
    }

    public void setHatchlingSpeed(double hatchlingSpeed) {
        this.hatchlingSpeed = hatchlingSpeed;
    }

    public double getAdultSpeed() {
        return adultSpeed;
    }

    public void setAdultSpeed(double adultSpeed) {
        this.adultSpeed = adultSpeed;
    }

    public double getHatchlingEnergyDrain() {
        return hatchlingEnergyDrain;
    }

    public void setHatchlingEnergyDrain(double hatchlingEnergyDrain) {
        this.hatchlingEnergyDrain = hatchlingEnergyDrain;
    }

    public double getAdultEnergyDrain() {
        return adultEnergyDrain;
    }

    public void setAdultEnergyDrain(double adultEnergyDrain) {
        this.adultEnergyDrain = adultEnergyDrain;
    }

    public double getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(double growthRate) {
        this.growthRate = growthRate;
    }

    public double getWalkingEnergyDrainMultiplier() {
        return walkingEnergyDrainMultiplier;
    }

    public void setWalkingEnergyDrainMultiplier(double walkingEnergyDrainMultiplier) {
        this.walkingEnergyDrainMultiplier = walkingEnergyDrainMultiplier;
    }

    public boolean isCanWalk() {
        return canWalk;
    }

    public void setCanWalk(boolean canWalk) {
        this.canWalk = canWalk;
    }

    public boolean isCanBeJuvenile() {
        return canBeJuvenile;
    }

    public void setCanBeJuvenile(boolean canBeJuvenile) {
        this.canBeJuvenile = canBeJuvenile;
    }

    public double getAttack() {
        return attack;
    }

    public void setAttack(double attack) {
        this.attack = attack;
    }

    public double getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(double maxHp) {
        this.maxHp = maxHp;
    }

    public double getHp() {
        return hp;
    }

    public void setHp(double hp) {
        this.hp = hp;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getHealthRegen() {
        return healthRegen;
    }

    public void setHealthRegen(double healthRegen) {
        this.healthRegen = healthRegen;
    }

    public double getHydration() {
        return hydration;
    }

    public void setHydration(double hydration) {
        this.hydration = hydration;
    }

    public double getHydrationDrain() {
        return hydrationDrain;
    }

    public void setHydrationDrain(double hydrationDrain) {
        this.hydrationDrain = hydrationDrain;
    }

    public double getAquaticBoost() {
        return aquaticBoost;
    }

    public void setAquaticBoost(double aquaticBoost) {
        this.aquaticBoost = aquaticBoost;
    }

    public double getInitialSpawnMultiplier() {
        return initialSpawnMultiplier;
    }

    public void setInitialSpawnMultiplier(double initialSpawnMultiplier) {
        this.initialSpawnMultiplier = initialSpawnMultiplier;
    }

    public boolean isMated() {
        return mated;
    }

    public void setMated(boolean mated) {
        this.mated = mated;
    }

    public int getTurnsUntilLayEggs() {
        return turnsUntilLayEggs;
    }

    public void setTurnsUntilLayEggs(int turnsUntilLayEggs) {
        this.turnsUntilLayEggs = turnsUntilLayEggs;
    }

    public List<Diet> getDiet() {
        return diet;
    }

    public void setDiet(List<Diet> diet) {
        this.diet = diet;
    }

    public List<String> getAbilities() {
        return abilities;
    }

    public void setAbilities(List<String> abilities) {
        this.abilities = abilities;
    }

    public int getAmbushStreak() {
        return ambushStreak;
    }

    public void setAmbushStreak(int ambushStreak) {
        this.ambushStreak = ambushStreak;
    }

    public int getBleeding() {
        return bleeding;
    }

    public void setBleeding(int bleeding) {
        this.bleeding = bleeding;
    }

    public int getBrokenBone() {
        return brokenBone;
    }

    public void setBrokenBone(int brokenBone) {
        this.brokenBone = brokenBone;
    }
}
