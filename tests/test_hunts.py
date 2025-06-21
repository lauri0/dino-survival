import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def test_live_hunt_counts_kill():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    # Clear animals
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=1.0)
    game.map.animals[game.y][game.x] = [npc]
    game.hunt_npc(npc.id)
    assert game.hunt_stats.get("Stegosaurus", [0, 0])[1] == 1


def test_carcass_eat_not_counted_as_hunt():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    carcass = NPCAnimal(id=1, name="Stegosaurus", sex=None, alive=False, weight=1.0)
    game.map.animals[game.y][game.x] = [carcass]
    game.hunt_npc(carcass.id)
    assert "Stegosaurus" not in game.hunt_stats
