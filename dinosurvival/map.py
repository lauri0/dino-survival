from dataclasses import dataclass
from typing import Dict, Tuple, List
import random

@dataclass
class Terrain:
    name: str
    spawn_chance: Dict[str, float]


class Map:
    def __init__(self, width: int, height: int, terrains: Dict[str, Terrain]):
        self.width = width
        self.height = height
        self.terrains = terrains
        terrain_list = list(terrains.values())
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
                    idx = min(int(n * len(terrain_list)), len(terrain_list) - 1)
                    terrain = terrain_list[idx]
                    if terrain.name == "lake":
                        has_lake = True
                    row.append(terrain)
                grid.append(row)
            if has_lake:
                self.grid = grid
        
        self.revealed = [[False for _ in range(width)] for _ in range(height)]

    def terrain_at(self, x: int, y: int) -> Terrain:
        return self.grid[y][x]

    def reveal(self, x: int, y: int) -> None:
        self.revealed[y][x] = True

    def is_revealed(self, x: int, y: int) -> bool:
        return self.revealed[y][x]

    def _generate_noise(self, width: int, height: int, scale: int = 4) -> List[List[float]]:
        """Create a simple value noise map for distributing biomes."""
        coarse_w = width // scale + 1
        coarse_h = height // scale + 1
        coarse = [[random.random() for _ in range(coarse_w)] for _ in range(coarse_h)]

        def lerp(a: float, b: float, t: float) -> float:
            return a + (b - a) * t

        noise = []
        for y in range(height):
            fy = y / (height - 1) * (coarse_h - 1)
            y0 = int(fy)
            y1 = min(y0 + 1, coarse_h - 1)
            ty = fy - y0
            row = []
            for x in range(width):
                fx = x / (width - 1) * (coarse_w - 1)
                x0 = int(fx)
                x1 = min(x0 + 1, coarse_w - 1)
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
