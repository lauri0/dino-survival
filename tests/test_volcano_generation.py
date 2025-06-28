import random
import dinosurvival.game as game_mod
from dinosurvival.settings import MORRISON


def test_volcano_generation_rate():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=40, height=40)
    volcano = 0
    mountain = 0
    for row in game.map.grid:
        for t in row:
            if t.name == "volcano":
                volcano += 1
            elif t.name == "mountain":
                mountain += 1
    ratio = volcano / (volcano + mountain)
    assert 0.4 <= ratio <= 0.6
