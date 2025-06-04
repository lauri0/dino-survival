from __future__ import annotations
from dataclasses import dataclass
from typing import Dict
from .map import Terrain

@dataclass
class Setting:
    name: str
    playable_dinos: Dict[str, Dict]
    terrains: Dict[str, Terrain]


MORRISON = Setting(
    name="Morrison Formation",
    playable_dinos={
        "Allosaurus": {"hunger_threshold": 5, "growth_stages": 3},
        "Ceratosaurus": {"hunger_threshold": 4, "growth_stages": 3},
        "Torvosaurus": {"hunger_threshold": 5, "growth_stages": 3},
        "Ornitholestes": {"hunger_threshold": 3, "growth_stages": 3},
    },
    terrains={
        "plains": Terrain("plains", {"small_prey": 0.8, "large_prey": 0.2}),
        "forest": Terrain("forest", {"small_prey": 0.6, "large_prey": 0.4}),
        "swamp": Terrain("swamp", {"small_prey": 0.7, "large_prey": 0.3}),
        "woodlands": Terrain("woodlands", {"small_prey": 0.6, "large_prey": 0.4}),
        "badlands": Terrain("badlands", {"small_prey": 0.3, "large_prey": 0.7}),
        "lake": Terrain("lake", {"small_prey": 0.9, "large_prey": 0.1}),
    },
)

HELL_CREEK = Setting(
    name="Hell Creek",
    playable_dinos={
        "Tyrannosaurus": {"hunger_threshold": 6, "growth_stages": 4},
        "Dakotaraptor": {"hunger_threshold": 4, "growth_stages": 3},
    },
    terrains={
        "floodplain": Terrain("floodplain", {"small_prey": 0.4, "large_prey": 0.6}),
        "forest": Terrain("forest", {"small_prey": 0.7, "large_prey": 0.3}),
        "swamp": Terrain("swamp", {"small_prey": 0.7, "large_prey": 0.3}),
        "woodlands": Terrain("woodlands", {"small_prey": 0.6, "large_prey": 0.4}),
        "badlands": Terrain("badlands", {"small_prey": 0.3, "large_prey": 0.7}),
        "lake": Terrain("lake", {"small_prey": 0.9, "large_prey": 0.1}),
    },
)
