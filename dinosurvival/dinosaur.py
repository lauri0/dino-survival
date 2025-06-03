from dataclasses import dataclass

@dataclass
class DinosaurStats:
    name: str
    hunger: int
    hunger_threshold: int
    growth_stages: int

    def is_starving(self) -> bool:
        return self.hunger >= self.hunger_threshold

    def grow(self):
        if self.growth_stages > 0:
            self.growth_stages -= 1
            self.hunger = 0
