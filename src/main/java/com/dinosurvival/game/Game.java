package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.model.Plant;

import java.util.Iterator;
import com.dinosurvival.util.StatsLoader;
import com.dinosurvival.util.Constants;
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
    private PlayerManager playerManager = new PlayerManager();
    private int x;
    private int y;
    private Weather weather;
    private int weatherTurns;
    private final Random weatherRng = new Random(1);
    private Random rng = new Random();
    private NpcController npcController;
    private boolean won;
    private int turn;
    private List<EncounterEntry> currentEncounters = new ArrayList<>();
    private List<Plant> currentPlants = new ArrayList<>();
    private String lastAction = "";
    private final java.util.Map<String, List<Integer>> populationHistory = new java.util.HashMap<>();
    private final java.util.Map<String, int[]> huntStats = new java.util.HashMap<>();
    private final List<Integer> turnHistory = new ArrayList<>();
    private List<String> turnMessages = new ArrayList<>();
    private String formation;
    private WorldStats worldStats = new WorldStats();

    /** Number of descendants required to win the game. */
    public static final int DESCENDANTS_TO_WIN = Constants.DESCENDANTS_TO_WIN;

    /** Energy multiplier applied when an NPC walks. */
    public static final double WALKING_ENERGY_DRAIN_MULTIPLIER =
            Constants.WALKING_ENERGY_DRAIN_MULTIPLIER;

    /**
     * Initialise the game world. Statistics are loaded from the YAML files and
     * a new map is generated. This mirrors the behaviour of the Python
     * {@code Game.__init__} method so that the Swing UI can display a running
     * world without depending on the Python code.
     */
    public void start() {
        start(Settings.MORRISON, null, new Random().nextLong());
    }

    /**
     * Start a new game using the given formation and player dinosaur name.
     * If {@code dinoName} is null the first available dinosaur is used.
     */
    public void start(String formation, String dinoName) {
        start(Settings.forFormation(formation), dinoName, new Random().nextLong());
    }

    /**
     * Start a new game using the given formation and player dinosaur name with
     * the provided random seed for map generation.
     */
    public void start(String formation, String dinoName, long seed) {
        start(Settings.forFormation(formation), dinoName, seed);
    }

    /**
     * Start a new game using the provided {@link Setting} configuration.
     *
     * @param setting   world configuration to use
     * @param dinoName  name of the player dinosaur (uses first available if null)
     * @param seed      random seed for map generation
     */
    public void start(Setting setting, String dinoName, long seed) {
        try {
            StatsLoader.load(Path.of("conf"), setting.getFormation());
            this.formation = setting.getFormation();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        rng = new Random(seed);
        worldStats = new WorldStats();
        worldStats.initSpecies(StatsLoader.getDinoStats().keySet());
        worldStats.initSpecies(StatsLoader.getCritterStats().keySet());

        map = new Map(18, 10, setting, seed);
        map.setStats(worldStats);
        map.populateBurrows(setting.getNumBurrows());

        // choose player dinosaur
        if (!StatsLoader.getDinoStats().isEmpty()) {
            DinosaurStats base = null;
            if (dinoName != null) {
                base = StatsLoader.getDinoStats().get(dinoName);
            }
            if (base == null) {
                base = StatsLoader.getDinoStats().values().iterator().next();
            }
            DinosaurStats combined = cloneStats(base);
            java.util.Map<String, java.util.Map<String, Object>> p = setting.getPlayableDinos();
            if (p != null) {
                java.util.Map<String, Object> overrides = p.get(combined.getName());
                if (overrides != null) {
                    applyDinoOverrides(combined, overrides);
                }
            }
            playerManager.initialisePlayer(combined);
        } else {
            playerManager.setPlayer(new DinosaurStats());
        }

        chooseStartingLocation();
        map.reveal(x, y);
        MapUtils.revealCardinals(map, x, y);
        MapUtils.revealAdjacentMountains(map, x, y);
        weather = chooseWeather();
        weatherTurns = 0;
        npcController = new NpcController(map, weather, worldStats);
        npcController.initMammalSpecies(setting.getFormation());
        npcController.populateAnimals();
        npcController.spawnCritters(true);
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
        dst.setPreferredBiomes(new ArrayList<>(src.getPreferredBiomes()));
        return dst;
    }

    /**
     * Apply any overrides from the provided map to the given statistics
     * instance. Only recognised fields are updated.
     */
    private void applyDinoOverrides(DinosaurStats stats, java.util.Map<String, Object> overrides) {
        Object gs = overrides.get("growth_stages");
        if (gs == null) {
            gs = overrides.get("growthStages");
        }
        if (gs instanceof Number num) {
            stats.setGrowthStages(num.intValue());
        } else if (gs != null) {
            try {
                stats.setGrowthStages(Integer.parseInt(gs.toString()));
            } catch (NumberFormatException ignored) {
                // ignore invalid value
            }
        }
    }

    /**
     * Build a basic default {@link Setting} mirroring the one used by
     * {@link Map} when no configuration is provided.
     */
    private static Setting defaultSetting() {
        return Settings.MORRISON;
    }

    // Player management is handled by PlayerManager

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

    void updateNpcs() {
        npcController.setMap(map);
        npcController.setWeather(weather);
        java.util.List<String> msgs = npcController.updateNpcs(x, y);
        turnMessages.addAll(msgs);
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
        if (this.x == tx && this.y == ty && playerManager.getPlayer().getName() != null && playerManager.getPlayer().getName().equals(npc.getName())) {
            return true;
        }
        return false;
    }

    private boolean playerPackHunterActive() {
        if (!playerManager.getPlayer().getAbilities().contains("pack_hunter")) {
            return false;
        }
        for (NPCAnimal npc : map.getAnimals(x, y)) {
            if (npc.isAlive() && playerManager.getPlayer().getName().equals(npc.getName())) {
                return true;
            }
        }
        return false;
    }

    public double playerEffectiveAttack() {
        double atk = playerManager.getPlayer().getAttack();
        if (playerPackHunterActive()) {
            atk *= 3;
        }
        double hpPct = 1.0;
        if (playerManager.getPlayer().getMaxHp() > 0) {
            hpPct = Math.max(0.0, Math.min(playerManager.getPlayer().getHp() / playerManager.getPlayer().getMaxHp(), 1.0));
        }
        return atk * hpPct;
    }

    private double scaleByWeight(double weight, double adultWeight, double val) {
        return CombatUtils.scaleByWeight(weight, adultWeight, val);
    }

    private double npcEffectiveAttack(NPCAnimal npc, Object stats, int tx, int ty) {
        return npcController.npcEffectiveAttack(npc, tx, ty);
    }

    /**
     * Determine if an aggressive NPC immediately attacks the playerManager.getPlayer().
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
                playerManager.getPlayer().setHp(0);
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
                    if (egg.getWeight() <= 0) {
                        it.remove();
                        continue;
                    }
                    if (egg.getTurnsUntilHatch() <= 0) {
                        hatchEggCluster(tx, ty, egg);
                        it.remove();
                    }
                }
            }
        }
    }

    private void hatchEggCluster(int tx, int ty, EggCluster cluster) {
        DinosaurStats stats = StatsLoader.getDinoStats().get(cluster.getSpecies());
        if (stats == null) {
            return;
        }
        double weight = stats.getHatchlingWeight();
        for (int i = 0; i < cluster.getNumber(); i++) {
            NPCAnimal npc = new NPCAnimal();
            npc.setId(npcController.allocateNpcId());
            npc.setName(cluster.getSpecies());
            npc.setWeight(weight);
            double pct = stats.getAdultWeight() > 0
                    ? weight / stats.getAdultWeight() : 1.0;
            pct = Math.max(0.0, Math.min(1.0, pct));
            npc.setAttack(stats.getAdultAttack() * pct);
            npc.setMaxHp(stats.getAdultHp() * pct);
            npc.setHp(npc.getMaxHp());
            npc.setSpeed(stats.getHatchlingSpeed());
            npc.setAbilities(new ArrayList<>(stats.getAbilities()));
            npc.setDescendant(cluster.isDescendant());
            npc.setLastAction("spawned");
            map.addAnimal(tx, ty, npc);
            npcController.trackSpawn(npc);
        }
        worldStats.recordEggsHatched(cluster.getSpecies(), cluster.getNumber());
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
        return playerManager.getPlayer().getName() + " (0)";
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

    /**
     * Base energy drain depends on the player's current weight.
     * Hatchling drain is used until the player exceeds half of its
     * adult weight.
     */
    private double baseEnergyDrain() {
        double halfAdult = playerManager.getPlayer().getAdultWeight() / 2.0;
        return playerManager.getPlayer().getWeight() <= halfAdult
                ? playerManager.getPlayer().getHatchlingEnergyDrain()
                : playerManager.getPlayer().getAdultEnergyDrain();
    }

    void applyTurnCosts(boolean moved, double multiplier) {
        double drain = baseEnergyDrain();
        if (moved) {
            drain *= WALKING_ENERGY_DRAIN_MULTIPLIER;
            if (playerManager.getPlayer().getBrokenBone() > 0) {
                drain *= 2;
            }
        }
        drain *= multiplier;
        drain *= weather.getPlayerEnergyMult();
        playerManager.getPlayer().setEnergy(Math.max(0.0, playerManager.getPlayer().getEnergy() - drain));
        if (playerManager.getPlayer().isExhausted()) {
            playerManager.getPlayer().setHp(0.0);
            turnMessages.add("You have collapsed from exhaustion! Game Over.");
        }
        applyBleedAndRegen(playerManager.getPlayer(), playerManager.getPlayer().getHealthRegen(), moved, !playerManager.getPlayer().isExhausted());
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

        if (playerManager.getPlayer().getAbilities().contains("ambush")) {
            if ("stay".equals(lastAction)) {
                playerManager.getPlayer().setAmbushStreak(Math.min(playerManager.getPlayer().getAmbushStreak() + 1, 3));
            } else {
                playerManager.getPlayer().setAmbushStreak(0);
            }
        }

        turnMessages.addAll(map.updateVolcanicActivity(x, y, playerManager.getPlayer()));
        turnMessages.addAll(map.updateFlood(x, y, playerManager.getPlayer(), weather.getFloodChance()));
        turnMessages.addAll(map.updateForestFire(weather));
        updateEggs();
        map.growPlants(StatsLoader.getPlantStats());
        npcController.spawnCritters(false);
        map.refreshBurrows();
        if (playerManager.getPlayer().getTurnsUntilLayEggs() > 0) {
            playerManager.getPlayer().setTurnsUntilLayEggs(playerManager.getPlayer().getTurnsUntilLayEggs() - 1);
        }

        playerManager.getPlayer().setHydration(Math.max(0.0,
                playerManager.getPlayer().getHydration() - playerManager.getPlayer().getHydrationDrain() * weather.getPlayerHydrationMult()));
        if (playerManager.getPlayer().isDehydrated()) {
            playerManager.getPlayer().setHp(0.0);
            turnMessages.add("You have perished from dehydration! Game Over.");
        }
    }

    private void endTurn() {
        updateNpcs();
        applyTerrainEffects();
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
        MapUtils.revealCardinals(map, x, y);
        Terrain t = map.terrainAt(x, y);
        if (t == Terrain.MOUNTAIN || t == Terrain.VOLCANO ||
                t == Terrain.VOLCANO_ERUPTING) {
            MapUtils.revealSurrounding(map, x, y);
        }
        applyTurnCosts(true, 1.0);
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "move";
        endTurn();
    }

    /** Skip a turn without moving. */
    public void rest() {
        startTurn();
        applyTurnCosts(false, 1.0);
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "stay";
        endTurn();
    }

    /** Drink if the player is on a lake tile. */
    public void drink() {
        startTurn();
        if (map.terrainAt(x, y) == Terrain.LAKE) {
            playerManager.getPlayer().setHydration(100.0);
        }
        applyTurnCosts(false, 1.0);
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "drink";
        endTurn();
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
            applyTurnCosts(false, 1.0);
            MapUtils.revealAdjacentMountains(map, x, y);
            checkVictory();
            lastAction = "hunt";
            endTurn();
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
            if (rng.nextDouble() > catchChance) {
                turnMessages.add("The " + npcLabel(target) + " escaped before you could catch it.");
                applyTurnCosts(false, 5.0);
                checkVictory();
                MapUtils.revealAdjacentMountains(map, x, y);
                lastAction = "hunt";
                endTurn();
                return;
            }
        }

        if (target.isAlive()) {
            DinosaurStats playerBase = StatsLoader.getDinoStats().get(playerManager.getPlayer().getName());
            if (playerBase == null) playerBase = new DinosaurStats();
            double dmg = damageAfterArmor(targetAtk, stats, playerBase);
            double beforePlayer = playerManager.getPlayer().getHp();
            boolean died = applyDamage(dmg, playerManager.getPlayer(), playerBase);
            double playerDamage = beforePlayer - playerManager.getPlayer().getHp();
            if (playerDamage > 0) {
                turnMessages.add(npcLabel(target) + " deals " +
                        String.format(java.util.Locale.US, "%.0f", playerDamage) +
                        " damage to " + playerLabel() + ".");
            }
            if (dmg > 0 && target.getAbilities().contains("bleed") && playerManager.getPlayer().getHp() > 0) {
                int bleed = (playerManager.getPlayer().getAbilities().contains("light_armor") || playerManager.getPlayer().getAbilities().contains("heavy_armor")) ? 2 : 5;
                playerManager.getPlayer().setBleeding(bleed);
            }
            if (dmg > 0 && target.getAbilities().contains("bone_break") && target.getWeight() >= playerManager.getPlayer().getWeight()/3 && playerManager.getPlayer().getHp() > 0) {
                playerManager.getPlayer().setBrokenBone(10);
            }
            if (died) {
                MapUtils.revealAdjacentMountains(map, x, y);
                lastAction = "hunt";
                endTurn();
                return;
            }
        }

        double dmgToTarget = damageAfterArmor(playerAtk,
                StatsLoader.getDinoStats().get(playerManager.getPlayer().getName()), stats);
        double beforeTarget = target.getHp();
        boolean targetDied = applyDamage(dmgToTarget, target, stats);
        double dealt = beforeTarget - target.getHp();
        if (dealt > 0) {
            turnMessages.add(playerLabel() + " deals " +
                    String.format(java.util.Locale.US, "%.0f", dealt) +
                    " damage to " + npcLabel(target) + ".");
        }
        if (dmgToTarget > 0 && playerManager.getPlayer().getAbilities().contains("bleed") && target.getHp() > 0 && target.isAlive()) {
            int bleed = (target.getAbilities().contains("light_armor") || target.getAbilities().contains("heavy_armor")) ? 2 : 5;
            target.setBleeding(bleed);
        }
        if (dmgToTarget > 0 && playerManager.getPlayer().getAbilities().contains("bone_break") && playerManager.getPlayer().getWeight() >= target.getWeight()/3 && target.getHp() > 0) {
            target.setBrokenBone(10);
        }

        if (!target.isAlive()) {
            worldStats.recordDeath(target.getName(), "combat");
            double meat = target.getWeight();
            double energyGain = 1000 * meat / Math.max(playerManager.getPlayer().getWeight(), 0.1);
            double need = 100.0 - playerManager.getPlayer().getEnergy();
            double actual = Math.min(energyGain, need);
            double beforeEnergy = playerManager.getPlayer().getEnergy();
            playerManager.getPlayer().setEnergy(Math.min(100.0, beforeEnergy + actual));
            double used = actual * playerManager.getPlayer().getWeight() / 1000.0;
            double leftover = Math.max(0.0, meat - used);
            double[] growth = playerManager.applyGrowth(leftover);
            double eaten = used + growth[0];
            target.setWeight(Math.max(0.0, target.getWeight() - eaten));
            if (target.getWeight() <= 0) {
                map.removeAnimal(x, y, target);
            }
            if (wasAlive && hunt != null) {
                hunt[1]++;
            }
            if (wasAlive) {
                turnMessages.add("You kill the " + npcLabel(target) + ".");
            }
            turnMessages.add("You eat " + String.format(java.util.Locale.US, "%.1f", eaten)
                    + "kg and regain " + String.format(java.util.Locale.US, "%.1f", playerManager.getPlayer().getEnergy() - beforeEnergy)
                    + "% energy gaining " + String.format(java.util.Locale.US, "%.1f", growth[0]) + "kg.");
        }

        applyTurnCosts(false, 1.0);
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "hunt";
        endTurn();
    }

    /** Eat eggs present on the current tile. */
    public void collectEggs() {
        startTurn();
        List<EggCluster> eggs = map.getEggs(x, y);
        if (eggs.isEmpty()) {
            applyTurnCosts(false, 1.0);
            MapUtils.revealAdjacentMountains(map, x, y);
            checkVictory();
            lastAction = "eggs";
            endTurn();
            return;
        }
        EggCluster egg = map.takeEggs(x, y);
        double weight = egg.getWeight();
        double energyGain = 1000 * weight / Math.max(playerManager.getPlayer().getWeight(), 0.1);
        double need = 100.0 - playerManager.getPlayer().getEnergy();
        double actual = Math.min(energyGain, need);
        double beforeEnergy = playerManager.getPlayer().getEnergy();
        playerManager.getPlayer().setEnergy(Math.min(100.0, beforeEnergy + actual));
        double used = actual * playerManager.getPlayer().getWeight() / 1000.0;
        double leftover = Math.max(0.0, weight - used);
        double[] growth = playerManager.applyGrowth(leftover);
        turnMessages.add("You eat " + String.format(java.util.Locale.US, "%.1f", weight)
                + "kg of eggs and regain " + String.format(java.util.Locale.US, "%.1f", playerManager.getPlayer().getEnergy() - beforeEnergy)
                + "% energy gaining " + String.format(java.util.Locale.US, "%.1f", growth[0]) + "kg.");
        applyTurnCosts(false, 1.0);
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "eggs";
        endTurn();
    }

    /** Dig into a burrow on the current tile if present. */
    public void digBurrow() {
        startTurn();
        Burrow b = map.getBurrow(x, y);
        if (b != null && b.isFull()) {
            double gain = playerManager.getPlayer().getAbilities().contains("digger") ? 100.0 : 25.0;
            b.setProgress(Math.min(100.0, b.getProgress() + gain));
            if (b.getProgress() >= 100.0) {
                b.setFull(false);
                b.setProgress(0.0);
                List<String> mammals = npcController.getMammalSpecies();
                if (!mammals.isEmpty()) {
                    String name = mammals.get(new Random().nextInt(mammals.size()));
                    java.util.Map<String, Object> stats = StatsLoader.getCritterStats().get(name);
                    double weight = 0.0;
                    Object wObj = stats.get("adult_weight");
                    if (wObj instanceof Number n) weight = n.doubleValue();
                    double hp = scaleByWeight(weight, getStat(stats, "adult_weight"), getStat(stats, "hp"));
                    NPCAnimal npc = new NPCAnimal();
                    npc.setId(npcController.allocateNpcId());
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
                    npcController.trackSpawn(npc);
                    turnMessages.add("You dug out a " + name + "!");
                }
            }
        }
        applyTurnCosts(false, 1.0);
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "dig";
        endTurn();
    }

    /** Lay eggs if conditions allow. */
    public void layEggs() {
        // capture the number of animals on the player's tile before turn start
        int animalsBefore = map.getAnimals(x, y).size();
        startTurn();
        // check conditions using the pre-turn animal count to avoid random spawns
        DinosaurStats player = playerManager.getPlayer();
        boolean canLay = player.getWeight() >= player.getAdultWeight()
                && player.getEnergy() >= 80
                && player.getHp() >= player.getMaxHp() * 0.8
                && player.getTurnsUntilLayEggs() == 0
                && animalsBefore < 4;
        if (!canLay) {
            applyTurnCosts(false, 1.0);
            MapUtils.revealAdjacentMountains(map, x, y);
            checkVictory();
            lastAction = "lay_eggs";
            endTurn();
            return;
        }
        playerManager.getPlayer().setEnergy(playerManager.getPlayer().getEnergy() * 0.7);
        Object stats = StatsLoader.getDinoStats().get(playerManager.getPlayer().getName());
        int numEggs = (int) getStat(stats, "num_eggs");
        double hatchW = getStat(stats, "hatchling_weight");
        if (hatchW <= 0) {
            double adultW = getStat(stats, "adult_weight");
            hatchW = Math.max(1.0, adultW * 0.001);
        }
        EggCluster ec = new EggCluster(playerManager.getPlayer().getName(), numEggs,
                hatchW * numEggs, 5, true);
        map.getEggs(x, y).add(ec);
        worldStats.recordEggsLaid(playerManager.getPlayer().getName(), numEggs);
        playerManager.getPlayer().setTurnsUntilLayEggs(10);
        int interval = (int) getStat(stats, "egg_laying_interval");
        playerManager.getPlayer().setTurnsUntilLayEggs(interval);
        applyTurnCosts(false, 1.0);
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "lay_eggs";
        endTurn();
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
            playerManager.getPlayer().setMated(true);
        }
        applyTurnCosts(false, 1.0);
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "mate";
        endTurn();
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
            playerManager.getPlayer().setHp(0.0);
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
        applyTurnCosts(false, 2.0);
        if (killed) {
            playerManager.getPlayer().setHp(0.0);
        }
        checkVictory();
        MapUtils.revealAdjacentMountains(map, x, y);
        lastAction = "threaten";
        endTurn();
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


    private double npcEffectiveSpeed(NPCAnimal npc, Object stats) {
        return npcController.npcEffectiveSpeed(npc);
    }

    private boolean npcTryHunt(int tx, int ty, NPCAnimal npc, Object stats,
                               List<NPCAnimal> animals, double adultWeight,
                               java.util.List<String> messages) {
        return npcController.npcTryHunt(tx, ty, npc, stats, animals, adultWeight, x, y, messages);
    }

    private void npcChooseMove(int x, int y, NPCAnimal npc, Object stats) {
        npcController.npcChooseMove(x, y, npc, stats);
    }

    /**
     * Choose a direction for the NPC and always move if a candidate exists.
     */
    private void npcChooseMoveForced(int x, int y, NPCAnimal npc, Object stats) {
        npcController.npcChooseMoveForced(x, y, npc, stats);
    }


    private void moveNpcs() {
        npcController.setMap(map);
        npcController.moveNpcs();
    }

    // ------------------------------------------------------------------
    // Player growth and combat helpers
    // ------------------------------------------------------------------

    // PlayerManager handles growth calculations

    // Growth helpers provided by PlayerManager

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
        return CombatUtils.effectiveArmor(targetStats, attackerStats);
    }

    private double damageAfterArmor(double dmg, Object attackerStats, Object targetStats) {
        return CombatUtils.damageAfterArmor(dmg, attackerStats, targetStats);
    }

    private boolean applyDamage(double damage, DinosaurStats dino, DinosaurStats stats) {
        return CombatUtils.applyDamage(damage, dino, stats);
    }

    private boolean applyDamage(double damage, NPCAnimal npc, Object stats) {
        return CombatUtils.applyDamage(damage, npc, stats);
    }

    private boolean npcDamageAdvantage(double hunterAtk, double hunterHp, Object hunterStats,
                                         double targetAtk, double targetHp, Object targetStats) {
        return CombatUtils.npcDamageAdvantage(hunterAtk, hunterHp, hunterStats,
                targetAtk, targetHp, targetStats);
    }

    /** Apply end-of-turn terrain effects similar to the Python version. */
    void applyTerrainEffects() {
        String terrain = map.terrainAt(x, y).getName();
        double prev = playerManager.getPlayer().getHp();
        if (terrain.equals("lava") || terrain.equals("volcano_erupting")) {
            if (prev > 0) {
                playerManager.getPlayer().setHp(0.0);
                turnMessages.add("You are burned alive by lava! Game Over.");
            }
        } else if (terrain.equals("forest_fire") || terrain.equals("highland_forest_fire")) {
            if (prev > 0) {
                playerManager.getPlayer().setHp(0.0);
                turnMessages.add("You burn up in the forest fire! Game Over.");
            }
        } else if (terrain.equals("toxic_badlands")) {
            double dmg = playerManager.getPlayer().getMaxHp() * 0.2;
            playerManager.getPlayer().setHp(Math.max(0.0, prev - dmg));
            if (prev > 0 && playerManager.getPlayer().getHp() <= 0) {
                turnMessages.add("The toxic fumes overwhelm you! Game Over.");
            }
        }

        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                String tname = map.terrainAt(tx, ty).getName();
                if (tname.equals("lava") || tname.equals("volcano_erupting") ||
                        tname.equals("forest_fire") ||
                        tname.equals("highland_forest_fire")) {
                    for (NPCAnimal npc : map.getAnimals(tx, ty)) {
                        if (npc.isAlive()) {
                            npc.setAlive(false);
                            npc.setAge(-1);
                            npc.setSpeed(0.0);
                            worldStats.recordDeath(npc.getName(), "disaster");
                            if (tx == x && ty == y) {
                                turnMessages.add(npcLabel(npc) + " perishes in the flames.");
                            }
                        }
                    }
                    map.getEggs(tx, ty).clear();
                    map.removeBurrow(tx, ty);
                    map.getPlants(tx, ty).clear();
                } else if (tname.equals("toxic_badlands")) {
                    for (NPCAnimal npc : map.getAnimals(tx, ty)) {
                        if (!npc.isAlive()) continue;
                        double dmg = npc.getMaxHp() * 0.2;
                        double beforeNpc = npc.getHp();
                        npc.setHp(Math.max(0.0, npc.getHp() - dmg));
                        if (npc.getHp() <= 0) {
                            npc.setAlive(false);
                            npc.setAge(-1);
                            npc.setSpeed(0.0);
                            worldStats.recordDeath(npc.getName(), "disaster");
                            if (tx == x && ty == y && beforeNpc > 0) {
                                turnMessages.add(npcLabel(npc) + " succumbs to the toxic fumes.");
                            }
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
        if (playerManager.getPlayer().getName() != null) {
            counts.merge(playerManager.getPlayer().getName(), 1, Integer::sum);
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

    public NpcController getNpcController() {
        return npcController;
    }

    public DinosaurStats getPlayer() {
        return playerManager.getPlayer();
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

    public WorldStats getWorldStats() {
        return worldStats;
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
        return playerManager.playerEffectiveSpeed(map, x, y);
    }

    /**
     * Return true if the player can currently lay eggs.
     */
    public boolean playerCanLayEggs() {
        return playerManager.canPlayerLayEggs(map, x, y);
    }

    /**
     * Determine the growth stage description for the player dinosaur.
     */
    public String playerGrowthStage() {
        return playerManager.playerGrowthStage();
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
        npcController.setMap(map);
        return npcController.npcEffectiveAttack(npc, x, y);
    }

    /**
     * Effective speed value for the given NPC.
     */
    public double npcEffectiveSpeed(NPCAnimal npc) {
        npcController.setMap(map);
        return npcController.npcEffectiveSpeed(npc);
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
