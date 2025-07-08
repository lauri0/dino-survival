package com.dinosurvival.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirror of the Python {@code Setting} dataclass. Stores terrain information
 * and other configuration for a particular geologic formation.
 */
public class Setting {

    private String name;
    private String formation;
    private Map<String, Map<String, Object>> playableDinos = new HashMap<>();
    private Map<String, Terrain> terrains = new HashMap<>();
    private Map<String, String> biomeImages = new HashMap<>();
    private Map<String, Double> heightLevels = new HashMap<>();
    private Map<String, Double> humidityLevels = new HashMap<>();
    private int numBurrows = 0;

    public Setting() {
        // default constructor
    }

    public Setting(String name, String formation) {
        this.name = name;
        this.formation = formation;
    }

    public Setting(
            String name,
            String formation,
            Map<String, Map<String, Object>> playableDinos,
            Map<String, Terrain> terrains,
            Map<String, String> biomeImages,
            Map<String, Double> heightLevels,
            Map<String, Double> humidityLevels,
            int numBurrows) {
        this.name = name;
        this.formation = formation;
        if (playableDinos != null) {
            this.playableDinos = playableDinos;
        }
        if (terrains != null) {
            this.terrains = terrains;
        }
        if (biomeImages != null) {
            this.biomeImages = biomeImages;
        }
        if (heightLevels != null) {
            this.heightLevels = heightLevels;
        }
        if (humidityLevels != null) {
            this.humidityLevels = humidityLevels;
        }
        this.numBurrows = numBurrows;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormation() {
        return formation;
    }

    public void setFormation(String formation) {
        this.formation = formation;
    }

    public Map<String, Map<String, Object>> getPlayableDinos() {
        return playableDinos;
    }

    public void setPlayableDinos(Map<String, Map<String, Object>> playableDinos) {
        this.playableDinos = playableDinos;
    }

    public Map<String, Terrain> getTerrains() {
        return terrains;
    }

    public void setTerrains(Map<String, Terrain> terrains) {
        this.terrains = terrains;
    }

    public Map<String, String> getBiomeImages() {
        return biomeImages;
    }

    public void setBiomeImages(Map<String, String> biomeImages) {
        this.biomeImages = biomeImages;
    }

    public Map<String, Double> getHeightLevels() {
        return heightLevels;
    }

    public void setHeightLevels(Map<String, Double> heightLevels) {
        this.heightLevels = heightLevels;
    }

    public Map<String, Double> getHumidityLevels() {
        return humidityLevels;
    }

    public void setHumidityLevels(Map<String, Double> humidityLevels) {
        this.humidityLevels = humidityLevels;
    }

    public int getNumBurrows() {
        return numBurrows;
    }

    public void setNumBurrows(int numBurrows) {
        this.numBurrows = numBurrows;
    }
}
