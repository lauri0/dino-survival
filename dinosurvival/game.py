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
        self.x = width // 2
        self.y = height // 2
        self.map.reveal(self.x, self.y)

    def hunt(self):
        terrain = self.map.terrain_at(self.x, self.y)
        prey_type = random.choices(list(terrain.spawn_chance.keys()), weights=list(terrain.spawn_chance.values()))[0]
        success = random.random() < 0.6  # simple success rate
        if success:
            self.player.energy = 100.0
            return f"Caught {prey_type}!"
        return f"Failed to catch {prey_type}."

    def move(self, dx: int, dy: int):
        nx = max(0, min(self.map.width - 1, self.x + dx))
        ny = max(0, min(self.map.height - 1, self.y + dy))
        self.x, self.y = nx, ny
        self.map.reveal(self.x, self.y)

    def turn(self, action: str) -> str:
        moved = False
        if action == "hunt":
            result = self.hunt()
        elif action == "north":
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
        else:
            result = "Unknown action"

        drain = (
            self.player.hatchling_energy_drain
            if self.player.growth_stages > 0
            else self.player.adult_energy_drain
        )
        if moved:
            drain *= self.player.walking_energy_drain_multiplier
        self.player.energy = max(0.0, self.player.energy - drain)

        if self.player.is_exhausted():
            return result + "\nYou have collapsed from exhaustion! Game Over."

        return result
