from dataclasses import dataclass
from typing import Dict, Tuple
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
        self.grid = [[random.choice(list(terrains.values())) for _ in range(width)] for _ in range(height)]

    def terrain_at(self, x: int, y: int) -> Terrain:
        return self.grid[y][x]
