package com.dinosurvival.util;

public class Setting {
    private final String name;
    private final String formation;

    public Setting(String name, String formation) {
        this.name = name;
        this.formation = formation;
    }

    public String getName() {
        return name;
    }

    public String getFormation() {
        return formation;
    }
}
