import os, sys, random
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def test_carcass_removed_after_spoilage():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    # clear animals
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    carcass = NPCAnimal(id=1, name="Stegosaurus", sex=None, alive=False, weight=1.0)
    game.map.animals[game.y][game.x] = [carcass]
    game._generate_encounters()
    assert any(e.npc is carcass for e in game.current_encounters)
    game.turn("stay")
    assert all(e.npc is not carcass for e in game.current_encounters)
    assert carcass not in game.map.animals[game.y][game.x]


def test_zero_weight_removed_on_generate():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    carcass = NPCAnimal(id=2, name="Stegosaurus", sex=None, alive=False, weight=0.0)
    game.map.animals[game.y][game.x] = [carcass]
    game._generate_encounters()
    assert carcass not in game.map.animals[game.y][game.x]
    assert all(e.npc is not carcass for e in game.current_encounters)


def test_spoilage_occurs_after_turn():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    carcass = NPCAnimal(id=3, name="Lizard", sex=None, alive=False, weight=1.0)
    game.map.animals[game.y][game.x] = [carcass]
    game._update_npcs()
    assert carcass.weight == 1.0
    game.turn("stay")
    assert carcass not in game.map.animals[game.y][game.x]
