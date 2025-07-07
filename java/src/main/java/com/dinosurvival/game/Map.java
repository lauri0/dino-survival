package com.dinosurvival.game;

import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.model.Plant;
import com.dinosurvival.model.PlantStats;
import com.dinosurvival.game.EggCluster;
import com.dinosurvival.game.Burrow;
import com.dinosurvival.game.LavaInfo;
import com.dinosurvival.model.DinosaurStats;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Rough Java port of the Python {@code Map} class. The implementation focuses
 * on biome generation and a few helper methods so that tests exercising the
 * model layer can run.
 */
public class Map {
    private final int width;
    private final int height;
    private final Terrain[][] grid;
    private final boolean[][] revealed;
    private final List<Plant>[][] plants;
    private final List<EggCluster>[][] eggs;
    private final List<NPCAnimal>[][] animals;
    private final Burrow[][] burrows;
    private final LavaInfo[][] lavaInfo;
    private final boolean[][] erupting;
    private final Terrain[][] lavaOrig;
    private final int[][] solidifiedTurns;
    private final int[][] fireTurns;
    private final int[][] burntTurns;
    private final Terrain[][] floodInfo;
    private final Random floodRng = new Random();
    private final Random fireRng = new Random();
    private final Random rng;
    private boolean activeFlood = false;
    private int floodTurn = 0;

    /**
     * Construct a map using the provided setting configuration.
     */
    public Map(int width, int height, Setting setting) {
        this(width, height, setting, new Random());
    }

    public Map(int width, int height, Setting setting, long seed) {
        this(width, height, setting, new Random(seed));
    }

    public Map(int width, int height, Setting setting, Random rng) {
        this.width = width;
        this.height = height;
        this.grid = new Terrain[height][width];
        this.revealed = new boolean[height][width];
        this.plants = (List<Plant>[][]) new ArrayList[height][width];
        this.eggs = (List<EggCluster>[][]) new ArrayList[height][width];
        this.animals = (List<NPCAnimal>[][]) new ArrayList[height][width];
        this.burrows = new Burrow[height][width];
        this.lavaInfo = new LavaInfo[height][width];
        this.erupting = new boolean[height][width];
        this.lavaOrig = new Terrain[height][width];
        this.solidifiedTurns = new int[height][width];
        this.fireTurns = new int[height][width];
        this.burntTurns = new int[height][width];
        this.floodInfo = new Terrain[height][width];
        this.rng = rng;
        generate(setting.getTerrains(), setting.getHeightLevels(), setting.getHumidityLevels());
    }

    /**
     * Legacy constructor used by older tests. Uses a basic default setting.
     */
    public Map(int width, int height) {
        this(width, height, defaultSetting(), new Random());
    }

    public Map(int width, int height, long seed) {
        this(width, height, defaultSetting(), new Random(seed));
    }

    private static Setting defaultSetting() {
        Setting s = new Setting();
        java.util.Map<String, Terrain> terrains = new HashMap<>();
        terrains.put("desert", Terrain.DESERT);
        terrains.put("plains", Terrain.PLAINS);
        terrains.put("woodlands", Terrain.WOODLANDS);
        terrains.put("forest", Terrain.FOREST);
        terrains.put("highland_forest", Terrain.HIGHLAND_FOREST);
        terrains.put("swamp", Terrain.SWAMP);
        terrains.put("lake", Terrain.LAKE);
        terrains.put("mountain", Terrain.MOUNTAIN);
        terrains.put("volcano", Terrain.VOLCANO);
        terrains.put("volcano_erupting", Terrain.VOLCANO_ERUPTING);
        terrains.put("lava", Terrain.LAVA);
        terrains.put("solidified_lava_field", Terrain.SOLIDIFIED_LAVA_FIELD);
        s.setTerrains(terrains);

        java.util.Map<String, Double> heights = new HashMap<>();
        heights.put("low", 0.3);
        heights.put("normal", 0.4);
        heights.put("hilly", 0.2);
        heights.put("mountain", 0.1);
        s.setHeightLevels(heights);

        java.util.Map<String, Double> humidity = new HashMap<>();
        humidity.put("arid", 0.35);
        humidity.put("normal", 0.4);
        humidity.put("humid", 0.25);
        s.setHumidityLevels(humidity);
        return s;
    }

    private void generate(java.util.Map<String, Terrain> terrains,
                          java.util.Map<String, Double> heightLevels,
                          java.util.Map<String, Double> humidityLevels) {
        double[][] hNoise = generateNoise(width, height, 3, rng);
        double[][] mNoise = generateNoise(width, height, 3, rng);

        double[] heightThresh = buildThresholds(heightLevels, new String[]{"low", "normal", "hilly", "mountain"});
        double[] humidityThresh = buildThresholds(humidityLevels, new String[]{"arid", "normal", "humid"});

        java.util.Map<String, String> biomeMap = new HashMap<>();
        biomeMap.put("arid:low", "desert");
        biomeMap.put("arid:normal", "plains");
        biomeMap.put("arid:hilly", "toxic_badlands");
        biomeMap.put("arid:mountain", "mountain");
        biomeMap.put("normal:low", "woodlands");
        biomeMap.put("normal:normal", "forest");
        biomeMap.put("normal:hilly", "highland_forest");
        biomeMap.put("normal:mountain", "mountain");
        biomeMap.put("humid:low", "lake");
        biomeMap.put("humid:normal", "swamp");
        biomeMap.put("humid:hilly", "highland_forest");
        biomeMap.put("humid:mountain", "mountain");

        Random r = rng;
        while (true) {
            int lakeCount = 0;
            int edgeLake = 0;
            int margin = 2;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    double hn = hNoise[y][x];
                    double mn = mNoise[y][x];

                    String hLevel = levelFor(hn, heightThresh, new String[]{"low","normal","hilly","mountain"});
                    String mLevel = levelFor(mn, humidityThresh, new String[]{"arid","normal","humid"});
                    String key = mLevel + ":" + hLevel;
                    String biome = biomeMap.getOrDefault(key, "plains");
                    Terrain terrain = terrains.getOrDefault(biome, Terrain.PLAINS);
                    if (terrain == Terrain.MOUNTAIN && r.nextDouble() < 0.45) {
                        terrain = terrains.getOrDefault("volcano", Terrain.VOLCANO);
                    }
                    grid[y][x] = terrain;
                    if (terrain == Terrain.LAKE) {
                        lakeCount++;
                        if (x < margin || x >= width - margin || y < margin || y >= height - margin) {
                            edgeLake++;
                        }
                    }
                }
            }
            if (lakeCount > 0) {
                int interior = lakeCount - edgeLake;
                if (interior > 0 && (double)edgeLake / lakeCount <= 0.6) {
                    break;
                }
            }
            // regenerate noise and try again
            hNoise = generateNoise(width, height, 3, rng);
            mNoise = generateNoise(width, height, 3, rng);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                revealed[y][x] = false;
                plants[y][x] = new ArrayList<>();
                eggs[y][x] = new ArrayList<>();
                animals[y][x] = new ArrayList<>();
                burrows[y][x] = null;
                lavaInfo[y][x] = null;
                erupting[y][x] = false;
                lavaOrig[y][x] = null;
                solidifiedTurns[y][x] = 0;
                fireTurns[y][x] = 0;
                burntTurns[y][x] = 0;
                floodInfo[y][x] = null;
            }
        }
    }

    private static double[] buildThresholds(java.util.Map<String, Double> levels, String[] order) {
        double[] vals = new double[order.length];
        double total = 0.0;
        for (int i = 0; i < order.length; i++) {
            double v = levels.getOrDefault(order[i], 0.0);
            vals[i] = v;
            total += v;
        }
        double[] th = new double[order.length];
        if (total <= 0) {
            for (int i = 0; i < order.length; i++) {
                th[i] = (i + 1) / (double) order.length;
            }
            return th;
        }
        double cumulative = 0.0;
        for (int i = 0; i < order.length; i++) {
            cumulative += vals[i];
            th[i] = cumulative / total;
        }
        return th;
    }

    private static String levelFor(double n, double[] thresholds, String[] names) {
        for (int i = 0; i < thresholds.length; i++) {
            if (n <= thresholds[i]) {
                return names[i];
            }
        }
        return names[names.length - 1];
    }

    private static double[][] generateNoise(int width, int height, int scale, Random r) {
        int coarseW = width / scale + 3;
        int coarseH = height / scale + 3;
        double[][] coarse = new double[coarseH][coarseW];
        for (int y = 0; y < coarseH; y++) {
            for (int x = 0; x < coarseW; x++) {
                coarse[y][x] = r.nextDouble();
            }
        }

        double[][] noise = new double[height][width];
        for (int y = 0; y < height; y++) {
            double fy = y / (double) (height - 1) * (coarseH - 3) + 1;
            int y0 = (int) fy;
            int y1 = y0 + 1;
            double ty = fy - y0;
            for (int x = 0; x < width; x++) {
                double fx = x / (double) (width - 1) * (coarseW - 3) + 1;
                int x0 = (int) fx;
                int x1 = x0 + 1;
                double tx = fx - x0;
                double n00 = coarse[y0][x0];
                double n10 = coarse[y0][x1];
                double n01 = coarse[y1][x0];
                double n11 = coarse[y1][x1];
                double n0 = lerp(n00, n10, tx);
                double n1 = lerp(n01, n11, tx);
                noise[y][x] = lerp(n0, n1, ty);
            }
        }
        return noise;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    // ---------------------------------------------------------------------
    // Basic helpers used by the tests
    // ---------------------------------------------------------------------

    public Terrain terrainAt(int x, int y) {
        return grid[y][x];
    }

    /** Compatibility helper for old tests. */
    public Terrain getTerrain(int x, int y) {
        return terrainAt(x, y);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void reveal(int x, int y) {
        revealed[y][x] = true;
    }

    public boolean isRevealed(int x, int y) {
        return revealed[y][x];
    }

    public boolean hasBurrow(int x, int y) {
        return burrows[y][x] != null;
    }

    public void spawnBurrow(int x, int y, boolean full) {
        if (terrainAt(x, y) == Terrain.LAKE) {
            return;
        }
        burrows[y][x] = new Burrow(full);
    }

    public Burrow getBurrow(int x, int y) {
        return burrows[y][x];
    }

    /** Remove any burrow present on the specified tile. */
    public void removeBurrow(int x, int y) {
        burrows[y][x] = null;
    }

    /**
     * Randomly place {@code count} burrows on land tiles. Mirrors the Python
     * {@code populate_burrows} helper so the Java {@link Game} class can
     * initialise the world in a similar fashion.
     */
    public void populateBurrows(int count) {
        List<int[]> land = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (terrainAt(x, y) != Terrain.LAKE) {
                    land.add(new int[]{x, y});
                }
            }
        }
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            if (land.isEmpty()) {
                break;
            }
            int idx = r.nextInt(land.size());
            int[] pos = land.remove(idx);
            spawnBurrow(pos[0], pos[1], true);
        }
    }

    // ---------------------------------------------------------------------
    // Basic accessors for animals used by the Game logic. These mirror the
    // simple data structures in the Python implementation and allow the Java
    // Game class to manipulate NPCs.
    // ---------------------------------------------------------------------

    /**
     * Get the mutable list of animals present at the given coordinates.
     */
    public List<NPCAnimal> getAnimals(int x, int y) {
        return animals[y][x];
    }

    /**
     * Add an animal to the specified tile.
     */
    public void addAnimal(int x, int y, NPCAnimal npc) {
        animals[y][x].add(npc);
    }

    /**
     * Remove an animal from the specified tile.
     */
    public void removeAnimal(int x, int y, NPCAnimal npc) {
        animals[y][x].remove(npc);
    }

    /**
     * Get the list of egg clusters present at the given coordinates.
     */
    public List<EggCluster> getEggs(int x, int y) {
        return eggs[y][x];
    }

    /**
     * Return {@code true} if any egg clusters are present in the cell.
     */
    public boolean hasNest(int x, int y) {
        return !eggs[y][x].isEmpty();
    }

    /**
     * Remove and return the first egg cluster from the cell, if any.
     */
    public EggCluster takeEggs(int x, int y) {
        List<EggCluster> cell = eggs[y][x];
        if (!cell.isEmpty()) {
            return cell.remove(0);
        }
        return null;
    }

    /**
     * Add an egg cluster to the specified cell.
     */
    public void addEggs(int x, int y, EggCluster cluster) {
        eggs[y][x].add(cluster);
    }

    /**
     * Get the list of plants present at the given coordinates.
     */
    public List<Plant> getPlants(int x, int y) {
        return plants[y][x];
    }

    // ---------------------------------------------------------------------
    // Minimal implementations of dynamic map effects. These are greatly
    // simplified compared to the Python version but allow tests to invoke
    // the methods without throwing errors.
    // ---------------------------------------------------------------------

    public List<String> startVolcanoEruption(int x, int y, String size) {
        return startVolcanoEruption(x, y, size, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }

    public List<String> startVolcanoEruption(int x, int y, String size,
                                             int playerX, int playerY) {
        List<String> msgs = new ArrayList<>();
        if (terrainAt(x, y) != Terrain.VOLCANO) {
            return msgs;
        }

        if (playerX != Integer.MIN_VALUE) {
            msgs.add("You feel an earthquake.");
        }

        int steps = switch (size) {
            case "large" -> 4;
            case "medium" -> 2;
            default -> 0;
        };

        erupting[y][x] = true;

        int[][] dirs = { {0,0}, {1,0}, {-1,0}, {0,1}, {0,-1} };
        for (int[] d : dirs) {
            int ax = x + d[0];
            int ay = y + d[1];
            if (ax < 0 || ax >= width || ay < 0 || ay >= height) {
                continue;
            }

            animals[ay][ax].clear();
            eggs[ay][ax].clear();
            burrows[ay][ax] = null;

            int spreadSteps;
            if (ax == x && ay == y) {
                grid[ay][ax] = Terrain.VOLCANO_ERUPTING;
                spreadSteps = steps;
            } else {
                if (lavaOrig[ay][ax] == null) {
                    lavaOrig[ay][ax] = grid[ay][ax];
                }
                solidifiedTurns[ay][ax] = 0;
                grid[ay][ax] = Terrain.LAVA;
                spreadSteps = Math.max(steps - 1, 0);
            }

            lavaInfo[ay][ax] = new LavaInfo(spreadSteps, 1);
            if (ax == playerX && ay == playerY) {
                msgs.add("A volcano erupts beneath you!");
            }
        }
        return msgs;
    }

    public List<String> updateVolcanicActivity() {
        return updateVolcanicActivity(Integer.MIN_VALUE, Integer.MIN_VALUE, null);
    }

    public List<String> updateVolcanicActivity(int playerX, int playerY,
                                               DinosaurStats player) {
        List<String> msgs = new ArrayList<>();
        List<int[]> spread = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                LavaInfo info = lavaInfo[y][x];
                if (info == null) {
                    continue;
                }
                if (info.getSteps() > 0) {
                    int[][] dirs = { {1,0}, {-1,0}, {0,1}, {0,-1} };
                    for (int[] d : dirs) {
                        int nx = x + d[0];
                        int ny = y + d[1];
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            if (lavaInfo[ny][nx] == null
                                    && grid[ny][nx] != Terrain.VOLCANO
                                    && grid[ny][nx] != Terrain.VOLCANO_ERUPTING) {
                                spread.add(new int[]{nx, ny, info.getSteps() - 1});
                            }
                        }
                    }
                    info.setSteps(info.getSteps() - 1);
                } else {
                    info.setCooldown(info.getCooldown() - 1);
                    if (info.getCooldown() <= 0) {
                        if (grid[y][x] == Terrain.VOLCANO_ERUPTING) {
                            grid[y][x] = Terrain.VOLCANO;
                        } else {
                            grid[y][x] = Terrain.SOLIDIFIED_LAVA_FIELD;
                            solidifiedTurns[y][x] = 100;
                        }
                        lavaInfo[y][x] = null;
                        erupting[y][x] = false;
                    }
                }
            }
        }

        for (int[] p : spread) {
            int nx = p[0];
            int ny = p[1];
            int steps = p[2];
            animals[ny][nx].clear();
            eggs[ny][nx].clear();
            burrows[ny][nx] = null;
            if (lavaOrig[ny][nx] == null) {
                lavaOrig[ny][nx] = grid[ny][nx];
            }
            solidifiedTurns[ny][nx] = 0;
            grid[ny][nx] = Terrain.LAVA;
            lavaInfo[ny][nx] = new LavaInfo(steps, 1);
            if (nx == playerX && ny == playerY) {
                msgs.add("Lava flows over you!");
            }
        }

        if (player != null && playerX != Integer.MIN_VALUE) {
            Terrain t = terrainAt(playerX, playerY);
            if (t == Terrain.LAVA || t == Terrain.VOLCANO_ERUPTING) {
                player.setHp(0.0);
                msgs.add("Game Over.");
            }
        }

        updateSolidifiedLava();
        return msgs;
    }

    /** Countdown solidified lava tiles and restore the original terrain. */
    public void updateSolidifiedLava() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int turns = solidifiedTurns[y][x];
                if (turns <= 0) {
                    continue;
                }
                solidifiedTurns[y][x]--;
                if (solidifiedTurns[y][x] <= 0) {
                    Terrain prev = lavaOrig[y][x];
                    if (prev != null) {
                        grid[y][x] = prev;
                    }
                    lavaOrig[y][x] = null;
                }
            }
        }

    /**
     * Ignite a forest fire at the specified location.
     *
     * <p>This mirrors the Python helper used by the tests and resets the
     * associated counters so {@link #updateForestFire()} can progress the
     * burning and eventual regrowth.</p>
     */
    public void startForestFire(int x, int y) {
        Terrain t = terrainAt(x, y);
        if (t == Terrain.FOREST) {
            grid[y][x] = Terrain.FOREST_FIRE;
        } else if (t == Terrain.HIGHLAND_FOREST) {
            grid[y][x] = Terrain.HIGHLAND_FOREST_FIRE;
        } else {
            return;
        }

        fireTurns[y][x] = 5;
        burntTurns[y][x] = 0;
        animals[y][x].clear();
        eggs[y][x].clear();
        burrows[y][x] = null;
        plants[y][x].clear();
    }

    public List<String> updateForestFire() {
        List<String> msgs = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (fireTurns[y][x] > 0) {
                    fireTurns[y][x]--;
                    if (fireTurns[y][x] == 0) {
                        if (grid[y][x] == Terrain.FOREST_FIRE) {
                            grid[y][x] = Terrain.FOREST_BURNT;
                            burntTurns[y][x] = 50;
                        } else if (grid[y][x] == Terrain.HIGHLAND_FOREST_FIRE) {
                            grid[y][x] = Terrain.HIGHLAND_FOREST_BURNT;
                            burntTurns[y][x] = 50;
                        }
                    }
                }
                if (burntTurns[y][x] > 0) {
                    burntTurns[y][x]--;
                    if (burntTurns[y][x] == 0) {
                        if (grid[y][x] == Terrain.FOREST_BURNT) {
                            grid[y][x] = Terrain.FOREST;
                        } else if (grid[y][x] == Terrain.HIGHLAND_FOREST_BURNT) {
                            grid[y][x] = Terrain.HIGHLAND_FOREST;
                        }
                    }
                }
            }
        }
        return msgs;
    }

    public void refreshBurrows() {
        Random r = new Random();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Burrow b = burrows[y][x];
                if (b != null && !b.isFull()) {
                    if (r.nextDouble() < 0.02) {
                        b.setFull(true);
                        b.setProgress(0.0);
                    }
                }
            }
        }
    }

    public void growPlants(java.util.Map<String, PlantStats> plantStats) {
        Random r = new Random();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                List<Plant> cell = plants[y][x];
                String terrain = terrainAt(x, y).getName();
                for (PlantStats ps : plantStats.values()) {
                    Double chance = ps.getGrowthChance().get(terrain);
                    if (chance == null) continue;
                    if (r.nextDouble() < chance) {
                        Plant existing = null;
                        for (Plant p : cell) {
                            if (ps.getName().equals(p.getName())) {
                                existing = p;
                                break;
                            }
                        }
                        if (existing != null) {
                            existing.setWeight(Math.min(existing.getWeight() + ps.getWeight(), ps.getWeight() * 10));
                        } else {
                            Plant p = new Plant();
                            p.setName(ps.getName());
                            p.setWeight(ps.getWeight());
                            cell.add(p);
                        }
                    }
                }
            }
        }
    }

    private Terrain floodedTerrain(Terrain t) {
        return switch (t) {
            case DESERT -> Terrain.DESERT_FLOODED;
            case PLAINS -> Terrain.PLAINS_FLOODED;
            case WOODLANDS -> Terrain.WOODLANDS_FLOODED;
            case FOREST -> Terrain.FOREST_FLOODED;
            case SWAMP -> Terrain.SWAMP_FLOODED;
            default -> null;
        };
    }

    private void floodTile(int x, int y) {
        floodTile(x, y, null, Integer.MIN_VALUE, Integer.MIN_VALUE, null);
    }

    private void floodTile(int x, int y, DinosaurStats player, int playerX,
                           int playerY, List<String> msgs) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }
        if (floodInfo[y][x] != null) {
            return;
        }
        String name = grid[y][x].getName();
        if (name.equals("lake") || name.equals("lava") ||
                name.equals("solidified_lava_field") || name.equals("volcano") ||
                name.equals("volcano_erupting") || name.equals("mountain") ||
                name.equals("highland_forest") || name.equals("toxic_badlands") ||
                name.endsWith("_flooded")) {
            return;
        }

        Terrain orig = grid[y][x];
        Terrain flooded = floodedTerrain(orig);
        if (flooded == null) {
            return;
        }
        floodInfo[y][x] = orig;
        grid[y][x] = flooded;
        plants[y][x].clear();
        for (NPCAnimal npc : new ArrayList<>(animals[y][x])) {
            if (npc.isAlive()) {
                double dmg = npc.getMaxHp() * 0.5;
                npc.setHp(Math.max(0.0, npc.getHp() - dmg));
                if (npc.getHp() <= 0) {
                    npc.setAlive(false);
                    npc.setAge(-1);
                    npc.setSpeed(0.0);
                }
            }
        }
        if (player != null && x == playerX && y == playerY) {
            player.setHp(Math.max(0.0,
                    player.getHp() - player.getMaxHp() * 0.5));
            if (msgs != null) {
                msgs.add("Flood waters sweep over you!");
            }
        }
    }

    private void clearFlood() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Terrain orig = floodInfo[y][x];
                if (orig != null) {
                    grid[y][x] = orig;
                    floodInfo[y][x] = null;
                }
            }
        }
    }

    public List<String> initiateFlood(DinosaurStats player, int playerX,
                                      int playerY) {
        List<String> msgs = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y][x] != Terrain.LAKE) {
                    continue;
                }
                floodTile(x + 1, y, player, playerX, playerY, msgs);
                floodTile(x - 1, y, player, playerX, playerY, msgs);
                floodTile(x, y + 1, player, playerX, playerY, msgs);
                floodTile(x, y - 1, player, playerX, playerY, msgs);
            }
        }
        return msgs;
    }

    public List<String> spreadFlood(DinosaurStats player, int playerX,
                                    int playerY) {
        List<String> msgs = new ArrayList<>();
        List<int[]> current = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (floodInfo[y][x] != null) {
                    current.add(new int[]{x, y});
                }
            }
        }
        List<int[]> toFlood = new ArrayList<>();
        for (int[] pos : current) {
            int cx = pos[0];
            int cy = pos[1];
            int[][] dirs = { {1,0}, {-1,0}, {0,1}, {0,-1} };
            for (int[] d : dirs) {
                int nx = cx + d[0];
                int ny = cy + d[1];
                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }
                if (floodInfo[ny][nx] != null) {
                    continue;
                }
                toFlood.add(new int[]{nx, ny});
            }
        }
        for (int[] t : toFlood) {
            floodTile(t[0], t[1], player, playerX, playerY, msgs);
        }
        return msgs;
    }

    public List<String> updateFlood(double chance) {
        return updateFlood(Integer.MIN_VALUE, Integer.MIN_VALUE, null, chance);
    }

    public List<String> updateFlood(int playerX, int playerY,
                                    DinosaurStats player, double chance) {
        List<String> msgs = new ArrayList<>();
        if (!activeFlood) {
            if (chance > 0 && floodRng.nextDouble() < chance) {
                activeFlood = true;
                floodTurn = 0;
                msgs.add(
                    "Recent heavy rains might be causing lakes and riverbanks to start overflowing.");
                msgs.addAll(initiateFlood(player, playerX, playerY));
            }
            return msgs;
        }

        floodTurn++;
        if (floodTurn == 1) {
            msgs.addAll(spreadFlood(player, playerX, playerY));
        } else if (floodTurn >= 3) {
            clearFlood();
            activeFlood = false;
            floodTurn = 0;
        }

        return msgs;
    }
}
