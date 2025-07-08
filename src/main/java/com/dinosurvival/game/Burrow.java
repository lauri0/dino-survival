package com.dinosurvival.game;

/**
 * Mirrors the Python {@code Burrow} dataclass. Represents a small ground
 * burrow that may contain an animal.
 */
public class Burrow {

    /** Whether the burrow currently has an occupant. */
    private boolean full = true;

    /** Progress towards being refilled if empty. */
    private double progress = 0.0;

    public Burrow() {
        // default constructor
    }

    public Burrow(boolean full) {
        this.full = full;
    }

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }
}
