import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def test_npc_starves_and_carcass_decays():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Allosaurus", sex=None, energy=1.0, weight=10.0)
    game.map.animals[0][0] = [npc]

    game._update_npcs()
    assert not npc.alive
    assert npc.energy == 0.0

    before = npc.weight
    game._spoil_carcasses()
    assert npc.weight < before
