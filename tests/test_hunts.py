import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON, HELL_CREEK


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


def test_player_takes_damage_when_hunting_critter():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Tyrannosaurus", width=6, height=6)
    game.player.health_regen = 0
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    critter = NPCAnimal(id=1, name="Didelphodon", sex=None, weight=5.0)
    game.map.animals[game.y][game.x] = [critter]
    game.hunt_npc(critter.id)
    assert game.player.hp < game.player.max_hp


def test_npc_takes_damage_when_hunting_critter():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Tyrannosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    predator = NPCAnimal(id=1, name="Acheroraptor", sex=None, weight=10.0, energy=50.0)
    prey = NPCAnimal(id=2, name="Didelphodon", sex=None, weight=5.0)
    game.map.animals[0][0] = [predator, prey]
    game._update_npcs()
    assert predator.hp < predator.max_hp
