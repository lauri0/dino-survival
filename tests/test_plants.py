import os, sys
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import random
from dinosurvival.plant import PlantStats
from dinosurvival.settings import MORRISON
import dinosurvival.game as game_mod


def test_plant_stats_dataclass():
    for stats in game_mod.PLANT_STATS.values():
        assert isinstance(stats, PlantStats)


def test_plants_generated_and_encounters(monkeypatch):
    custom_stats = {
        "TestPlant": PlantStats(
            name="TestPlant",
            formations=["Morrison"],
            image="",
            weight=1.0,
            growth_chance={terrain: 1.0 for terrain in MORRISON.terrains}
        )
    }
    monkeypatch.setattr(game_mod, "PLANT_STATS", custom_stats)
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.turn("stay")
    assert len(game.current_plants) > 0
    assert game.current_plants[0].name == "TestPlant"

