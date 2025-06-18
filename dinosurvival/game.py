import random
import json
import os
from typing import Optional


def calculate_catch_chance(rel_speed: float) -> float:
    """Return the chance to catch prey based on relative speed.

    ``rel_speed`` is the target's speed divided by the player's speed.
    If the value is below 0.5 the catch is guaranteed. Between 0.5 and 1.0 the
    chance linearly decreases from 1.0 to 0.5. Above 1.0 the player cannot
    catch the prey.
    """

    if rel_speed < 0.5:
        return 1.0
    if rel_speed <= 1.0:
        return 1.0 - (rel_speed - 0.5)
    return 0.0
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
        self.map = Map(
            width,
            height,
            setting.terrains,
            setting.height_levels,
            setting.humidity_levels,
        )

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
        self._reveal_adjacent_mountains()
        self._energy_multiplier = 1.0
        self.current_encounters: list[tuple[str, bool, bool, str | None]] = []
        self.pack: list[bool] = []  # store juvenile status of packmates
        self.last_action: Optional[str] = None
        # Tracking and win state
        self.turn_count = 0
        # Track how many turns the player spends in each biome
        self.biome_turns: dict[str, int] = {
            name: 0 for name in setting.terrains.keys()
        }
        self.hunt_stats: dict[str, list[int]] = {}
        self.won = False

    def _generate_encounters(self) -> None:
        terrain = self.map.terrain_at(self.x, self.y).name
        danger = self.map.danger_at(self.x, self.y)
        spawn_mult = max(0.0, 1.0 - danger / 100.0)
        found: list[tuple[str, bool, str | None]] = []
        for name, stats in DINO_STATS.items():
            if len(found) >= 5:
                break
            formations = stats.get("formations", [])
            if self.setting.formation not in formations:
                continue
            chance = stats.get("encounter_chance", {}).get(terrain, 0)
            chance *= spawn_mult
            if random.random() < chance:
                allow_j = stats.get("can_be_juvenile", True)
                juvenile = allow_j and random.random() < 0.5
                # Skip extremely small encounters compared to the player
                if juvenile:
                    weight = (
                        stats.get("hatchling_weight", 0)
                        + stats.get("adult_weight", 0)
                    ) / 2
                else:
                    weight = stats.get("adult_weight", 0)
                if weight >= self.player.weight / 1000:
                    sex: str | None = None
                    if name == self.player.name:
                        sex = random.choice(["M", "F"])
                    found.append((name, juvenile, sex))
        entries: list[tuple[str, bool, bool, str | None]] = []
        nest_state = self.map.nest_state(self.x, self.y)
        if nest_state and nest_state != "none":
            entries.append((f"eggs:{nest_state}", False, False, None))
        for j in self.pack:
            entries.append((self.player.name, j, True, None))
        for name, juvenile, sex in found:
            entries.append((name, juvenile, False, sex))
        self.current_encounters = entries[:5]

    def _aggressive_attack_check(self) -> Optional[str]:
        player_f = max(self.effective_fierceness(), 0.1)
        for name, juvenile, in_pack, _ in self.current_encounters:
            if name.startswith("eggs:"):
                continue
            if in_pack:
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

    def effective_fierceness(self) -> float:
        total = self.player.fierceness
        stats = DINO_STATS.get(self.player.name, {})
        for j in self.pack:
            if j:
                f = (
                    stats.get("hatchling_fierceness", 0)
                    + stats.get("adult_fierceness", 0)
                ) / 2
            else:
                f = stats.get("adult_fierceness", 0)
            total += f
        return total

    def _start_turn(self) -> str:
        self.turn_count += 1
        terrain = self.map.terrain_at(self.x, self.y).name
        self.biome_turns[terrain] = self.biome_turns.get(terrain, 0) + 1
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

    def _reveal_adjacent_mountains(self) -> None:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                nx, ny = self.x + dx, self.y + dy
                if 0 <= nx < self.map.width and 0 <= ny < self.map.height:
                    if self.map.terrain_at(nx, ny).name == "mountain":
                        self.map.reveal(nx, ny)

    def _reveal_surrounding(self, x: int, y: int) -> None:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < self.map.width and 0 <= ny < self.map.height:
                    self.map.reveal(nx, ny)

    def _check_victory(self) -> Optional[str]:
        """Check if the player has reached adult weight and mated."""
        if (
            not self.won
            and self.player.weight >= self.player.adult_weight
            and self.player.mated
        ):
            self.won = True
            return "\nYou have grown to full size and mated! You win!"
        return None

    def player_growth_stage(self) -> str:
        """Return the descriptive growth stage of the player."""
        if self.player.adult_weight <= 0:
            return "Adult"
        pct = self.player.weight / self.player.adult_weight
        if pct <= 0.10:
            return "Hatchling"
        if pct <= 1 / 3:
            return "Juvenile"
        if pct <= 2 / 3:
            return "Sub-Adult"
        return "Adult"

    def _max_growth_gain(self) -> float:
        """Return the biological limit on weight gain for this turn."""
        weight = self.player.weight
        adult = self.player.adult_weight
        if weight >= adult:
            return 0.0
        # Allow the growth curve to asymptotically approach 5% above the
        # target adult weight so that the player can realistically reach the
        # goal weight.
        max_weight = adult * 1.05
        r = 0.35
        gain = r * weight * (1 - weight / max_weight)
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
        pre = self._start_turn()
        if pre:
            return pre

        target = DINO_STATS.get(target_name)
        if not target:
            return f"Unknown target {target_name}."

        hunt = self.hunt_stats.setdefault(target_name, [0, 0])
        hunt[0] += 1

        if juvenile and not target.get("can_be_juvenile", True):
            juvenile = False

        terrain = self.map.terrain_at(self.x, self.y).name
        boost = 0.0
        if terrain == "lake":
            boost = self.player.aquatic_boost
        elif terrain == "swamp":
            boost = self.player.aquatic_boost / 2
        player_speed = max(self.player.speed * (1 + boost / 100.0), 0.1)
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
        rel_speed = target_speed / max(player_speed, 0.1)
        catch_chance = calculate_catch_chance(rel_speed)
        if random.random() > catch_chance:
            msg = f"The {target_name} escaped before you could catch it."
            end_msg = self._apply_turn_costs(False, 5.0)
            msg += end_msg
            win = self._check_victory()
            if win:
                msg += win
            self.last_action = "hunt"
            if "Game Over" in end_msg:
                return msg
            attack = self._aggressive_attack_check()
            if attack:
                msg += "\n" + attack
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return msg

        player_f = max(self.effective_fierceness(), 0.1)
        target_f = max(target_f, 0.0)
        rel_f = target_f / player_f
        damage = (rel_f ** 2) * 100
        self.player.health = max(0.0, self.player.health - damage)
        if self.player.health <= 0:
            return (
                f"You fought the {target_name} but received fatal injuries. Game Over."
            )

        prey_meat = target_weight * target.get("carcass_food_value_modifier", 1.0)
        prey_meat /= max(1, len(self.pack) + 1)
        self.map.increase_danger(self.x, self.y)
        energy_gain = 1000 * prey_meat / max(self.player.weight, 0.1)
        needed = 100.0 - self.player.energy
        actual_energy_gain = min(energy_gain, needed)
        self.player.energy = min(100.0, self.player.energy + actual_energy_gain)

        meat_used = actual_energy_gain * self.player.weight / 1000
        leftover_meat = max(0.0, prey_meat - meat_used)

        weight_gain, max_gain = self._apply_growth(leftover_meat)
        hunt[1] += 1

        msg = (
            f"You caught and defeated the {target_name} but lost {damage:.0f}% health. "
            f"Energy +{actual_energy_gain:.1f}%, "
            f"Weight +{weight_gain:.1f}kg (max {max_gain:.1f}kg)."
        )
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "hunt"
        if "Game Over" in end_msg:
            return msg
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return msg

    def pack_up(self, juvenile: bool) -> str:
        pre = self._start_turn()
        if pre:
            return pre
        self.pack.append(juvenile)
        msg = f"A {self.player.name} joins your pack."
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "pack"
        if "Game Over" in end_msg:
            return msg
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return msg

    def leave_pack(self) -> str:
        pre = self._start_turn()
        if pre:
            return pre
        self.pack.clear()
        msg = "You leave your pack behind."
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "pack"
        if "Game Over" in end_msg:
            return msg
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return msg

    def mate(self) -> str:
        pre = self._start_turn()
        if pre:
            return pre
        self.player.mated = True
        msg = "You mate successfully."
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "mate"
        if "Game Over" in end_msg:
            return msg
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return msg

    def collect_eggs(self) -> str:
        pre = self._start_turn()
        if pre:
            return pre

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

        weight_gain, max_gain = self._apply_growth(leftover_meat)

        msg = (
            f"You eat a {state} pile of eggs. "
            f"Energy +{actual_energy_gain:.1f}%, "
            f"Weight +{weight_gain:.1f}kg (max {max_gain:.1f}kg)."
        )
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "eggs"
        if "Game Over" in end_msg:
            return msg
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return msg

    def move(self, dx: int, dy: int):
        nx = max(0, min(self.map.width - 1, self.x + dx))
        ny = max(0, min(self.map.height - 1, self.y + dy))
        self.x, self.y = nx, ny
        self.map.reveal(self.x, self.y)
        if self.map.terrain_at(self.x, self.y).name == "mountain":
            self._reveal_surrounding(self.x, self.y)

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

        multiplier = self._energy_multiplier
        if moved and self.map.terrain_at(self.x, self.y).name == "mountain":
            multiplier *= 3
        end_msg = self._apply_turn_costs(moved, multiplier)
        result += end_msg
        win = self._check_victory()
        if win:
            result += win
        self._energy_multiplier = 1.0
        self.last_action = action
        if action in ("stay", "drink"):
            attack = self._aggressive_attack_check()
            if attack:
                result += "\n" + attack
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        if "Game Over" in end_msg:
            return result
        return result
