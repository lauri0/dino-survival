import random
import dinosurvival.game as game_mod
from dinosurvival.settings import MORRISON


def test_volcano_tile_remains_after_eruption():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    for j in range(game.map.height):
        for i in range(game.map.width):
            game.map.grid[j][i] = MORRISON.terrains["plains"]
            game.map.lava_info[j][i] = None
            game.map.erupting[j][i] = False
    game.map.grid[3][3] = MORRISON.terrains["volcano"]

    game.map.start_volcano_eruption(3, 3, "medium")
    assert game.map.grid[3][3].name == "volcano_erupting"

    for _ in range(3):
        game.map.update_lava()

    assert game.map.grid[3][3].name == "volcano"
