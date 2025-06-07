import random
import json
import os
from typing import Optional
from .dinosaur import DinosaurStats
from .map import Map
from .settings import Setting

# Constants used to derive hatchling values from adult stats
HATCHLING_WEIGHT_DIVISOR = 1000
HATCHLING_FIERCENESS_DIVISOR = 1000
HATCHLING_SPEED_MULTIPLIER = 3
HATCHLING_ENERGY_DRAIN_DIVISOR = 2

STATS_FILE = os.path.join(os.path.dirname(__file__), "dino_stats.yaml")
with open(STATS_FILE) as f:
    DINO_STATS = json.load(f)

# Fill in derived hatchling stats if they were omitted from the YAML
for stats in DINO_STATS.values():
    aw = stats.get("adult_weight", 0)
    if "hatchling_weight" not in stats:
        stats["hatchling_weight"] = aw / HATCHLING_WEIGHT_DIVISOR
    af = stats.get("adult_fierceness", 0)
    if "hatchling_fierceness" not in stats:
        stats["hatchling_fierceness"] = af / HATCHLING_FIERCENESS_DIVISOR
    aspeed = stats.get("adult_speed", 0)
    if "hatchling_speed" not in stats:
        stats["hatchling_speed"] = aspeed * HATCHLING_SPEED_MULTIPLIER
    adrain = stats.get("adult_energy_drain", 0)
    if "hatchling_energy_drain" not in stats:
        stats["hatchling_energy_drain"] = adrain / HATCHLING_ENERGY_DRAIN_DIVISOR


class Game:
    def __init__(self, setting: Setting, dinosaur_name: str, width: int = 18, height: int = 10):
        self.setting = setting
        dstats = setting.playable_dinos[dinosaur_name]
        base = DINO_STATS.get(dinosaur_name, {"name": dinosaur_name})
        combined = {**base, **dstats}
        allowed_fields = set(DinosaurStats.__dataclass_fields__.keys())
        filtered = {k: v for k, v in combined.items() if k in allowed_fields}
        self.player = DinosaurStats(**filtered)
        self.player.weight = self.player.hatchling_weight
        self.player.fierceness = self.player.hatchling_fierceness
        self.player.speed = self.player.hatchling_speed
        self.map = Map(width, height, setting.terrains)

        # Pick a random starting location that is within two tiles of a lake but
        # not on a lake tile itself
        candidates = set()
        for ly in range(height):
            for lx in range(width):
                if self.map.terrain_at(lx, ly).name == "lake":
                    for dy in range(-2, 3):
                        for dx in range(-2, 3):
                            nx, ny = lx + dx, ly + dy
                            if 0 <= nx < width and 0 <= ny < height:
                                if self.map.terrain_at(nx, ny).name != "lake":
                                    candidates.add((nx, ny))
        # Fallback to map center if something went wrong
        if candidates:
            self.x, self.y = random.choice(list(candidates))
        else:
            self.x = width // 2
            self.y = height // 2
        self.map.reveal(self.x, self.y)
        self._energy_multiplier = 1.0
        self.current_encounters: list[tuple[str, bool]] = []
        self.last_action: Optional[str] = None

    def _generate_encounters(self) -> None:
        terrain = self.map.terrain_at(self.x, self.y).name
        danger = self.map.danger_at(self.x, self.y)
        spawn_mult = max(0.0, 1.0 - danger / 100.0)
        found: list[tuple[str, bool]] = []
        for name, stats in DINO_STATS.items():
            if len(found) >= 4:
                break
            formations = stats.get("formations", [])
            if self.setting.formation not in formations:
                continue
            chance = stats.get("encounter_chance", {}).get(terrain, 0)
            chance *= spawn_mult
            if random.random() < chance:
                allow_j = stats.get("can_be_juvenile", True)
                juvenile = allow_j and random.random() < 0.5
                found.append((name, juvenile))
        entries: list[tuple[str, bool]] = []
        nest_state = self.map.nest_state(self.x, self.y)
        if nest_state and nest_state != "none":
            entries.append((f"eggs:{nest_state}", False))
        entries.extend(found)
        self.current_encounters = entries

    def _aggressive_attack_check(self) -> Optional[str]:
        player_f = max(self.player.fierceness, 0.1)
        for name, juvenile in self.current_encounters:
            if name.startswith("eggs:"):
                continue
            stats = DINO_STATS.get(name, {})
            if not stats.get("aggressive"):
                continue
            if juvenile:
                target_f = (
                    stats.get("hatchling_fierceness", 0)
                    + stats.get("adult_fierceness", 0)
                ) / 2
            else:
                target_f = stats.get("adult_fierceness", 0)
            rel_f = target_f / player_f
            if rel_f > 2.0:
                if random.random() < 0.5:
                    self.player.health = 0
                    return f"A fierce {name} attacks and kills you! Game Over."
        return None

    def _base_energy_drain(self) -> float:
        return (
            self.player.hatchling_energy_drain
            if self.player.growth_stages > 0
            else self.player.adult_energy_drain
        )

    def _start_turn(self) -> str:
        self.map.decay_danger()
        self.map.update_nests()
        self.player.hydration = max(
            0.0, self.player.hydration - self.player.hydration_drain
        )
        if self.player.is_dehydrated():
            return "\nYou have perished from dehydration! Game Over."
        return ""

    def _apply_turn_costs(self, moved: bool, multiplier: float = 1.0) -> str:
        drain = self._base_energy_drain()
        if moved:
            drain *= self.player.walking_energy_drain_multiplier
        drain *= multiplier
        self.player.energy = max(0.0, self.player.energy - drain)
        message = ""
        if self.player.is_exhausted():
            message = "\nYou have collapsed from exhaustion! Game Over."
        regen = getattr(self.player, "health_regen", 0.0)
        if regen and not message:
            self.player.health = min(100.0, self.player.health + regen)
        return message

    def _max_growth_gain(self) -> float:
        """Return the biological limit on weight gain for this turn."""
        weight = self.player.weight
        adult = self.player.adult_weight
        if weight >= adult:
            return 0.0
        r = 0.35
        gain = r * weight * (1 - weight / adult)
        return min(gain, adult - weight)

    def _apply_growth(self, available_meat: float) -> tuple[float, float]:
        """Grow the player using available meat.

        Returns a tuple of (actual_gain, max_possible_gain).
        """
        max_gain = self._max_growth_gain()
        weight_gain = min(available_meat, max_gain)
        self.player.weight = min(self.player.weight + weight_gain, self.player.adult_weight)

        growth_range = self.player.adult_weight - self.player.hatchling_weight
        if growth_range > 0:
            pct = (self.player.weight - self.player.hatchling_weight) / growth_range
            self.player.fierceness = (
                self.player.hatchling_fierceness
                + pct * (self.player.adult_fierceness - self.player.hatchling_fierceness)
            )
            self.player.speed = (
                self.player.hatchling_speed
                + pct * (self.player.adult_speed - self.player.hatchling_speed)
            )

        return weight_gain, max_gain

    def hunt_dinosaur(self, target_name: str, juvenile: bool = False) -> str:
        """Hunt a specific dinosaur encountered on the map."""
        target = DINO_STATS.get(target_name)
        if not target:
            return f"Unknown target {target_name}."

        if juvenile and not target.get("can_be_juvenile", True):
            juvenile = False

        player_speed = max(self.player.speed, 0.1)
        if juvenile:
            target_speed = max(
                (target.get("hatchling_speed", 0) + target.get("adult_speed", 0))
                / 2,
                0.1,
            )
            target_f = (
                target.get("hatchling_fierceness", 0)
                + target.get("adult_fierceness", 0)
            ) / 2
            target_weight = (
                target.get("hatchling_weight", 0) + target.get("adult_weight", 0)
            ) / 2
        else:
            target_speed = max(target.get("adult_speed", 0.1), 0.1)
            target_f = target.get("adult_fierceness", 0.1)
            target_weight = target.get("adult_weight", 0.0)
        catch_chance = player_speed / (player_speed + target_speed)
        if random.random() > catch_chance:
            msg = f"The {target_name} escaped before you could catch it."
            end_msg = self._apply_turn_costs(False, 5.0)
            msg += end_msg
            self.last_action = "hunt"
            self._generate_encounters()
            if "Game Over" in end_msg:
                return msg
            attack = self._aggressive_attack_check()
            if attack:
                msg += "\n" + attack
            return msg

        player_f = max(self.player.fierceness, 0.1)
        target_f = max(target_f, 0.0)
        rel_f = target_f / player_f
        damage = (rel_f ** 2) * 100
        self.player.health = max(0.0, self.player.health - damage)
        if self.player.health <= 0:
            return (
                f"You fought the {target_name} but received fatal injuries. Game Over."
            )

        prey_meat = target_weight * target.get("carcass_food_value_modifier", 1.0)
        self.map.increase_danger(self.x, self.y)
        energy_gain = 1000 * prey_meat / max(self.player.weight, 0.1)
        needed = 100.0 - self.player.energy
        actual_energy_gain = min(energy_gain, needed)
        self.player.energy = min(100.0, self.player.energy + actual_energy_gain)

        meat_used = actual_energy_gain * self.player.weight / 1000
        leftover_meat = max(0.0, prey_meat - meat_used)

        weight_gain, max_gain = self._apply_growth(leftover_meat)

        msg = (
            f"You caught and defeated the {target_name} but lost {damage:.0f}% health. "
            f"Energy +{actual_energy_gain:.1f}%, "
            f"Weight +{weight_gain:.1f}kg (max {max_gain:.1f}kg)."
        )
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        self.last_action = "hunt"
        self._generate_encounters()
        if "Game Over" in end_msg:
            return msg
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        return msg

    def collect_eggs(self) -> str:
        state = self.map.nest_state(self.x, self.y)
        if state in (None, "none"):
            return "There are no eggs here."

        weight_map = {"small": 4.0, "medium": 10.0, "large": 20.0}
        egg_weight = weight_map.get(state, 0.0)
        self.map.take_eggs(self.x, self.y)

        energy_gain = 1000 * egg_weight / max(self.player.weight, 0.1)
        needed = 100.0 - self.player.energy
        actual_energy_gain = min(energy_gain, needed)
        self.player.energy = min(100.0, self.player.energy + actual_energy_gain)

        meat_used = actual_energy_gain * self.player.weight / 1000
        leftover_meat = max(0.0, egg_weight - meat_used)

        weight_gain, _ = self._apply_growth(leftover_meat)


        msg = f"You eat a {state} pile of eggs."
        msg += self._apply_turn_costs(False)
        self.last_action = "eggs"
        self._generate_encounters()
        return msg

    def move(self, dx: int, dy: int):
        nx = max(0, min(self.map.width - 1, self.x + dx))
        ny = max(0, min(self.map.height - 1, self.y + dy))
        self.x, self.y = nx, ny
        self.map.reveal(self.x, self.y)

    def turn(self, action: str) -> str:
        pre = self._start_turn()
        if pre:
            return pre
        moved = False
        if action == "north":
            self.move(0, -1)
            moved = True
            result = "Moved north"
        elif action == "south":
            self.move(0, 1)
            moved = True
            result = "Moved south"
        elif action == "east":
            self.move(1, 0)
            moved = True
            result = "Moved east"
        elif action == "west":
            self.move(-1, 0)
            moved = True
            result = "Moved west"
        elif action == "stay":
            result = "Stayed put"
        elif action == "drink":
            if self.map.terrain_at(self.x, self.y).name == "lake":
                self.player.hydration = 100.0
                result = "You drink from the lake."
            else:
                result = "There is no water source here."
        else:
            result = "Unknown action"

        end_msg = self._apply_turn_costs(moved, self._energy_multiplier)
        result += end_msg
        self._energy_multiplier = 1.0
        self.last_action = action
        if action == "stay":
            attack = self._aggressive_attack_check()
            if attack:
                result += "\n" + attack
        self._generate_encounters()
        if "Game Over" in end_msg:
            return result
        return result
