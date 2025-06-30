import random
import json
import os
import configparser
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
from .map import Map, EggCluster, Burrow
from .settings import Setting
from .logging_utils import append_event_log

_config = configparser.ConfigParser()
_config.read(os.path.join(os.path.dirname(__file__), "constants.ini"))

# Constants used throughout the game
WALKING_ENERGY_DRAIN_MULTIPLIER = _config.getfloat(
    "DEFAULT", "walking_energy_drain_multiplier", fallback=1.0
)
HATCHLING_WEIGHT_DIVISOR = _config.getint(
    "DEFAULT", "hatchling_weight_divisor", fallback=1000
)
HATCHLING_SPEED_MULTIPLIER = _config.getint(
    "DEFAULT", "hatchling_speed_multiplier", fallback=3
)
HATCHLING_ENERGY_DRAIN_DIVISOR = _config.getint(
    "DEFAULT", "hatchling_energy_drain_divisor", fallback=2
)
MIN_HATCHING_WEIGHT = _config.getfloat(
    "DEFAULT", "min_hatching_weight", fallback=2.0
)

# Number of living descendants required to win the game
DESCENDANTS_TO_WIN = 2


def _load_stats(formation: str) -> tuple[dict, dict[str, PlantStats], dict]:
    """Load dinosaur, plant and critter stats for the given formation."""
    base_dir = os.path.dirname(__file__)
    suffix = formation.lower().replace(" ", "_")
    dino_file = os.path.join(base_dir, f"dino_stats_{suffix}.yaml")
    plant_file = os.path.join(base_dir, f"plant_stats_{suffix}.yaml")
    critter_file = os.path.join(base_dir, f"critter_stats_{suffix}.yaml")

    with open(dino_file) as f:
        dino_stats = json.load(f)

    with open(plant_file) as f:
        plant_stats = json.load(f)

    critter_stats = {}
    if os.path.exists(critter_file):
        with open(critter_file) as f:
            critter_stats = json.load(f)

    # Convert plant dictionaries to dataclass instances
    for name, stats in list(plant_stats.items()):
        plant_stats[name] = PlantStats(**stats)

    # Fill in derived hatchling stats if they were omitted from the YAML
    for stats in dino_stats.values():
        if "diet" in stats:
            stats["diet"] = [Diet(item) for item in stats.get("diet", [])]
        if "abilities" in stats:
            stats["abilities"] = list(stats.get("abilities", []))
        aw = stats.get("adult_weight", 0)
        if "hatchling_weight" not in stats:
            stats["hatchling_weight"] = max(
                aw / HATCHLING_WEIGHT_DIVISOR,
                MIN_HATCHING_WEIGHT,
            )
        else:
            stats["hatchling_weight"] = max(
                stats["hatchling_weight"], MIN_HATCHING_WEIGHT
            )
        aspeed = stats.get("adult_speed", 0)
        if "hatchling_speed" not in stats:
            stats["hatchling_speed"] = aspeed * HATCHLING_SPEED_MULTIPLIER
        adrain = stats.get("adult_energy_drain", 0)
        if "hatchling_energy_drain" not in stats:
            stats["hatchling_energy_drain"] = adrain / HATCHLING_ENERGY_DRAIN_DIVISOR

    return dino_stats, plant_stats, critter_stats


DINO_STATS: dict[str, dict] = {}
PLANT_STATS: dict[str, PlantStats] = {}
CRITTER_STATS: dict[str, dict] = {}
_CURRENT_FORMATION = None


def set_stats_for_formation(formation: str) -> None:
    """Load stats for the given formation and store them globally."""
    global DINO_STATS, PLANT_STATS, CRITTER_STATS, _CURRENT_FORMATION
    if _CURRENT_FORMATION == formation:
        return
    DINO_STATS, PLANT_STATS, CRITTER_STATS = _load_stats(formation)
    _CURRENT_FORMATION = formation



@dataclass
class EncounterEntry:
    npc: NPCAnimal | None = None
    in_pack: bool = False
    eggs: EggCluster | None = None
    burrow: Burrow | None = None


@dataclass
class Weather:
    name: str
    icon: str
    flood_chance: float
    player_hydration_mult: float = 1.0
    player_energy_mult: float = 1.0
    npc_energy_mult: float = 1.0


WEATHER_TABLE: list[tuple[Weather, float]] = [
    (
        Weather(
            "Cloudy",
            os.path.join("assets", "weather", "clouds.png"),
            0.0,
        ),
        0.30,
    ),
    (
        Weather(
            "Sunny",
            os.path.join("assets", "weather", "sun.png"),
            0.0,
            player_hydration_mult=1.2,
        ),
        0.25,
    ),
    (
        Weather(
            "Heatwave",
            os.path.join("assets", "weather", "heatwave.png"),
            0.0,
            player_hydration_mult=1.5,
        ),
        0.10,
    ),
    (
        Weather(
            "Light Rain",
            os.path.join("assets", "weather", "light_rain.png"),
            0.01,
            player_hydration_mult=0.9,
            player_energy_mult=1.1,
            npc_energy_mult=1.1,
        ),
        0.20,
    ),
    (
        Weather(
            "Heavy Rain",
            os.path.join("assets", "weather", "heavy_rain.png"),
            0.10,
            player_hydration_mult=0.8,
            player_energy_mult=1.2,
            npc_energy_mult=1.2,
        ),
        0.15,
    ),
    (
        Weather(
            "Freezing",
            os.path.join("assets", "weather", "freeze.png"),
            0.0,
            player_energy_mult=1.3,
            npc_energy_mult=1.3,
        ),
        0.0,
    ),
]

# Load default stats for the Morrison formation
set_stats_for_formation("Morrison")


class Game:
    def __init__(self, setting: Setting, dinosaur_name: str, width: int = 18, height: int = 10):
        set_stats_for_formation(setting.formation)
        self.setting = setting
        dstats = setting.playable_dinos[dinosaur_name]
        base = DINO_STATS.get(dinosaur_name, {"name": dinosaur_name})
        combined = {**base, **dstats}
        allowed_fields = set(DinosaurStats.__dataclass_fields__.keys())
        filtered = {k: v for k, v in combined.items() if k in allowed_fields}
        self.player = DinosaurStats(**filtered)
        self.player.hatchling_weight = max(
            self.player.hatchling_weight, MIN_HATCHING_WEIGHT
        )
        self.player.weight = self.player.hatchling_weight
        # Scale initial stats based on current weight using adult values
        pct = 1.0
        if self.player.adult_weight > 0:
            pct = self.player.weight / self.player.adult_weight
            pct = max(0.0, min(pct, 1.0))
        self.player.adult_attack = filtered.get("attack", 0.0)
        self.player.hatchling_attack = self.player.adult_attack * pct
        self.player.attack = self.player.hatchling_attack
        self.player.adult_hp = filtered.get("hp", 0.0)
        self.player.hatchling_hp = self.player.adult_hp * pct
        self.player.hp = self.player.hatchling_hp
        self.player.speed = self._stat_from_weight(
            self.player.weight,
            filtered,
            "hatchling_speed",
            "adult_speed",
        )
        self.map = Map(
            width,
            height,
            setting.terrains,
            setting.height_levels,
            setting.humidity_levels,
        )
        self.map.populate_burrows(setting.num_burrows)
        self.mammal_species: list[str] = [
            n for n, s in CRITTER_STATS.items() if s.get("class") == "mammal"
        ]
        self.next_npc_id = 1
        self._populate_animals()
        self._spawn_critters(initial=True)

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
        self._reveal_cardinals(self.x, self.y)
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
        # Track population counts for all species in this formation
        self.population_history: dict[str, list[int]] = {
            n: [] for n in list(DINO_STATS.keys()) + list(CRITTER_STATS.keys())
        }
        self.turn_history: list[int] = []
        self._record_population()

        self.last_hydration_loss = 0.0
        self.last_energy_loss = 0.0

        self._weather_rng = random.Random(1)
        self.weather = self._choose_weather()
        self.weather_turns = 0

    def _npc_label(self, npc: NPCAnimal) -> str:
        """Return the formatted name with ID for logging."""
        return f"{npc.name} ({npc.id})"

    def _populate_animals(self) -> None:
        """Populate the map with NPC animals based on initial spawn counts."""
        land_tiles: list[tuple[int, int]] = []
        lake_tiles: list[tuple[int, int]] = []
        for y in range(self.map.height):
            for x in range(self.map.width):
                terrain = self.map.terrain_at(x, y).name
                if terrain == "lake":
                    lake_tiles.append((x, y))
                elif terrain != "toxic_badlands":
                    land_tiles.append((x, y))

        species = list(DINO_STATS.items())

        multipliers: dict[str, float] = {
            name: stats.get("initial_spawn_multiplier", 0)
            for name, stats in species
        }

        total_multiplier = sum(multipliers.values())
        total_animals = 100
        spawn_counts: dict[str, int] = {name: 0 for name, _ in species}

        if total_multiplier > 0:
            raw_counts = {
                name: total_animals * mult / total_multiplier
                for name, mult in multipliers.items()
            }
            base_counts = {name: int(val) for name, val in raw_counts.items()}
            leftover = total_animals - sum(base_counts.values())
            remainders = sorted(
                ((raw_counts[name] - base_counts[name], name) for name in multipliers),
                reverse=True,
            )
            for i in range(leftover):
                name = remainders[i % len(remainders)][1]
                base_counts[name] += 1
            spawn_counts = base_counts

        for name, stats in species:
            old_count = stats.get("initial_spawn_multiplier", 0)
            spawn_count = spawn_counts.get(name, 0)
            spawn_tiles = lake_tiles if not stats.get("can_walk", True) else land_tiles
            loop_count = max(old_count, spawn_count)
            if not spawn_tiles or loop_count <= 0:
                # still consume random numbers to keep sequence stable
                for _ in range(loop_count):
                    random.choice(spawn_tiles)
                    if stats.get("can_be_juvenile", True):
                        random.uniform(3.0, stats.get("adult_weight", 0.0))
                continue
            for i in range(loop_count):
                x, y = random.choice(spawn_tiles)
                sex: str | None = None
                allow_j = stats.get("can_be_juvenile", True)
                if allow_j:
                    weight = random.uniform(3.0, stats.get("adult_weight", 0.0))
                else:
                    weight = stats.get("adult_weight", 0.0)
                if i < spawn_count:
                    self.map.animals[y][x].append(
                        NPCAnimal(
                            id=self.next_npc_id,
                            name=name,
                            sex=sex,
                            weight=weight,
                            abilities=stats.get("abilities", []),
                        )
                    )
                    self.next_npc_id += 1

    def _spawn_critters(self, *, initial: bool = False) -> None:
        """Spawn critters based on per-turn rates and population caps.

        If ``initial`` is True, spawn half of the maximum number of each
        critter regardless of the per-turn spawn rate.
        """
        if not CRITTER_STATS:
            return

        land_tiles: list[tuple[int, int]] = []
        lake_tiles: list[tuple[int, int]] = []
        for y in range(self.map.height):
            for x in range(self.map.width):
                terrain = self.map.terrain_at(x, y).name
                if terrain == "lake":
                    lake_tiles.append((x, y))
                elif terrain != "toxic_badlands":
                    land_tiles.append((x, y))

        for name, stats in CRITTER_STATS.items():
            avg_rate = stats.get("avg_spawned_per_turn", 0.0)
            max_individuals = stats.get("maximum_individuals", 0)
            count = 0
            for row in self.map.animals:
                for cell in row:
                    for npc in cell:
                        if npc.name == name:
                            count += 1
            available = max_individuals - count
            if initial:
                spawn_count = max_individuals // 2
            else:
                spawn_count = round(random.gauss(avg_rate, 0.5))
                spawn_count = max(0, spawn_count)
            to_spawn = min(spawn_count, max(0, available))
            spawn_tiles = lake_tiles if not stats.get("can_walk", True) else land_tiles
            for _ in range(to_spawn):
                if not spawn_tiles:
                    break
                x, y = random.choice(spawn_tiles)
                if any(npc.name == name for npc in self.map.animals[y][x]):
                    continue
                self.map.animals[y][x].append(
                    NPCAnimal(
                        id=self.next_npc_id,
                        name=name,
                        sex=None,
                        weight=stats.get("adult_weight", 0.0),
                        abilities=stats.get("abilities", []),
                    )
                )
                self.next_npc_id += 1

    def _generate_encounters(self) -> None:
        """Load encounter information from the current cell."""
        entries: list[EncounterEntry] = []
        cell_animals = self.map.animals[self.y][self.x]
        for npc in list(cell_animals):
            if npc.weight <= 0:
                cell_animals.remove(npc)
                continue
        cell_plants = self.map.plants[self.y][self.x]
        burrow = self.map.get_burrow(self.x, self.y)
        if burrow is not None:
            entries.append(EncounterEntry(burrow=burrow))
        for egg in self.map.eggs[self.y][self.x]:
            entries.append(EncounterEntry(npc=None, eggs=egg))
        for npc in cell_animals:
            entries.append(EncounterEntry(npc=npc))
        self.current_encounters = entries
        self.current_plants = list(cell_plants)

    def _aggressive_attack_check(self) -> Optional[str]:
        player_a = max(self.player_effective_attack(), 0.1)
        for entry in self.current_encounters:
            if entry.eggs or entry.in_pack or entry.npc is None:
                continue
            npc = entry.npc
            if not npc.alive:
                continue
            stats = DINO_STATS.get(npc.name)
            if not stats:
                stats = CRITTER_STATS.get(npc.name, {})
            if not stats.get("aggressive"):
                continue
            target_a = self.npc_effective_attack(npc, stats, self.x, self.y)
            rel_a = target_a / player_a
            if rel_a > 2.0 and random.random() < 0.5:
                self.player.health = 0
                return (
                    f"A fierce {self._npc_label(npc)} attacks and kills you! Game Over."
                )
        return None

    def _base_energy_drain(self) -> float:
        return (
            self.player.hatchling_energy_drain
            if self.player.growth_stages > 0
            else self.player.adult_energy_drain
        )

    def player_pack_hunter_active(self) -> bool:
        return (
            "pack_hunter" in self.player.abilities
            and any(
                npc.alive and npc.name == self.player.name
                for npc in self.map.animals[self.y][self.x]
            )
        )

    def player_effective_attack(self) -> float:
        atk = self.player.attack
        if self.player_pack_hunter_active():
            atk *= 3
        return atk

    def player_effective_speed(self) -> float:
        speed = self.player.speed
        terrain = self.map.terrain_at(self.x, self.y).name
        boost = 0.0
        if terrain == "lake":
            boost = self.player.aquatic_boost
        elif terrain == "swamp":
            boost = self.player.aquatic_boost / 2
        speed *= 1 + boost / 100.0
        if "ambush" in self.player.abilities:
            speed *= 1 + min(self.player.ambush_streak, 3) * 0.05
        return max(speed, 0.1)

    def npc_effective_speed(self, npc: NPCAnimal, stats: dict) -> float:
        speed = self._stat_from_weight(
            npc.weight, stats, "hatchling_speed", "adult_speed"
        )
        if "ambush" in npc.abilities:
            speed *= 1 + min(npc.ambush_streak, 3) * 0.05
        return max(speed, 0.1)

    def _npc_has_packmate(self, npc: NPCAnimal, x: int, y: int) -> bool:
        for other in self.map.animals[y][x]:
            if other is npc:
                continue
            if other.alive and other.name == npc.name:
                return True
        if self.x == x and self.y == y and self.player.name == npc.name:
            return True
        return False

    def npc_effective_attack(self, npc: NPCAnimal, stats: dict, x: int, y: int) -> float:
        atk = self._scale_by_weight(npc.weight, stats, "attack")
        if "pack_hunter" in npc.abilities and self._npc_has_packmate(npc, x, y):
            atk *= 3
        return atk

    def _start_turn(self) -> str:
        self.turn_messages = []
        self.turn_count += 1
        if self.weather_turns >= 10:
            self.weather = self._choose_weather()
            self.weather_turns = 0
            self.turn_messages.append(f"The weather changes to {self.weather.name}.")
        self.weather_turns += 1
        if "ambush" in self.player.abilities:
            if self.last_action == "stay":
                self.player.ambush_streak = min(self.player.ambush_streak + 1, 3)
            else:
                self.player.ambush_streak = 0
        self._record_population()
        terrain = self.map.terrain_at(self.x, self.y).name
        self.biome_turns[terrain] = self.biome_turns.get(terrain, 0) + 1
        self.turn_messages.extend(
            self.map.update_volcanic_activity((self.x, self.y))
        )
        self.turn_messages.extend(
            self.map.update_flood(self.player, (self.x, self.y), self.weather.flood_chance)
        )
        self.turn_messages.extend(
            self.map.update_forest_fire(self.weather, (self.x, self.y))
        )
        if self.map.terrain_at(self.x, self.y).name in ("lava", "volcano_erupting"):
            append_event_log(f"Player killed by lava at ({self.x},{self.y})")
            return "\nYou are consumed by lava! Game Over."
        self.turn_messages.extend(self._update_eggs())
        self.map.grow_plants(PLANT_STATS, self.setting.formation)
        self._spawn_critters()
        self.map.refresh_burrows()
        if getattr(self.player, "turns_until_lay_eggs", 0) > 0:
            self.player.turns_until_lay_eggs -= 1
        drain = self.player.hydration_drain * self.weather.player_hydration_mult
        prev_h = self.player.hydration
        self.player.hydration = max(0.0, self.player.hydration - drain)
        self.last_hydration_loss = prev_h - self.player.hydration
        if self.player.is_dehydrated():
            return "\nYou have perished from dehydration! Game Over."
        return ""

    def _apply_turn_costs(self, moved: bool, multiplier: float = 1.0) -> str:
        drain = self._base_energy_drain()
        if moved:
            drain *= WALKING_ENERGY_DRAIN_MULTIPLIER
        drain *= multiplier
        drain *= self.weather.player_energy_mult
        prev_e = self.player.energy
        self.player.energy = max(0.0, self.player.energy - drain)
        self.last_energy_loss = prev_e - self.player.energy
        message = ""
        if self.player.is_exhausted():
            message = "\nYou have collapsed from exhaustion! Game Over."
        regen = getattr(self.player, "health_regen", 0.0)
        if regen and not message:
            self.player.health = min(100.0, self.player.health + regen)
        return message

    def _reveal_cardinals(self, x: int, y: int) -> None:
        """Reveal the four orthogonally adjacent tiles to ``(x, y)``."""
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < self.map.width and 0 <= ny < self.map.height:
                self.map.reveal(nx, ny)

    def _reveal_adjacent_mountains(self) -> None:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                nx, ny = self.x + dx, self.y + dy
                if 0 <= nx < self.map.width and 0 <= ny < self.map.height:
                    if self.map.terrain_at(nx, ny).name in (
                        "mountain",
                        "volcano",
                        "volcano_erupting",
                    ):
                        self.map.reveal(nx, ny)

    def _reveal_surrounding(self, x: int, y: int) -> None:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < self.map.width and 0 <= ny < self.map.height:
                    self.map.reveal(nx, ny)

    def _apply_terrain_effects(self) -> None:
        """Apply end-of-turn biome effects to the player and NPCs."""
        terrain = self.map.terrain_at(self.x, self.y).name
        if terrain in ("lava", "volcano_erupting", "forest_fire", "highland_forest_fire"):
            self.player.health = 0.0
            self.turn_messages.append("Game Over.")
        if terrain == "toxic_badlands":
            self.player.health = max(0.0, self.player.health - 20.0)
            msg = "You take 20% damage from toxic fumes."
            if self.player.health <= 0:
                msg += " Game Over."
            self.turn_messages.append(msg)

        for y in range(self.map.height):
            for x in range(self.map.width):
                tname = self.map.terrain_at(x, y).name
                if tname in ("lava", "volcano_erupting", "forest_fire", "highland_forest_fire"):
                    for npc in self.map.animals[y][x]:
                        npc.alive = False
                        npc.age = -1
                        npc.speed = 0.0
                    self.map.eggs[y][x] = []
                    self.map.burrows[y][x] = None
                    self.map.plants[y][x] = []
                    continue
                if tname != "toxic_badlands":
                    continue
                for npc in list(self.map.animals[y][x]):
                    if not npc.alive:
                        continue
                    npc.health = max(0.0, npc.health - 20.0)
                    if npc.health <= 0:
                        npc.alive = False
                        npc.age = -1
                        npc.speed = 0.0

    def _check_victory(self) -> Optional[str]:
        """Check if the player has enough living descendants."""
        if not self.won and self.descendant_count() >= DESCENDANTS_TO_WIN:
            self.won = True
            return "\nYou have raised a thriving lineage! You win!"
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

        if self.player.adult_weight > 0:
            pct = self.player.weight / self.player.adult_weight
            pct = max(0.0, min(pct, 1.0))
            self.player.attack = self.player.adult_attack * pct
            self.player.hp = self.player.adult_hp * pct
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
        a_weight = stats.get("adult_weight", 0.0)
        if a_weight <= 0:
            pct = 1.0
        else:
            pct = weight / a_weight
        pct = max(0.0, min(pct, 1.0))
        h_val = stats.get(hatch_key, 0.0)
        a_val = stats.get(adult_key, 0.0)
        return h_val + pct * (a_val - h_val)

    def _scale_by_weight(self, weight: float, stats: dict, key: str) -> float:
        a_weight = stats.get("adult_weight", 0.0)
        val = stats.get(key, 0.0)
        if a_weight <= 0:
            pct = 1.0
        else:
            pct = weight / a_weight
        pct = max(0.0, min(pct, 1.0))
        return val * pct

    def _apply_damage(self, damage: float, animal, stats: dict) -> bool:
        """Apply damage to an animal or the player.

        Returns True if the target died."""
        max_hp = self._scale_by_weight(getattr(animal, "weight", 0.0), stats, "hp")
        curr = max_hp * (animal.health / 100.0)
        curr -= damage
        animal.health = 0.0 if max_hp <= 0 else max(0.0, curr / max_hp * 100.0)
        died = curr <= 0
        if died and isinstance(animal, NPCAnimal):
            animal.alive = False
            animal.age = -1
            animal.speed = 0.0
        return died

    def _npc_apply_growth(
        self, npc: NPCAnimal, available_food: float, stats: dict
    ) -> tuple[float, float]:
        max_gain = self._npc_max_growth_gain(npc.weight, stats)
        weight_gain = min(available_food, max_gain)
        npc.weight = min(npc.weight + weight_gain, stats.get("adult_weight", 0.0))
        pct = 1.0
        aw = stats.get("adult_weight", 0.0)
        if aw > 0:
            pct = npc.weight / aw
            pct = max(0.0, min(pct, 1.0))
        npc.attack = stats.get("attack", 0.0) * pct
        npc.hp = stats.get("hp", 0.0) * pct
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

    def _npc_consume_eggs(self, npc: NPCAnimal, eggs: EggCluster, stats: dict) -> float:
        energy_needed = 100.0 - npc.energy
        growth_target = self._npc_max_growth_gain(npc.weight, stats)
        eat_amount = eggs.weight

        energy_gain_possible = 1000 * eat_amount / max(npc.weight, 0.1)
        actual_energy_gain = min(energy_needed, energy_gain_possible)
        npc.energy = min(100.0, npc.energy + actual_energy_gain)
        weight_used = actual_energy_gain * npc.weight / 1000
        remaining = eat_amount - weight_used
        self._npc_apply_growth(npc, remaining, stats)
        eggs.weight = 0
        if eat_amount > 0:
            npc.egg_clusters_eaten += 1
        return eat_amount

    def _npc_dig_burrow(self, x: int, y: int) -> bool:
        """Dig out a burrow instantly and spawn a mammal if present."""
        burrow = self.map.get_burrow(x, y)
        if burrow is None or not burrow.full:
            return False
        burrow.full = False
        burrow.progress = 0.0
        if self.mammal_species:
            name = random.choice(self.mammal_species)
            stats = CRITTER_STATS.get(name, {})
            npc = NPCAnimal(
                id=self.next_npc_id,
                name=name,
                sex=None,
                weight=stats.get("adult_weight", 0.0),
                abilities=stats.get("abilities", []),
                last_action="spawned",
            )
            if not any(a.name == name for a in self.map.animals[y][x]):
                self.map.animals[y][x].append(npc)
                self.next_npc_id += 1
        return True

    def _can_player_lay_eggs(self) -> bool:
        stats = DINO_STATS.get(self.player.name, {})
        animals = self.map.animals[self.y][self.x]
        return (
            self.player.weight >= self.player.adult_weight
            and stats.get("can_be_juvenile", True)
            and self.player.energy >= 80
            and self.player.health >= 80
            and getattr(self.player, "turns_until_lay_eggs", 0) == 0
            and len(animals) < 4
        )

    def lay_eggs(self) -> str:
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)

        if not self._can_player_lay_eggs():
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn("You cannot lay eggs right now.")

        stats = DINO_STATS.get(self.player.name, {})
        self.player.energy *= 0.7
        num_eggs = stats.get("num_eggs", 0)
        hatch_w = stats.get(
            "hatchling_weight",
            max(MIN_HATCHING_WEIGHT, stats.get("adult_weight", 0.0) / HATCHLING_WEIGHT_DIVISOR),
        )
        hatch_w = max(hatch_w, MIN_HATCHING_WEIGHT)
        eggs = EggCluster(
            species=self.player.name,
            number=num_eggs,
            weight=hatch_w * num_eggs,
            turns_until_hatch=5,
            is_descendant=True,
        )
        self.map.add_eggs(self.x, self.y, eggs)
        append_event_log(f"Player laid eggs at ({self.x},{self.y})")
        self.player.turns_until_lay_eggs = stats.get("egg_laying_interval", 0)

        msg = "You lay eggs."
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "lay_eggs"
        if "Game Over" in end_msg:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)
        self._move_npcs()
        self.turn_messages.extend(self._update_npcs())
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(msg)

    def threaten(self) -> str:
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)

        cell = self.map.animals[self.y][self.x]
        player_a = max(self.player_effective_attack(), 0.1)

        weaker: list[tuple[NPCAnimal, dict]] = []
        stronger: list[NPCAnimal] = []
        for npc in cell:
            if not npc.alive:
                continue
            stats = DINO_STATS.get(npc.name)
            if stats is None:
                stats = CRITTER_STATS.get(npc.name, {})
            npc_a = self.npc_effective_attack(npc, stats, self.x, self.y)
            if npc_a > player_a:
                stronger.append(npc)
            else:
                weaker.append((npc, stats))

        if stronger:
            attacker = random.choice(stronger)
            msg = (
                f"You threaten the animals, but the {self._npc_label(attacker)} "
                "attacks and kills you! Game Over."
            )
            end_msg = self._apply_turn_costs(False, 2.0)
            self.player.health = 0
            msg += end_msg
            append_event_log(
                f"Player threatened and was killed by {self._npc_label(attacker)} "
                f"at ({self.x},{self.y})"
            )
            self.last_action = "threaten"
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)

        directions = {
            "Up": (0, -1),
            "Right": (1, 0),
            "Down": (0, 1),
            "Left": (-1, 0),
        }
        moved_count = 0
        for npc, stats in weaker:
            options = []
            can_walk = stats.get("can_walk", True)
            for dname, (dx, dy) in directions.items():
                nx, ny = self.x + dx, self.y + dy
                if not (0 <= nx < self.map.width and 0 <= ny < self.map.height):
                    continue
                terrain = self.map.terrain_at(nx, ny).name
                if not can_walk and terrain != "lake":
                    continue
                options.append(dname)
            npc.next_move = random.choice(options) if options else "None"
            if options:
                moved_count += 1

        if moved_count:
            msg = "You threaten the animals. They scatter!"
            append_event_log(
                f"Player threatened {moved_count} animals at ({self.x},{self.y})"
            )
        else:
            msg = "You threaten the animals, but nothing happens."
            append_event_log(
                f"Player threatened but nothing fled at ({self.x},{self.y})"
            )

        end_msg = self._apply_turn_costs(False, 2.0)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "threaten"
        if "Game Over" in end_msg:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)
        self._move_npcs()
        self.turn_messages.extend(self._update_npcs())
        attack = self._aggressive_attack_check()
        if attack:
            msg += "\n" + attack
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(msg)

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
                                f"The {self._npc_label(npc)} carcass lost {lost:.1f}kg to spoilage."
                            )
                        if npc.weight <= 0:
                            animals.remove(npc)
        return messages

    def _update_eggs(self) -> list[str]:
        """Advance egg timers and hatch any ready clusters."""
        messages: list[str] = []
        for y in range(self.map.height):
            for x in range(self.map.width):
                for egg in list(self.map.eggs[y][x]):
                    egg.turns_until_hatch -= 1
                    if egg.turns_until_hatch <= 0 and egg.weight > 0:
                        stats = DINO_STATS.get(egg.species, {})
                        hatch_w = stats.get(
                            "hatchling_weight",
                            max(
                                MIN_HATCHING_WEIGHT,
                                stats.get("adult_weight", 0.0) / HATCHLING_WEIGHT_DIVISOR,
                            ),
                        )
                        hatch_w = max(hatch_w, MIN_HATCHING_WEIGHT)
                        for _ in range(egg.number):
                            self.map.animals[y][x].append(
                                NPCAnimal(
                                    id=self.next_npc_id,
                                    name=egg.species,
                                    sex=None,
                                    weight=hatch_w,
                                    abilities=DINO_STATS.get(egg.species, {}).get("abilities", []),
                                    is_descendant=egg.is_descendant,
                                )
                            )
                            self.next_npc_id += 1
                        append_event_log(
                            f"{egg.number} {egg.species} eggs hatched at ({x},{y})"
                        )
                        if x == self.x and y == self.y:
                            messages.append(f"{egg.number} {egg.species} eggs hatch!")
                        self.map.eggs[y][x].remove(egg)
                    elif egg.turns_until_hatch <= 0 or egg.weight <= 0:
                        self.map.eggs[y][x].remove(egg)
        return messages

    def _record_population(self) -> None:
        counts, _ = self.population_stats()
        for name in self.population_history:
            self.population_history[name].append(counts.get(name, 0))
        self.turn_history.append(self.turn_count)

    def _choose_weather(self) -> Weather:
        weathers = [w for w, _ in WEATHER_TABLE]
        weights = [p for _, p in WEATHER_TABLE]
        return self._weather_rng.choices(weathers, weights=weights, k=1)[0]

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
                    if not npc.alive:
                        npc.next_move = "None"
                        continue
                    dx, dy = directions.get(d, (0, 0))
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.map.width and 0 <= ny < self.map.height:
                        moves.append((x, y, nx, ny, npc))
        for x, y, nx, ny, npc in moves:
            self.map.animals[y][x].remove(npc)
            self.map.animals[ny][nx].append(npc)
            npc.next_move = "None"

    def _npc_choose_move(self, x: int, y: int, npc: NPCAnimal, stats: dict) -> None:
        """Choose a movement direction for the NPC using normal logic."""
        if random.random() < 0.5:
            npc.next_move = "None"
            return
        dirs = {"Up": (0, -1), "Right": (1, 0), "Down": (0, 1), "Left": (-1, 0)}
        pref = stats.get("preferred_biomes", [])
        can_walk = stats.get("can_walk", True)
        candidates = []
        pref_candidates = []
        for dname, (dx, dy) in dirs.items():
            nx, ny = x + dx, y + dy
            if not (0 <= nx < self.map.width and 0 <= ny < self.map.height):
                continue
            terrain = self.map.terrain_at(nx, ny).name
            if terrain == "toxic_badlands":
                continue
            if not can_walk and terrain != "lake":
                continue
            candidates.append(dname)
            if terrain in pref:
                pref_candidates.append(dname)
        move_choice = None
        """Given the option of choosing between preferred and unpreferred biomes the NPC will choose the latter 20% of the time."""
        if pref_candidates and candidates and random.random() < 0.2:
            move_choice = random.choice(candidates)
        elif pref_candidates:
            move_choice = random.choice(pref_candidates)
        elif candidates:
            move_choice = random.choice(candidates)
        npc.next_move = move_choice or "None"

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
                    prev_action = npc.last_action
                    npc.last_action = "stay"
                    if prev_action == "spawned":
                        continue
                    if "ambush" in npc.abilities:
                        if prev_action == "stay":
                            npc.ambush_streak = min(npc.ambush_streak + 1, 3)
                        else:
                            npc.ambush_streak = 0
                    stats = DINO_STATS.get(npc.name)
                    if stats is None:
                        cstats = CRITTER_STATS.get(npc.name)
                        npc.next_move = "None"
                        if cstats is not None:
                            self._npc_choose_move(x, y, npc, cstats)
                        continue
                    npc.next_move = "None"
                    if npc.turns_until_lay_eggs > 0:
                        npc.turns_until_lay_eggs -= 1
                    base_drain = (
                        stats.get("adult_energy_drain", 0.0)
                        * 0.5
                        * self.weather.npc_energy_mult
                    )
                    npc.energy = max(0.0, npc.energy - base_drain)
                    if npc.energy <= 0:
                        npc.alive = False
                        npc.age = -1
                        npc.speed = 0.0
                        continue
                    regen = stats.get("health_regen", 0.0)
                    if npc.health < 100.0 and regen:
                        npc.health = min(100.0, npc.health + regen)

                    if (
                        npc.weight >= stats.get("adult_weight", 0.0)
                        and stats.get("can_be_juvenile", True)
                        and npc.energy >= 80
                        and npc.health >= 80
                        and npc.turns_until_lay_eggs == 0
                    ):
                        if len(animals) >= 4:
                            self._npc_choose_move(x, y, npc, stats)
                            if npc.next_move != "None":
                                extra = base_drain * (
                                    WALKING_ENERGY_DRAIN_MULTIPLIER - 1.0
                                )
                                if extra > 0:
                                    npc.energy = max(0.0, npc.energy - extra)
                                    if npc.energy <= 0:
                                        npc.alive = False
                                        npc.age = -1
                                        npc.speed = 0.0
                                        continue
                            continue
                        npc.energy *= 0.7
                        num_eggs = stats.get("num_eggs", 0)
                        hatch_w = stats.get(
                            "hatchling_weight",
                            max(1.0, stats.get("adult_weight", 0.0) * 0.001),
                        )
                        eggs = EggCluster(
                            species=npc.name,
                            number=num_eggs,
                            weight=hatch_w * num_eggs,
                            turns_until_hatch=5,
                            is_descendant=npc.is_descendant,
                        )
                        self.map.add_eggs(x, y, eggs)
                        append_event_log(
                            f"{self._npc_label(npc)} laid eggs at ({x},{y})"
                        )
                        npc.turns_until_lay_eggs = stats.get("egg_laying_interval", 0)
                        if x == self.x and y == self.y:
                            messages.append(f"The {self._npc_label(npc)} lays eggs.")
                        npc.last_action = "act"
                        continue

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
                                messages.append(
                                    f"The {self._npc_label(npc)} eats {eaten:.1f}kg from a carcass."
                                )
                            if carcass.weight <= 0:
                                animals.remove(carcass)
                            found_food = True
                            npc.next_move = "None"
                            npc.last_action = "act"
                            continue

                        egg_clusters = self.map.eggs[y][x]
                        if egg_clusters:
                            egg = next(
                                (e for e in egg_clusters if e.species != npc.name),
                                None,
                            )
                            if egg is not None:
                                eaten = self._npc_consume_eggs(npc, egg, stats)
                                if x == self.x and y == self.y:
                                    messages.append(
                                        f"The {self._npc_label(npc)} eats {eaten:.1f}kg of eggs."
                                    )
                                egg_clusters.remove(egg)
                                found_food = True
                                npc.next_move = "None"
                                npc.last_action = "act"
                                continue

                        if "digger" in npc.abilities and self._npc_dig_burrow(x, y):
                            if x == self.x and y == self.y:
                                messages.append(
                                    f"The {self._npc_label(npc)} digs up a burrow."
                                )
                            found_food = True
                            npc.next_move = "None"
                            npc.last_action = "act"
                            continue

                    if plants and any(d in diet for d in (Diet.FERNS, Diet.CYCADS, Diet.CONIFERS, Diet.FRUITS)):
                        allowed_plants = {
                            d.value
                            for d in diet
                            if d in (Diet.FERNS, Diet.CYCADS, Diet.CONIFERS, Diet.FRUITS)
                        }
                        options = [p for p in plants if p.name.lower() in allowed_plants]
                        if options:
                            chosen = max(options, key=lambda p: p.weight)
                            eaten = self._npc_consume_plant(npc, chosen, stats)
                            if x == self.x and y == self.y:
                                messages.append(
                                    f"The {self._npc_label(npc)} eats {eaten:.1f}kg of {chosen.name}."
                                )
                            if chosen.weight <= 0:
                                plants.remove(chosen)
                            found_food = True
                            npc.next_move = "None"
                            npc.last_action = "act"
                            continue

                    if Diet.MEAT in diet:
                        npc_speed = self._stat_from_weight(
                            npc.weight, stats, "hatchling_speed", "adult_speed"
                        )
                        if "ambush" in npc.abilities:
                            npc_speed *= 1 + min(npc.ambush_streak, 3) * 0.05
                        npc_atk = self.npc_effective_attack(npc, stats, x, y)
                        npc_hp = self._scale_by_weight(npc.weight, stats, "hp")
                        potential = []
                        for other in animals:
                            if other is npc or not other.alive:
                                continue
                            o_stats = DINO_STATS.get(other.name) or CRITTER_STATS.get(other.name, {})
                            o_atk = self.npc_effective_attack(other, o_stats, x, y)
                            o_hp = self._scale_by_weight(other.weight, o_stats, "hp")
                            if o_atk / max(npc_hp, 0.1) >= npc_atk / max(o_hp, 0.1):
                                continue
                            o_speed = self._stat_from_weight(other.weight, o_stats, "hatchling_speed", "adult_speed")
                            if o_speed >= npc_speed:
                                continue
                            if other.weight < npc.weight * 0.01:
                                continue
                            potential.append((other, o_speed, o_atk, o_hp, o_stats))
                        if potential:
                            target, t_speed, t_atk, t_hp, t_stats = random.choice(potential)
                            rel_speed = t_speed / max(npc_speed, 0.1)
                            if random.random() <= calculate_catch_chance(rel_speed):
                                self._apply_damage(t_atk, npc, stats)
                                killed = self._apply_damage(npc_atk, target, t_stats)
                                if killed:
                                    npc.hunts[target.name] = npc.hunts.get(target.name, 0) + 1
                                    eaten = self._npc_consume_meat(npc, target, stats)
                                    if x == self.x and y == self.y:
                                        messages.append(
                                            f"The {self._npc_label(npc)} hunts and eats {eaten:.1f}kg of {self._npc_label(target)}."
                                        )
                                    if target.weight <= 0:
                                        animals.remove(target)
                                if npc.health <= 0:
                                    npc.alive = False
                                    npc.age = -1
                                    npc.speed = 0.0
                                found_food = True
                                npc.next_move = "None"
                                npc.last_action = "act"
                                continue

                    if found_food:
                        continue

                    self._npc_choose_move(x, y, npc, stats)
                    if npc.next_move != "None":
                        extra = base_drain * (
                            WALKING_ENERGY_DRAIN_MULTIPLIER - 1.0
                        )
                        if extra > 0:
                            npc.energy = max(0.0, npc.energy - extra)
                            if npc.energy <= 0:
                                npc.alive = False
                                npc.age = -1
                                npc.speed = 0.0
                                continue
                        npc.last_action = "move"
        return messages


    def hunt_npc(self, npc_id: int) -> str:
        """Hunt a specific NPC animal by its ID."""
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)

        cell = self.map.animals[self.y][self.x]
        target = next((n for n in cell if n.id == npc_id), None)
        if target is None:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn("Unknown target.")

        was_alive = target.alive
        hunt = None
        if was_alive:
            hunt = self.hunt_stats.setdefault(target.name, [0, 0])
            hunt[0] += 1

        stats = DINO_STATS.get(target.name)
        if not stats:
            stats = CRITTER_STATS.get(target.name, {})

        player_speed = self.player_effective_speed()

        if target.alive:
            target_speed = self.npc_effective_speed(target, stats)
            target_attack = self.npc_effective_attack(target, stats, self.x, self.y)
        else:
            target_speed = 0.0
            target_attack = 0.0

        if target.alive:
            rel_speed = target_speed / max(player_speed, 0.1)
            catch_chance = calculate_catch_chance(rel_speed)
            if random.random() > catch_chance:
                msg = f"The {self._npc_label(target)} escaped before you could catch it."
                end_msg = self._apply_turn_costs(False, 5.0)
                msg += end_msg
                win = self._check_victory()
                if win:
                    msg += win
                self.last_action = "hunt"
                if "Game Over" in end_msg:
                    self.turn_messages.extend(self._update_npcs())
                    self._move_npcs()
                    self.turn_messages.extend(self._spoil_carcasses())
                    self._generate_encounters()
                    self._reveal_adjacent_mountains()
                    return self._finish_turn(msg)
                self.turn_messages.extend(self._update_npcs())
                self._move_npcs()
                attack = self._aggressive_attack_check()
                if attack:
                    msg += "\n" + attack
                self.turn_messages.extend(self._spoil_carcasses())
                self._generate_encounters()
                self._reveal_adjacent_mountains()
                return self._finish_turn(msg)

        player_attack = self.player_effective_attack()
        died_player = False
        if target.alive:
            before = self.player.health
            died_player = self._apply_damage(target_attack, self.player, DINO_STATS.get(self.player.name, {}))
            player_damage = before - self.player.health
            target_died = self._apply_damage(player_attack, target, stats)
        if died_player:
                self.turn_messages.extend(self._update_npcs())
                self._move_npcs()
                self.turn_messages.extend(self._spoil_carcasses())
                self._generate_encounters()
                self._reveal_adjacent_mountains()
                return self._finish_turn(
                    f"You fought the {self._npc_label(target)} but received fatal injuries. Game Over."
                )
        else:
            player_damage = 0.0
            target_died = self._apply_damage(player_attack, target, stats)

        if not target_died:
            end_msg = self._apply_turn_costs(False)
            msg = f"You attack the {self._npc_label(target)} dealing {player_attack:.0f} damage." + end_msg
            self.last_action = "hunt"
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            attack = self._aggressive_attack_check()
            if attack:
                msg += "\n" + attack
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)

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
        target.speed = 0.0
        target.weight = max(0.0, target.weight - consumed)
        if target.weight <= 0:
            self.map.remove_animal(self.x, self.y, npc_id=target.id)
        if was_alive and hunt is not None:
            hunt[1] += 1

        msg = (
            f"You caught and defeated the {self._npc_label(target)} "
            f"but lost {player_damage:.0f}% health. "
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
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return msg
        self.turn_messages.extend(self._update_npcs())
        self._move_npcs()
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
        # Remove the recruited dinosaur from the cell and capture its ID
        cell = self.map.animals[self.y][self.x]
        partner = next((n for n in cell if n.name == self.player.name), None)
        partner_id = partner.id if partner else 0
        if partner:
            self.map.remove_animal(self.x, self.y, npc_id=partner.id)
        msg = f"A {self.player.name} ({partner_id}) joins your pack."
        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "pack"
        if "Game Over" in end_msg:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)
        self.turn_messages.extend(self._update_npcs())
        self._move_npcs()
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
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)
        self.turn_messages.extend(self._update_npcs())
        self._move_npcs()
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
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)
        self.turn_messages.extend(self._update_npcs())
        self._move_npcs()
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

        egg = self.map.take_eggs(self.x, self.y)
        if egg is None:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn("There are no eggs here.")

        egg_weight = egg.weight

        energy_gain = 1000 * egg_weight / max(self.player.weight, 0.1)
        needed = 100.0 - self.player.energy
        actual_energy_gain = min(energy_gain, needed)
        self.player.energy = min(100.0, self.player.energy + actual_energy_gain)

        meat_used = actual_energy_gain * self.player.weight / 1000
        leftover_meat = max(0.0, egg_weight - meat_used)

        weight_gain, max_gain = self._apply_growth(leftover_meat)

        msg = (
            f"You eat {egg.number} {egg.species} eggs. "
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
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)
        self.turn_messages.extend(self._update_npcs())
        self._move_npcs()
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(msg)

    def dig_burrow(self) -> str:
        pre = self._start_turn()
        if pre:
            return self._finish_turn(pre)

        burrow = self.map.get_burrow(self.x, self.y)
        if burrow is None:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn("There is no burrow here.")

        if not burrow.full:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn("The burrow is empty.")

        gain = 100.0 if "digger" in self.player.abilities else 25.0
        burrow.progress = min(100.0, burrow.progress + gain)
        msg = f"Digging... {burrow.progress:.0f}%"
        spawned = None
        if burrow.progress >= 100.0:
            burrow.full = False
            burrow.progress = 0.0
            if self.mammal_species:
                name = random.choice(self.mammal_species)
                stats = CRITTER_STATS.get(name, {})
                npc = NPCAnimal(
                    id=self.next_npc_id,
                    name=name,
                    sex=None,
                    weight=stats.get("adult_weight", 0.0),
                    abilities=stats.get("abilities", []),
                    last_action="spawned",
                )
                self.map.animals[self.y][self.x].append(npc)
                self.next_npc_id += 1
                spawned = name
        if spawned:
            msg = f"You dug out a {spawned}!"

        end_msg = self._apply_turn_costs(False)
        msg += end_msg
        win = self._check_victory()
        if win:
            msg += win
        self.last_action = "dig"
        if "Game Over" in end_msg:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(msg)
        self.turn_messages.extend(self._update_npcs())
        self._move_npcs()
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

    def descendant_count(self) -> int:
        """Return the number of living NPCs descended from the player."""
        count = 0
        for row in self.map.animals:
            for cell in row:
                for npc in cell:
                    if npc.is_descendant and npc.alive:
                        count += 1
        return count

    def move(self, dx: int, dy: int):
        nx = max(0, min(self.map.width - 1, self.x + dx))
        ny = max(0, min(self.map.height - 1, self.y + dy))
        self.x, self.y = nx, ny
        self.map.reveal(self.x, self.y)
        self._reveal_cardinals(self.x, self.y)
        if self.map.terrain_at(self.x, self.y).name in (
            "mountain",
            "volcano",
            "volcano_erupting",
        ):
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
        if moved and self.map.terrain_at(self.x, self.y).name in (
            "mountain",
            "volcano",
            "volcano_erupting",
        ):
            multiplier *= 3
        end_msg = self._apply_turn_costs(moved, multiplier)
        if action == "stay":
            result += f" (-{self.last_hydration_loss:.1f} hydration, -{self.last_energy_loss:.1f} energy)"
        result += end_msg
        win = self._check_victory()
        if win:
            result += win
        self._energy_multiplier = 1.0
        self.last_action = action
        if "Game Over" in end_msg:
            self.turn_messages.extend(self._update_npcs())
            self._move_npcs()
            self.turn_messages.extend(self._spoil_carcasses())
            self._generate_encounters()
            self._reveal_adjacent_mountains()
            return self._finish_turn(result)

        self._move_npcs()
        self.turn_messages.extend(self._update_npcs())
        self._apply_terrain_effects()
        if action in ("stay", "drink"):
            attack = self._aggressive_attack_check()
            if attack:
                result += "\n" + attack
        self.turn_messages.extend(self._spoil_carcasses())
        self._generate_encounters()
        self._reveal_adjacent_mountains()
        return self._finish_turn(result)
