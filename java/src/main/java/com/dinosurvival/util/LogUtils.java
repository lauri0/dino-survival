package com.dinosurvival.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility methods for logging game results and hunter statistics.
 */
public class LogUtils {
    private static Path baseDir = Path.of("").toAbsolutePath();

    private LogUtils() {
        // utility class
    }

    /**
     * Set the directory used for log files. Mainly for testing.
     */
    public static void setBaseDir(Path dir) {
        baseDir = dir.toAbsolutePath();
    }

    private static Path gameLogPath() {
        return baseDir.resolve("game_log.txt");
    }

    private static Path hunterLogPath() {
        return baseDir.resolve("hunter_stats.yaml");
    }

    private static Map<String, Object> ensureSection(Map<String, Object> parent, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) parent.get(key);
        if (map == null) {
            map = new LinkedHashMap<>();
            parent.put(key, map);
        }
        return map;
    }

    private static Object parseNumber(String val) {
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException ex) {
                return val;
            }
        }
    }

    /**
     * Parse a very small subset of YAML consisting of nested dictionaries.
     */
    public static Map<String, Object> parseSimpleYaml(String text) {
        Map<String, Object> data = new LinkedHashMap<>();
        Deque<Map.Entry<Map<String, Object>, Integer>> stack = new ArrayDeque<>();
        stack.push(Map.entry(data, -1));
        for (String raw : text.split("\n")) {
            if (raw.strip().isEmpty()) {
                continue;
            }
            int indent = raw.length() - raw.stripLeading().length();
            String keyPart = raw.strip();
            String key;
            Object value;
            if (keyPart.endsWith(":")) {
                key = keyPart.substring(0, keyPart.length() - 1);
                value = new LinkedHashMap<String, Object>();
            } else {
                int idx = keyPart.indexOf(":");
                if (idx < 0) {
                    continue;
                }
                key = keyPart.substring(0, idx);
                String val = keyPart.substring(idx + 1).strip();
                value = parseNumber(val);
            }
            while (indent <= stack.peek().getValue()) {
                stack.pop();
            }
            Map<String, Object> parent = stack.peek().getKey();
            parent.put(key, value);
            if (value instanceof Map) {
                stack.push(Map.entry(castMap(value), indent));
            }
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object obj) {
        return (Map<String, Object>) obj;
    }

    /**
     * Dump a nested map to the simple YAML format used by the Python code.
     */
    public static String dumpSimpleYaml(Map<String, Object> data) {
        return dumpSimpleYaml(data, 0);
    }

    private static String dumpSimpleYaml(Map<String, Object> data, int indent) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            Object v = e.getValue();
            sb.append("  ".repeat(indent)).append(e.getKey());
            if (v instanceof Map) {
                sb.append(':').append('\n');
                sb.append(dumpSimpleYaml(castMap(v), indent + 1));
            } else {
                sb.append(": ").append(v.toString()).append('\n');
            }
        }
        return sb.toString();
    }

    /** Load hunter statistics from the YAML file. */
    public static Map<String, Object> loadHunterStats() throws IOException {
        Path path = hunterLogPath();
        if (!Files.exists(path)) {
            return new LinkedHashMap<>();
        }
        String text = Files.readString(path);
        if (text.isBlank()) {
            return new LinkedHashMap<>();
        }
        return parseSimpleYaml(text);
    }

    /** Save hunter statistics to disk. */
    public static void saveHunterStats(Map<String, Object> data) throws IOException {
        String text = dumpSimpleYaml(data);
        Files.writeString(hunterLogPath(), text, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /** Append a game result to the log file. */
    public static void appendGameLog(String formation, String dino, int turns, double weight, boolean won) throws IOException {
        String line = String.format(Locale.US, "%s|%s|%d|%.1f|%s%n", formation, dino, turns, weight, won ? "Win" : "Loss");
        Files.writeString(gameLogPath(), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Append a single event message to the game log. */
    public static void appendEventLog(String message) throws IOException {
        String line = "EVENT|" + message + System.lineSeparator();
        Files.writeString(gameLogPath(), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Update hunter statistics with the kills from a game session.
     * The {@code hunts} map should contain prey names mapped to int arrays
     * of length 2 where index 1 represents successful kills.
     */
    public static void updateHunterLog(String formation, String dino, Map<String, int[]> hunts) throws IOException {
        Map<String, Object> data = loadHunterStats();
        Map<String, Object> form = ensureSection(data, formation);
        Map<String, Object> dsection = ensureSection(form, dino);
        for (Map.Entry<String, int[]> e : hunts.entrySet()) {
            int kill = e.getValue().length > 1 ? e.getValue()[1] : 0;
            if (kill > 0) {
                Object val = dsection.get(e.getKey());
                int current = 0;
                if (val instanceof Number num) {
                    current = num.intValue();
                } else if (val != null) {
                    try {
                        current = Integer.parseInt(val.toString());
                    } catch (NumberFormatException ignored) {
                    }
                }
                dsection.put(e.getKey(), current + kill);
            }
        }
        saveHunterStats(data);
    }

    /**
     * Return the number of wins and losses recorded for a dinosaur.
     */
    public static int[] getDinoGameStats(String formation, String dino) throws IOException {
        int wins = 0;
        int losses = 0;
        Path path = gameLogPath();
        if (!Files.exists(path)) {
            return new int[]{wins, losses};
        }
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.trim().split("\\|");
                if (parts.length < 5) {
                    continue;
                }
                if (parts[0].equals(formation) && parts[1].equals(dino)) {
                    if ("Win".equals(parts[4])) {
                        wins++;
                    } else {
                        losses++;
                    }
                }
            }
        }
        return new int[]{wins, losses};
    }

    /**
     * Return total games, wins, successful hunts and turns across all dinosaurs.
     */
    public static int[] getPlayerStats() throws IOException {
        int games = 0;
        int wins = 0;
        int turns = 0;
        Path path = gameLogPath();
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\|");
                    if (parts.length < 5) {
                        continue;
                    }
                    games++;
                    if ("Win".equals(parts[4])) {
                        wins++;
                    }
                    try {
                        turns += Integer.parseInt(parts[2]);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        int hunts = 0;
        Map<String, Object> data = loadHunterStats();
        for (Object formVal : data.values()) {
            if (formVal instanceof Map<?, ?> formMap) {
                for (Object dVal : formMap.values()) {
                    if (dVal instanceof Map<?, ?> preyMap) {
                        for (Object val : preyMap.values()) {
                            if (val instanceof Number num) {
                                hunts += num.intValue();
                            } else if (val != null) {
                                try {
                                    hunts += Integer.parseInt(val.toString());
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }
        return new int[]{games, wins, hunts, turns};
    }
}
