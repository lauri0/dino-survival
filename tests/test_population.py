import random
import dinosurvival.game as game_mod
from dinosurvival.settings import HELL_CREEK


def test_hell_creek_population_scaling():
    random.seed(0)
    game = game_mod.Game(HELL_CREEK, "Tyrannosaurus", width=6, height=6)
    counts = {}
    for row in game.map.animals:
        for cell in row:
            for npc in cell:
                counts[npc.name] = counts.get(npc.name, 0) + 1
    multipliers = {
        name: stats.get("initial_spawn_multiplier", 0)
        for name, stats in game_mod.DINO_STATS.items()
    }
    total = sum(counts.get(name, 0) for name in multipliers)
    assert total == 100
    assert any(counts.get(name, 0) != multipliers[name] for name in multipliers)
