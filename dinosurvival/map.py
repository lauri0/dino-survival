from dataclasses import dataclass
from typing import Dict, Tuple, List, Optional
from .plant import PlantStats, Plant
from .dinosaur import NPCAnimal
import random

@dataclass
class Terrain:
    name: str
    spawn_chance: Dict[str, float]
    abundance: float = 1.0


@dataclass
class EggCluster:
    species: str
    number: int
    weight: float
    turns_until_hatch: int
    is_descendant: bool = False


@dataclass
class Burrow:
    """A small ground burrow that may contain an animal."""

    full: bool = True
    progress: float = 0.0


class Map:
    def __init__(
        self,
        width: int,
        height: int,
        terrains: Dict[str, Terrain],
        height_levels: Dict[str, float],
        humidity_levels: Dict[str, float],
    ):
        self.width = width
        self.height = height
        self.terrains = terrains

        height_order = ["low", "normal", "mountain"]
        humidity_order = ["arid", "normal", "humid"]

        def build_thresholds(levels: Dict[str, float], order: List[str]) -> List[float]:
            vals = [levels.get(name, 0.0) for name in order]
            total = sum(vals)
            if total <= 0:
                return [(i + 1) / len(order) for i in range(len(order))]
            cumulative = 0.0
            thresholds = []
            for v in vals:
                cumulative += v
                thresholds.append(cumulative / total)
            return thresholds

        height_thresholds = build_thresholds(height_levels, height_order)
        humidity_thresholds = build_thresholds(humidity_levels, humidity_order)

        # Keep generating maps until at least one lake tile is present and the
        # majority of lakes are not sitting on the outermost rows or columns.
        while True:
            h_noise = self._generate_noise(width, height)
            m_noise = self._generate_noise(width, height)
            grid: List[List[Terrain]] = []
            lake_count = 0
            edge_lake_count = 0
            edge_margin = 2
            for y in range(height):
                row: List[Terrain] = []
                for x in range(width):
                    hn = h_noise[y][x]
                    mn = m_noise[y][x]

                    for idx, tval in enumerate(height_thresholds):
                        if hn <= tval:
                            h_level = height_order[idx]
                            break
                    else:
                        h_level = height_order[-1]

                    for idx, tval in enumerate(humidity_thresholds):
                        if mn <= tval:
                            m_level = humidity_order[idx]
                            break
                    else:
                        m_level = humidity_order[-1]

                    biome_map = {
                        ("arid", "low"): "desert",
                        ("arid", "normal"): "plains",
                        ("arid", "mountain"): "toxic_badlands",
                        ("normal", "low"): "woodlands",
                        ("normal", "normal"): "forest",
                        ("normal", "mountain"): "mountain",
                        ("humid", "low"): "lake",
                        ("humid", "normal"): "swamp",
                        ("humid", "mountain"): "mountain",
                    }

                    terrain_name = biome_map.get((m_level, h_level), "plains")
                    if terrain_name == "mountain" and random.random() < 0.5:
                        terrain_name = "volcano"
                    terrain = terrains[terrain_name]
                    if terrain_name == "lake":
                        lake_count += 1
                        if (
                            x < edge_margin
                            or x >= width - edge_margin
                            or y < edge_margin
                            or y >= height - edge_margin
                        ):
                            edge_lake_count += 1
                    row.append(terrain)
                grid.append(row)

            if lake_count > 0:
                # Require at least one interior lake and limit the fraction of
                # lakes that appear near the map border.
                interior_lakes = lake_count - edge_lake_count
                if interior_lakes > 0 and edge_lake_count / lake_count <= 0.6:
                    self.grid = grid
                    break
        
        self.revealed = [[False for _ in range(width)] for _ in range(height)]
        self.plants: List[List[List[Plant]]] = [
            [[] for _ in range(width)] for _ in range(height)
        ]
        self.eggs: List[List[List[EggCluster]]] = [
            [[] for _ in range(width)] for _ in range(height)
        ]
        # List of animals present in each cell.
        self.animals: List[List[List["NPCAnimal"]]] = [
            [[] for _ in range(width)] for _ in range(height)
        ]
        # Optional burrow present in each cell
        self.burrows: List[List[Optional[Burrow]]] = [
            [None for _ in range(width)] for _ in range(height)
        ]
        # Track active lava tiles and erupting volcanoes
        self.lava_info: List[List[Optional[Dict[str, int]]]] = [
            [None for _ in range(width)] for _ in range(height)
        ]
        self.erupting: List[List[bool]] = [
            [False for _ in range(width)] for _ in range(height)
        ]

    def terrain_at(self, x: int, y: int) -> Terrain:
        return self.grid[y][x]

    def reveal(self, x: int, y: int) -> None:
        self.revealed[y][x] = True

    def is_revealed(self, x: int, y: int) -> bool:
        return self.revealed[y][x]


    def update_eggs(self) -> None:
        for y in range(self.height):
            for x in range(self.width):
                for egg in list(self.eggs[y][x]):
                    egg.turns_until_hatch -= 1
                    if egg.turns_until_hatch <= 0 or egg.weight <= 0:
                        self.eggs[y][x].remove(egg)

    def take_eggs(self, x: int, y: int) -> Optional[EggCluster]:
        cell = self.eggs[y][x]
        if cell:
            return cell.pop(0)
        return None

    def add_eggs(self, x: int, y: int, eggs: EggCluster) -> None:
        self.eggs[y][x].append(eggs)

    def has_nest(self, x: int, y: int) -> bool:
        """Return ``True`` if any eggs are present in the cell."""
        return bool(self.eggs[y][x])

    def has_burrow(self, x: int, y: int) -> bool:
        """Return ``True`` if a burrow exists in the cell."""
        return self.burrows[y][x] is not None

    def spawn_burrow(self, x: int, y: int, full: bool = True) -> None:
        """Create a burrow on the specified cell if it is not a lake."""
        if self.terrain_at(x, y).name == "lake":
            return
        self.burrows[y][x] = Burrow(full=full)

    def get_burrow(self, x: int, y: int) -> Optional[Burrow]:
        return self.burrows[y][x]

    def populate_burrows(self, count: int) -> None:
        """Randomly place the given number of burrows on land tiles."""
        land_tiles: List[Tuple[int, int]] = []
        for y in range(self.height):
            for x in range(self.width):
                if self.terrain_at(x, y).name != "lake":
                    land_tiles.append((x, y))
        for _ in range(count):
            if not land_tiles:
                break
            x, y = random.choice(land_tiles)
            land_tiles.remove((x, y))
            self.spawn_burrow(x, y, full=True)

    def refresh_burrows(self, chance: float = 0.02) -> None:
        """Randomly refill empty burrows based on a probability."""
        for y in range(self.height):
            for x in range(self.width):
                burrow = self.burrows[y][x]
                if burrow is not None and not burrow.full:
                    if random.random() < chance:
                        burrow.full = True
                        burrow.progress = 0.0

    def grow_plants(self, plant_stats: dict[str, "PlantStats"], formation: str) -> None:
        for y in range(self.height):
            for x in range(self.width):
                cell_plants = self.plants[y][x]
                terrain = self.terrain_at(x, y).name
                for name, stats in plant_stats.items():
                    if formation not in stats.formations:
                        continue
                    chance = stats.growth_chance.get(terrain, 0)
                    if random.random() >= chance:
                        continue

                    existing = next((p for p in cell_plants if p.name == name), None)
                    if existing is not None:
                        existing.weight = min(
                            existing.weight + stats.weight, stats.weight * 10
                        )
                    else:
                        cell_plants.append(Plant(name=name, weight=stats.weight))

    def remove_animal(
        self,
        x: int,
        y: int,
        name: Optional[str] = None,
        sex: Optional[str] = None,
        npc_id: Optional[int] = None,
    ) -> bool:
        """Remove an animal from the specified cell.

        Returns ``True`` if an animal was removed.
        """
        cell = self.animals[y][x]
        for idx, npc in enumerate(cell):
            if npc_id is not None and npc.id != npc_id:
                continue
            if name is not None and npc.name != name:
                continue
            if sex is not None and npc.sex != sex:
                continue
            del cell[idx]
            return True
        return False

    # ------------------------------------------------------------------
    # Volcano and lava handling
    # ------------------------------------------------------------------

    def start_volcano_eruption(self, x: int, y: int, size: str = "small") -> None:
        """Trigger a volcanic eruption at ``(x, y)``.

        ``size`` may be ``"small"``, ``"medium"``, or ``"large"`` and
        controls how far the lava will spread.
        """
        if self.terrain_at(x, y).name != "volcano":
            return

        steps_map = {"small": 0, "medium": 2, "large": 4}
        steps = steps_map.get(size, 0)

        self.erupting[y][x] = True

        affected = [(x, y), (x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
        for ax, ay in affected:
            if not (0 <= ax < self.width and 0 <= ay < self.height):
                continue
            # Kill animals, eggs, and burrows
            self.animals[ay][ax] = []
            self.eggs[ay][ax] = []
            self.burrows[ay][ax] = None

            self.grid[ay][ax] = self.terrains["lava"]
            spread_steps = steps if (ax == x and ay == y) else max(steps - 1, 0)
            self.lava_info[ay][ax] = {"steps": spread_steps, "cooldown": 1}

    def update_lava(self) -> None:
        """Advance lava spread and handle solidification."""

        new_lava: List[Tuple[int, int, int]] = []
        to_solidify: List[Tuple[int, int]] = []

        for y in range(self.height):
            for x in range(self.width):
                info = self.lava_info[y][x]
                if info is None:
                    continue

                if info["steps"] > 0:
                    for nx, ny in (
                        (x + 1, y),
                        (x - 1, y),
                        (x, y + 1),
                        (x, y - 1),
                    ):
                        if 0 <= nx < self.width and 0 <= ny < self.height:
                            if self.lava_info[ny][nx] is None:
                                new_lava.append((nx, ny, info["steps"] - 1))
                    info["steps"] -= 1
                else:
                    info["cooldown"] -= 1
                    if info["cooldown"] <= 0:
                        to_solidify.append((x, y))

        for nx, ny, steps in new_lava:
            self.animals[ny][nx] = []
            self.eggs[ny][nx] = []
            self.burrows[ny][nx] = None
            self.grid[ny][nx] = self.terrains["lava"]
            self.lava_info[ny][nx] = {"steps": steps, "cooldown": 1}

        for x, y in to_solidify:
            self.grid[y][x] = self.terrains["solidified_lava_field"]
            self.lava_info[y][x] = None
            self.erupting[y][x] = False

    def roll_volcanoes(self) -> None:
        """Randomly erupt dormant volcanoes."""
        for y in range(self.height):
            for x in range(self.width):
                if self.terrain_at(x, y).name != "volcano":
                    continue
                if random.random() < 0.01:
                    r = random.random()
                    if r < 0.1:
                        size = "large"
                    elif r < 0.4:
                        size = "medium"
                    else:
                        size = "small"
                    self.start_volcano_eruption(x, y, size)

    def update_volcanic_activity(self) -> None:
        """Run volcanic eruption checks and update lava spread."""
        self.roll_volcanoes()
        self.update_lava()

    def _generate_noise(self, width: int, height: int, scale: int = 3) -> List[List[float]]:
        """Create a simple value noise map for distributing biomes.

        The ``scale`` parameter controls the granularity of the generated
        noise. Lower values result in more, smaller features across the map.
        A value of ``3`` gives slightly more detail compared to the previous
        value of ``4`` so that additional landscape features can fit within
        typical map sizes.
        """
        # Generate a noise grid larger than the map itself so that the
        # noise used for the map comes from the interior of this grid.
        # This avoids the edges of the map coinciding with the edges of
        # the noise which previously biased lake placement towards map
        # borders.
        coarse_w = width // scale + 3
        coarse_h = height // scale + 3
        coarse = [[random.random() for _ in range(coarse_w)] for _ in range(coarse_h)]

        def lerp(a: float, b: float, t: float) -> float:
            return a + (b - a) * t

        noise = []
        for y in range(height):
            # Sample from the interior of the coarse grid so that map
            # boundaries are not aligned with the noise boundaries.
            fy = y / (height - 1) * (coarse_h - 3) + 1
            y0 = int(fy)
            y1 = y0 + 1
            ty = fy - y0
            row = []
            for x in range(width):
                fx = x / (width - 1) * (coarse_w - 3) + 1
                x0 = int(fx)
                x1 = x0 + 1
                tx = fx - x0

                n00 = coarse[y0][x0]
                n10 = coarse[y0][x1]
                n01 = coarse[y1][x0]
                n11 = coarse[y1][x1]

                n0 = lerp(n00, n10, tx)
                n1 = lerp(n01, n11, tx)
                val = lerp(n0, n1, ty)
                row.append(val)
            noise.append(row)
        return noise


def map_reveal(m: "Map") -> None:
    """Reveal the entire map for debugging purposes."""
    for y in range(m.height):
        for x in range(m.width):
            m.reveal(x, y)
