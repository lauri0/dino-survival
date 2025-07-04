from dataclasses import dataclass, field
from enum import Enum


class Diet(Enum):
    MEAT = "meat"
    INSECTS = "insects"
    FERNS = "ferns"
    CYCADS = "cycads"
    CONIFERS = "conifers"
    FRUITS = "fruits"


@dataclass
class DinosaurStats:
    name: str
    growth_stages: int
    hatchling_weight: float = 0.0
    adult_weight: float = 0.0
    hatchling_attack: float = 0.0
    adult_attack: float = 0.0
    hatchling_hp: float = 0.0
    adult_hp: float = 0.0
    hatchling_speed: float = 0.0
    adult_speed: float = 0.0
    hatchling_energy_drain: float = 0.0
    adult_energy_drain: float = 0.0
    growth_rate: float = 0.35
    walking_energy_drain_multiplier: float = 1.0
    attack: float = 0.0
    max_hp: float = 100.0
    hp: float = 100.0
    speed: float = 0.0
    energy: float = 100.0
    weight: float = 0.0
    health_regen: float = 0.0
    hydration: float = 100.0
    hydration_drain: float = 0.0
    aquatic_boost: float = 0.0
    mated: bool = False
    turns_until_lay_eggs: int = 0
    diet: list[Diet] = field(default_factory=list)
    abilities: list[str] = field(default_factory=list)
    ambush_streak: int = 0
    bleeding: int = 0
    broken_bone: int = 0

    def is_exhausted(self) -> bool:
        return self.energy <= 0

    def is_dehydrated(self) -> bool:
        return self.hydration <= 0


@dataclass
class NPCAnimal:
    """State for a non-player animal present on the map."""

    id: int
    name: str
    sex: str | None
    weight: float = 0.0
    age: int = 0
    energy: float = 100.0
    max_hp: float = 100.0
    hp: float = 100.0
    alive: bool = True
    attack: float = 0.0
    speed: float = 0.0
    next_move: str = "None"
    turns_until_lay_eggs: int = 0
    hunts: dict[str, int] = field(default_factory=dict)
    egg_clusters_eaten: int = 0
    is_descendant: bool = False
    abilities: list[str] = field(default_factory=list)
    ambush_streak: int = 0
    last_action: str = "None"
    bleeding: int = 0
    broken_bone: int = 0
    bleed_wait_target: int = -1
    bleed_wait_turns: int = 0
