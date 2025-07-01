import random
import dinosurvival.game as game_mod
from dinosurvival.settings import MORRISON

SEED_ERUPT = 206  # ensures eruption and medium size


def prepare_game_for_volcano(x: int, y: int, player_pos: tuple[int, int]):
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    for j in range(game.map.height):
        for i in range(game.map.width):
            game.map.grid[j][i] = MORRISON.terrains["plains"]
            game.map.lava_info[j][i] = None
            game.map.erupting[j][i] = False
    game.map.grid[y][x] = MORRISON.terrains["volcano"]
    game.x, game.y = player_pos
    return game


def test_player_dies_on_volcano_eruption():
    game = prepare_game_for_volcano(3, 3, (3, 3))
    random.seed(SEED_ERUPT)
    result = game._start_turn()
    assert "Game Over" in result
    assert game.map.grid[3][3].name == "volcano_erupting"
    game._apply_terrain_effects()
    assert game.player.hp == 0.0


def test_player_dies_adjacent_volcano_eruption():
    game = prepare_game_for_volcano(3, 3, (3, 2))
    random.seed(SEED_ERUPT)
    result = game._start_turn()
    assert "Game Over" in result
    assert game.map.grid[2][3].name == "lava"
    assert game.map.grid[3][3].name == "volcano_erupting"
    game._apply_terrain_effects()
    assert game.player.hp == 0.0
