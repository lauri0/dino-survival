package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;

import java.util.List;

/**
 * Manages the player dinosaur state and growth related operations.
 */
public class PlayerManager {

    private DinosaurStats player;

    /** Get the managed player statistics instance. */
    public DinosaurStats getPlayer() {
        return player;
    }

    /** Set the player statistics instance. */
    public void setPlayer(DinosaurStats player) {
        this.player = player;
    }

    /** Initialise the player state using the provided base statistics. */
    public void initialisePlayer(DinosaurStats base) {
        player = base;
        player.setWeight(player.getHatchlingWeight());
        double pct = player.getAdultWeight() > 0
                ? player.getWeight() / player.getAdultWeight() : 1.0;
        pct = Math.max(0.0, Math.min(1.0, pct));
        player.setAttack(player.getAdultAttack() * pct);
        player.setMaxHp(player.getAdultHp() * pct);
        player.setHp(player.getMaxHp());
        // When starting the game the player's weight equals the hatchling
        // weight so the speed should exactly match the hatchling value.
        player.setSpeed(player.getHatchlingSpeed());
    }

    /** Linear interpolation between hatchling and adult values based on weight. */
    private double statFromWeight(double weight, double adultWeight,
                                  double hatchVal, double adultVal) {
        return CombatUtils.statFromWeight(weight, adultWeight, hatchVal, adultVal);
    }

    /** Maximum potential weight gain this turn. */
    public double maxGrowthGain() {
        double weight = player.getWeight();
        double adult = player.getAdultWeight();
        if (weight >= adult) return 0.0;
        double maxWeight = adult * 1.05;
        double r = player.getGrowthRate();
        if (r == 0.0) r = 0.35;
        double gain = r * weight * (1 - weight / maxWeight);
        return Math.min(gain, adult - weight);
    }

    /** Apply growth and update derived stats. */
    public double[] applyGrowth(double available) {
        double maxGain = maxGrowthGain();
        double weightGain = Math.min(available, maxGain);
        double oldWeight = player.getWeight();
        player.setWeight(Math.min(player.getWeight() + weightGain, player.getAdultWeight()));
        if (player.getAdultWeight() > 0) {
            double pct = player.getWeight() / player.getAdultWeight();
            pct = Math.max(0.0, Math.min(pct, 1.0));
            player.setAttack(player.getAdultAttack() * pct);
            double oldMax = statFromWeight(oldWeight, player.getAdultWeight(),
                    player.getHatchlingHp(), player.getAdultHp());
            double newMax = statFromWeight(player.getWeight(), player.getAdultWeight(),
                    player.getHatchlingHp(), player.getAdultHp());
            double ratio = oldMax <= 0 ? 1.0 : player.getHp() / oldMax;
            player.setMaxHp(newMax);
            player.setHp(newMax * ratio);
            player.setSpeed(statFromWeight(player.getWeight(), player.getAdultWeight(),
                    player.getHatchlingSpeed(), player.getAdultSpeed()));
        }
        return new double[]{weightGain, maxGain};
    }

    /** Determine if the player can currently lay eggs. */
    public boolean canPlayerLayEggs(Map map, int x, int y) {
        List<NPCAnimal> animals = map.getAnimals(x, y);
        return player.getWeight() >= player.getAdultWeight()
                && player.getEnergy() >= 80
                && player.getHp() >= player.getMaxHp() * 0.8
                && player.getTurnsUntilLayEggs() == 0
                && animals.size() < 4;
    }

    /** Effective speed value for the player dinosaur. */
    public double playerEffectiveSpeed(Map map, int x, int y) {
        double speed = player.getSpeed();
        Terrain terrain = map.terrainAt(x, y);
        double boost = 0.0;
        if (terrain == Terrain.LAKE) {
            boost = player.getAquaticBoost();
        } else if (terrain == Terrain.SWAMP) {
            boost = player.getAquaticBoost() / 2.0;
        }
        speed *= 1 + boost / 100.0;
        if (player.getAbilities().contains("ambush")) {
            speed *= 1 + Math.min(player.getAmbushStreak(), 3) * 0.05;
        }
        if (player.getBrokenBone() > 0) {
            speed *= 0.5;
        }
        return Math.max(speed, 0.1);
    }

    /** Determine the growth stage description for the player dinosaur. */
    public String playerGrowthStage() {
        double adult = player.getAdultWeight();
        if (adult <= 0) {
            return "Adult";
        }
        double pct = player.getWeight() / adult;
        if (pct <= 0.10) {
            return "Hatchling";
        }
        if (pct <= 1.0 / 3.0) {
            return "Juvenile";
        }
        if (pct <= 2.0 / 3.0) {
            return "Sub-Adult";
        }
        return "Adult";
    }
}
