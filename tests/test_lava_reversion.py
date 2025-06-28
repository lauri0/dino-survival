import random
import dinosurvival.game as game_mod
from dinosurvival.settings import MORRISON


def test_solidified_lava_reverts_to_original_tile():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    for j in range(game.map.height):
        for i in range(game.map.width):
            game.map.grid[j][i] = MORRISON.terrains["plains"]
            game.map.lava_info[j][i] = None
            game.map.erupting[j][i] = False
            game.map.lava_orig[j][i] = None
            game.map.solidified_turns[j][i] = 0
    game.map.grid[3][3] = MORRISON.terrains["volcano"]

    game.map.start_volcano_eruption(3, 3, "medium")
    for _ in range(3):
        game.map.update_lava()

    x, y = 3, 2  # north of the volcano
    assert game.map.grid[y][x].name == "solidified_lava_field"

    for _ in range(100):
        game.map.update_solidified_lava()

    assert game.map.grid[y][x].name == "plains"
