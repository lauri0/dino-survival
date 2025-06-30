import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK


def test_player_pack_hunter_bonus():
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.player.weight = game.player.adult_weight
    pct = game.player.weight / game.player.adult_weight
    game.player.attack = game.player.adult_attack * pct
    base = game.player_effective_attack()
    assert "pack_hunter" in game.player.abilities
    assert base == game.player.adult_attack
    npc = NPCAnimal(id=1, name="Acheroraptor", sex=None, weight=10.0, abilities=["pack_hunter"])
    game.map.animals[game.y][game.x] = [npc]
    assert game.player_effective_attack() == base * 3
    game_mod.set_stats_for_formation("Morrison")


def test_npc_pack_hunter_bonus():
    game = game_mod.Game(HELL_CREEK, "Tyrannosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc1 = NPCAnimal(id=1, name="Acheroraptor", sex=None, weight=10.0, abilities=["pack_hunter"])
    stats = game_mod.DINO_STATS["Acheroraptor"]
    game.map.animals[0][0] = [npc1]
    f1 = game.npc_effective_attack(npc1, stats, 0, 0)
    npc2 = NPCAnimal(id=2, name="Acheroraptor", sex=None, weight=10.0, abilities=["pack_hunter"])
    game.map.animals[0][0].append(npc2)
    f2 = game.npc_effective_attack(npc1, stats, 0, 0)
    assert f2 == f1 * 3
    game_mod.set_stats_for_formation("Morrison")
