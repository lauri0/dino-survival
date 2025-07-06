package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;

/**
 * Mirrors the Python {@code EncounterEntry} dataclass. Represents a single
 * encounter the player can interact with on the current tile.
 */
public class EncounterEntry {

    private NPCAnimal npc;
    private EggCluster eggs;
    private Burrow burrow;

    public EncounterEntry() {
        // default constructor
    }

    public EncounterEntry(NPCAnimal npc, EggCluster eggs, Burrow burrow) {
        this.npc = npc;
        this.eggs = eggs;
        this.burrow = burrow;
    }

    public NPCAnimal getNpc() {
        return npc;
    }

    public void setNpc(NPCAnimal npc) {
        this.npc = npc;
    }

    public EggCluster getEggs() {
        return eggs;
    }

    public void setEggs(EggCluster eggs) {
        this.eggs = eggs;
    }

    public Burrow getBurrow() {
        return burrow;
    }

    public void setBurrow(Burrow burrow) {
        this.burrow = burrow;
    }
}
