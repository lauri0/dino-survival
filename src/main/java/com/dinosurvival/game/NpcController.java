package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Controller responsible for spawning NPC animals on the map.
 */
public class NpcController {
    private Map map;
    private Weather weather;
    private int nextNpcId = 1;
    private final List<NPCAnimal> spawned = new ArrayList<>();

    public NpcController(Map map, Weather weather) {
        this.map = map;
        this.weather = weather;
    }

    public void setMap(Map map) {
        this.map = map;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public int allocateNpcId() {
        return nextNpcId++;
    }

    public void trackSpawn(NPCAnimal npc) {
        spawned.add(npc);
    }

    public List<NPCAnimal> getSpawned() {
        return spawned;
    }

    /** Populate the map with initial dinosaur NPCs. */
    public void populateAnimals() {
        List<int[]> land = new ArrayList<>();
        List<int[]> lake = new ArrayList<>();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                Terrain t = map.terrainAt(tx, ty);
                if (t == Terrain.LAKE) {
                    lake.add(new int[]{tx, ty});
                } else if (t != Terrain.TOXIC_BADLANDS) {
                    land.add(new int[]{tx, ty});
                }
            }
        }

        List<java.util.Map.Entry<String, DinosaurStats>> species =
                new ArrayList<>(StatsLoader.getDinoStats().entrySet());

        java.util.Map<String, Double> multipliers = new java.util.HashMap<>();
        double totalMult = 0.0;
        for (var e : species) {
            double m = e.getValue().getInitialSpawnMultiplier();
            multipliers.put(e.getKey(), m);
            totalMult += m;
        }

        int totalAnimals = 100;
        java.util.Map<String, Integer> spawnCounts = new java.util.HashMap<>();
        if (totalMult > 0) {
            java.util.Map<String, Double> raw = new java.util.HashMap<>();
            for (var e : multipliers.entrySet()) {
                raw.put(e.getKey(), totalAnimals * e.getValue() / totalMult);
            }
            java.util.Map<String, Integer> base = new java.util.HashMap<>();
            int sum = 0;
            for (var e : raw.entrySet()) {
                int v = (int) Math.floor(e.getValue());
                base.put(e.getKey(), v);
                sum += v;
            }
            int leftover = totalAnimals - sum;
            java.util.List<java.util.Map.Entry<String, Double>> rem = new java.util.ArrayList<>();
            for (var e : raw.entrySet()) {
                rem.add(new java.util.AbstractMap.SimpleEntry<>(e.getKey(), e.getValue() - base.get(e.getKey())));
            }
            rem.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            for (int i = 0; i < leftover; i++) {
                String n = rem.get(i % rem.size()).getKey();
                base.put(n, base.get(n) + 1);
            }
            spawnCounts = base;
        }

        Random rand = new Random();
        for (var e : species) {
            String name = e.getKey();
            DinosaurStats stats = e.getValue();
            int oldCount = (int) Math.round(stats.getInitialSpawnMultiplier());
            int spawnCount = spawnCounts.getOrDefault(name, 0);
            List<int[]> tiles = stats.isCanWalk() ? land : lake;
            int loopCount = Math.max(oldCount, spawnCount);
            if (tiles.isEmpty() || loopCount <= 0) {
                for (int i = 0; i < loopCount; i++) {
                    if (!tiles.isEmpty()) {
                        rand.nextInt(tiles.size());
                    }
                    if (stats.isCanBeJuvenile()) {
                        rand.nextDouble();
                    }
                }
                continue;
            }
            for (int i = 0; i < loopCount; i++) {
                int[] pos = tiles.get(rand.nextInt(tiles.size()));
                double weight;
                if (stats.isCanBeJuvenile()) {
                    double max = Math.max(stats.getAdultWeight(), 0.0);
                    weight = 3.0 + rand.nextDouble() * (max - 3.0);
                    if (weight > max) weight = max;
                } else {
                    weight = stats.getAdultWeight();
                }
                if (i < spawnCount) {
                    double maxHp = scaleByWeight(weight, stats.getAdultWeight(), stats.getAdultHp());
                    NPCAnimal npc = new NPCAnimal();
                    npc.setId(allocateNpcId());
                    npc.setName(name);
                    npc.setWeight(weight);
                    npc.setMaxHp(maxHp);
                    npc.setHp(maxHp);
                    npc.setAbilities(new ArrayList<>(stats.getAbilities()));
                    map.addAnimal(pos[0], pos[1], npc);
                    trackSpawn(npc);
                }
            }
        }
    }

    /** Spawn critter NPCs either for the initial game setup or a normal turn. */
    public void spawnCritters(boolean initial) {
        if (StatsLoader.getCritterStats().isEmpty()) {
            return;
        }

        List<int[]> land = new ArrayList<>();
        List<int[]> lake = new ArrayList<>();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                Terrain t = map.terrainAt(tx, ty);
                if (t == Terrain.LAKE) {
                    lake.add(new int[]{tx, ty});
                } else if (t != Terrain.TOXIC_BADLANDS) {
                    land.add(new int[]{tx, ty});
                }
            }
        }

        Random rand = new Random();
        StatsLoader.getCritterStats().forEach((name, stats) -> {
            int maxInd = 0;
            Object maxObj = stats.get("maximum_individuals");
            if (maxObj instanceof Number num) {
                maxInd = num.intValue();
            }

            int current = 0;
            for (int y = 0; y < map.getHeight(); y++) {
                for (int x = 0; x < map.getWidth(); x++) {
                    for (NPCAnimal npc : map.getAnimals(x, y)) {
                        if (name.equals(npc.getName())) {
                            current++;
                        }
                    }
                }
            }

            int available = Math.max(0, maxInd - current);
            int spawnCount;
            if (initial) {
                spawnCount = maxInd / 2;
            } else {
                double avg = 0.0;
                Object avgObj = stats.get("avg_spawned_per_turn");
                if (avgObj instanceof Number num) {
                    avg = num.doubleValue();
                }
                spawnCount = (int) Math.round(rand.nextGaussian() * 0.5 + avg);
                if (spawnCount < 0) spawnCount = 0;
            }

            int toSpawn = Math.min(spawnCount, available);
            boolean canWalk = !Boolean.FALSE.equals(stats.get("can_walk"));
            List<int[]> tiles = canWalk ? land : lake;

            for (int i = 0; i < toSpawn && !tiles.isEmpty(); i++) {
                int[] pos = tiles.get(rand.nextInt(tiles.size()));

                boolean occupied = false;
                for (NPCAnimal npc : map.getAnimals(pos[0], pos[1])) {
                    if (name.equals(npc.getName())) {
                        occupied = true;
                        break;
                    }
                }
                if (occupied) {
                    continue;
                }

                NPCAnimal npc = new NPCAnimal();
                npc.setId(allocateNpcId());
                npc.setName(name);

                double weight = 0.0;
                Object wObj = stats.get("adult_weight");
                if (wObj instanceof Number num) {
                    weight = num.doubleValue();
                }

                double hp = 0.0;
                Object hpObj = stats.get("hp");
                if (hpObj instanceof Number num) {
                    hp = num.doubleValue();
                }

                npc.setWeight(weight);
                npc.setMaxHp(hp);
                npc.setHp(hp);
                map.addAnimal(pos[0], pos[1], npc);
                trackSpawn(npc);
            }
        });
    }

    private double scaleByWeight(double weight, double adultWeight, double val) {
        double pct = adultWeight > 0 ? weight / adultWeight : 1.0;
        pct = Math.max(0.0, Math.min(pct, 1.0));
        return val * pct;
    }
}
