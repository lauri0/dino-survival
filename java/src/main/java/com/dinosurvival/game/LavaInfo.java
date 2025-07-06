package com.dinosurvival.game;

/** Simple holder for lava spread state. */
public class LavaInfo {
    private int steps;
    private int cooldown;

    public LavaInfo(int steps, int cooldown) {
        this.steps = steps;
        this.cooldown = cooldown;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getCooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }
}
