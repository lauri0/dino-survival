package com.dinosurvival.game;

import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.model.NPCAnimal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for combat calculations shared by {@link Game}
 * and {@link NpcController}.
 */
public final class CombatUtils {
    private CombatUtils() {
    }

    /** Linear interpolation between hatchling and adult values based on weight. */
    public static double statFromWeight(double weight, double adultWeight,
                                        double hatchVal, double adultVal) {
        double pct = adultWeight > 0 ? weight / adultWeight : 1.0;
        pct = Math.max(0.0, Math.min(1.0, pct));
        return hatchVal + pct * (adultVal - hatchVal);
    }

    /** Scale a value proportionally to an animal's weight. */
    public static double scaleByWeight(double weight, double adultWeight, double val) {
        double pct = adultWeight > 0 ? weight / adultWeight : 1.0;
        pct = Math.max(0.0, Math.min(pct, 1.0));
        return val * pct;
    }

    /** Extract the given stat value from either {@link DinosaurStats} or a map. */
    public static double getStat(Object stats, String key) {
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
        } else if (stats instanceof Map<?, ?> map) {
            Object val = map.get(key);
            if (val instanceof Number n) {
                return n.doubleValue();
            }
        }
        return 0.0;
    }

    /** Return the ability list for the given stats object. */
    public static List<String> abilities(Object stats) {
        if (stats instanceof DinosaurStats ds) {
            return ds.getAbilities();
        } else if (stats instanceof Map<?, ?> map) {
            Object val = map.get("abilities");
            if (val instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) {
                    out.add(o.toString());
                }
                return out;
            }
        }
        return List.of();
    }

    /** Calculate the effective armor percentage for a target. */
    public static double effectiveArmor(Object targetStats, Object attackerStats) {
        List<String> abil = abilities(targetStats);
        double base = 0.0;
        if (abil.contains("heavy_armor")) {
            base = 40.0;
        } else if (abil.contains("light_armor")) {
            base = 20.0;
        }
        if (abilities(attackerStats).contains("bone_break")) {
            base *= 0.5;
        }
        return Math.max(0.0, base);
    }

    /** Compute damage after applying armor modifiers. */
    public static double damageAfterArmor(double dmg, Object attackerStats, Object targetStats) {
        double eff = effectiveArmor(targetStats, attackerStats);
        return dmg * Math.max(0.0, 1.0 - eff / 100.0);
    }

    /** Apply damage to a dinosaur. */
    public static boolean applyDamage(double damage, DinosaurStats dino, DinosaurStats stats) {
        double maxHp = statFromWeight(dino.getWeight(), stats.getAdultWeight(),
                stats.getHatchlingHp(), stats.getAdultHp());
        dino.setMaxHp(maxHp);
        if (dino.getHp() > maxHp) {
            dino.setHp(maxHp);
        }
        dino.setHp(Math.max(0.0, dino.getHp() - damage));
        return dino.getHp() <= 0;
    }

    /** Apply damage to an NPC animal. */
    public static boolean applyDamage(double damage, NPCAnimal npc, Object stats) {
        double maxHp = scaleByWeight(npc.getWeight(), getStat(stats, "adult_weight"),
                getStat(stats, "hp"));
        npc.setMaxHp(maxHp);
        if (npc.getHp() > maxHp) {
            npc.setHp(maxHp);
        }
        npc.setHp(Math.max(0.0, npc.getHp() - damage));
        boolean died = npc.getHp() <= 0;
        if (died) {
            npc.setAlive(false);
            npc.setAge(-1);
            npc.setSpeed(0.0);
        }
        return died;
    }

    /**
     * Determine if the hunter has a damage advantage over its target.
     * This is based on raw damage, bleed effects and regeneration.
     */
    public static boolean npcDamageAdvantage(double hunterAtk, double hunterHp, Object hunterStats,
                                             double targetAtk, double targetHp, Object targetStats) {
        double dmgToTarget = damageAfterArmor(hunterAtk, hunterStats, targetStats);
        double dmgToHunter = damageAfterArmor(targetAtk, targetStats, hunterStats);

        int targetBleed = 0;
        int hunterBleed = 0;
        if (dmgToTarget > 0 && abilities(hunterStats).contains("bleed")) {
            if (abilities(targetStats).contains("light_armor") || abilities(targetStats).contains("heavy_armor")) {
                targetBleed = 2;
            } else {
                targetBleed = 5;
            }
        }
        if (dmgToHunter > 0 && abilities(targetStats).contains("bleed")) {
            if (abilities(hunterStats).contains("light_armor") || abilities(hunterStats).contains("heavy_armor")) {
                hunterBleed = 2;
            } else {
                hunterBleed = 5;
            }
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
}

