import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.map import EggCluster
from dinosurvival.settings import MORRISON


def test_carnivore_skips_own_eggs():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.map.eggs = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Allosaurus", sex=None, energy=50.0, weight=10.0)
    game.map.animals[0][0] = [npc]
    game.map.eggs[0][0] = [EggCluster(species="Allosaurus", number=1, weight=1.0, turns_until_hatch=5)]
    game._update_npcs()
    assert game.map.eggs[0][0][0].weight == 1.0


def test_egg_weight_equals_hatchling_weight_times_number():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    stats = game_mod.DINO_STATS["Allosaurus"]
    game.player.weight = stats.get("adult_weight", 0.0)
    game.player.energy = 100.0
    game.player.health = 100.0
    game.player.turns_until_lay_eggs = 0
    game.lay_eggs()
    egg = game.map.eggs[game.y][game.x][0]
    expected = stats.get("hatchling_weight", stats.get("adult_weight", 0.0) * 0.001) * stats.get("num_eggs", 0)
    assert egg.weight == expected
