import random
import dinosurvival.game as game_mod
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.settings import MORRISON

def test_player_attack_damage():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    npc = NPCAnimal(id=1, name="Stegosaurus", sex=None, weight=1.0)
    game.map.animals[game.y][game.x] = [npc]
    player_hp_before = game.player.health
    game.hunt_npc(npc.id)
    assert game.player.health < player_hp_before
    assert npc.health < 100

def test_npc_target_selection():
    random.seed(0)
    game = game_mod.Game(MORRISON, "Allosaurus", width=6, height=6)
    game.map.animals = [[[] for _ in range(6)] for _ in range(6)]
    predator = NPCAnimal(id=1, name="Allosaurus", sex=None, weight=3000.0)
    strong = NPCAnimal(id=2, name="Allosaurus", sex=None, weight=3000.0)
    game.map.animals[0][0] = [predator, strong]
    game._update_npcs()
    # predator should not attack equal strength target
    assert predator.health == 100

