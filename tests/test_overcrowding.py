import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def test_overcrowded_laying_moves():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    # populate 4 NPCs to crowd the cell
    for i in range(4):
        npc = NPCAnimal(id=i, name="Stegosaurus", sex="F", weight=10.0)
        game.map.animals[0][0].append(npc)
    stats = game_mod.DINO_STATS["Stegosaurus"]
    ready = NPCAnimal(
        id=99,
        name="Stegosaurus",
        sex="F",
        weight=stats.get("adult_weight", 0.0),
        energy=100.0,
        hp=100.0,
        turns_until_lay_eggs=0,
    )
    game.map.animals[0][0].append(ready)
    random.seed(0)
    game._update_npcs()
    assert not game.map.eggs[0][0]
    assert ready.next_move != "None"
