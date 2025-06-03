from dinosurvival.game import Game
from dinosurvival.settings import MORRISON, HELL_CREEK

SETTINGS = {
    "morrison": MORRISON,
    "hell_creek": HELL_CREEK,
}


def choose_setting() -> Game:
    print("Choose a setting:")
    for idx, key in enumerate(SETTINGS.keys(), start=1):
        print(f"{idx}. {key}")
    sidx = int(input("> ")) - 1
    setting_key = list(SETTINGS.keys())[sidx]
    setting = SETTINGS[setting_key]

    print("Choose a dinosaur:")
    for idx, d in enumerate(setting.playable_dinos.keys(), start=1):
        print(f"{idx}. {d}")
    didx = int(input("> ")) - 1
    dino_name = list(setting.playable_dinos.keys())[didx]

    return Game(setting, dino_name)


def main():
    game = choose_setting()
    print("Type commands: north, south, east, west, hunt, quit")
    while True:
        action = input("> ").strip()
        if action == "quit":
            break
        result = game.turn(action)
        print(result)
        if "Game Over" in result:
            break


if __name__ == "__main__":
    main()
