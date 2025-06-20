from dataclasses import dataclass, field
from enum import Enum


class Diet(Enum):
    MEAT = "meat"
    INSECTS = "insects"
    FERNS = "ferns"
    CYCADS = "cycads"
    CONIFERS = "conifers"

@dataclass
class DinosaurStats:
    name: str
    growth_stages: int
    hatchling_weight: float = 0.0
    adult_weight: float = 0.0
    hatchling_fierceness: float = 0.0
    adult_fierceness: float = 0.0
    hatchling_speed: float = 0.0
    adult_speed: float = 0.0
    hatchling_energy_drain: float = 0.0
    adult_energy_drain: float = 0.0
    growth_rate: float = 0.35
    walking_energy_drain_multiplier: float = 1.0
    carcass_food_value_modifier: float = 1.0
    fierceness: float = 0.0
    speed: float = 0.0
    health: float = 100.0
    energy: float = 100.0
    weight: float = 0.0
    health_regen: float = 0.0
    hydration: float = 100.0
    hydration_drain: float = 0.0
    aquatic_boost: float = 0.0
    forms_packs: bool = False
    mated: bool = False
    diet: list[Diet] = field(default_factory=list)

    def is_exhausted(self) -> bool:
        return self.energy <= 0

    def is_dehydrated(self) -> bool:
        return self.hydration <= 0


@dataclass
class NPCAnimal:
    """State for a non-player animal present on the map."""

    name: str
    juvenile: bool
    sex: str | None
    energy: float = 100.0
    health: float = 100.0
    weight: float = 0.0


