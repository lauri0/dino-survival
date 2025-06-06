import random
import json
import os
from typing import Optional
from .dinosaur import DinosaurStats
from .map import Map
from .settings import Setting

STATS_FILE = os.path.join(os.path.dirname(__file__), "dino_stats.yaml")
with open(STATS_FILE) as f:
    DINO_STATS = json.load(f)


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

    def hunt_dinosaur(self, target_name: str, juvenile: bool = False) -> str:
        """Hunt a specific dinosaur encountered on the map."""
        target = DINO_STATS.get(target_name)
        if not target:
            return f"Unknown target {target_name}."

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
            msg += self._apply_turn_costs(False, 5.0)
            return msg

        player_f = max(self.player.fierceness, 0.1)
        target_f = max(target_f, 0.1)
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

        weight_gain = min(leftover_meat, self.player.growth_speed)
        self.player.weight = min(
            self.player.weight + weight_gain,
            self.player.adult_weight,
        )

        growth_range = self.player.adult_weight - self.player.hatchling_weight
        if growth_range > 0:
            pct = (
                self.player.weight - self.player.hatchling_weight
            ) / growth_range
            self.player.fierceness = (
                self.player.hatchling_fierceness
                + pct * (self.player.adult_fierceness - self.player.hatchling_fierceness)
            )
            self.player.speed = (
                self.player.hatchling_speed
                + pct * (self.player.adult_speed - self.player.hatchling_speed)
            )

        msg = (
            f"You caught and defeated the {target_name} but lost {damage:.0f}% health."
        )
        msg += self._apply_turn_costs(False)
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

        weight_gain = min(leftover_meat, self.player.growth_speed)
        self.player.weight = min(
            self.player.weight + weight_gain,
            self.player.adult_weight,
        )

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

        msg = f"You eat a {state} pile of eggs."
        msg += self._apply_turn_costs(False)
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

        result += self._apply_turn_costs(moved, self._energy_multiplier)
        self._energy_multiplier = 1.0
        return result
