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

    /** Number of descendants required to win the game. */
    public static final int DESCENDANTS_TO_WIN = 5;

    /**
     * Initialise the game world. Statistics are loaded from the YAML files and
     * a new map is generated. This mirrors the behaviour of the Python
     * {@code Game.__init__} method so that the Swing UI can display a running
     * world without depending on the Python code.
     */
    public void start() {
        try {
            StatsLoader.load(Path.of("dinosurvival"), "Morrison");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        map = new Map(18, 10);
        map.populateBurrows(5);

        // pick an arbitrary playable dinosaur
        if (!StatsLoader.getDinoStats().isEmpty()) {
            player = cloneStats(StatsLoader.getDinoStats().values().iterator().next());
            player.setWeight(player.getHatchlingWeight());
            double pct = player.getAdultWeight() > 0
                    ? player.getWeight() / player.getAdultWeight() : 1.0;
            pct = Math.max(0.0, Math.min(1.0, pct));
            player.setAttack(player.getAdultAttack() * pct);
            player.setMaxHp(player.getAdultHp() * pct);
            player.setHp(player.getMaxHp());
            player.setSpeed(statFromWeight(player.getWeight(),
                    player.getAdultWeight(),
                    player.getHatchlingSpeed(),
                    player.getAdultSpeed()));
        } else {
            player = new DinosaurStats();
        }

        chooseStartingLocation();
        map.reveal(x, y);
        weather = chooseWeather();
        weatherTurns = 0;
        _populateAnimals();
        _spawnCritters(true);
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

    /** Populate the map with initial dinosaur NPCs. */
    private void _populateAnimals() {
        List<int[]> land = new ArrayList<>();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                Terrain t = map.terrainAt(tx, ty);
                if (t != Terrain.LAKE && t != Terrain.TOXIC_BADLANDS) {
                    land.add(new int[]{tx, ty});
                }
            }
        }

        Random r = new Random();
        StatsLoader.getDinoStats().forEach((name, stats) -> {
            int spawnCount = (int) Math.max(1, stats.getAdultWeight() / 1000);
            for (int i = 0; i < spawnCount && !land.isEmpty(); i++) {
                int[] pos = land.get(r.nextInt(land.size()));
                NPCAnimal npc = new NPCAnimal();
                npc.setId(nextNpcId++);
                npc.setName(name);
                npc.setWeight(stats.getAdultWeight());
                npc.setMaxHp(stats.getAdultHp());
                npc.setHp(npc.getMaxHp());
                map.addAnimal(pos[0], pos[1], npc);
                spawned.add(npc);
            }
        });
    }

    /** Spawn critter NPCs either for the initial game setup or a normal turn. */
    private void _spawnCritters(boolean initial) {
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

    private void updateNpcs() {
        for (NPCAnimal npc : spawned) {
            if (!npc.isAlive()) continue;
            npc.setEnergy(Math.max(0.0, npc.getEnergy() - 1.0 * weather.getNpcEnergyMult()));
            if (npc.getEnergy() <= 0) {
                npc.setAlive(false);
            }
        }
    }

    /**
     * Load encounter information for the player's current tile.
     */
    private void _generateEncounters() {
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

    private double playerEffectiveAttack() {
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
    private String _aggressiveAttackCheck() {
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

    private void _updateEggs() {
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

    private void _applyBleedAndRegen(DinosaurStats dino, double regen,
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

    private void _applyTurnCosts(boolean moved, double multiplier) {
        double drain = player.getHatchlingEnergyDrain();
        if (moved) {
            drain *= player.getWalkingEnergyDrainMultiplier();
            if (player.getBrokenBone() > 0) {
                drain *= 2;
            }
        }
        drain *= multiplier;
        drain *= weather.getPlayerEnergyMult();
        player.setEnergy(Math.max(0.0, player.getEnergy() - drain));
        if (player.isExhausted()) {
            player.setHp(0.0);
        }
        _applyBleedAndRegen(player, player.getHealthRegen(), moved, !player.isExhausted());
    }

    private void _startTurn() {
        turn++;
        if (weatherTurns >= 10) {
            weather = chooseWeather();
            weatherTurns = 0;
        }
        weatherTurns++;

        if (player.getAbilities().contains("ambush")) {
            if ("stay".equals(lastAction)) {
                player.setAmbushStreak(Math.min(player.getAmbushStreak() + 1, 3));
            } else {
                player.setAmbushStreak(0);
            }
        }

        map.updateVolcanicActivity();
        map.updateFlood(weather.getFloodChance());
        map.updateForestFire();
        _updateEggs();
        map.growPlants(StatsLoader.getPlantStats());
        _spawnCritters(false);
        map.refreshBurrows();
        if (player.getTurnsUntilLayEggs() > 0) {
            player.setTurnsUntilLayEggs(player.getTurnsUntilLayEggs() - 1);
        }

        player.setHydration(Math.max(0.0,
                player.getHydration() - player.getHydrationDrain() * weather.getPlayerHydrationMult()));
        if (player.isDehydrated()) {
            player.setHp(0.0);
        }

        updateNpcs();
        _generateEncounters();
        _aggressiveAttackCheck();
    }

    /** Move the player by the specified delta. */
    public void move(int dx, int dy) {
        _startTurn();
        x = Math.max(0, Math.min(map.getWidth() - 1, x + dx));
        y = Math.max(0, Math.min(map.getHeight() - 1, y + dy));
        map.reveal(x, y);
        _generateEncounters();
        _aggressiveAttackCheck();
        _applyTurnCosts(true, 1.0);
        lastAction = "move";
    }

    /** Skip a turn without moving. */
    public void rest() {
        _startTurn();
        _generateEncounters();
        _aggressiveAttackCheck();
        _applyTurnCosts(false, 1.0);
        lastAction = "stay";
    }

    /** Drink if the player is on a lake tile. */
    public void drink() {
        _startTurn();
        if (map.terrainAt(x, y) == Terrain.LAKE) {
            player.setHydration(100.0);
        }
        _generateEncounters();
        _aggressiveAttackCheck();
        _applyTurnCosts(false, 1.0);
        lastAction = "drink";
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
