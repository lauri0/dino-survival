import random
import dinosurvival.game as game_mod
from dinosurvival.settings import MORRISON


def test_initial_critter_spawn_and_placement():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    for name, stats in game_mod.CRITTER_STATS.items():
        count = 0
        for y in range(game.map.height):
            for x in range(game.map.width):
                species_in_tile = [npc.name for npc in game.map.animals[y][x] if npc.name == name]
                if species_in_tile:
                    terrain = game.map.terrain_at(x, y).name
                    if stats.get("can_walk", True):
                        assert terrain != "lake"
                    else:
                        assert terrain == "lake"
                    # ensure only one per tile
                    assert len(species_in_tile) == 1
                    count += 1
        assert count <= stats.get("maximum_individuals", 0) // 2
