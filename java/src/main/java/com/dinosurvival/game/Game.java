package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Very small Java translation of the Python {@code Game} class. It mirrors a
 * subset of the behaviour so the Swing UI can display a running world without
 * relying on the Python implementation.
 */
public class Game {
    private Map map;
    private DinosaurStats player;
    private int x;
    private int y;
    private Weather weather;
    private int weatherTurns;
    private final Random weatherRng = new Random(1);
    private final List<NPCAnimal> spawned = new ArrayList<>();
    private boolean won;
    private int turn;

    /** Number of descendants required to win the game. */
    public static final int DESCENDANTS_TO_WIN = 5;

    /**
     * Initialise the game world. Stats are loaded from the YAML files under the
     * {@code dinosurvival} directory and a 10x10 map is generated. The first
     * dinosaur listed in the stats is used as the player for simplicity.
     */
    public void start() {
        try {
            StatsLoader.load(Path.of("dinosurvival"), "Morrison");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        map = new Map(10, 10);
        // pick an arbitrary playable dinosaur
        if (!StatsLoader.getDinoStats().isEmpty()) {
            player = cloneStats(StatsLoader.getDinoStats().values().iterator().next());
            // start as a hatchling
            player.setWeight(player.getHatchlingWeight());
            double pct = player.getAdultWeight() > 0
                    ? player.getWeight() / player.getAdultWeight() : 1.0;
            pct = Math.max(0.0, Math.min(1.0, pct));
            player.setAttack(player.getAdultAttack() * pct);
            player.setMaxHp(player.getAdultHp() * pct);
            player.setHp(player.getMaxHp());
            player.setSpeed(player.getHatchlingSpeed());
        } else {
            player = new DinosaurStats();
        }

        // centre of the map
        x = map.getWidth() / 2;
        y = map.getHeight() / 2;
        map.reveal(x, y);
        weather = chooseWeather();
        weatherTurns = 0;
        populateAnimals();
    }

    private DinosaurStats cloneStats(DinosaurStats src) {
        DinosaurStats dst = new DinosaurStats();
        dst.setName(src.getName());
        dst.setGrowthStages(src.getGrowthStages());
        dst.setHatchlingWeight(src.getHatchlingWeight());
        dst.setAdultWeight(src.getAdultWeight());
        dst.setHatchlingAttack(src.getHatchlingAttack());
        dst.setAdultAttack(src.getAdultAttack());
        dst.setHatchlingHp(src.getHatchlingHp());
        dst.setAdultHp(src.getAdultHp());
        dst.setHatchlingSpeed(src.getHatchlingSpeed());
        dst.setAdultSpeed(src.getAdultSpeed());
        dst.setHatchlingEnergyDrain(src.getHatchlingEnergyDrain());
        dst.setAdultEnergyDrain(src.getAdultEnergyDrain());
        dst.setGrowthRate(src.getGrowthRate());
        dst.setWalkingEnergyDrainMultiplier(src.getWalkingEnergyDrainMultiplier());
        dst.setDiet(new ArrayList<>(src.getDiet()));
        dst.setAbilities(new ArrayList<>(src.getAbilities()));
        return dst;
    }

    private static class WeatherEntry {
        Weather w;
        double weight;
        WeatherEntry(Weather w, double weight) { this.w = w; this.weight = weight; }
    }

    private static final List<WeatherEntry> WEATHER_TABLE = List.of(
        new WeatherEntry(new Weather("Cloudy", "", 0.0), 0.30),
        new WeatherEntry(new Weather("Sunny", "", 0.0, 1.2, 1.0, 1.0), 0.25),
        new WeatherEntry(new Weather("Heatwave", "", 0.0, 1.5, 1.0, 1.0), 0.10),
        new WeatherEntry(new Weather("Light Rain", "", 0.01, 0.9, 1.1, 1.1), 0.20),
        new WeatherEntry(new Weather("Heavy Rain", "", 0.10, 0.8, 1.2, 1.2), 0.15)
    );

    private Weather chooseWeather() {
        double total = 0.0;
        for (WeatherEntry e : WEATHER_TABLE) {
            total += e.weight;
        }
        double n = weatherRng.nextDouble() * total;
        double cumulative = 0.0;
        for (WeatherEntry e : WEATHER_TABLE) {
            cumulative += e.weight;
            if (n <= cumulative) {
                return e.w;
            }
        }
        return WEATHER_TABLE.get(0).w;
    }

    private void populateAnimals() {
        Random r = new Random();
        StatsLoader.getDinoStats().forEach((name, stats) -> {
            int count = (int) Math.max(1, stats.getAdultWeight() / 1000);
            for (int i = 0; i < count; i++) {
                int ax = r.nextInt(map.getWidth());
                int ay = r.nextInt(map.getHeight());
                NPCAnimal npc = new NPCAnimal();
                npc.setId(spawned.size() + 1);
                npc.setName(name);
                npc.setWeight(stats.getAdultWeight());
                npc.setMaxHp(stats.getAdultHp());
                npc.setHp(npc.getMaxHp());
                map.addAnimal(ax, ay, npc);
                spawned.add(npc);
            }
        });
    }

    private void updateNpcs() {
        for (NPCAnimal npc : spawned) {
            if (!npc.isAlive()) continue;
            npc.setEnergy(Math.max(0.0, npc.getEnergy() - 1.0 * weather.getNpcEnergyMult()));
            if (npc.getEnergy() <= 0) {
                npc.setAlive(false);
            }
        }
    }

    private void startTurn() {
        turn++;
        if (weatherTurns >= 10) {
            weather = chooseWeather();
            weatherTurns = 0;
        }
        weatherTurns++;
        player.setHydration(Math.max(0.0,
                player.getHydration() - player.getHydrationDrain() * weather.getPlayerHydrationMult()));
        player.setEnergy(Math.max(0.0,
                player.getEnergy() - player.getHatchlingEnergyDrain() * weather.getPlayerEnergyMult()));
        updateNpcs();
    }

    /** Move the player by the specified delta. */
    public void move(int dx, int dy) {
        startTurn();
        x = Math.max(0, Math.min(map.getWidth() - 1, x + dx));
        y = Math.max(0, Math.min(map.getHeight() - 1, y + dy));
        map.reveal(x, y);
    }

    /** Skip a turn without moving. */
    public void rest() {
        startTurn();
    }

    /** Drink if the player is on a lake tile. */
    public void drink() {
        startTurn();
        if (map.terrainAt(x, y) == Terrain.LAKE) {
            player.setHydration(100.0);
        }
    }

    public Map getMap() {
        return map;
    }

    public DinosaurStats getPlayer() {
        return player;
    }

    public int getPlayerX() {
        return x;
    }

    public int getPlayerY() {
        return y;
    }

    public Weather getWeather() {
        return weather;
    }

    public int getTurn() {
        return turn;
    }

    public boolean hasWon() {
        return won;
    }
}
