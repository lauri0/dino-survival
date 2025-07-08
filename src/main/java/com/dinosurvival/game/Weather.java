package com.dinosurvival.game;

/**
 * Mirrors the Python {@code Weather} dataclass. Holds values used for
 * determining turn effects from the current weather.
 */
public class Weather {

    private String name;
    private String icon;
    private double floodChance;
    private double playerHydrationMult = 1.0;
    private double playerEnergyMult = 1.0;
    private double npcEnergyMult = 1.0;

    public Weather() {
        // default constructor
    }

    public Weather(String name, String icon, double floodChance) {
        this(name, icon, floodChance, 1.0, 1.0, 1.0);
    }

    public Weather(
            String name,
            String icon,
            double floodChance,
            double playerHydrationMult,
            double playerEnergyMult,
            double npcEnergyMult) {
        this.name = name;
        this.icon = icon;
        this.floodChance = floodChance;
        this.playerHydrationMult = playerHydrationMult;
        this.playerEnergyMult = playerEnergyMult;
        this.npcEnergyMult = npcEnergyMult;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public double getFloodChance() {
        return floodChance;
    }

    public void setFloodChance(double floodChance) {
        this.floodChance = floodChance;
    }

    public double getPlayerHydrationMult() {
        return playerHydrationMult;
    }

    public void setPlayerHydrationMult(double playerHydrationMult) {
        this.playerHydrationMult = playerHydrationMult;
    }

    public double getPlayerEnergyMult() {
        return playerEnergyMult;
    }

    public void setPlayerEnergyMult(double playerEnergyMult) {
        this.playerEnergyMult = playerEnergyMult;
    }

    public double getNpcEnergyMult() {
        return npcEnergyMult;
    }

    public void setNpcEnergyMult(double npcEnergyMult) {
        this.npcEnergyMult = npcEnergyMult;
    }
}
