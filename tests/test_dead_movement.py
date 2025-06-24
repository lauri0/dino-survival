import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON


def test_dead_npc_does_not_move():
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=10.0)
    game.map.animals[0][0] = [npc]
    npc.next_move = "Right"
    npc.alive = False
    game._move_npcs()
    assert npc in game.map.animals[0][0]
    assert npc.next_move == "None"
