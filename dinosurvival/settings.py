from __future__ import annotations
from dataclasses import dataclass
from typing import Dict
from .map import Terrain

@dataclass
class Setting:
    name: str
    formation: str
    playable_dinos: Dict[str, Dict]
    terrains: Dict[str, Terrain]


MORRISON = Setting(
    name="Morrison Formation",
    formation="Morrison",
    playable_dinos={
        "Allosaurus": {"energy_threshold": 0, "growth_stages": 3},
        "Ceratosaurus": {"energy_threshold": 0, "growth_stages": 3},
        "Torvosaurus": {"energy_threshold": 0, "growth_stages": 3},
        "Ornitholestes": {"energy_threshold": 0, "growth_stages": 3},
    },
    terrains={
        "badlands": Terrain("badlands", {"small_prey": 0.3, "large_prey": 0.7}, abundance=0.15),
        "plains": Terrain("plains", {"small_prey": 0.8, "large_prey": 0.2}, abundance=0.25),
        "woodlands": Terrain("woodlands", {"small_prey": 0.6, "large_prey": 0.4}, abundance=0.15),
        "forest": Terrain("forest", {"small_prey": 0.6, "large_prey": 0.4}, abundance=0.25),
        "swamp": Terrain("swamp", {"small_prey": 0.7, "large_prey": 0.3}, abundance=0.05),
        "lake": Terrain("lake", {"small_prey": 0.9, "large_prey": 0.1}, abundance=0.15),
    },
)

HELL_CREEK = Setting(
    name="Hell Creek",
    formation="Hell Creek",
    playable_dinos={
        "Tyrannosaurus": {"energy_threshold": 0, "growth_stages": 4},
        "Dakotaraptor": {"energy_threshold": 0, "growth_stages": 3},
    },
    terrains={
        "badlands": Terrain("badlands", {"small_prey": 0.3, "large_prey": 0.7}, abundance=0.15),
        "floodplain": Terrain("floodplain", {"small_prey": 0.4, "large_prey": 0.6}, abundance=0.25),
        "woodlands": Terrain("woodlands", {"small_prey": 0.6, "large_prey": 0.4}, abundance=0.15),
        "forest": Terrain("forest", {"small_prey": 0.7, "large_prey": 0.3}, abundance=0.25),
        "swamp": Terrain("swamp", {"small_prey": 0.7, "large_prey": 0.3}, abundance=0.05),
        "lake": Terrain("lake", {"small_prey": 0.9, "large_prey": 0.1}, abundance=0.15),
    },
)
