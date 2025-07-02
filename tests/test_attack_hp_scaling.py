import os, sys
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def test_player_attack_scales_with_hp():
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    game.player.weight = game.player.adult_weight
    pct = game.player.weight / game.player.adult_weight
    game.player.attack = game.player.adult_attack * pct
    game.player.hp = game.player.max_hp / 2
    assert game.player_effective_attack() == game.player.adult_attack * 0.5


def test_npc_attack_scales_with_hp():
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=1.0, hp=50.0, max_hp=100.0)
    stats = game_mod.DINO_STATS["Stegosaurus"]
    game.map.animals[0][0] = [npc]
    expected = game._scale_by_weight(npc.weight, stats, "attack") * 0.5
    assert game.npc_effective_attack(npc, stats, 0, 0) == expected
