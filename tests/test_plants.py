import os, sys
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import random
from dinosurvival.plant import PlantStats, Plant
from dinosurvival.dinosaur import NPCAnimal
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


def test_max_two_plants_per_tile(monkeypatch):
    custom_stats = {
        "TP": PlantStats(
            name="TP",
            formations=["Morrison"],
            image="",
            weight=1.0,
            growth_chance={terrain: 1.0 for terrain in MORRISON.terrains},
        )
    }
    monkeypatch.setattr(game_mod, "PLANT_STATS", custom_stats)
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    for _ in range(5):
        game.turn("stay")
    assert len(game.map.plants[0][0]) <= 2


def test_npc_state_and_feeding(monkeypatch):
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    # clear existing
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.map.plants = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Stegosaurus", sex=None, energy=50.0, weight=10.0)
    game.map.animals[0][0] = [npc]
    game.map.plants[0][0] = [Plant(name="Ferns", weight=20.0)]
    game._update_npcs()
    assert npc.energy > 50.0
    assert npc.weight > 10.0
    assert game.map.plants[0][0][0].weight < 20.0


def test_npc_fruit_feeding(monkeypatch):
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.map.plants = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=2, name="Nanosaurus", sex=None, energy=50.0, weight=5.0)
    game.map.animals[0][0] = [npc]
    game.map.plants[0][0] = [Plant(name="Fruits", weight=20.0)]
    game._update_npcs()
    assert npc.energy > 50.0
    assert npc.weight > 5.0
    assert game.map.plants[0][0][0].weight < 20.0


def test_npc_initial_state():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    for row in game.map.animals:
        for cell in row:
            for npc in cell:
                assert npc.energy == 100.0
                assert npc.health == 100.0
                stats = game_mod.DINO_STATS[npc.name]
                if stats.get("can_be_juvenile", True):
                    assert 3.0 <= npc.weight <= stats.get("adult_weight", 0.0)
                else:
                    assert npc.weight == stats.get("adult_weight", 0.0)

