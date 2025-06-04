from dataclasses import dataclass

@dataclass
class DinosaurStats:
    name: str
    growth_stages: int
    growth_speed: float = 0.0
    hatchling_weight: float = 0.0
    adult_weight: float = 0.0
    hatchling_fierceness: float = 0.0
    adult_fierceness: float = 0.0
    hatchling_speed: float = 0.0
    adult_speed: float = 0.0
    hatchling_energy_drain: float = 0.0
    adult_energy_drain: float = 0.0
    walking_energy_drain_multiplier: float = 1.0
    carcass_food_value_modifier: float = 1.0
    fierceness: float = 0.0
    speed: float = 0.0
    health: float = 100.0
    energy: float = 100.0
    weight: float = 0.0
    health_regen: float = 0.0

    def is_exhausted(self) -> bool:
        return self.energy <= 0

    def grow(self):
        if self.growth_stages > 0:
            self.growth_stages -= 1
            self.energy = 100.0
            if self.growth_stages == 0:
                self.weight = self.adult_weight
                self.fierceness = self.adult_fierceness
                self.speed = self.adult_speed

