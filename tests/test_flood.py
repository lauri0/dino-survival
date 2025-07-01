import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def prepare_simple_map():
    game = game_mod.Game(MORRISON, "Allosaurus", width=5, height=5)
    for y in range(game.map.height):
        for x in range(game.map.width):
            game.map.grid[y][x] = MORRISON.terrains["plains"]
            game.map.flood_info[y][x] = None
    game.map.grid[2][2] = MORRISON.terrains["lake"]
    game.x, game.y = 0, 0
    return game


def test_flood_spread_and_revert():
    game = prepare_simple_map()
    npc = NPCAnimal(id=1, name="Test", sex=None, weight=10.0)
    game.map.animals[2][1] = [npc]
    game.map.active_flood = True
    game.map.flood_turn = 0
    game.map._initiate_flood(game.player, (game.x, game.y))
    assert game.map.active_flood
    assert game.map.grid[2][1].name == "plains_flooded"
    assert npc.hp == 50.0

    game.map.update_flood(game.player, (game.x, game.y))
    assert game.map.grid[2][0].name == "plains_flooded"

    game.map.update_flood(game.player, (game.x, game.y))
    assert game.map.active_flood

    game.map.update_flood(game.player, (game.x, game.y))
    assert not game.map.active_flood
    for y in range(game.map.height):
        for x in range(game.map.width):
            if (x, y) == (2, 2):
                continue
            assert game.map.grid[y][x].name == "plains"
