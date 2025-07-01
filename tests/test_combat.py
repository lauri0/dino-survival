import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON

def test_player_attack_damage():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=1.0)
    game.map.animals[game.y][game.x] = [npc]
    player_hp_before = game.player.health
    game.hunt_npc(npc.id)
    assert game.player.health < player_hp_before
    assert npc.health < 100

def test_npc_target_selection():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    predator = NPCAnimal(id=1, name="Allosaurus", sex=None, weight=3000.0)
    strong = NPCAnimal(id=2, name="Allosaurus", sex=None, weight=3000.0)
    game.map.animals[0][0] = [predator, strong]
    game._update_npcs()
    # predator should not attack equal strength target
    assert predator.health == 100


def test_damage_after_armor_function():
    dmg = game_mod.damage_after_armor(
        100,
        {"armor_penetration": 20},
        {"armor": 40},
    )
    assert dmg == 80


def test_armor_reduces_damage_in_hunt():
    random.seed(0)
    game1 = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game1.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc1 = NPCAnimal(id=1, name="Dryosaurus", sex=None, weight=80.0)
    game1.map.animals[game1.y][game1.x] = [npc1]
    game1.hunt_npc(npc1.id)
    damage_unarm = 100 - npc1.health

    random.seed(0)
    game2 = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game2.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc2 = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=80.0)
    game2.map.animals[game2.y][game2.x] = [npc2]
    game2.hunt_npc(npc2.id)
    damage_arm = 100 - npc2.health

    assert damage_arm < damage_unarm

