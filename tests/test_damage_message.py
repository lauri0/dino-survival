import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK


def test_hunt_only_one_damage_message():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Tyrannosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    stats = game_mod.CRITTER_STATS["Didelphodon"]
    weight = stats["adult_weight"]
    hp = game._scale_by_weight(weight, stats, "hp")
    npc = NPCAnimal(id=1, name="Didelphodon", sex=None, weight=weight, max_hp=hp, hp=hp)
    game.map.animals[game.y][game.x] = [npc]
    game.hunt_npc(npc.id)
    count = sum(1 for m in game.turn_messages if m.startswith("You deal"))
    assert count == 1
