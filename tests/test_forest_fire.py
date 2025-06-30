import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def prepare_forest_map():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=5, height=5)
    for y in range(game.map.height):
        for x in range(game.map.width):
            game.map.grid[y][x] = MORRISON.terrains["forest"]
            game.map.fire_turns[y][x] = 0
            game.map.burnt_turns[y][x] = 0
    game.x, game.y = 0, 0
    return game


def test_forest_fire_reverts_to_original_tile():
    game = prepare_forest_map()
    x, y = 2, 2
    npc = NPCAnimal(id=1, name="Test", sex=None, weight=10.0)
    game.map.animals[y][x] = [npc]
    game.map.start_forest_fire(x, y)
    assert game.map.grid[y][x].name == "forest_fire"
    assert game.map.animals[y][x] == []

    weather = game_mod.Weather("Sunny", "", 0.0)
    for _ in range(5):
        game.map.update_forest_fire(weather)
    assert game.map.grid[y][x].name == "forest_burnt"

    for _ in range(50):
        game.map.update_forest_fire(weather)
    assert game.map.grid[y][x].name == "forest"
