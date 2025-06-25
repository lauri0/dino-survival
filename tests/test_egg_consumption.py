import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.map import EggCluster
from dinosurvival.settings import MORRISON


def test_npc_egg_cluster_removed_after_eating():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.map.eggs = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Ceratosaurus", sex=None, energy=50.0, weight=10.0)
    game.map.animals[0][0] = [npc]
    game.map.eggs[0][0] = [EggCluster(species="Stegosaurus", number=1, weight=1.0, turns_until_hatch=5)]
    game._update_npcs()
    assert not game.map.eggs[0][0]
    assert npc.egg_clusters_eaten == 1


def test_player_collects_eggs_removes_cluster():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.map.eggs = [[[] for _ in range(6)] for _ in range(6)]
    game.map.eggs[game.y][game.x] = [EggCluster(species="Stegosaurus", number=2, weight=2.0, turns_until_hatch=5)]
    game.collect_eggs()
    assert not game.map.eggs[game.y][game.x]
