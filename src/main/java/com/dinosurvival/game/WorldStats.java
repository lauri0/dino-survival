package com.dinosurvival.game;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks egg and death statistics for all species in the current world.
 */
public class WorldStats {
    private final Map<String, Map<String, Integer>> deaths = new HashMap<>();
    private final Map<String, int[]> eggs = new HashMap<>();

    /** Ensure entries exist for the provided species names. */
    public void initSpecies(Collection<String> species) {
        for (String name : species) {
            deaths.computeIfAbsent(name, k -> new HashMap<>());
            eggs.computeIfAbsent(name, k -> new int[2]);
        }
    }

    /** Record a death for the given species and cause. */
    public void recordDeath(String species, String cause) {
        Map<String, Integer> map = deaths.computeIfAbsent(species, k -> new HashMap<>());
        map.merge(cause, 1, Integer::sum);
    }

    /** Record eggs laid for a species. */
    public void recordEggsLaid(String species, int number) {
        int[] vals = eggs.computeIfAbsent(species, k -> new int[2]);
        vals[0] += number;
    }

    /** Record eggs hatched for a species. */
    public void recordEggsHatched(String species, int number) {
        int[] vals = eggs.computeIfAbsent(species, k -> new int[2]);
        vals[1] += number;
    }

    /** Get immutable death counts for a species. */
    public Map<String, Integer> getDeathCounts(String species) {
        Map<String, Integer> map = deaths.get(species);
        return map != null ? Collections.unmodifiableMap(map) : Map.of();
    }

    /** Get [laid, hatched] egg counts for a species. */
    public int[] getEggStats(String species) {
        int[] vals = eggs.get(species);
        if (vals == null) {
            return new int[]{0, 0};
        }
        return new int[]{vals[0], vals[1]};
    }
}
