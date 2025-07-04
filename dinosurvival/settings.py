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
        "desert_flooded": Terrain("desert_flooded", {}),
        "toxic_badlands": Terrain("toxic_badlands", {}),
        "plains": Terrain("plains", {}),
        "plains_flooded": Terrain("plains_flooded", {}),
        "woodlands": Terrain("woodlands", {}),
        "woodlands_flooded": Terrain("woodlands_flooded", {}),
        "forest": Terrain("forest", {}),
        "forest_flooded": Terrain("forest_flooded", {}),
        "forest_fire": Terrain("forest_fire", {}),
        "forest_burnt": Terrain("forest_burnt", {}),
        "highland_forest": Terrain("highland_forest", {}),
        "highland_forest_fire": Terrain("highland_forest_fire", {}),
        "highland_forest_burnt": Terrain("highland_forest_burnt", {}),
        "swamp": Terrain("swamp", {}),
        "swamp_flooded": Terrain("swamp_flooded", {}),
        "lake": Terrain("lake", {}),
        "mountain": Terrain("mountain", {}),
        "volcano": Terrain("volcano", {}),
        "volcano_erupting": Terrain("volcano_erupting", {}),
        "lava": Terrain("lava", {}),
        "solidified_lava_field": Terrain("solidified_lava_field", {}),
    },
    biome_images={
        "desert": "desert.png",
        "desert_flooded": "desert_flooded.png",
        "toxic_badlands": "badlands.png",
        "plains": "plains.png",
        "plains_flooded": "plains_flooded.png",
        "woodlands": "woodlands.png",
        "woodlands_flooded": "woodlands_flooded.png",
        "forest": "forest.png",
        "forest_flooded": "forest_flooded.png",
        "forest_fire": "forest_fire.png",
        "forest_burnt": "forest_burnt.png",
        "highland_forest": "highland_forest.png",
        "highland_forest_fire": "highland_forest_fire.png",
        "highland_forest_burnt": "highland_forest_burnt.png",
        "swamp": "swamp.png",
        "swamp_flooded": "swamp_flooded.png",
        "lake": "lake.png",
        "mountain": "mountain.png",
        "volcano": "volcano.png",
        "volcano_erupting": "volcano_erupting.png",
        "lava": "lava.png",
        "solidified_lava_field": "solidified_lava_field.png",
    },
    height_levels={"low": 0.3, "normal": 0.4, "hilly": 0.2, "mountain": 0.1},
    humidity_levels={"arid": 0.35, "normal": 0.4, "humid": 0.25},
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
        "desert_flooded": Terrain("desert_flooded", {}),
        "toxic_badlands": Terrain("toxic_badlands", {}),
        "plains": Terrain("plains", {}),
        "plains_flooded": Terrain("plains_flooded", {}),
        "woodlands": Terrain("woodlands", {}),
        "woodlands_flooded": Terrain("woodlands_flooded", {}),
        "forest": Terrain("forest", {}),
        "forest_flooded": Terrain("forest_flooded", {}),
        "forest_fire": Terrain("forest_fire", {}),
        "forest_burnt": Terrain("forest_burnt", {}),
        "highland_forest": Terrain("highland_forest", {}),
        "highland_forest_fire": Terrain("highland_forest_fire", {}),
        "highland_forest_burnt": Terrain("highland_forest_burnt", {}),
        "swamp": Terrain("swamp", {}),
        "swamp_flooded": Terrain("swamp_flooded", {}),
        "lake": Terrain("lake", {}),
        "mountain": Terrain("mountain", {}),
        "volcano": Terrain("volcano", {}),
        "volcano_erupting": Terrain("volcano_erupting", {}),
        "lava": Terrain("lava", {}),
        "solidified_lava_field": Terrain("solidified_lava_field", {}),
    },
    biome_images={
        "desert": "desert.png",
        "desert_flooded": "desert_flooded.png",
        "toxic_badlands": "badlands.png",
        "plains": "plains.png",
        "plains_flooded": "plains_flooded.png",
        "woodlands": "woodlands.png",
        "woodlands_flooded": "woodlands_flooded.png",
        "forest": "forest.png",
        "forest_flooded": "forest_flooded.png",
        "forest_fire": "forest_fire.png",
        "forest_burnt": "forest_burnt.png",
        "highland_forest": "highland_forest.png",
        "highland_forest_fire": "highland_forest_fire.png",
        "highland_forest_burnt": "highland_forest_burnt.png",
        "swamp": "swamp.png",
        "swamp_flooded": "swamp_flooded.png",
        "lake": "lake.png",
        "mountain": "mountain.png",
        "volcano": "volcano.png",
        "volcano_erupting": "volcano_erupting.png",
        "lava": "lava.png",
        "solidified_lava_field": "solidified_lava_field.png",
    },
    height_levels={"low": 0.3, "normal": 0.45, "hilly": 0.15, "mountain": 0.1},
    humidity_levels={"arid": 0.2, "normal": 0.5, "humid": 0.3},
    num_burrows=5,
)
