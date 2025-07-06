import configparser
import os
from dataclasses import dataclass


@dataclass
class Constants:
    """Configuration values loaded from ``constants.ini``."""

    walking_energy_drain_multiplier: float = 1.0
    hatchling_weight_divisor: int = 1000
    hatchling_speed_multiplier: int = 3
    hatchling_energy_drain_divisor: int = 2
    min_hatching_weight: float = 2.0
    descendants_to_win: int = 5

    @classmethod
    def load_from_file(cls, path: str | None = None) -> "Constants":
        """Create a ``Constants`` instance from an ini file."""
        if path is None:
            path = os.path.join(os.path.dirname(__file__), "constants.ini")
        parser = configparser.ConfigParser()
        parser.read(path)
        section = parser["DEFAULT"] if "DEFAULT" in parser else {}
        return cls(
            walking_energy_drain_multiplier=float(
                section.get(
                    "walking_energy_drain_multiplier",
                    cls.walking_energy_drain_multiplier,
                )
            ),
            hatchling_weight_divisor=int(
                section.get("hatchling_weight_divisor", cls.hatchling_weight_divisor)
            ),
            hatchling_speed_multiplier=int(
                section.get(
                    "hatchling_speed_multiplier",
                    cls.hatchling_speed_multiplier,
                )
            ),
            hatchling_energy_drain_divisor=int(
                section.get(
                    "hatchling_energy_drain_divisor",
                    cls.hatchling_energy_drain_divisor,
                )
            ),
            min_hatching_weight=float(
                section.get("min_hatching_weight", cls.min_hatching_weight)
            ),
            descendants_to_win=int(
                section.get("descendants_to_win", cls.descendants_to_win)
            ),
        )


DEFAULT_CONSTANTS = Constants.load_from_file()
