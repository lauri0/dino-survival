import random
import json
import os
from typing import Optional
from dataclasses import dataclass


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
from .dinosaur import DinosaurStats, Diet, NPCAnimal
from .plant import PlantStats, Plant
from .map import Map
from .settings import Setting

# Constants used to derive hatchling values from adult stats
HATCHLING_WEIGHT_DIVISOR = 1000
HATCHLING_FIERCENESS_DIVISOR = 1000
HATCHLING_SPEED_MULTIPLIER = 3
HATCHLING_ENERGY_DRAIN_DIVISOR = 2


@dataclass
class EncounterEntry:
    npc: NPCAnimal | None = None
    in_pack: bool = False
    eggs: str | None = None

STATS_FILE = os.path.join(os.path.dirname(__file__), "dino_stats.yaml")
with open(STATS_FILE) as f:
    DINO_STATS = json.load(f)

PLANT_STATS_FILE = os.path.join(os.path.dirname(__file__), "plant_stats.yaml")
with open(PLANT_STATS_FILE) as f:
    PLANT_STATS = json.load(f)

# Convert plant dictionaries to dataclass instances
for name, stats in list(PLANT_STATS.items()):
    PLANT_STATS[name] = PlantStats(**stats)

# Fill in derived hatchling stats if they were omitted from the YAML
for stats in DINO_STATS.values():
    if "diet" in stats:
        stats["diet"] = [Diet(item) for item in stats.get("diet", [])]
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
        self.next_npc_id = 1
        self._populate_animals()

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
        self.current_encounters: list[EncounterEntry] = []
        self.current_plants: list[Plant] = []
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
        self.turn_messages: list[str] = []

    def _populate_animals(self) -> None:
        """Populate the map with NPC animals."""
        for y in range(self.map.height):
            for x in range(self.map.width):
                terrain = self.map.terrain_at(x, y).name
                animals: list[NPCAnimal] = []
                for name, stats in DINO_STATS.items():
                    if self.setting.formation not in stats.get("formations", []):
                        continue
                    chance = stats.get("encounter_chance", {}).get(terrain, 0)
                    if random.random() < chance:
                        sex: str | None = None
                        if name == self.player.name:
                            sex = random.choice(["M", "F"])
                        allow_j = stats.get("can_be_juvenile", True)
                        if allow_j:
                            weight = random.uniform(3.0, stats.get("adult_weight", 0.0))
                        else:
                            weight = stats.get("adult_weight", 0.0)
                        animals.append(
                            NPCAnimal(
                                id=self.next_npc_id,
                                name=name,
                                sex=sex,
                                weight=weight,
                            )
                        )
                        self.next_npc_id += 1
                self.map.animals[y][x] = animals

    def _generate_encounters(self) -> None:
        """Load encounter information from the current cell."""
        entries: list[EncounterEntry] = []
        cell_animals = self.map.animals[self.y][self.x]
        for npc in list(cell_animals):
            if npc.weight <= 0:
                cell_animals.remove(npc)
                continue
        cell_plants = self.map.plants[self.y][self.x]
        nest_state = self.map.nest_state(self.x, self.y)
        if nest_state and nest_state != "none":
            entries.append(EncounterEntry(npc=None, eggs=nest_state))
        for npc in cell_animals:
            entries.append(EncounterEntry(npc=npc))
        self.current_encounters = entries
        self.current_plants = list(cell_plants)

    def _aggressive_attack_check(self) -> Optional[str]:
        player_f = max(self.effective_fierceness(), 0.1)
        for entry in self.current_encounters:
            if entry.eggs or entry.in_pack or entry.npc is None:
                continue
            npc = entry.npc
            stats = DINO_STATS.get(npc.name, {})
            if not stats.get("aggressive"):
                continue
            target_f = self._stat_from_weight(
                npc.weight,
                stats,
                "hatchling_fierceness",
                "adult_fierceness",
            )
            rel_f = target_f / player_f
            if rel_f > 2.0 and random.random() < 0.5:
                self.player.health = 0
                return f"A fierce {npc.name} attacks and kills you! Game Over."
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
        self.turn_messages = []
        self.turn_count += 1
        terrain = self.map.terrain_at(self.x, self.y).name
        self.biome_turns[terrain] = self.biome_turns.get(terrain, 0) + 1
        self.map.update_nests()
        self.map.grow_plants(PLANT_STATS, self.setting.formation)
        self.turn_messages.extend(self._update_npcs())
        self._move_npcs()
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

    def _format_turn(self, text: str) -> str:
        text = text.lstrip("\n")
        if not text:
            return ""
        lines = [line for line in text.split("\n") if line]
        prefix = f"{self.turn_count}: "
        return "\n".join(prefix + line for line in lines)

    def _finish_turn(self, msg: str) -> str:
        combined = ""
        if self.turn_messages:
            combined = "\n".join(self.turn_messages)
        if msg:
            part = msg.lstrip("\n")
            combined = f"{combined}\n{part}" if combined else part
        return self._format_turn(combined)

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
        r = getattr(self.player, "growth_rate", 0.35)
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

    def _npc_max_growth_gain(self, weight: float, stats: dict) -> float:
        adult = stats.get("adult_weight", 0.0)
        if weight >= adult:
            return 0.0
        max_weight = adult * 1.05
        r = stats.get("growth_rate", 0.35)
        gain = r * weight * (1 - weight / max_weight)
        return min(gain, adult - weight)

    def _stat_from_weight(
        self,
        weight: float,
        stats: dict,
        hatch_key: str,
        adult_key: str,
    ) -> float:
        h_weight = stats.get("hatchling_weight", 0.0)
        a_weight = stats.get("adult_weight", 0.0)
        if a_weight <= h_weight:
            pct = 1.0
        else:
            pct = (weight - h_weight) / (a_weight - h_weight)
        pct = max(0.0, min(pct, 1.0))
        h_val = stats.get(hatch_key, 0.0)
        a_val = stats.get(adult_key, 0.0)
        return h_val + pct * (a_val - h_val)

    def _npc_apply_growth(
        self, npc: NPCAnimal, available_food: float, stats: dict
    ) -> tuple[float, float]:
        max_gain = self._npc_max_growth_gain(npc.weight, stats)
        weight_gain = min(available_food, max_gain)
        npc.weight = min(npc.weight + weight_gain, stats.get("adult_weight", 0.0))
        return weight_gain, max_gain

    def _npc_consume_plant(self, npc: NPCAnimal, plant: Plant, stats: dict) -> float:
        energy_needed = 100.0 - npc.energy
        weight_for_energy = energy_needed * npc.weight / 1000
        growth_target = self._npc_max_growth_gain(npc.weight, stats)
        eat_amount = min(plant.weight, weight_for_energy + growth_target)

        energy_gain_possible = 1000 * eat_amount / max(npc.weight, 0.1)
        actual_energy_gain = min(energy_needed, energy_gain_possible)
        npc.energy = min(100.0, npc.energy + actual_energy_gain)
        weight_used = actual_energy_gain * npc.weight / 1000
        remaining = eat_amount - weight_used
        self._npc_apply_growth(npc, remaining, stats)
        plant.weight -= eat_amount
        return eat_amount

    def _npc_consume_meat(self, npc: NPCAnimal, carcass: NPCAnimal, stats: dict) -> float:
        energy_needed = 100.0 - npc.energy
        weight_for_energy = energy_needed * npc.weight / 1000
        growth_target = self._npc_max_growth_gain(npc.weight, stats)
        eat_amount = min(carcass.weight, weight_for_energy + growth_target)

        energy_gain_possible = 1000 * eat_amount / max(npc.weight, 0.1)
        actual_energy_gain = min(energy_needed, energy_gain_possible)
        npc.energy = min(100.0, npc.energy + actual_energy_gain)
        weight_used = actual_energy_gain * npc.weight / 1000
        remaining = eat_amount - weight_used
        self._npc_apply_growth(npc, remaining, stats)
        carcass.weight -= eat_amount
        return eat_amount

    def _spoil_carcasses(self) -> list[str]:
        """Apply spoilage to all carcasses after feeding has occurred."""
        messages: list[str] = []
        for y in range(self.map.height):
            for x in range(self.map.width):
                animals = self.map.animals[y][x]
                for npc in list(animals):
                    if not npc.alive:
                        before = npc.weight
                        npc.weight -= npc.weight * 0.10 + 2
                        lost = max(0.0, before - npc.weight)
                        if lost > 0 and x == self.x and y == self.y:
                            messages.append(
                                f"The {npc.name} carcass lost {lost:.1f}kg to spoilage."
                            )
                        if npc.weight <= 0:
                            animals.remove(npc)
        return messages

    def _move_npcs(self) -> None:
        moves: list[tuple[int,int,int,int,NPCAnimal]] = []
        directions = {
            "Up": (0, -1),
            "Right": (1, 0),
            "Down": (0, 1),
            "Left": (-1, 0),
        }
        for y in range(self.map.height):
            for x in range(self.map.width):
                for npc in list(self.map.animals[y][x]):
                    d = npc.next_move
                    if not d or d == "None":
                        continue
                    dx, dy = directions.get(d, (0, 0))
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.map.width and 0 <= ny < self.map.height:
                        moves.append((x, y, nx, ny, npc))
        for x, y, nx, ny, npc in moves:
            self.map.animals[y][x].remove(npc)
            self.map.animals[ny][nx].append(npc)
            npc.next_move = "None"

    def _update_npcs(self) -> list[str]:
        messages: list[str] = []
        for y in range(self.map.height):
            for x in range(self.map.width):
                animals = self.map.animals[y][x]
                plants = self.map.plants[y][x]
                for npc in list(animals):
                    if npc.weight <= 0:
                        if npc in animals:
                            animals.remove(npc)
                        continue
                    if not npc.alive:
                        # Spoilage is applied after all creatures, including the
                        # player, have had a chance to eat. Dead animals simply
                        # remain in place for now.
                        continue

                    npc.age += 1
                    stats = DINO_STATS.get(npc.name, {})
                    npc.next_move = "None"
                    npc.energy = max(0.0, npc.energy - stats.get("adult_energy_drain", 0.0))
                    if npc.energy <= 0:
                        npc.alive = False
                        npc.age = -1
                        npc.fierceness = 0.0
                        npc.speed = 0.0
                        continue
                    regen = stats.get("health_regen", 0.0)
                    if npc.health < 100.0 and regen:
                        npc.health = min(100.0, npc.health + regen)

                    if npc.energy > 90:
                        continue

                    diet = stats.get("diet", [])
                    found_food = False

                    if Diet.MEAT in diet:
                        carcasses = [c for c in animals if not c.alive and c.weight > 0]
                        if carcasses:
                            carcass = max(carcasses, key=lambda c: c.weight)
                            eaten = self._npc_consume_meat(npc, carcass, stats)
                            if x == self.x and y == self.y:
                                messages.append(f"The {npc.name} eats {eaten:.1f}kg from a carcass.")
                            if carcass.weight <= 0:
                                animals.remove(carcass)
                            found_food = True
                            npc.next_move = "None"
                            continue

                    if plants and any(d in diet for d in (Diet.FERNS, Diet.CYCADS, Diet.CONIFERS)):
                        options = [p for p in plants if p.name.lower() in ("ferns", "cycads", "conifers")]
                        if options:
                            chosen = max(options, key=lambda p: p.weight)
                            eaten = self._npc_consume_plant(npc, chosen, stats)
                            if x == self.x and y == self.y:
                                messages.append(f"The {npc.name} eats {eaten:.1f}kg of {chosen.name}.")
                            if chosen.weight <= 0:
                                plants.remove(chosen)
                            found_food = True
                            npc.next_move = "None"
                            continue

                    if Diet.MEAT in diet:
                        npc_speed = self._stat_from_weight(npc.weight, stats, "hatchling_speed", "adult_speed")
                        npc_f = self._stat_from_weight(npc.weight, stats, "hatchling_fierceness", "adult_fierceness")
                        potential = []
                        for other in animals:
                            if other is npc or not other.alive:
                                continue
                            o_stats = DINO_STATS.get(other.name, {})
                            o_f = self._stat_from_weight(other.weight, o_stats, "hatchling_fierceness", "adult_fierceness")
                            if o_f >= npc_f:
                                continue
                            rel_f = o_f / max(npc_f, 0.1)
                            damage = (rel_f ** 2) * 100
                            if npc.health - damage <= 0:
                                continue
                            o_speed = self._stat_from_weight(other.weight, o_stats, "hatchling_speed", "adult_speed")
                            if o_speed >= npc_speed:
                                continue
                            if other.weight < npc.weight * 0.01:
                                continue
                            potential.append((other, o_speed, o_f, o_stats, damage))
                        if potential:
                            target, t_speed, t_f, t_stats, damage = random.choice(potential)
                            rel_speed = t_speed / max(npc_speed, 0.1)
                            if random.random() <= calculate_catch_chance(rel_speed):
                                npc.health = max(0.0, npc.health - damage)
                                target.alive = False
                                target.age = -1
                                target.fierceness = 0.0
                                target.speed = 0.0
                                eaten = self._npc_consume_meat(npc, target, stats)
                                if x == self.x and y == self.y:
                                    messages.append(f"The {npc.name} hunts and eats {eaten:.1f}kg of {target.name}.")
                                if target.weight <= 0:
                                    animals.remove(target)
                                found_food = True
                                npc.next_move = "None"
                                continue

                    if found_food:
                        continue

                    if random.random() < 0.5:
                        npc.next_move = "None"
                        continue

                    dirs = {"Up": (0, -1), "Right": (1, 0), "Down": (0, 1), "Left": (-1, 0)}
                    pref = stats.get("preferred_biomes", [])
                    can_walk = stats.get("can_walk", True)
                    candidates = []
                    pref_candidates = []
                    for dname,(dx,dy) in dirs.items():
                        nx, ny = x + dx, y + dy
                        if not (0 <= nx < self.map.width and 0 <= ny < self.map.height):
                            continue
                        terrain = self.map.terrain_at(nx, ny).name
                        if not can_walk and terrain != "lake":
                            continue
                        candidates.append(dname)
                        if terrain in pref:
                            pref_candidates.append(dname)
                    move_choice = None
                    if pref_candidates:
                        move_choice = random.choice(pref_candidates)
                    elif candidates:
                        move_choice = random.choice(candidates)
                    npc.next_move = move_choice or "None"
        return messages


    def hunt_npc(self, npc_id: int) -> str:
        """Hunt a specific NPC animal by its ID."""
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)

        cell = self.map.animals[self.y][self.x]
        target = next((n for n in cell if n.id == npc_id), None)
        if target is None:
            return self._finish_turn("Unknown target.")

        was_alive = target.alive
        hunt = None
        if was_alive:
            hunt = self.hunt_stats.setdefault(target.name, [0, 0])
            hunt[0] += 1

        stats = DINO_STATS.get(target.name, {})

        terrain = self.map.terrain_at(self.x, self.y).name
        boost = 0.0
        if terrain == "lake":
            boost = self.player.aquatic_boost
        elif terrain == "swamp":
            boost = self.player.aquatic_boost / 2
        player_speed = max(self.player.speed * (1 + boost / 100.0), 0.1)

        if target.alive:
            target_speed = max(
                self._stat_from_weight(target.weight, stats, "hatchling_speed", "adult_speed"),
                0.1,
            )
            target_f = self._stat_from_weight(target.weight, stats, "hatchling_fierceness", "adult_fierceness")
        else:
            target_speed = 0.0
            target_f = 0.0

        if target.alive:
            rel_speed = target_speed / max(player_speed, 0.1)
            catch_chance = calculate_catch_chance(rel_speed)
            if random.random() > catch_chance:
                msg = f"The {target.name} escaped before you could catch it."
                end_msg = self._apply_turn_costs(False, 5.0)
                msg += end_msg
                win = self._check_victory()
                if win:
                    msg += win
                self.last_action = "hunt"
                if "Game Over" in end_msg:
                    return self._finish_turn(msg)
                attack = self._aggressive_attack_check()
                if attack:
                    msg += "\n" + attack
                self.turn_messages.extend(self._spoil_carcasses())
                self._generate_encounters()
                self._reveal_adjacent_mountains()
                return self._finish_turn(msg)

        player_f = max(self.effective_fierceness(), 0.1)
        target_f = max(target_f, 0.0)
        damage = 0.0
        if target.alive:
            rel_f = target_f / player_f
            damage = (rel_f ** 2) * 100
            self.player.health = max(0.0, self.player.health - damage)
            if self.player.health <= 0:
                return self._finish_turn(
                    f"You fought the {target.name} but received fatal injuries. Game Over."
                )

        prey_meat = target.weight
        prey_meat /= max(1, len(self.pack) + 1)
        energy_gain = 1000 * prey_meat / max(self.player.weight, 0.1)
        needed = 100.0 - self.player.energy
        actual_energy_gain = min(energy_gain, needed)
        self.player.energy = min(100.0, self.player.energy + actual_energy_gain)

        meat_used = actual_energy_gain * self.player.weight / 1000
        leftover_meat = max(0.0, prey_meat - meat_used)

        weight_gain, max_gain = self._apply_growth(leftover_meat)
        consumed = meat_used + weight_gain
        target.alive = False
        target.age = -1
        target.fierceness = 0.0
        target.speed = 0.0
        target.weight = max(0.0, target.weight - consumed)
        if target.weight <= 0:
            self.map.remove_animal(self.x, self.y, npc_id=target.id)
        if was_alive and hunt is not None:
            hunt[1] += 1

        if damage > 0:
            msg = (
                f"You caught and defeated the {target.name} but lost {damage:.0f}% health. "
                f"Energy +{actual_energy_gain:.1f}%, "
                f"Weight +{weight_gain:.1f}kg (max {max_gain:.1f}kg)."
            )
        else:
            msg = (
                f"You eat from the {target.name} carcass. "
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
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(msg)

    def pack_up(self, juvenile: bool) -> str:
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)
        self.pack.append(juvenile)
        # Remove the recruited dinosaur from the cell
        self.map.remove_animal(self.x, self.y, name=self.player.name)
        msg = f"A {self.player.name} joins your pack."
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "pack"
        if "Game Over" in end_msg:
            return self._finish_turn(msg)
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(msg)

    def leave_pack(self) -> str:
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)
        self.pack.clear()
        msg = "You leave your pack behind."
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "pack"
        if "Game Over" in end_msg:
            return self._finish_turn(msg)
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(msg)

    def mate(self, partner_id: int) -> str:
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)
        # Remove the mating partner from the cell
        self.map.remove_animal(self.x, self.y, npc_id=partner_id)
        self.player.mated = True
        msg = "You mate successfully."
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "mate"
        if "Game Over" in end_msg:
            return self._finish_turn(msg)
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(msg)

    def collect_eggs(self) -> str:
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)

        state = self.map.nest_state(self.x, self.y)
        if state in (None, "none"):
            return self._finish_turn("There are no eggs here.")

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
            return self._finish_turn(msg)
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(msg)

    def population_stats(self) -> tuple[dict[str, int], int]:
        """Return a mapping of species to counts and the total population."""
        counts: dict[str, int] = {}
        total = 0
        for row in self.map.animals:
            for cell in row:
                for npc in cell:
                    counts[npc.name] = counts.get(npc.name, 0) + 1
                    total += 1
        # Include the player and pack members
        counts[self.player.name] = counts.get(self.player.name, 0) + 1 + len(self.pack)
        total += 1 + len(self.pack)
        return counts, total

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
            return self._finish_turn(pre)
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
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        if "Game Over" in end_msg:
            return self._finish_turn(result)
        return self._finish_turn(result)
