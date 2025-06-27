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
    biome_images: Dict[str, str]
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
        "desert": Terrain("desert", {}),
        "toxic_badlands": Terrain("toxic_badlands", {}),
        "plains": Terrain("plains", {}),
        "woodlands": Terrain("woodlands", {}),
        "forest": Terrain("forest", {}),
        "swamp": Terrain("swamp", {}),
        "lake": Terrain("lake", {}),
        "mountain": Terrain("mountain", {}),
    },
    biome_images={
        "desert": "desert.png",
        "toxic_badlands": "badlands.png",
        "plains": "plains.png",
        "woodlands": "woodlands.png",
        "forest": "forest.png",
        "swamp": "swamp.png",
        "lake": "lake.png",
        "mountain": "mountain.png",
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
        "Acheroraptor": {"energy_threshold": 0, "growth_stages": 3},
        "Pectinodon": {"energy_threshold": 0, "growth_stages": 3},
    },
    terrains={
        "desert": Terrain("desert", {}),
        "toxic_badlands": Terrain("toxic_badlands", {}),
        "plains": Terrain("plains", {}),
        "woodlands": Terrain("woodlands", {}),
        "forest": Terrain("forest", {}),
        "swamp": Terrain("swamp", {}),
        "lake": Terrain("lake", {}),
        "mountain": Terrain("mountain", {}),
    },
    biome_images={
        "desert": "desert.png",
        "toxic_badlands": "badlands.png",
        "plains": "plains.png",
        "woodlands": "woodlands.png",
        "forest": "forest.png",
        "swamp": "swamp.png",
        "lake": "lake.png",
        "mountain": "mountain.png",
    },
    height_levels={"low": 0.3, "normal": 0.6, "mountain": 0.1},
    humidity_levels={"arid": 0.2, "normal": 0.5, "humid": 0.3},
    num_burrows=5,
)
