package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import java.util.ArrayList;
import java.util.List;

public class Game {
    private Map map;
    private List<NPCAnimal> animals = new ArrayList<>();

    public void start() {
        map = new Map(10, 10);
        // spawn a few animals for demo
        animals.add(new NPCAnimal());
    }

    public Map getMap() {
        return map;
    }

    public List<NPCAnimal> getAnimals() {
        return animals;
    }
}
