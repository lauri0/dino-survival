package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.model.Plant;
import com.dinosurvival.util.StatsLoader;
import com.dinosurvival.game.CombatUtils;

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
    private final List<String> mammalSpecies = new ArrayList<>();

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

    public void initMammalSpecies(String formation) {
        mammalSpecies.clear();
        for (var entry : StatsLoader.getCritterStats().entrySet()) {
            Object cls = entry.getValue().get("class");
            if (cls != null && cls.toString().equals("mammal")) {
                mammalSpecies.add(entry.getKey());
            }
        }
        if (mammalSpecies.isEmpty() && "Hell Creek".equals(formation)
                && StatsLoader.getCritterStats().containsKey("Didelphodon")) {
            mammalSpecies.add("Didelphodon");
        }
    }

    public List<String> getMammalSpecies() {
        return mammalSpecies;
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
                    double maxHp = CombatUtils.scaleByWeight(weight, stats.getAdultWeight(), stats.getAdultHp());
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

    // ------------------------------------------------------------------
    // Helper methods for NPC behaviour
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
                case "num_eggs" -> ds.getNumEggs();
                case "egg_laying_interval" -> ds.getEggLayingInterval();
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

    private boolean npcApplyWalkingDrain(NPCAnimal npc, double baseDrain) {
        double extra = baseDrain * (Game.WALKING_ENERGY_DRAIN_MULTIPLIER - 1.0);
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
        double oldMax = CombatUtils.scaleByWeight(oldWeight, adultW, getStat(stats, "hp"));
        double newMax = CombatUtils.scaleByWeight(npc.getWeight(), adultW, getStat(stats, "hp"));
        double ratio = oldMax <= 0 ? 1.0 : npc.getHp() / oldMax;
        npc.setMaxHp(newMax);
        npc.setHp(newMax * ratio);
    }

    private void npcConsumePlant(int tx, int ty, NPCAnimal npc, Plant plant, Object stats,
                                 int playerX, int playerY, List<String> messages) {
        double energyNeeded = 100.0 - npc.getEnergy();
        double weightForEnergy = energyNeeded * npc.getWeight() / 1000.0;
        double growthTarget = npcMaxGrowthGain(npc.getWeight(), stats);
        double eatAmount = Math.min(plant.getWeight(), weightForEnergy + growthTarget);
        double energyGainPossible = 1000 * eatAmount / Math.max(npc.getWeight(), 0.1);
        double actualGain = Math.min(energyNeeded, energyGainPossible);
        double beforeEnergy = npc.getEnergy();
        npc.setEnergy(Math.min(100.0, beforeEnergy + actualGain));
        double used = actualGain * npc.getWeight() / 1000.0;
        double remaining = eatAmount - used;
        double beforeWeight = npc.getWeight();
        npcApplyGrowth(npc, remaining, stats);
        double weightGain = npc.getWeight() - beforeWeight;
        plant.setWeight(plant.getWeight() - eatAmount);
        if (tx == playerX && ty == playerY) {
            messages.add(npcLabel(npc) + " eats " + String.format(java.util.Locale.US, "%.1f", eatAmount)
                    + "kg and regains " + String.format(java.util.Locale.US, "%.1f", npc.getEnergy() - beforeEnergy)
                    + " energy gaining " + String.format(java.util.Locale.US, "%.1f", weightGain) + "kg.");
        }
    }

    private void npcConsumeMeat(int tx, int ty, NPCAnimal npc, NPCAnimal carcass, Object stats,
                                int playerX, int playerY, List<String> messages) {
        double energyNeeded = 100.0 - npc.getEnergy();
        double weightForEnergy = energyNeeded * npc.getWeight() / 1000.0;
        double growthTarget = npcMaxGrowthGain(npc.getWeight(), stats);
        double eatAmount = Math.min(carcass.getWeight(), weightForEnergy + growthTarget);
        double energyGainPossible = 1000 * eatAmount / Math.max(npc.getWeight(), 0.1);
        double actualGain = Math.min(energyNeeded, energyGainPossible);
        double beforeEnergy = npc.getEnergy();
        npc.setEnergy(Math.min(100.0, beforeEnergy + actualGain));
        double used = actualGain * npc.getWeight() / 1000.0;
        double remaining = eatAmount - used;
        double beforeWeight = npc.getWeight();
        npcApplyGrowth(npc, remaining, stats);
        double weightGain = npc.getWeight() - beforeWeight;
        carcass.setWeight(carcass.getWeight() - eatAmount);
        if (tx == playerX && ty == playerY) {
            messages.add(npcLabel(npc) + " eats " + String.format(java.util.Locale.US, "%.1f", eatAmount)
                    + "kg and regains " + String.format(java.util.Locale.US, "%.1f", npc.getEnergy() - beforeEnergy)
                    + " energy gaining " + String.format(java.util.Locale.US, "%.1f", weightGain) + "kg.");
        }
    }

    private void npcConsumeEggs(int tx, int ty, NPCAnimal npc, EggCluster egg, Object stats,
                                int playerX, int playerY, List<String> messages) {
        double energyNeeded = 100.0 - npc.getEnergy();
        double growthTarget = npcMaxGrowthGain(npc.getWeight(), stats);
        double eatAmount = egg.getWeight();
        double energyGainPossible = 1000 * eatAmount / Math.max(npc.getWeight(), 0.1);
        double actualGain = Math.min(energyNeeded, energyGainPossible);
        double beforeEnergy = npc.getEnergy();
        npc.setEnergy(Math.min(100.0, beforeEnergy + actualGain));
        double used = actualGain * npc.getWeight() / 1000.0;
        double remaining = eatAmount - used;
        double beforeWeight = npc.getWeight();
        npcApplyGrowth(npc, remaining, stats);
        double weightGain = npc.getWeight() - beforeWeight;
        egg.setWeight(egg.getWeight() - eatAmount);
        if (eatAmount > 0) {
            npc.setEggClustersEaten(npc.getEggClustersEaten() + 1);
        }
        if (tx == playerX && ty == playerY) {
            messages.add(npcLabel(npc) + " eats " + String.format(java.util.Locale.US, "%.1f", eatAmount)
                    + "kg and regains " + String.format(java.util.Locale.US, "%.1f", npc.getEnergy() - beforeEnergy)
                    + " energy gaining " + String.format(java.util.Locale.US, "%.1f", weightGain) + "kg.");
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
            Random r = new Random();
            String name = mammalSpecies.get(r.nextInt(mammalSpecies.size()));
            java.util.Map<String, Object> stats = StatsLoader.getCritterStats().get(name);
            double weight = 0.0;
            Object wObj = stats.get("adult_weight");
            if (wObj instanceof Number n) weight = n.doubleValue();
            double hp = CombatUtils.scaleByWeight(weight, getStat(stats, "adult_weight"), getStat(stats, "hp"));
            NPCAnimal npc = new NPCAnimal();
            npc.setId(allocateNpcId());
            npc.setName(name);
            npc.setWeight(weight);
            npc.setMaxHp(hp);
            npc.setHp(hp);
            Object abil = stats.get("abilities");
            if (abil instanceof java.util.List<?> list) {
                List<String> abilList = new ArrayList<>();
                for (Object a : list) {
                    abilList.add(a.toString());
                }
                npc.setAbilities(abilList);
            }
            npc.setLastAction("spawned");
            map.addAnimal(x, y, npc);
            trackSpawn(npc);
        }
        return true;
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

    private boolean npcHasPackmate(NPCAnimal npc, int tx, int ty) {
        for (NPCAnimal other : map.getAnimals(tx, ty)) {
            if (other == npc) continue;
            if (other.isAlive() && other.getName().equals(npc.getName())) {
                return true;
            }
        }
        return false;
    }

    private double npcEffectiveAttack(NPCAnimal npc, Object stats, int tx, int ty) {
        double adultWeight = 0.0;
        double baseAtk = 0.0;
        List<String> abil = null;
        if (stats instanceof DinosaurStats ds) {
            adultWeight = ds.getAdultWeight();
            baseAtk = ds.getAdultAttack();
            abil = ds.getAbilities();
        } else if (stats instanceof java.util.Map<?, ?> map) {
            Object aw = map.get("adult_weight");
            if (aw instanceof Number n) adultWeight = n.doubleValue();
            Object atk = map.get("attack");
            if (atk instanceof Number n) baseAtk = n.doubleValue();
            Object a = map.get("abilities");
            if (a instanceof List<?> list) {
                abil = new ArrayList<>();
                for (Object o : list) {
                    abil.add(o.toString());
                }
            }
        }
        double atk = CombatUtils.scaleByWeight(npc.getWeight(), adultWeight, baseAtk);
        if (abil != null && abil.contains("pack_hunter") && npcHasPackmate(npc, tx, ty)) {
            atk *= 3;
        }
        double hpPct = 1.0;
        if (npc.getMaxHp() > 0) {
            hpPct = Math.max(0.0, Math.min(npc.getHp() / npc.getMaxHp(), 1.0));
        }
        return atk * hpPct;
    }

    private double npcEffectiveSpeed(NPCAnimal npc, Object stats) {
        double speed;
        double adultW = getStat(stats, "adult_weight");
        double hatchSpeed = getStat(stats, "hatchling_speed");
        double adultSpeed = getStat(stats, "adult_speed");
        if (hatchSpeed > 0 || adultSpeed > 0) {
            speed = CombatUtils.statFromWeight(npc.getWeight(), adultW, hatchSpeed, adultSpeed);
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

    private double calculateCatchChance(double relSpeed) {
        if (relSpeed < 0.5) {
            return 1.0;
        }
        if (relSpeed <= 1.0) {
            return 1.0 - (relSpeed - 0.5);
        }
        return 0.0;
    }

    // ------------------------------------------------------------------
    // NPC turn processing
    // ------------------------------------------------------------------

    public List<String> updateNpcs(int playerX, int playerY) {
        List<String> messages = new ArrayList<>();
        updateNpcsInternal(playerX, playerY, messages);
        return messages;
    }

    public void updateNpcs() {
        updateNpcsInternal(-1, -1, new ArrayList<>());
    }

    private void updateNpcsInternal(int playerX, int playerY, List<String> messages) {
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
                        java.util.Map<String, Object> cstats = StatsLoader.getCritterStats().get(npc.getName());
                        npc.setNextMove("None");
                        if (cstats != null) {
                            npcChooseMove(tx, ty, npc, cstats);
                            double regen = getStat(cstats, "health_regen");
                            if (applyBleedAndRegen(npc, regen)) {
                                if (tx == playerX && ty == playerY) {
                                    messages.add(npcLabel(npc) + " bleeds to death.");
                                }
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
                        if (tx == playerX && ty == playerY) {
                            messages.add(npcLabel(npc) + " starves to death.");
                        }
                        continue;
                    }

                    double regen = getStat(stats, "health_regen");
                    if (applyBleedAndRegen(npc, regen)) {
                        if (tx == playerX && ty == playerY) {
                            messages.add(npcLabel(npc) + " bleeds to death.");
                        }
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
                                    if (tx == playerX && ty == playerY) {
                                        messages.add(npcLabel(npc) + " starves to death.");
                                    }
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
                                npcConsumeMeat(tx, ty, npc, carcass, stats, playerX, playerY, messages);
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
                                npcConsumeEggs(tx, ty, npc, targetEgg, stats, playerX, playerY, messages);
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
                                if (statsDietHas(stats, p.getName())) {
                                    if (chosen == null || p.getWeight() > chosen.getWeight()) {
                                        chosen = p;
                                    }
                                }
                            }
                            if (chosen != null) {
                                npcConsumePlant(tx, ty, npc, chosen, stats, playerX, playerY, messages);
                                if (chosen.getWeight() <= 0) {
                                    plants.remove(chosen);
                                }
                                npc.setLastAction("act");
                                continue;
                            }
                        }

                        if (npcTryHunt(tx, ty, npc, stats, animals, adultWeight, playerX, playerY, messages)) {
                            continue;
                        }
                    }

                    npcChooseMove(tx, ty, npc, stats);
                    if (!"None".equals(npc.getNextMove())) {
                        if (npcApplyWalkingDrain(npc, baseDrain)) {
                            if (tx == playerX && ty == playerY) {
                                messages.add(npcLabel(npc) + " starves to death.");
                            }
                            continue;
                        }
                        npc.setLastAction("move");
                    }
                }
            }
        }
        moveNpcs();
    }

    public void npcChooseMove(int x, int y, NPCAnimal npc, Object stats) {
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

    public void npcChooseMoveForced(int x, int y, NPCAnimal npc, Object stats) {
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

    public boolean npcTryHunt(int tx, int ty, NPCAnimal npc, Object stats,
                               List<NPCAnimal> animals, double adultWeight,
                               int playerX, int playerY, List<String> messages) {
        if (!statsDietHas(stats, "meat")) {
            return false;
        }
        Random r = new Random();
        double npcSpeed = npcEffectiveSpeed(npc, stats);
        double npcAtk = npcEffectiveAttack(npc, stats, tx, ty);
        double npcHp = CombatUtils.scaleByWeight(npc.getWeight(), adultWeight, getStat(stats, "hp"));

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
            double oHp = CombatUtils.scaleByWeight(other.getWeight(), getStat(oStats, "adult_weight"), getStat(oStats, "hp"));
            if (!CombatUtils.npcDamageAdvantage(npcAtk, npcHp, stats, oAtk, oHp, oStats)) {
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
        double dmgHunter = CombatUtils.damageAfterArmor(pt.attack, pt.stats, stats);
        CombatUtils.applyDamage(dmgHunter, npc, stats);
        double dealtHunter = beforeHunter - npc.getHp();
        if (dealtHunter > 0 && pt.npc.getAbilities().contains("bleed") && npc.isAlive()) {
            int bleedTurns = (npc.getAbilities().contains("light_armor") || npc.getAbilities().contains("heavy_armor")) ? 2 : 5;
            npc.setBleeding(bleedTurns);
        }
        if (dealtHunter > 0 && pt.npc.getAbilities().contains("bone_break") && pt.npc.getWeight() >= npc.getWeight() / 3 && npc.isAlive()) {
            npc.setBrokenBone(10);
        }

        double beforeTarget = pt.npc.getHp();
        double dmgTarget = CombatUtils.damageAfterArmor(npcAtk, stats, pt.stats);
        boolean killed = CombatUtils.applyDamage(dmgTarget, pt.npc, pt.stats);
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
        if (killed) {
            java.util.Map<String, Integer> hunts = npc.getHunts();
            hunts.put(pt.npc.getName(), hunts.getOrDefault(pt.npc.getName(), 0) + 1);
            if (tx == playerX && ty == playerY) {
                messages.add(npcLabel(npc) + " kills " + npcLabel(pt.npc) + ".");
            }
            npcConsumeMeat(tx, ty, npc, pt.npc, stats, playerX, playerY, messages);
            if (pt.npc.getWeight() <= 0) {
                map.removeAnimal(tx, ty, pt.npc);
            }
        }

        if (npc.getHp() <= 0) {
            npc.setAlive(false);
            npc.setAge(-1);
            npc.setSpeed(0.0);
        }

        npc.setNextMove("None");
        npc.setLastAction("act");
        return true;
    }

    public void moveNpcs() {
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

    // Class used by npcTryHunt
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

    private String npcLabel(NPCAnimal npc) {
        return npc.getName() + " (" + npc.getId() + ")";
    }

    /** Public helpers used by {@link Game}. */
    public double npcEffectiveAttack(NPCAnimal npc, int tx, int ty) {
        Object stats = StatsLoader.getDinoStats().get(npc.getName());
        if (stats == null) {
            stats = StatsLoader.getCritterStats().get(npc.getName());
        }
        return npcEffectiveAttack(npc, stats, tx, ty);
    }

    public double npcEffectiveSpeed(NPCAnimal npc) {
        Object stats = StatsLoader.getDinoStats().get(npc.getName());
        if (stats == null) {
            stats = StatsLoader.getCritterStats().get(npc.getName());
        }
        return npcEffectiveSpeed(npc, stats);
    }
}
