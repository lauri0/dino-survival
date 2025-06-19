from dataclasses import dataclass
from typing import Dict, List


@dataclass
class PlantStats:
    name: str
    formations: List[str]
    image: str
    weight: float
    growth_chance: Dict[str, float]

