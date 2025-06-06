from dataclasses import dataclass
from typing import Dict, Tuple, List, Optional
import random

@dataclass
class Terrain:
    name: str
    spawn_chance: Dict[str, float]
    abundance: float = 1.0


@dataclass
class Nest:
    eggs: str  # 'none', 'small', 'medium', 'large'


class Map:
    def __init__(self, width: int, height: int, terrains: Dict[str, Terrain]):
        self.width = width
        self.height = height
        self.terrains = terrains
        terrain_list = list(terrains.values())
        abundances = [t.abundance for t in terrain_list]
        total_abundance = sum(abundances)
        if total_abundance <= 0:
            thresholds = [(i + 1) / len(terrain_list) for i in range(len(terrain_list))]
        else:
            cumulative = 0.0
            thresholds = []
            for a in abundances:
                cumulative += a
                thresholds.append(cumulative / total_abundance)

        # Keep generating maps until at least one lake tile is present
        has_lake = False
        while not has_lake:
            noise = self._generate_noise(width, height)
            grid: List[List[Terrain]] = []
            has_lake = False
            for y in range(height):
                row: List[Terrain] = []
                for x in range(width):
                    n = noise[y][x]
                    for idx, tval in enumerate(thresholds):
                        if n <= tval:
                            terrain = terrain_list[idx]
                            break
                    else:
                        terrain = terrain_list[-1]
                    if terrain.name == "lake":
                        has_lake = True
                    row.append(terrain)
                grid.append(row)
            if has_lake:
                self.grid = grid
        
        self.revealed = [[False for _ in range(width)] for _ in range(height)]
        self.danger = [[0.0 for _ in range(width)] for _ in range(height)]
        self.nests: Dict[Tuple[int, int], Nest] = {}

        # Place 5 nests randomly across the map
        num_nests = 5
        all_coords = [(x, y) for y in range(height) for x in range(width)]
        random.shuffle(all_coords)
        for x, y in all_coords[:num_nests]:
            state = random.choice(["none", "small", "medium", "large"])
            self.nests[(x, y)] = Nest(state)

    def terrain_at(self, x: int, y: int) -> Terrain:
        return self.grid[y][x]

    def reveal(self, x: int, y: int) -> None:
        self.revealed[y][x] = True

    def is_revealed(self, x: int, y: int) -> bool:
        return self.revealed[y][x]

    def danger_at(self, x: int, y: int) -> float:
        return self.danger[y][x]

    def increase_danger(self, x: int, y: int) -> None:
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                nx, ny = x + dx, y + dy
                if 0 <= nx < self.width and 0 <= ny < self.height:
                    if dx == 0 and dy == 0:
                        amt = 50.0
                    elif abs(dx) + abs(dy) == 1:
                        amt = 20.0
                    else:
                        amt = 10.0
                    self.danger[ny][nx] = min(100.0, self.danger[ny][nx] + amt)

    def decay_danger(self, amount: float = 1.0) -> None:
        for y in range(self.height):
            for x in range(self.width):
                self.danger[y][x] = max(0.0, self.danger[y][x] - amount)

    def has_nest(self, x: int, y: int) -> bool:
        return (x, y) in self.nests

    def nest_state(self, x: int, y: int) -> Optional[str]:
        nest = self.nests.get((x, y))
        return nest.eggs if nest else None

    def update_nests(self) -> None:
        for nest in self.nests.values():
            if nest.eggs == "none":
                if random.random() < 0.01:
                    nest.eggs = random.choice(["small", "medium", "large"])
            else:
                if random.random() < 0.01:
                    nest.eggs = "none"

    def take_eggs(self, x: int, y: int) -> Optional[str]:
        nest = self.nests.get((x, y))
        if nest and nest.eggs != "none":
            eggs = nest.eggs
            nest.eggs = "none"
            return eggs
        return None

    def _generate_noise(self, width: int, height: int, scale: int = 4) -> List[List[float]]:
        """Create a simple value noise map for distributing biomes."""
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
