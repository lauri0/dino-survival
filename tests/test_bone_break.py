import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK


def test_player_breaks_bones_target():
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    target = NPCAnimal(id=1, name="Acheroraptor", sex=None, weight=40.0)
    game.map.animals[game.y][game.x] = [target]
    game.player.abilities.append("bone_break")
    game.player.weight = 60.0
    game.player.attack = 1.0
    game.player.hp = game.player.adult_hp
    game.player.speed = 1000.0
    game.hunt_npc(target.id)
    assert target.broken_bone == 9


def test_npc_breaks_player_bones():
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(
        id=1, name="Acheroraptor", sex=None, weight=60.0, abilities=["bone_break"]
    )
    game.map.animals[game.y][game.x] = [npc]
    game.player.weight = 40.0
    game.player.attack = 1.0
    game.player.hp = game.player.adult_hp
    game.player.speed = 1000.0
    game.hunt_npc(npc.id)
    assert game.player.broken_bone == 9


def test_broken_bone_halves_speed():
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.player.speed = 10.0
    game.player.broken_bone = 5
    assert game.player_effective_speed() == 5.0
