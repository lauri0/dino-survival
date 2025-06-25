import dinosurvival.game as game_mod
from dinosurvival.settings import MORRISON


def test_effective_fierceness_uses_health():
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.player.weight = game.player.adult_weight
    game.player.fierceness = game.player.adult_fierceness
    game.player.health = 50.0
    assert game.effective_fierceness() == game.player.adult_fierceness * 0.5


def test_aggressive_attack_uses_health():
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    npc = game_mod.NPCAnimal(id=1, name="Allosaurus", sex=None, weight=3000.0, health=0.1)
    game.current_encounters = [game_mod.EncounterEntry(npc=npc)]
    assert game._aggressive_attack_check() is None

