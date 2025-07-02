import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK


def test_bleed_damage_and_expires():
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    stats = game_mod.DINO_STATS["Acheroraptor"]
    weight = stats["adult_weight"]
    max_hp = game._scale_by_weight(weight, stats, "hp")
    target = NPCAnimal(id=1, name="Acheroraptor", sex=None, weight=weight, max_hp=max_hp, hp=max_hp)
    game.map.animals[game.y][game.x] = [target]

    # Apply bleeding manually
    target.bleeding = 5
    hp_before = target.hp
    for _ in range(5):
        game._update_npcs()
    assert target.hp < hp_before
    assert target.bleeding == 0
