import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK


def test_player_does_not_bleed_carcass():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    carcass = NPCAnimal(id=1, name="Triceratops", sex=None, alive=False, weight=100.0, hp=100.0, max_hp=100.0)
    game.map.animals[game.y][game.x] = [carcass]
    game.player.weight = game.player.adult_weight
    game.player.attack = game.player.adult_attack
    game.player.hp = game.player.adult_hp
    game.player.speed = 1000.0
    game.hunt_npc(carcass.id)
    assert carcass.bleeding == 0
