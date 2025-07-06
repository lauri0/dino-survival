package com.dinosurvival.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * State for a non-player animal present on the map. Mirrors the Python
 * {@code NPCAnimal} dataclass.
 */
public class NPCAnimal {

    private int id;
    private String name;
    private String sex;
    private double weight = 0.0;
    private int age = 0;
    private double energy = 100.0;
    private double maxHp = 100.0;
    private double hp = 100.0;
    private boolean alive = true;
    private double attack = 0.0;
    private double speed = 0.0;
    private String nextMove = "None";
    private int turnsUntilLayEggs = 0;
    private Map<String, Integer> hunts = new HashMap<>();
    private int eggClustersEaten = 0;
    private boolean isDescendant = false;
    private List<String> abilities = new ArrayList<>();
    private int ambushStreak = 0;
    private String lastAction = "None";
    private int bleeding = 0;
    private int brokenBone = 0;
    private int bleedWaitTarget = -1;
    private int bleedWaitTurns = 0;

    public NPCAnimal() {
        // default constructor
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
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

    public boolean isAlive() {
        return alive;
    }

    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    public double getAttack() {
        return attack;
    }

    public void setAttack(double attack) {
        this.attack = attack;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getNextMove() {
        return nextMove;
    }

    public void setNextMove(String nextMove) {
        this.nextMove = nextMove;
    }

    public int getTurnsUntilLayEggs() {
        return turnsUntilLayEggs;
    }

    public void setTurnsUntilLayEggs(int turnsUntilLayEggs) {
        this.turnsUntilLayEggs = turnsUntilLayEggs;
    }

    public Map<String, Integer> getHunts() {
        return hunts;
    }

    public void setHunts(Map<String, Integer> hunts) {
        this.hunts = hunts;
    }

    public int getEggClustersEaten() {
        return eggClustersEaten;
    }

    public void setEggClustersEaten(int eggClustersEaten) {
        this.eggClustersEaten = eggClustersEaten;
    }

    public boolean isDescendant() {
        return isDescendant;
    }

    public void setDescendant(boolean descendant) {
        isDescendant = descendant;
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

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
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

    public int getBleedWaitTarget() {
        return bleedWaitTarget;
    }

    public void setBleedWaitTarget(int bleedWaitTarget) {
        this.bleedWaitTarget = bleedWaitTarget;
    }

    public int getBleedWaitTurns() {
        return bleedWaitTurns;
    }

    public void setBleedWaitTurns(int bleedWaitTurns) {
        this.bleedWaitTurns = bleedWaitTurns;
    }
}
