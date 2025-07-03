import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import HELL_CREEK, MORRISON


def test_bleeding_npc_does_not_move():
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=10.0)
    game.map.animals[0][0] = [npc]
    npc.next_move = "Right"
    npc.bleeding = 3
    game._move_npcs()
    assert npc in game.map.animals[0][0]
    assert npc.next_move == "None"


def test_player_moving_while_bleeding_takes_extra_damage():
    game = game_mod.Game(HELL_CREEK, "Acheroraptor", width=6, height=6)
    game.player.weight = game.player.adult_weight
    game.player.max_hp = game.player.adult_hp
    game.player.hp = game.player.max_hp
    game.player.bleeding = 1

    hp_before_move = game.player.hp
    game._apply_turn_costs(True)
    move_loss = hp_before_move - game.player.hp

    game.player.hp = hp_before_move
    game.player.bleeding = 1
    game._apply_turn_costs(False)
    stay_loss = hp_before_move - game.player.hp

    assert abs(move_loss - stay_loss * 2) < 1e-6
