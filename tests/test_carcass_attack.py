import os, sys, random
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def test_carcass_cannot_attack():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    carcass = NPCAnimal(id=1, name="Allosaurus", sex=None, alive=False, weight=100.0)
    game.map.animals[game.y][game.x] = [carcass]
    game._generate_encounters()
    random.seed(1)
    attack = game._aggressive_attack_check()
    assert attack is None

