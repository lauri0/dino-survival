package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.model.Plant;
import com.dinosurvival.game.EncounterEntry;
import com.dinosurvival.game.EggCluster;
import com.dinosurvival.game.Burrow;
import java.util.Iterator;
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
    private int nextNpcId = 1;
    private boolean won;
    private int turn;
    private List<EncounterEntry> currentEncounters = new ArrayList<>();
    private List<Plant> currentPlants = new ArrayList<>();
    private String lastAction = "";
    private final java.util.Map<String, List<Integer>> populationHistory = new java.util.HashMap<>();
    private final java.util.Map<String, int[]> huntStats = new java.util.HashMap<>();
    private final List<Integer> turnHistory = new ArrayList<>();
    private List<String> turnMessages = new ArrayList<>();
    private final List<String> mammalSpecies = new ArrayList<>();
    private String formation;

    /** Number of descendants required to win the game. */
    public static final int DESCENDANTS_TO_WIN = 5;

    /** Energy multiplier applied when an NPC walks. */
    public static final double WALKING_ENERGY_DRAIN_MULTIPLIER = 1.3;

    /**
     * Initialise the game world. Statistics are loaded from the YAML files and
     * a new map is generated. This mirrors the behaviour of the Python
     * {@code Game.__init__} method so that the Swing UI can display a running
     * world without depending on the Python code.
     */
    public void start() {
        start("Morrison", null, new Random().nextLong());
    }

    /**
     * Start a new game using the given formation and player dinosaur name.
     * If {@code dinoName} is null the first available dinosaur is used.
     */
    public void start(String formation, String dinoName) {
        start(formation, dinoName, new Random().nextLong());
    }

    /**
     * Start a new game using the given formation and player dinosaur name with
     * the provided random seed for map generation.
     */
    public void start(String formation, String dinoName, long seed) {
        try {
            StatsLoader.load(Path.of("dinosurvival"), formation);
            this.formation = formation;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        map = new Map(18, 10, seed);
        map.populateBurrows(5);
        mammalSpecies.clear();
        for (var entry : StatsLoader.getCritterStats().entrySet()) {
            Object cls = entry.getValue().get("class");
            if (cls != null && cls.toString().equals("mammal")) {
                mammalSpecies.add(entry.getKey());
            }
        }

        // choose player dinosaur
        if (!StatsLoader.getDinoStats().isEmpty()) {
            DinosaurStats base = null;
            if (dinoName != null) {
                base = StatsLoader.getDinoStats().get(dinoName);
            }
            if (base == null) {
                base = StatsLoader.getDinoStats().values().iterator().next();
            }
            initialisePlayer(base);
        } else {
            player = new DinosaurStats();
        }

        chooseStartingLocation();
        map.reveal(x, y);
        weather = chooseWeather();
        weatherTurns = 0;
        populateAnimals();
        spawnCritters(true);
        huntStats.clear();
        populationHistory.clear();
        for (String name : StatsLoader.getDinoStats().keySet()) {
            populationHistory.put(name, new ArrayList<>());
        }
        for (String name : StatsLoader.getCritterStats().keySet()) {
            populationHistory.putIfAbsent(name, new ArrayList<>());
        }
        recordPopulation();
        turn = 0;
        turnMessages.clear();
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
        dst.setHealthRegen(src.getHealthRegen());
        dst.setHydrationDrain(src.getHydrationDrain());
        dst.setAquaticBoost(src.getAquaticBoost());
        dst.setCanWalk(src.isCanWalk());
        dst.setCanBeJuvenile(src.isCanBeJuvenile());
        dst.setInitialSpawnMultiplier(src.getInitialSpawnMultiplier());
        dst.setDiet(new ArrayList<>(src.getDiet()));
        dst.setAbilities(new ArrayList<>(src.getAbilities()));
        return dst;
    }

    /**
     * Initialise the player state using the provided base statistics.
     */
    private void initialisePlayer(DinosaurStats base) {
        player = cloneStats(base);
        player.setWeight(player.getHatchlingWeight());
        double pct = player.getAdultWeight() > 0
                ? player.getWeight() / player.getAdultWeight() : 1.0;
        pct = Math.max(0.0, Math.min(1.0, pct));
        player.setAttack(player.getAdultAttack() * pct);
        player.setMaxHp(player.getAdultHp() * pct);
        player.setHp(player.getMaxHp());
        // When starting the game the player's weight equals the hatchling
        // weight so the speed should exactly match the hatchling value.
        // Using statFromWeight here resulted in a slightly lower value due to
        // floating point rounding so set it explicitly instead.
        player.setSpeed(player.getHatchlingSpeed());
    }

    /** Linear interpolation between hatchling and adult values based on weight. */
    private double statFromWeight(double weight, double adultWeight,
                                  double hatchVal, double adultVal) {
        double pct = adultWeight > 0 ? weight / adultWeight : 1.0;
        pct = Math.max(0.0, Math.min(1.0, pct));
        return hatchVal + pct * (adultVal - hatchVal);
    }

    /** Choose a starting location within two tiles of a lake if possible. */
    private void chooseStartingLocation() {
        List<int[]> candidates = new ArrayList<>();
        for (int ly = 0; ly < map.getHeight(); ly++) {
            for (int lx = 0; lx < map.getWidth(); lx++) {
                if (map.terrainAt(lx, ly) == Terrain.LAKE) {
                    for (int dy = -2; dy <= 2; dy++) {
                        for (int dx = -2; dx <= 2; dx++) {
                            int nx = lx + dx;
                            int ny = ly + dy;
                            if (nx >= 0 && nx < map.getWidth()
                                    && ny >= 0 && ny < map.getHeight()) {
                                if (map.terrainAt(nx, ny) != Terrain.LAKE) {
                                    candidates.add(new int[]{nx, ny});
                                }
                            }
                        }
                    }
                }
            }
        }
        Random r = new Random();
        if (!candidates.isEmpty()) {
            int[] pos = candidates.get(r.nextInt(candidates.size()));
            x = pos[0];
            y = pos[1];
        } else {
            x = map.getWidth() / 2;
            y = map.getHeight() / 2;
        }
    }

    private static class WeatherEntry {
        Weather w;
        double weight;
        WeatherEntry(Weather w, double weight) { this.w = w; this.weight = weight; }
    }

    private static final List<WeatherEntry> WEATHER_TABLE = List.of(
        new WeatherEntry(new Weather("Cloudy", "assets/weather/clouds.png", 0.0), 0.30),
        new WeatherEntry(new Weather("Sunny", "assets/weather/sun.png", 0.0, 1.2, 1.0, 1.0), 0.25),
        new WeatherEntry(new Weather("Heatwave", "assets/weather/heatwave.png", 0.0, 1.5, 1.0, 1.0), 0.10),
        new WeatherEntry(new Weather("Light Rain", "assets/weather/light_rain.png", 0.01, 0.9, 1.1, 1.1), 0.20),
        new WeatherEntry(new Weather("Heavy Rain", "assets/weather/heavy_rain.png", 0.10, 0.8, 1.2, 1.2), 0.15)
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

    private static class PotentialTarget {
        NPCAnimal npc;
        double speed;
        double attack;
        Object stats;
        PotentialTarget(NPCAnimal npc, double speed, double attack, Object stats) {
            this.npc = npc;
            this.speed = speed;
            this.attack = attack;
            this.stats = stats;
        }
    }

    /** Populate the map with initial dinosaur NPCs. */
    private void populateAnimals() {
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

        Random r = new Random();
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
                        r.nextInt(tiles.size());
                    }
                    if (stats.isCanBeJuvenile()) {
                        r.nextDouble();
                    }
                }
                continue;
            }
            for (int i = 0; i < loopCount; i++) {
                int[] pos = tiles.get(r.nextInt(tiles.size()));
                double weight;
                if (stats.isCanBeJuvenile()) {
                    double max = Math.max(stats.getAdultWeight(), 0.0);
                    weight = 3.0 + r.nextDouble() * (max - 3.0);
                    if (weight > max) weight = max;
                } else {
                    weight = stats.getAdultWeight();
                }
                if (i < spawnCount) {
                    double maxHp = scaleByWeight(weight, stats.getAdultWeight(), stats.getAdultHp());
                    NPCAnimal npc = new NPCAnimal();
                    npc.setId(nextNpcId++);
                    npc.setName(name);
                    npc.setWeight(weight);
                    npc.setMaxHp(maxHp);
                    npc.setHp(maxHp);
                    npc.setAbilities(new ArrayList<>(stats.getAbilities()));
                    map.addAnimal(pos[0], pos[1], npc);
                    spawned.add(npc);
                }
            }
        }
    }

    /** Spawn critter NPCs either for the initial game setup or a normal turn. */
    private void spawnCritters(boolean initial) {
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

        Random r = new Random();
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
                spawnCount = (int) Math.round(r.nextGaussian() * 0.5 + avg);
                if (spawnCount < 0) spawnCount = 0;
            }

            int toSpawn = Math.min(spawnCount, available);
            boolean canWalk = !Boolean.FALSE.equals(stats.get("can_walk"));
            List<int[]> tiles = canWalk ? land : lake;

            for (int i = 0; i < toSpawn && !tiles.isEmpty(); i++) {
                int[] pos = tiles.get(r.nextInt(tiles.size()));

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
                npc.setId(nextNpcId++);
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
                spawned.add(npc);
            }
        });
    }

    void updateNpcs() {
        Random r = new Random();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                List<NPCAnimal> animals = new ArrayList<>(map.getAnimals(tx, ty));
                List<Plant> plants = map.getPlants(tx, ty);
                List<EggCluster> eggs = map.getEggs(tx, ty);
                for (NPCAnimal npc : animals) {
                    if (npc.getWeight() <= 0) {
                        map.removeAnimal(tx, ty, npc);
                        continue;
                    }

                    if (npc.getHp() <= 0 && npc.isAlive()) {
                        npc.setAlive(false);
                        npc.setAge(-1);
                        npc.setSpeed(0.0);
                        continue;
                    }

                    if (!npc.isAlive()) {
                        continue;
                    }

                    npc.setAge(npc.getAge() + 1);
                    String prev = npc.getLastAction();
                    npc.setLastAction("stay");
                    if ("spawned".equals(prev)) {
                        continue;
                    }
                    if (npc.getAbilities().contains("ambush")) {
                        if ("stay".equals(prev)) {
                            npc.setAmbushStreak(Math.min(npc.getAmbushStreak() + 1, 3));
                        } else {
                            npc.setAmbushStreak(0);
                        }
                    }

                    Object stats = StatsLoader.getDinoStats().get(npc.getName());
                    if (stats == null) {
                        java.util.Map<String, Object> cstats =
                                StatsLoader.getCritterStats().get(npc.getName());
                        npc.setNextMove("None");
                        if (cstats != null) {
                            npcChooseMove(tx, ty, npc, cstats);
                            double regen = getStat(cstats, "health_regen");
                            if (applyBleedAndRegen(npc, regen)) {
                                continue;
                            }
                        }
                        continue;
                    }

                    npc.setNextMove("None");
                    if (npc.getTurnsUntilLayEggs() > 0) {
                        npc.setTurnsUntilLayEggs(npc.getTurnsUntilLayEggs() - 1);
                    }

                    double baseDrain = getStat(stats, "adult_energy_drain") * 0.5 * weather.getNpcEnergyMult();
                    npc.setEnergy(Math.max(0.0, npc.getEnergy() - baseDrain));
                    if (npc.getEnergy() <= 0) {
                        npc.setAlive(false);
                        npc.setAge(-1);
                        npc.setSpeed(0.0);
                        continue;
                    }

                    double regen = getStat(stats, "health_regen");
                    if (applyBleedAndRegen(npc, regen)) {
                        continue;
                    }

                    double adultWeight = getStat(stats, "adult_weight");
                    boolean canBeJuvenile = getBool(stats, "can_be_juvenile", true);
                    if (npc.getWeight() >= adultWeight && canBeJuvenile
                            && npc.getEnergy() >= 80 && npc.getHp() >= npc.getMaxHp() * 0.8
                            && npc.getTurnsUntilLayEggs() == 0) {
                        if (animals.size() >= 4) {
                            npcChooseMoveForced(tx, ty, npc, stats);
                            if (!"None".equals(npc.getNextMove())) {
                                if (npcApplyWalkingDrain(npc, baseDrain)) {
                                    continue;
                                }
                            }
                            npc.setLastAction("move");
                            continue;
                        } else {
                            npc.setEnergy(npc.getEnergy() * 0.7);
                            int numEggs = (int) getStat(stats, "num_eggs");
                            if (numEggs > 0) {
                                double hatchW = getStat(stats, "hatchling_weight");
                                if (hatchW <= 0) hatchW = Math.max(1.0, adultWeight * 0.001);
                                EggCluster ec = new EggCluster(npc.getName(), numEggs,
                                        hatchW * numEggs, 5, npc.isDescendant());
                                eggs.add(ec);
                            }
                            npc.setTurnsUntilLayEggs((int) getStat(stats, "egg_laying_interval"));
                            npc.setLastAction("act");
                            continue;
                        }
                    }

                    if (npc.getAbilities().contains("bleed") && npc.getBleedWaitTurns() > 0) {
                        NPCAnimal target = null;
                        for (NPCAnimal a : animals) {
                            if (a.getId() == npc.getBleedWaitTarget()) {
                                target = a;
                                break;
                            }
                        }
                        if (target != null && target.isAlive() && target.getBleeding() > 0
                                && npc.getEnergy() >= 30) {
                            npc.setBleedWaitTurns(npc.getBleedWaitTurns() - 1);
                            npc.setNextMove("None");
                            npc.setLastAction("stay");
                            continue;
                        } else {
                            npc.setBleedWaitTurns(0);
                            npc.setBleedWaitTarget(-1);
                        }
                    }

                    if (npc.getEnergy() <= 90) {
                        if (statsDietHas(stats, "meat")) {
                            NPCAnimal carcass = null;
                            for (NPCAnimal other : animals) {
                                if (other != npc && !other.isAlive() && other.getWeight() > 0) {
                                    if (carcass == null || other.getWeight() > carcass.getWeight()) {
                                        carcass = other;
                                    }
                                }
                            }
                            if (carcass != null) {
                                npcConsumeMeat(tx, ty, npc, carcass, stats);
                                if (carcass.getWeight() <= 0) {
                                    map.removeAnimal(tx, ty, carcass);
                                }
                                npc.setLastAction("act");
                                continue;
                            }

                            EggCluster targetEgg = null;
                            for (EggCluster e : eggs) {
                                if (!npc.getName().equals(e.getSpecies())) {
                                    targetEgg = e;
                                    break;
                                }
                            }
                            if (targetEgg != null) {
                                npcConsumeEggs(npc, targetEgg, stats);
                                if (targetEgg.getWeight() <= 0) {
                                    eggs.remove(targetEgg);
                                }
                                npc.setLastAction("act");
                                continue;
                            }

                            if (npc.getAbilities().contains("digger") && npcDigBurrow(tx, ty)) {
                                npc.setLastAction("act");
                                continue;
                            }
                        }

                        if (statsDietHasPlant(stats) && !plants.isEmpty()) {
                            Plant chosen = null;
                            for (Plant p : plants) {
                                if (chosen == null || p.getWeight() > chosen.getWeight()) {
                                    chosen = p;
                                }
                            }
                            if (chosen != null) {
                                npcConsumePlant(tx, ty, npc, chosen, stats);
                                if (chosen.getWeight() <= 0) {
                                    plants.remove(chosen);
                                }
                                npc.setLastAction("act");
                                continue;
                            }
                        }

                        if (npcTryHunt(tx, ty, npc, stats, animals, adultWeight)) {
                            continue;
                        }
                    }

                    npcChooseMove(tx, ty, npc, stats);
                    if (!"None".equals(npc.getNextMove())) {
                        if (npcApplyWalkingDrain(npc, baseDrain)) {
                            continue;
                        }
                        npc.setLastAction("move");
                    }
                }
            }
        }
        moveNpcs();
    }

    /**
     * Load encounter information for the player's current tile.
     */
    private void generateEncounters() {
        List<EncounterEntry> entries = new ArrayList<>();

        // Clean up any invalid animals on this tile
        List<NPCAnimal> cellAnimals = map.getAnimals(x, y);
        for (Iterator<NPCAnimal> it = cellAnimals.iterator(); it.hasNext(); ) {
            NPCAnimal npc = it.next();
            if (npc.getWeight() <= 0) {
                it.remove();
            }
        }

        List<Plant> cellPlants = map.getPlants(x, y);
        Burrow burrow = map.getBurrow(x, y);
        if (burrow != null) {
            EncounterEntry e = new EncounterEntry();
            e.setBurrow(burrow);
            entries.add(e);
        }
        for (EggCluster egg : map.getEggs(x, y)) {
            EncounterEntry e = new EncounterEntry();
            e.setEggs(egg);
            entries.add(e);
        }
        for (NPCAnimal npc : cellAnimals) {
            EncounterEntry e = new EncounterEntry();
            e.setNpc(npc);
            entries.add(e);
        }

        currentEncounters = entries;
        currentPlants = new ArrayList<>(cellPlants);
    }

    private boolean npcHasPackmate(NPCAnimal npc, int tx, int ty) {
        for (NPCAnimal other : map.getAnimals(tx, ty)) {
            if (other == npc) continue;
            if (other.isAlive() && other.getName().equals(npc.getName())) {
                return true;
            }
        }
        if (this.x == tx && this.y == ty && player.getName() != null && player.getName().equals(npc.getName())) {
            return true;
        }
        return false;
    }

    private boolean playerPackHunterActive() {
        if (!player.getAbilities().contains("pack_hunter")) {
            return false;
        }
        for (NPCAnimal npc : map.getAnimals(x, y)) {
            if (npc.isAlive() && player.getName().equals(npc.getName())) {
                return true;
            }
        }
        return false;
    }

    public double playerEffectiveAttack() {
        double atk = player.getAttack();
        if (playerPackHunterActive()) {
            atk *= 3;
        }
        double hpPct = 1.0;
        if (player.getMaxHp() > 0) {
            hpPct = Math.max(0.0, Math.min(player.getHp() / player.getMaxHp(), 1.0));
        }
        return atk * hpPct;
    }

    private double scaleByWeight(double weight, double adultWeight, double val) {
        double pct = adultWeight > 0 ? weight / adultWeight : 1.0;
        pct = Math.max(0.0, Math.min(pct, 1.0));
        return val * pct;
    }

    private double npcEffectiveAttack(NPCAnimal npc, Object stats, int tx, int ty) {
        double adultWeight = 0.0;
        double baseAtk = 0.0;
        List<String> abilities = null;
        if (stats instanceof DinosaurStats ds) {
            adultWeight = ds.getAdultWeight();
            baseAtk = ds.getAdultAttack();
            abilities = ds.getAbilities();
        } else if (stats instanceof java.util.Map<?, ?> map) {
            Object aw = ((java.util.Map<?, ?>) map).get("adult_weight");
            if (aw instanceof Number n) adultWeight = n.doubleValue();
            Object atk = ((java.util.Map<?, ?>) map).get("attack");
            if (atk instanceof Number n) baseAtk = n.doubleValue();
            Object abil = ((java.util.Map<?, ?>) map).get("abilities");
            if (abil instanceof List<?> list) {
                abilities = new ArrayList<>();
                for (Object o : list) {
                    abilities.add(o.toString());
                }
            }
        }
        double atk = scaleByWeight(npc.getWeight(), adultWeight, baseAtk);
        if (abilities != null && abilities.contains("pack_hunter") && npcHasPackmate(npc, tx, ty)) {
            atk *= 3;
        }
        double hpPct = 1.0;
        if (npc.getMaxHp() > 0) {
            hpPct = Math.max(0.0, Math.min(npc.getHp() / npc.getMaxHp(), 1.0));
        }
        return atk * hpPct;
    }

    /**
     * Determine if an aggressive NPC immediately attacks the player.
     */
    private String aggressiveAttackCheck() {
        double playerA = Math.max(playerEffectiveAttack(), 0.1);
        Random r = new Random();
        for (EncounterEntry entry : currentEncounters) {
            if (entry.getEggs() != null || entry.getNpc() == null) {
                continue;
            }
            NPCAnimal npc = entry.getNpc();
            if (!npc.isAlive()) {
                continue;
            }
            Object stats = StatsLoader.getDinoStats().get(npc.getName());
            if (stats == null) {
                stats = StatsLoader.getCritterStats().get(npc.getName());
            }
            boolean aggressive = false;
            if (stats instanceof java.util.Map<?, ?> map) {
                Object ag = ((java.util.Map<?, ?>) map).get("aggressive");
                if (ag instanceof Boolean b) {
                    aggressive = b;
                }
            }
            // DinosaurStats currently lacks an aggressive flag
            if (!aggressive) {
                continue;
            }

            double targetA = npcEffectiveAttack(npc, stats, x, y);
            double rel = targetA / playerA;
            if (rel > 2.0 && r.nextDouble() < 0.5) {
                player.setHp(0);
                return "A fierce " + npc.getName() + " (" + npc.getId() + ") attacks and kills you! Game Over.";
            }
        }
        return null;
    }

    private void updateEggs() {
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                List<EggCluster> cell = map.getEggs(tx, ty);
                for (Iterator<EggCluster> it = cell.iterator(); it.hasNext(); ) {
                    EggCluster egg = it.next();
                    egg.setTurnsUntilHatch(egg.getTurnsUntilHatch() - 1);
                    if (egg.getTurnsUntilHatch() <= 0 || egg.getWeight() <= 0) {
                        it.remove();
                    }
                }
            }
        }
    }

    /**
     * Apply spoilage to all carcasses after NPC actions have completed.
     * Dead animals lose weight each turn and are removed once fully decayed.
     */
    public java.util.List<String> spoilCarcasses() {
        java.util.List<String> messages = new java.util.ArrayList<>();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                java.util.List<NPCAnimal> animals = new java.util.ArrayList<>(map.getAnimals(tx, ty));
                for (NPCAnimal npc : animals) {
                    if (!npc.isAlive()) {
                        double before = npc.getWeight();
                        double spoiled = npc.getWeight() * 0.10 + 2;
                        double after = Math.max(0.0, npc.getWeight() - spoiled);
                        npc.setWeight(after);
                        double lost = before - after;
                        if (lost > 0 && tx == x && ty == y) {
                            String msg = "The " + npcLabel(npc) + " carcass lost " +
                                    String.format(java.util.Locale.US, "%.1f", lost) + "kg to spoilage.";
                            messages.add(msg);
                        }
                        if (npc.getWeight() <= 0) {
                            map.removeAnimal(tx, ty, npc);
                        }
                    }
                }
            }
        }
        return messages;
    }

    private String npcLabel(NPCAnimal npc) {
        return npc.getName() + " (" + npc.getId() + ")";
    }

    private String playerLabel() {
        return player.getName() + " (0)";
    }

    private void applyBleedAndRegen(DinosaurStats dino, double regen,
                                     boolean moved, boolean allowRegen) {
        if (dino.getBleeding() > 0) {
            int mult = moved ? 2 : 1;
            dino.setHp(Math.max(0.0,
                    dino.getHp() - dino.getMaxHp() * 0.05 * mult));
            dino.setBleeding(dino.getBleeding() - 1);
        } else if (regen > 0 && allowRegen && dino.getHp() < dino.getMaxHp()) {
            dino.setHp(Math.min(dino.getMaxHp(),
                    dino.getHp() + dino.getMaxHp() * regen / 100.0));
        }
        if (dino.getBrokenBone() > 0) {
            dino.setBrokenBone(dino.getBrokenBone() - 1);
        }
    }

    void applyTurnCosts(boolean moved, double multiplier) {
        double drain = player.getHatchlingEnergyDrain();
        if (moved) {
            drain *= WALKING_ENERGY_DRAIN_MULTIPLIER;
            if (player.getBrokenBone() > 0) {
                drain *= 2;
            }
        }
        drain *= multiplier;
        drain *= weather.getPlayerEnergyMult();
        player.setEnergy(Math.max(0.0, player.getEnergy() - drain));
        if (player.isExhausted()) {
            player.setHp(0.0);
            turnMessages.add("You have collapsed from exhaustion! Game Over.");
        }
        applyBleedAndRegen(player, player.getHealthRegen(), moved, !player.isExhausted());
    }

    void applyTurnCosts(boolean moved) {
        applyTurnCosts(moved, 1.0);
    }

    private void startTurn() {
        turnMessages.clear();
        turn++;
        recordPopulation();
        if (weatherTurns >= 10) {
            weather = chooseWeather();
            weatherTurns = 0;
            turnMessages.add("The weather changes to " + weather.getName() + ".");
        }
        weatherTurns++;

        if (player.getAbilities().contains("ambush")) {
            if ("stay".equals(lastAction)) {
                player.setAmbushStreak(Math.min(player.getAmbushStreak() + 1, 3));
            } else {
                player.setAmbushStreak(0);
            }
        }

        map.updateVolcanicActivity(x, y, player);
        map.updateFlood(x, y, player, weather.getFloodChance());
        map.updateForestFire();
        updateEggs();
        map.growPlants(StatsLoader.getPlantStats());
        spawnCritters(false);
        map.refreshBurrows();
        if (player.getTurnsUntilLayEggs() > 0) {
            player.setTurnsUntilLayEggs(player.getTurnsUntilLayEggs() - 1);
        }

        player.setHydration(Math.max(0.0,
                player.getHydration() - player.getHydrationDrain() * weather.getPlayerHydrationMult()));
        if (player.isDehydrated()) {
            player.setHp(0.0);
            turnMessages.add("You have perished from dehydration! Game Over.");
        }

        updateNpcs();
        _apply_terrain_effects();
        spoilCarcasses();
        generateEncounters();
        aggressiveAttackCheck();
    }

    /** Move the player by the specified delta. */
    public void move(int dx, int dy) {
        startTurn();
        x = Math.max(0, Math.min(map.getWidth() - 1, x + dx));
        y = Math.max(0, Math.min(map.getHeight() - 1, y + dy));
        map.reveal(x, y);
        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(true, 1.0);
        checkVictory();
        lastAction = "move";
    }

    /** Skip a turn without moving. */
    public void rest() {
        startTurn();
        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(false, 1.0);
        checkVictory();
        lastAction = "stay";
    }

    /** Drink if the player is on a lake tile. */
    public void drink() {
        startTurn();
        if (map.terrainAt(x, y) == Terrain.LAKE) {
            player.setHydration(100.0);
        }
        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(false, 1.0);
        checkVictory();
        lastAction = "drink";
    }

    /** Convenience movement helpers matching the Python API. */
    public void moveNorth() { move(0, -1); }
    public void moveSouth() { move(0, 1); }
    public void moveEast()  { move(1, 0); }
    public void moveWest()  { move(-1, 0); }

    /** Hunt the NPC with the given identifier on the current tile. */
    public void huntNpc(int id) {
        NPCAnimal target = null;
        for (NPCAnimal npc : map.getAnimals(x, y)) {
            if (npc.getId() == id) { target = npc; break; }
        }
        startTurn();
        if (target == null) {
            generateEncounters();
            aggressiveAttackCheck();
            applyTurnCosts(false, 1.0);
            lastAction = "hunt";
            return;
        }

        boolean wasAlive = target.isAlive();
        int[] hunt = null;
        if (wasAlive) {
            hunt = huntStats.computeIfAbsent(target.getName(), k -> new int[]{0, 0});
            hunt[0]++;
        }

        Object stats = StatsLoader.getDinoStats().get(target.getName());
        if (stats == null) stats = StatsLoader.getCritterStats().get(target.getName());

        double playerAtk = playerEffectiveAttack();
        double targetAtk = target.isAlive() ? npcEffectiveAttack(target, stats, x, y) : 0.0;

        if (target.isAlive()) {
            double playerSpeed = playerEffectiveSpeed();
            double targetSpeed = npcEffectiveSpeed(target, stats);
            double relSpeed = targetSpeed / Math.max(playerSpeed, 0.1);
            double catchChance = calculateCatchChance(relSpeed);
            if (Math.random() > catchChance) {
                turnMessages.add("The " + npcLabel(target) + " escaped before you could catch it.");
                generateEncounters();
                aggressiveAttackCheck();
                applyTurnCosts(false, 5.0);
                checkVictory();
                lastAction = "hunt";
                return;
            }
        }

        if (target.isAlive()) {
            DinosaurStats playerBase = StatsLoader.getDinoStats().get(player.getName());
            if (playerBase == null) playerBase = new DinosaurStats();
            double dmg = damageAfterArmor(targetAtk, stats, playerBase);
            double beforePlayer = player.getHp();
            boolean died = applyDamage(dmg, player, playerBase);
            double playerDamage = beforePlayer - player.getHp();
            if (playerDamage > 0) {
                turnMessages.add(npcLabel(target) + " deals " +
                        String.format(java.util.Locale.US, "%.0f", playerDamage) +
                        " damage to " + playerLabel() + ".");
            }
            if (dmg > 0 && target.getAbilities().contains("bleed") && player.getHp() > 0) {
                int bleed = (player.getAbilities().contains("light_armor") || player.getAbilities().contains("heavy_armor")) ? 2 : 5;
                player.setBleeding(bleed);
            }
            if (dmg > 0 && target.getAbilities().contains("bone_break") && target.getWeight() >= player.getWeight()/3 && player.getHp() > 0) {
                player.setBrokenBone(10);
            }
            if (died) {
                generateEncounters();
                lastAction = "hunt";
                return;
            }
        }

        double dmgToTarget = damageAfterArmor(playerAtk,
                StatsLoader.getDinoStats().get(player.getName()), stats);
        double beforeTarget = target.getHp();
        boolean targetDied = applyDamage(dmgToTarget, target, stats);
        double dealt = beforeTarget - target.getHp();
        if (dealt > 0) {
            turnMessages.add(playerLabel() + " deals " +
                    String.format(java.util.Locale.US, "%.0f", dealt) +
                    " damage to " + npcLabel(target) + ".");
        }
        if (dmgToTarget > 0 && player.getAbilities().contains("bleed") && target.getHp() > 0 && target.isAlive()) {
            int bleed = (target.getAbilities().contains("light_armor") || target.getAbilities().contains("heavy_armor")) ? 2 : 5;
            target.setBleeding(bleed);
        }
        if (dmgToTarget > 0 && player.getAbilities().contains("bone_break") && player.getWeight() >= target.getWeight()/3 && target.getHp() > 0) {
            target.setBrokenBone(10);
        }

        if (!target.isAlive()) {
            double meat = target.getWeight();
            double energyGain = 1000 * meat / Math.max(player.getWeight(), 0.1);
            double need = 100.0 - player.getEnergy();
            double actual = Math.min(energyGain, need);
            player.setEnergy(Math.min(100.0, player.getEnergy() + actual));
            double used = actual * player.getWeight() / 1000.0;
            double leftover = Math.max(0.0, meat - used);
            double[] growth = applyGrowth(leftover);
            target.setWeight(Math.max(0.0, target.getWeight() - (used + growth[0])));
            if (target.getWeight() <= 0) {
                map.removeAnimal(x, y, target);
            }
            if (wasAlive && hunt != null) {
                hunt[1]++;
            }
        }

        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(false, 1.0);
        checkVictory();
        lastAction = "hunt";
    }

    /** Eat eggs present on the current tile. */
    public void collectEggs() {
        startTurn();
        List<EggCluster> eggs = map.getEggs(x, y);
        if (eggs.isEmpty()) {
            generateEncounters();
            aggressiveAttackCheck();
            applyTurnCosts(false, 1.0);
            lastAction = "eggs";
            return;
        }
        EggCluster egg = map.takeEggs(x, y);
        double weight = egg.getWeight();
        double energyGain = 1000 * weight / Math.max(player.getWeight(), 0.1);
        double need = 100.0 - player.getEnergy();
        double actual = Math.min(energyGain, need);
        player.setEnergy(Math.min(100.0, player.getEnergy() + actual));
        double used = actual * player.getWeight() / 1000.0;
        double leftover = Math.max(0.0, weight - used);
        applyGrowth(leftover);
        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(false, 1.0);
        checkVictory();
        lastAction = "eggs";
    }

    /** Dig into a burrow on the current tile if present. */
    public void digBurrow() {
        startTurn();
        Burrow b = map.getBurrow(x, y);
        if (b != null && b.isFull()) {
            double gain = player.getAbilities().contains("digger") ? 100.0 : 25.0;
            b.setProgress(Math.min(100.0, b.getProgress() + gain));
            if (b.getProgress() >= 100.0) {
                b.setFull(false);
                b.setProgress(0.0);
                if (!mammalSpecies.isEmpty()) {
                    String name = mammalSpecies.get(new Random().nextInt(mammalSpecies.size()));
                    java.util.Map<String, Object> stats = StatsLoader.getCritterStats().get(name);
                    double weight = 0.0;
                    Object wObj = stats.get("adult_weight");
                    if (wObj instanceof Number n) weight = n.doubleValue();
                    double hp = scaleByWeight(weight, getStat(stats, "adult_weight"), getStat(stats, "hp"));
                    NPCAnimal npc = new NPCAnimal();
                    npc.setId(nextNpcId++);
                    npc.setName(name);
                    npc.setWeight(weight);
                    npc.setMaxHp(hp);
                    npc.setHp(hp);
                    Object abil = stats.get("abilities");
                    if (abil instanceof java.util.List<?> list) {
                        java.util.List<String> abilList = new java.util.ArrayList<>();
                        for (Object a : list) {
                            abilList.add(a.toString());
                        }
                        npc.setAbilities(abilList);
                    }
                    npc.setLastAction("spawned");
                    map.addAnimal(x, y, npc);
                    spawned.add(npc);
                    turnMessages.add("You dug out a " + name + "!");
                }
            }
        }
        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(false, 1.0);
        checkVictory();
        lastAction = "dig";
    }

    /** Lay eggs if conditions allow. */
    public void layEggs() {
        startTurn();
        if (!canPlayerLayEggs()) {
            generateEncounters();
            aggressiveAttackCheck();
            applyTurnCosts(false, 1.0);
            lastAction = "lay_eggs";
            return;
        }
        player.setEnergy(player.getEnergy() * 0.7);
        double hatchW = player.getHatchlingWeight();
        EggCluster ec = new EggCluster(player.getName(), 1, hatchW, 5, true);
        map.getEggs(x, y).add(ec);
        player.setTurnsUntilLayEggs(10);
        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(false, 1.0);
        checkVictory();
        lastAction = "lay_eggs";
    }

    /** Mate with an NPC on the current tile. */
    public void mate(int partnerId) {
        startTurn();
        List<NPCAnimal> cell = map.getAnimals(x, y);
        NPCAnimal partner = null;
        for (NPCAnimal npc : cell) {
            if (npc.getId() == partnerId) { partner = npc; break; }
        }
        if (partner != null) {
            cell.remove(partner);
            player.setMated(true);
        }
        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(false, 1.0);
        checkVictory();
        lastAction = "mate";
    }

    /** Attempt to frighten nearby animals. */
    public void threaten() {
        startTurn();
        List<NPCAnimal> cell = map.getAnimals(x, y);
        double playerA = Math.max(playerEffectiveAttack(), 0.1);
        List<NPCAnimal> stronger = new ArrayList<>();
        List<NPCAnimal> weaker = new ArrayList<>();
        for (NPCAnimal npc : cell) {
            if (!npc.isAlive()) continue;
            Object stats = StatsLoader.getDinoStats().get(npc.getName());
            if (stats == null) stats = StatsLoader.getCritterStats().get(npc.getName());
            double npcA = npcEffectiveAttack(npc, stats, x, y);
            if (npcA > playerA) stronger.add(npc); else weaker.add(npc);
        }
        Random r = new Random();
        boolean killed = false;
        if (!stronger.isEmpty()) {
            player.setHp(0.0);
            killed = true;
        } else {
            java.util.Map<String,int[]> dirs = java.util.Map.of(
                    "Up", new int[]{0,-1}, "Right", new int[]{1,0},
                    "Down", new int[]{0,1}, "Left", new int[]{-1,0});
            for (NPCAnimal npc : weaker) {
                List<String> opts = new ArrayList<>();
                Object stats = StatsLoader.getDinoStats().get(npc.getName());
                if (stats == null) stats = StatsLoader.getCritterStats().get(npc.getName());
                boolean canWalk = !getBool(stats, "can_walk", true) ? false : true;
                for (var e : dirs.entrySet()) {
                    int nx = x + e.getValue()[0];
                    int ny = y + e.getValue()[1];
                    if (nx<0||ny<0||nx>=map.getWidth()||ny>=map.getHeight()) continue;
                    Terrain t = map.terrainAt(nx, ny);
                    if (!canWalk && t != Terrain.LAKE) continue;
                    opts.add(e.getKey());
                }
                npc.setNextMove(opts.isEmpty()?"None":opts.get(r.nextInt(opts.size())));
            }
        }
        generateEncounters();
        aggressiveAttackCheck();
        applyTurnCosts(false, 2.0);
        if (killed) {
            player.setHp(0.0);
        }
        checkVictory();
        lastAction = "threaten";
    }

    // ------------------------------------------------------------------
    // Helper methods for NPC logic
    // ------------------------------------------------------------------

    private double getStat(Object stats, String key) {
        if (stats instanceof DinosaurStats ds) {
            return switch (key) {
                case "adult_weight" -> ds.getAdultWeight();
                case "adult_energy_drain" -> ds.getAdultEnergyDrain();
                case "health_regen" -> ds.getHealthRegen();
                case "hatchling_speed" -> ds.getHatchlingSpeed();
                case "adult_speed" -> ds.getAdultSpeed();
                case "attack" -> ds.getAttack();
                case "hatchling_weight" -> ds.getHatchlingWeight();
                case "hp" -> ds.getAdultHp();
                default -> 0.0;
            };
        } else if (stats instanceof java.util.Map<?,?> map) {
            Object val = map.get(key);
            if (val instanceof Number n) return n.doubleValue();
        }
        return 0.0;
    }

    private boolean getBool(Object stats, String key, boolean def) {
        if (stats instanceof java.util.Map<?,?> map) {
            Object val = map.get(key);
            if (val instanceof Boolean b) return b;
        }
        return def;
    }

    private boolean statsDietHas(Object stats, String diet) {
        if (stats instanceof DinosaurStats ds) {
            for (var d : ds.getDiet()) {
                if (d.name().equalsIgnoreCase(diet)) return true;
            }
        } else if (stats instanceof java.util.Map<?,?> map) {
            Object val = map.get("diet");
            if (val instanceof List<?> list) {
                for (Object o : list) {
                    if (o.toString().equalsIgnoreCase(diet)) return true;
                }
            }
        }
        return false;
    }

    private boolean statsDietHasPlant(Object stats) {
        return statsDietHas(stats, "ferns") || statsDietHas(stats, "cycads")
                || statsDietHas(stats, "conifers") || statsDietHas(stats, "fruits");
    }

    private boolean applyBleedAndRegen(NPCAnimal npc, double regen) {
        if (npc.getBleeding() > 0) {
            npc.setHp(Math.max(0.0, npc.getHp() - npc.getMaxHp() * 0.05));
            npc.setBleeding(npc.getBleeding() - 1);
            if (npc.getHp() <= 0) {
                npc.setAlive(false);
                npc.setAge(-1);
                npc.setSpeed(0.0);
                return true;
            }
        } else if (regen > 0 && npc.getHp() < npc.getMaxHp()) {
            npc.setHp(Math.min(npc.getMaxHp(), npc.getHp() + npc.getMaxHp() * regen / 100.0));
        }
        if (npc.getBrokenBone() > 0) {
            npc.setBrokenBone(npc.getBrokenBone() - 1);
        }
        return false;
    }

    /**
     * Apply additional energy drain when an NPC walks.
     *
     * @param npc       the NPC animal
     * @param baseDrain the base drain for this turn
     * @return true if the NPC dies from energy loss
     */
    private boolean npcApplyWalkingDrain(NPCAnimal npc, double baseDrain) {
        double extra = baseDrain * (WALKING_ENERGY_DRAIN_MULTIPLIER - 1.0);
        if (npc.getBrokenBone() > 0) {
            extra *= 2;
        }
        if (extra > 0) {
            npc.setEnergy(Math.max(0.0, npc.getEnergy() - extra));
            if (npc.getEnergy() <= 0) {
                npc.setAlive(false);
                npc.setAge(-1);
                npc.setSpeed(0.0);
                return true;
            }
        }
        return false;
    }

    private double npcMaxGrowthGain(double weight, Object stats) {
        double adult = getStat(stats, "adult_weight");
        if (adult <= 0 || weight >= adult) {
            return 0.0;
        }
        double maxWeight = adult * 1.05;
        double r = getStat(stats, "growth_rate");
        if (r == 0.0) r = 0.35;
        double gain = r * weight * (1 - weight / maxWeight);
        return Math.min(gain, adult - weight);
    }

    private void npcApplyGrowth(NPCAnimal npc, double available, Object stats) {
        double maxGain = npcMaxGrowthGain(npc.getWeight(), stats);
        double gain = Math.min(available, maxGain);
        double oldWeight = npc.getWeight();
        double adultW = getStat(stats, "adult_weight");
        npc.setWeight(Math.min(npc.getWeight() + gain, adultW));
        double pct = adultW > 0 ? npc.getWeight() / adultW : 1.0;
        pct = Math.max(0.0, Math.min(pct, 1.0));
        double baseAtk = getStat(stats, "attack");
        npc.setAttack(baseAtk * pct);
        double oldMax = scaleByWeight(oldWeight, adultW, getStat(stats, "hp"));
        double newMax = scaleByWeight(npc.getWeight(), adultW, getStat(stats, "hp"));
        double ratio = oldMax <= 0 ? 1.0 : npc.getHp() / oldMax;
        npc.setMaxHp(newMax);
        npc.setHp(newMax * ratio);
    }

    private void npcConsumePlant(int tx, int ty, NPCAnimal npc, Plant plant, Object stats) {
        double energyNeeded = 100.0 - npc.getEnergy();
        double weightForEnergy = energyNeeded * npc.getWeight() / 1000.0;
        double growthTarget = npcMaxGrowthGain(npc.getWeight(), stats);
        double eatAmount = Math.min(plant.getWeight(), weightForEnergy + growthTarget);
        double energyGainPossible = 1000 * eatAmount / Math.max(npc.getWeight(), 0.1);
        double actualGain = Math.min(energyNeeded, energyGainPossible);
        npc.setEnergy(Math.min(100.0, npc.getEnergy() + actualGain));
        double used = actualGain * npc.getWeight() / 1000.0;
        double remaining = eatAmount - used;
        npcApplyGrowth(npc, remaining, stats);
        plant.setWeight(plant.getWeight() - eatAmount);
        if (tx == x && ty == y && eatAmount > 0) {
            String msg = npcLabel(npc) + " eats "
                    + String.format(java.util.Locale.US, "%.1f", eatAmount)
                    + "kg of " + plant.getName() + ".";
            turnMessages.add(msg);
        }
    }

    private void npcConsumeMeat(int tx, int ty, NPCAnimal npc, NPCAnimal carcass, Object stats) {
        double energyNeeded = 100.0 - npc.getEnergy();
        double weightForEnergy = energyNeeded * npc.getWeight() / 1000.0;
        double growthTarget = npcMaxGrowthGain(npc.getWeight(), stats);
        double eatAmount = Math.min(carcass.getWeight(), weightForEnergy + growthTarget);
        double energyGainPossible = 1000 * eatAmount / Math.max(npc.getWeight(), 0.1);
        double actualGain = Math.min(energyNeeded, energyGainPossible);
        npc.setEnergy(Math.min(100.0, npc.getEnergy() + actualGain));
        double used = actualGain * npc.getWeight() / 1000.0;
        double remaining = eatAmount - used;
        npcApplyGrowth(npc, remaining, stats);
        carcass.setWeight(carcass.getWeight() - eatAmount);
        if (tx == x && ty == y && eatAmount > 0) {
            String msg = npcLabel(npc) + " eats "
                    + String.format(java.util.Locale.US, "%.1f", eatAmount)
                    + "kg from the " + npcLabel(carcass) + " carcass.";
            turnMessages.add(msg);
        }
    }

    private void npcConsumeEggs(NPCAnimal npc, EggCluster egg, Object stats) {
        double energyNeeded = 100.0 - npc.getEnergy();
        double growthTarget = npcMaxGrowthGain(npc.getWeight(), stats);
        double eatAmount = egg.getWeight();
        double energyGainPossible = 1000 * eatAmount / Math.max(npc.getWeight(), 0.1);
        double actualGain = Math.min(energyNeeded, energyGainPossible);
        npc.setEnergy(Math.min(100.0, npc.getEnergy() + actualGain));
        double used = actualGain * npc.getWeight() / 1000.0;
        double remaining = eatAmount - used;
        npcApplyGrowth(npc, remaining, stats);
        egg.setWeight(egg.getWeight() - eatAmount);
        if (eatAmount > 0) {
            npc.setEggClustersEaten(npc.getEggClustersEaten() + 1);
        }
    }

    private boolean npcDigBurrow(int x, int y) {
        Burrow b = map.getBurrow(x, y);
        if (b == null || !b.isFull()) {
            return false;
        }
        b.setFull(false);
        b.setProgress(0.0);
        if (!mammalSpecies.isEmpty()) {
            String name = mammalSpecies.get(new Random().nextInt(mammalSpecies.size()));
            java.util.Map<String, Object> stats = StatsLoader.getCritterStats().get(name);
            double weight = 0.0;
            Object wObj = stats.get("adult_weight");
            if (wObj instanceof Number n) weight = n.doubleValue();
            double hp = scaleByWeight(weight, getStat(stats, "adult_weight"), getStat(stats, "hp"));
            NPCAnimal npc = new NPCAnimal();
            npc.setId(nextNpcId++);
            npc.setName(name);
            npc.setWeight(weight);
            npc.setMaxHp(hp);
            npc.setHp(hp);
            Object abil = stats.get("abilities");
            if (abil instanceof java.util.List<?> list) {
                java.util.List<String> abilList = new java.util.ArrayList<>();
                for (Object a : list) {
                    abilList.add(a.toString());
                }
                npc.setAbilities(abilList);
            }
            npc.setLastAction("spawned");
            map.addAnimal(x, y, npc);
            spawned.add(npc);
        }
        return true;
    }

    private double npcEffectiveSpeed(NPCAnimal npc, Object stats) {
        double speed;
        double adultW = getStat(stats, "adult_weight");
        double hatchSpeed = getStat(stats, "hatchling_speed");
        double adultSpeed = getStat(stats, "adult_speed");
        if (hatchSpeed > 0 || adultSpeed > 0) {
            speed = statFromWeight(npc.getWeight(), adultW, hatchSpeed, adultSpeed);
        } else {
            speed = adultSpeed;
        }
        if (npc.getAbilities().contains("ambush")) {
            speed *= 1 + Math.min(npc.getAmbushStreak(), 3) * 0.05;
        }
        if (npc.getBrokenBone() > 0) {
            speed *= 0.5;
        }
        return Math.max(speed, 0.1);
    }

    private boolean npcTryHunt(int tx, int ty, NPCAnimal npc, Object stats,
                               List<NPCAnimal> animals, double adultWeight) {
        if (!statsDietHas(stats, "meat")) {
            return false;
        }
        Random r = new Random();
        double npcSpeed = npcEffectiveSpeed(npc, stats);
        double npcAtk = npcEffectiveAttack(npc, stats, tx, ty);
        double npcHp = scaleByWeight(npc.getWeight(), adultWeight, getStat(stats, "hp"));

        List<PotentialTarget> options = new ArrayList<>();
        for (NPCAnimal other : animals) {
            if (other == npc || !other.isAlive()) {
                continue;
            }
            Object oStats = StatsLoader.getDinoStats().get(other.getName());
            if (oStats == null) {
                oStats = StatsLoader.getCritterStats().get(other.getName());
            }
            if (oStats == null) {
                continue;
            }
            double oAtk = npcEffectiveAttack(other, oStats, tx, ty);
            double oHp = scaleByWeight(other.getWeight(), getStat(oStats, "adult_weight"), getStat(oStats, "hp"));
            if (!npcDamageAdvantage(npcAtk, npcHp, stats, oAtk, oHp, oStats)) {
                continue;
            }
            double oSpeed = npcEffectiveSpeed(other, oStats);
            if (oSpeed >= npcSpeed) {
                continue;
            }
            if (other.getWeight() < npc.getWeight() * 0.01) {
                continue;
            }
            options.add(new PotentialTarget(other, oSpeed, oAtk, oStats));
        }

        if (options.isEmpty()) {
            return false;
        }

        PotentialTarget pt = options.get(r.nextInt(options.size()));
        double relSpeed = pt.speed / Math.max(npcSpeed, 0.1);
        if (r.nextDouble() > calculateCatchChance(relSpeed)) {
            return false;
        }

        double beforeHunter = npc.getHp();
        double dmgHunter = damageAfterArmor(pt.attack, pt.stats, stats);
        applyDamage(dmgHunter, npc, stats);
        double dealtHunter = beforeHunter - npc.getHp();
        if (dealtHunter > 0 && pt.npc.getAbilities().contains("bleed") && npc.isAlive()) {
            int bleedTurns = (npc.getAbilities().contains("light_armor") || npc.getAbilities().contains("heavy_armor")) ? 2 : 5;
            npc.setBleeding(bleedTurns);
        }
        if (dealtHunter > 0 && pt.npc.getAbilities().contains("bone_break") && pt.npc.getWeight() >= npc.getWeight() / 3 && npc.isAlive()) {
            npc.setBrokenBone(10);
        }
        if (tx == x && ty == y && dealtHunter > 0) {
            turnMessages.add(npcLabel(pt.npc) + " deals " +
                    String.format(java.util.Locale.US, "%.0f", dealtHunter) + " damage to " + npcLabel(npc) + ".");
        }

        double beforeTarget = pt.npc.getHp();
        double dmgTarget = damageAfterArmor(npcAtk, stats, pt.stats);
        boolean killed = applyDamage(dmgTarget, pt.npc, pt.stats);
        double dealtTarget = beforeTarget - pt.npc.getHp();
        if (dealtTarget > 0 && npc.getAbilities().contains("bleed") && pt.npc.isAlive()) {
            int bleedTurns = (pt.npc.getAbilities().contains("light_armor") || pt.npc.getAbilities().contains("heavy_armor")) ? 2 : 5;
            pt.npc.setBleeding(bleedTurns);
            if (npc.getEnergy() >= 30 && !killed && pt.npc.getBleeding() == 5) {
                npc.setBleedWaitTurns(4);
                npc.setBleedWaitTarget(pt.npc.getId());
            }
        }
        if (dealtTarget > 0 && npc.getAbilities().contains("bone_break") && npc.getWeight() >= pt.npc.getWeight() / 3 && pt.npc.isAlive()) {
            pt.npc.setBrokenBone(10);
        }
        if (tx == x && ty == y && dealtTarget > 0) {
            turnMessages.add(npcLabel(npc) + " deals " +
                    String.format(java.util.Locale.US, "%.0f", dealtTarget) + " damage to " + npcLabel(pt.npc) + ".");
        }
        if (killed) {
            java.util.Map<String, Integer> hunts = npc.getHunts();
            hunts.put(pt.npc.getName(), hunts.getOrDefault(pt.npc.getName(), 0) + 1);
            npcConsumeMeat(tx, ty, npc, pt.npc, stats);
            if (pt.npc.getWeight() <= 0) {
                map.removeAnimal(tx, ty, pt.npc);
            }
            if (tx == x && ty == y) {
                turnMessages.add(npcLabel(npc) + " kills " + npcLabel(pt.npc) + ".");
            }
        }

        if (npc.getHp() <= 0) {
            npc.setAlive(false);
            npc.setAge(-1);
            npc.setSpeed(0.0);
            if (tx == x && ty == y) {
                turnMessages.add(npcLabel(pt.npc) + " kills " + npcLabel(npc) + ".");
            }
        }

        npc.setNextMove("None");
        npc.setLastAction("act");
        return true;
    }

    private void npcChooseMove(int x, int y, NPCAnimal npc, Object stats) {
        Random r = new Random();
        if (r.nextDouble() < 0.5) {
            npc.setNextMove("None");
            return;
        }
        java.util.Map<String, int[]> dirs = java.util.Map.of(
                "Up", new int[]{0, -1},
                "Right", new int[]{1, 0},
                "Down", new int[]{0, 1},
                "Left", new int[]{-1, 0});
        boolean canWalk = !getBool(stats, "can_walk", true) ? false : true;
        List<String> candidates = new ArrayList<>();
        List<String> preferredCandidates = new ArrayList<>();
        List<String> prefBiomes = preferredBiomes(stats);
        for (var e : dirs.entrySet()) {
            int nx = x + e.getValue()[0];
            int ny = y + e.getValue()[1];
            if (nx < 0 || ny < 0 || nx >= map.getWidth() || ny >= map.getHeight())
                continue;
            Terrain t = map.terrainAt(nx, ny);
            if (t == Terrain.TOXIC_BADLANDS) continue;
            if (!canWalk && t != Terrain.LAKE) continue;
            candidates.add(e.getKey());
            if (prefBiomes.contains(t.getName())) {
                preferredCandidates.add(e.getKey());
            }
        }
        String moveChoice = null;
        if (!preferredCandidates.isEmpty() && !candidates.isEmpty() && r.nextDouble() < 0.2) {
            moveChoice = candidates.get(r.nextInt(candidates.size()));
        } else if (!preferredCandidates.isEmpty()) {
            moveChoice = preferredCandidates.get(r.nextInt(preferredCandidates.size()));
        } else if (!candidates.isEmpty()) {
            moveChoice = candidates.get(r.nextInt(candidates.size()));
        }
        npc.setNextMove(moveChoice != null ? moveChoice : "None");
    }

    /**
     * Choose a direction for the NPC and always move if a candidate exists.
     */
    private void npcChooseMoveForced(int x, int y, NPCAnimal npc, Object stats) {
        Random r = new Random();
        java.util.Map<String, int[]> dirs = java.util.Map.of(
                "Up", new int[]{0, -1},
                "Right", new int[]{1, 0},
                "Down", new int[]{0, 1},
                "Left", new int[]{-1, 0});
        boolean canWalk = !getBool(stats, "can_walk", true) ? false : true;
        List<String> candidates = new ArrayList<>();
        for (var e : dirs.entrySet()) {
            int nx = x + e.getValue()[0];
            int ny = y + e.getValue()[1];
            if (nx < 0 || ny < 0 || nx >= map.getWidth() || ny >= map.getHeight())
                continue;
            Terrain t = map.terrainAt(nx, ny);
            if (t == Terrain.TOXIC_BADLANDS) continue;
            if (!canWalk && t != Terrain.LAKE) continue;
            candidates.add(e.getKey());
        }
        if (candidates.isEmpty()) {
            npc.setNextMove("None");
        } else {
            npc.setNextMove(candidates.get(r.nextInt(candidates.size())));
        }
    }


    private void moveNpcs() {
        class Move { int x; int y; int nx; int ny; NPCAnimal npc; Move(int x,int y,int nx,int ny,NPCAnimal n){this.x=x;this.y=y;this.nx=nx;this.ny=ny;this.npc=n;} }
        List<Move> moves = new ArrayList<>();
        java.util.Map<String,int[]> dirs = java.util.Map.of(
                "Up", new int[]{0,-1},
                "Right", new int[]{1,0},
                "Down", new int[]{0,1},
                "Left", new int[]{-1,0});
        for (int ty=0; ty<map.getHeight(); ty++) {
            for (int tx=0; tx<map.getWidth(); tx++) {
                for (NPCAnimal npc : map.getAnimals(tx, ty)) {
                    String d = npc.getNextMove();
                    if (d == null || d.equals("None")) continue;
                    if (npc.getBleeding() > 0 || !npc.isAlive()) { npc.setNextMove("None"); continue; }
                    int[] dd = dirs.getOrDefault(d, new int[]{0,0});
                    int nx = tx + dd[0];
                    int ny = ty + dd[1];
                    if (nx>=0 && nx<map.getWidth() && ny>=0 && ny<map.getHeight()) {
                        moves.add(new Move(tx,ty,nx,ny,npc));
                    }
                }
            }
        }
        for (Move m : moves) {
            map.removeAnimal(m.x, m.y, m.npc);
            map.addAnimal(m.nx, m.ny, m.npc);
            m.npc.setNextMove("None");
        }
    }

    // ------------------------------------------------------------------
    // Player growth and combat helpers
    // ------------------------------------------------------------------

    private double maxGrowthGain() {
        double weight = player.getWeight();
        double adult = player.getAdultWeight();
        if (weight >= adult) return 0.0;
        double maxWeight = adult * 1.05;
        double r = player.getGrowthRate();
        if (r == 0.0) r = 0.35;
        double gain = r * weight * (1 - weight / maxWeight);
        return Math.min(gain, adult - weight);
    }

    private double[] applyGrowth(double available) {
        double maxGain = maxGrowthGain();
        double weightGain = Math.min(available, maxGain);
        double oldWeight = player.getWeight();
        player.setWeight(Math.min(player.getWeight() + weightGain, player.getAdultWeight()));
        if (player.getAdultWeight() > 0) {
            double pct = player.getWeight() / player.getAdultWeight();
            pct = Math.max(0.0, Math.min(pct, 1.0));
            player.setAttack(player.getAdultAttack() * pct);
            double oldMax = statFromWeight(oldWeight, player.getAdultWeight(), player.getHatchlingHp(), player.getAdultHp());
            double newMax = statFromWeight(player.getWeight(), player.getAdultWeight(), player.getHatchlingHp(), player.getAdultHp());
            double ratio = oldMax <= 0 ? 1.0 : player.getHp() / oldMax;
            player.setMaxHp(newMax);
            player.setHp(newMax * ratio);
            player.setSpeed(statFromWeight(player.getWeight(), player.getAdultWeight(),
                    player.getHatchlingSpeed(), player.getAdultSpeed()));
        }
        return new double[]{weightGain, maxGain};
    }

    private boolean canPlayerLayEggs() {
        List<NPCAnimal> animals = map.getAnimals(x, y);
        return player.getWeight() >= player.getAdultWeight()
                && player.getEnergy() >= 80
                && player.getHp() >= player.getMaxHp() * 0.8
                && player.getTurnsUntilLayEggs() == 0
                && animals.size() < 4;
    }

    private List<String> abilities(Object stats) {
        if (stats instanceof DinosaurStats ds) {
            return ds.getAbilities();
        } else if (stats instanceof java.util.Map<?,?> map) {
            Object val = map.get("abilities");
            if (val instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) out.add(o.toString());
                return out;
            }
        }
        return List.of();
    }

    private List<String> preferredBiomes(Object stats) {
        if (stats instanceof DinosaurStats ds) {
            List<String> pref = ds.getPreferredBiomes();
            return pref != null ? pref : List.of();
        } else if (stats instanceof java.util.Map<?,?> map) {
            Object val = map.get("preferred_biomes");
            if (val instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) out.add(o.toString());
                return out;
            }
        }
        return List.of();
    }

    private double effectiveArmor(Object targetStats, Object attackerStats) {
        List<String> abil = abilities(targetStats);
        double base = 0.0;
        if (abil.contains("heavy_armor")) base = 40.0;
        else if (abil.contains("light_armor")) base = 20.0;
        if (abilities(attackerStats).contains("bone_break")) base *= 0.5;
        return Math.max(0.0, base);
    }

    private double damageAfterArmor(double dmg, Object attackerStats, Object targetStats) {
        double eff = effectiveArmor(targetStats, attackerStats);
        return dmg * Math.max(0.0, 1.0 - eff / 100.0);
    }

    private boolean applyDamage(double damage, DinosaurStats dino, DinosaurStats stats) {
        double maxHp = statFromWeight(dino.getWeight(), stats.getAdultWeight(), stats.getHatchlingHp(), stats.getAdultHp());
        dino.setMaxHp(maxHp);
        if (dino.getHp() > maxHp) dino.setHp(maxHp);
        dino.setHp(Math.max(0.0, dino.getHp() - damage));
        return dino.getHp() <= 0;
    }

    private boolean applyDamage(double damage, NPCAnimal npc, Object stats) {
        double maxHp = scaleByWeight(npc.getWeight(), getStat(stats, "adult_weight"), getStat(stats, "hp"));
        npc.setMaxHp(maxHp);
        if (npc.getHp() > maxHp) npc.setHp(maxHp);
        npc.setHp(Math.max(0.0, npc.getHp() - damage));
        boolean died = npc.getHp() <= 0;
        if (died) { npc.setAlive(false); npc.setAge(-1); npc.setSpeed(0.0); }
        return died;
    }

    private boolean npcDamageAdvantage(double hunterAtk, double hunterHp, Object hunterStats,
                                         double targetAtk, double targetHp, Object targetStats) {
        double dmgToTarget = damageAfterArmor(hunterAtk, hunterStats, targetStats);
        double dmgToHunter = damageAfterArmor(targetAtk, targetStats, hunterStats);

        int targetBleed = 0;
        int hunterBleed = 0;
        if (dmgToTarget > 0 && abilities(hunterStats).contains("bleed")) {
            if (abilities(targetStats).contains("light_armor") || abilities(targetStats).contains("heavy_armor"))
                targetBleed = 2; else targetBleed = 5;
        }
        if (dmgToHunter > 0 && abilities(targetStats).contains("bleed")) {
            if (abilities(hunterStats).contains("light_armor") || abilities(hunterStats).contains("heavy_armor"))
                hunterBleed = 2; else hunterBleed = 5;
        }

        boolean bleed = targetBleed > 0 || hunterBleed > 0;
        double bleedDmgTarget = bleed ? targetBleed * 0.05 * targetHp : 0.0;
        double bleedDmgHunter = bleed ? hunterBleed * 0.05 * hunterHp : 0.0;

        double regenDmgTarget = 0.0;
        double regenDmgHunter = 0.0;
        if (bleed) {
            double regenTarget = getStat(targetStats, "health_regen");
            double regenHunter = getStat(hunterStats, "health_regen");
            int regenTurnsTarget = Math.max(0, 5 - targetBleed);
            int regenTurnsHunter = Math.max(0, 5 - hunterBleed);
            regenDmgTarget = -regenTarget / 100.0 * targetHp * regenTurnsTarget;
            regenDmgHunter = -regenHunter / 100.0 * hunterHp * regenTurnsHunter;
        }

        double totalTarget = Math.max(0.0, dmgToTarget + bleedDmgTarget + regenDmgTarget);
        double totalHunter = Math.max(0.0, dmgToHunter + bleedDmgHunter + regenDmgHunter);

        double pctTarget = totalTarget / Math.max(targetHp, 0.1);
        double pctHunter = totalHunter / Math.max(hunterHp, 0.1);

        return pctHunter < pctTarget;
    }

    /** Apply end-of-turn terrain effects similar to the Python version. */
    void _apply_terrain_effects() {
        String terrain = map.terrainAt(x, y).getName();
        if (terrain.equals("lava") || terrain.equals("volcano_erupting") ||
                terrain.equals("forest_fire") ||
                terrain.equals("highland_forest_fire")) {
            player.setHp(0.0);
        }
        if (terrain.equals("toxic_badlands")) {
            double dmg = player.getMaxHp() * 0.2;
            player.setHp(Math.max(0.0, player.getHp() - dmg));
        }

        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                String tname = map.terrainAt(tx, ty).getName();
                if (tname.equals("lava") || tname.equals("volcano_erupting") ||
                        tname.equals("forest_fire") ||
                        tname.equals("highland_forest_fire")) {
                    for (NPCAnimal npc : map.getAnimals(tx, ty)) {
                        npc.setAlive(false);
                        npc.setAge(-1);
                        npc.setSpeed(0.0);
                    }
                    map.getEggs(tx, ty).clear();
                    map.removeBurrow(tx, ty);
                    map.getPlants(tx, ty).clear();
                } else if (tname.equals("toxic_badlands")) {
                    for (NPCAnimal npc : map.getAnimals(tx, ty)) {
                        if (!npc.isAlive()) continue;
                        double dmg = npc.getMaxHp() * 0.2;
                        npc.setHp(Math.max(0.0, npc.getHp() - dmg));
                        if (npc.getHp() <= 0) {
                            npc.setAlive(false);
                            npc.setAge(-1);
                            npc.setSpeed(0.0);
                        }
                    }
                }
            }
        }
    }

    private void checkVictory() {
        if (!won && descendantCount() >= DESCENDANTS_TO_WIN) {
            won = true;
        }
    }

    public java.util.Map<String, Integer> populationStats() {
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                for (NPCAnimal npc : map.getAnimals(tx, ty)) {
                    counts.merge(npc.getName(), 1, Integer::sum);
                }
            }
        }
        if (player.getName() != null) {
            counts.merge(player.getName(), 1, Integer::sum);
        }
        return counts;
    }

    public int descendantCount() {
        int count = 0;
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                for (NPCAnimal npc : map.getAnimals(tx, ty)) {
                    if (npc.isDescendant() && npc.isAlive()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void recordPopulation() {
        java.util.Map<String, Integer> counts = populationStats();
        for (String name : populationHistory.keySet()) {
            List<Integer> list = populationHistory.get(name);
            list.add(counts.getOrDefault(name, 0));
        }
        turnHistory.add(turn);
    }


    public Map getMap() {
        return map;
    }

    public DinosaurStats getPlayer() {
        return player;
    }

    public java.util.List<Integer> getPopulationHistory(String name) {
        java.util.List<Integer> list = populationHistory.get(name);
        return list != null ? java.util.Collections.unmodifiableList(list)
                : java.util.List.of();
    }

    public java.util.List<Integer> getTurnHistory() {
        return java.util.Collections.unmodifiableList(turnHistory);
    }

    public java.util.List<String> getTurnMessages() {
        return java.util.Collections.unmodifiableList(turnMessages);
    }

    public java.util.Map<String, int[]> getHuntStats() {
        return java.util.Collections.unmodifiableMap(huntStats);
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

    public String getFormation() {
        return formation;
    }

    /**
     * Effective speed value for the player dinosaur.
     */
    public double playerEffectiveSpeed() {
        double speed = player.getSpeed();
        Terrain terrain = map.terrainAt(x, y);
        double boost = 0.0;
        if (terrain == Terrain.LAKE) {
            boost = player.getAquaticBoost();
        } else if (terrain == Terrain.SWAMP) {
            boost = player.getAquaticBoost() / 2.0;
        }
        speed *= 1 + boost / 100.0;
        if (player.getAbilities().contains("ambush")) {
            speed *= 1 + Math.min(player.getAmbushStreak(), 3) * 0.05;
        }
        if (player.getBrokenBone() > 0) {
            speed *= 0.5;
        }
        return Math.max(speed, 0.1);
    }

    /**
     * Return true if the player can currently lay eggs.
     */
    public boolean playerCanLayEggs() {
        return canPlayerLayEggs();
    }

    /**
     * Determine the growth stage description for the player dinosaur.
     */
    public String playerGrowthStage() {
        double adult = player.getAdultWeight();
        if (adult <= 0) {
            return "Adult";
        }
        double pct = player.getWeight() / adult;
        if (pct <= 0.10) {
            return "Hatchling";
        }
        if (pct <= 1.0 / 3.0) {
            return "Juvenile";
        }
        if (pct <= 2.0 / 3.0) {
            return "Sub-Adult";
        }
        return "Adult";
    }

    /**
     * Get the list of encounters on the player's current tile.
     */
    public java.util.List<EncounterEntry> getCurrentEncounters() {
        return currentEncounters;
    }

    /**
     * Effective attack value for the given NPC.
     */
    public double npcEffectiveAttack(NPCAnimal npc) {
        Object stats = StatsLoader.getDinoStats().get(npc.getName());
        if (stats == null) {
            stats = StatsLoader.getCritterStats().get(npc.getName());
        }
        return npcEffectiveAttack(npc, stats, x, y);
    }

    /**
     * Effective speed value for the given NPC.
     */
    public double npcEffectiveSpeed(NPCAnimal npc) {
        Object stats = StatsLoader.getDinoStats().get(npc.getName());
        if (stats == null) {
            stats = StatsLoader.getCritterStats().get(npc.getName());
        }
        return npcEffectiveSpeed(npc, stats);
    }

    /**
     * Calculate the chance of catching prey based on relative speed of the prey.
     */
    public double calculateCatchChance(double relSpeed) {
        if (relSpeed < 0.5) {
            return 1.0;
        }
        if (relSpeed <= 1.0) {
            return 1.0 - (relSpeed - 0.5);
        }
        return 0.0;
    }

    /**
     * Maximum health for the given NPC based on its weight.
     */
    public double npcMaxHp(NPCAnimal npc) {
        Object stats = StatsLoader.getDinoStats().get(npc.getName());
        if (stats == null) {
            stats = StatsLoader.getCritterStats().get(npc.getName());
        }
        return scaleByWeight(npc.getWeight(), getStat(stats, "adult_weight"),
                getStat(stats, "hp"));
    }

    /**
     * Get the list of plants on the player's current tile.
     */
    public java.util.List<Plant> getCurrentPlants() {
        return currentPlants;
    }

    public boolean hasWon() {
        return won;
    }
}
