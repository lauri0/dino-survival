import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK


def _spawn_critter(game, name):
    stats = game_mod.CRITTER_STATS[name]
    weight = stats.get("adult_weight", 0.0)
    max_hp = game._scale_by_weight(weight, stats, "hp")
    critter = NPCAnimal(id=1, name=name, sex=None, weight=weight, max_hp=max_hp, hp=max_hp)
    game.map.animals[game.y][game.x] = [critter]
    return critter


def test_critter_bleed_damage_and_expires():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    critter = _spawn_critter(game, "Didelphodon")
    critter.bleeding = 5
    hp_before = critter.hp
    for _ in range(5):
        game._update_npcs()
    assert critter.hp < hp_before
    assert critter.bleeding == 0


def test_critter_health_regen():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    critter = _spawn_critter(game, "Didelphodon")
    critter.hp -= 0.2
    hp_before = critter.hp
    regen = game_mod.CRITTER_STATS["Didelphodon"].get("health_regen", 0.0)
    game._update_npcs()
    expected = min(critter.max_hp, hp_before + critter.max_hp * regen / 100.0)
    assert critter.hp == expected
