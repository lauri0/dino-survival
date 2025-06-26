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
    height_levels: Dict[str, float]
    humidity_levels: Dict[str, float]
    num_burrows: int = 0


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
        "badlands": Terrain("badlands", {"small_prey": 0.3, "large_prey": 0.7}),
        "plains": Terrain("plains", {"small_prey": 0.8, "large_prey": 0.2}),
        "woodlands": Terrain("woodlands", {"small_prey": 0.6, "large_prey": 0.4}),
        "forest": Terrain("forest", {"small_prey": 0.6, "large_prey": 0.4}),
        "swamp": Terrain("swamp", {"small_prey": 0.7, "large_prey": 0.3}),
        "lake": Terrain("lake", {"small_prey": 0.9, "large_prey": 0.1}),
        "mountain": Terrain("mountain", {"small_prey": 0.4, "large_prey": 0.6}),
    },
    height_levels={"low": 0.3, "normal": 0.45, "mountain": 0.25},
    humidity_levels={"arid": 0.3, "normal": 0.4, "humid": 0.3},
    num_burrows=0,
)

HELL_CREEK = Setting(
    name="Hell Creek",
    formation="Hell Creek",
    playable_dinos={
        "Tyrannosaurus": {"energy_threshold": 0, "growth_stages": 4},
    },
    terrains={
        "badlands": Terrain("badlands", {"small_prey": 0.3, "large_prey": 0.7}),
        "plains": Terrain("plains", {"small_prey": 0.4, "large_prey": 0.6}),
        "woodlands": Terrain("woodlands", {"small_prey": 0.6, "large_prey": 0.4}),
        "forest": Terrain("forest", {"small_prey": 0.7, "large_prey": 0.3}),
        "swamp": Terrain("swamp", {"small_prey": 0.7, "large_prey": 0.3}),
        "lake": Terrain("lake", {"small_prey": 0.9, "large_prey": 0.1}),
        "mountain": Terrain("mountain", {"small_prey": 0.4, "large_prey": 0.6}),
    },
    height_levels={"low": 0.3, "normal": 0.6, "mountain": 0.1},
    humidity_levels={"arid": 0.2, "normal": 0.5, "humid": 0.3},
    num_burrows=5,
)
