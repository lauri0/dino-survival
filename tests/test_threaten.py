import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def test_threaten_scatter():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.x = 3
    game.y = 3
    npc1 = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=1.0)
    npc2 = NPCAnimal(id=2, name="Stegosaurus", sex=None, weight=1.0)
    game.map.animals[3][3] = [npc1, npc2]
    base = game._base_energy_drain()
    game.player.energy = 100.0
    game.threaten()
    assert game.player.energy == 100.0 - base * 2
    assert npc1 not in game.map.animals[3][3]
    assert npc2 not in game.map.animals[3][3]


def test_threaten_killed_by_stronger():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.x = 2
    game.y = 2
    strong = NPCAnimal(id=1, name="Allosaurus", sex=None, weight=3000.0)
    game.map.animals[2][2] = [strong]
    game.threaten()
    assert game.player.hp == 0


