import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.map import EggCluster, Burrow
from dinosurvival.plant import Plant
from dinosurvival.settings import MORRISON


def test_player_dies_on_lava_tile():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.grid[game.y][game.x] = MORRISON.terrains["lava"]
    game.turn_messages = []
    game._apply_terrain_effects()
    assert game.player.health == 0.0
    assert any("Game Over" in m for m in game.turn_messages)


def test_lava_clears_cell_contents():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    x, y = 1, 1
    game.map.grid[y][x] = MORRISON.terrains["volcano_erupting"]
    npc = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=10.0)
    game.map.animals[y][x] = [npc]
    game.map.eggs[y][x] = [EggCluster(species="Stegosaurus", number=1, weight=1.0, turns_until_hatch=5)]
    game.map.burrows[y][x] = Burrow(full=True)
    game.map.plants[y][x] = [Plant(name="Ferns", weight=5.0)]
    game._apply_terrain_effects()
    assert not npc.alive
    assert game.map.eggs[y][x] == []
    assert game.map.burrows[y][x] is None
    assert game.map.plants[y][x] == []

