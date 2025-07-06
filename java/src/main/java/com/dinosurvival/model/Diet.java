package com.dinosurvival.model;

public enum Diet {
    MEAT("meat"),
    INSECTS("insects"),
    FERNS("ferns"),
    CYCADS("cycads"),
    CONIFERS("conifers"),
    FRUITS("fruits");

    private final String value;

    Diet(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
