import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK, MORRISON


def test_player_bleeds_target():
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    target = NPCAnimal(id=1, name="Acheroraptor", sex=None, weight=100.0)
    game.map.animals[game.y][game.x] = [target]
    game.player.weight = game.player.adult_weight
    pct = game.player.weight / game.player.adult_weight
    game.player.attack = 1.0
    game.player.hp = game.player.adult_hp
    game.player.speed = 1000.0
    game.hunt_npc(target.id)
    assert target.bleeding == 4


def test_npc_bleeds_player():
    game = game_mod.Game(MORRISON, "Ceratosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Allosaurus", sex=None, weight=100.0, abilities=["bleed"])
    game.map.animals[game.y][game.x] = [npc]
    game.player.weight = game.player.adult_weight
    pct = game.player.weight / game.player.adult_weight
    game.player.attack = game.player.adult_attack * pct
    game.player.hp = game.player.adult_hp
    game.player.speed = 1000.0
    game.hunt_npc(npc.id)
    assert game.player.bleeding == 4
