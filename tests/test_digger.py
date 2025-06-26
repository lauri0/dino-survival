import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK


def test_pectinodon_dig_burrow_one_turn():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Pectinodon", width=6, height=6)
    game.map.spawn_burrow(game.x, game.y, full=True)
    burrow = game.map.get_burrow(game.x, game.y)
    assert burrow is not None and burrow.full
    game.dig_burrow()
    burrow = game.map.get_burrow(game.x, game.y)
    assert burrow is not None and not burrow.full
    assert burrow.progress == 0.0
    found = any(npc.name == "Didelphodon" for npc in game.map.animals[game.y][game.x])
    assert found


def test_npc_digger_digs_burrow():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Tyrannosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.map.spawn_burrow(0, 0, full=True)
    npc = NPCAnimal(id=1, name="Pectinodon", sex=None, energy=50.0, weight=10.0, abilities=["digger"])
    game.map.animals[0][0] = [npc]
    game._update_npcs()
    burrow = game.map.get_burrow(0, 0)
    assert burrow is not None and not burrow.full
    found = any(n.name == "Didelphodon" for n in game.map.animals[0][0] if n is not npc)
    assert found


def test_hell_creek_playable_list():
    assert "Pectinodon" in HELL_CREEK.playable_dinos
    assert "Acheroraptor" in HELL_CREEK.playable_dinos
