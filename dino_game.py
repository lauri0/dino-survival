import tkinter as tk
from dinosurvival.game import Game
from dinosurvival.settings import MORRISON, HELL_CREEK

SETTINGS = {
    "morrison": MORRISON,
    "hell_creek": HELL_CREEK,
}


def choose_dinosaur(setting) -> str:
    """Prompt the user to pick a dinosaur for the chosen setting."""
    print("Choose a dinosaur:")
    for idx, d in enumerate(setting.playable_dinos.keys(), start=1):
        print(f"{idx}. {d}")
    didx = int(input("> ")) - 1
    return list(setting.playable_dinos.keys())[didx]


def run_game(setting):
    """Run the text-based portion of the game for the given setting."""
    dino_name = choose_dinosaur(setting)
    game = Game(setting, dino_name)
    print("Type commands: north, south, east, west, hunt, quit")
    while True:
        action = input("> ").strip()
        if action == "quit":
            break
        result = game.turn(action)
        print(result)
        if "Game Over" in result:
            break


def launch_menu():
    """Display the opening menu as a full-screen window."""
    root = tk.Tk()
    root.title("Dinosaur Survival")
    root.attributes("-fullscreen", True)

    frame = tk.Frame(root)
    frame.pack(expand=True)

    tk.Label(frame, text="Dinosaur Survival", font=("Helvetica", 24)).pack(pady=20)

    tk.Button(frame, text="Morrison", width=20, height=2,
              command=lambda: (root.destroy(), run_game(MORRISON))).pack(pady=10)
    tk.Button(frame, text="Hell Creek", width=20, height=2,
              command=lambda: (root.destroy(), run_game(HELL_CREEK))).pack(pady=10)
    tk.Button(frame, text="Quit", width=20, height=2, command=root.destroy).pack(pady=10)

    root.mainloop()


def main():
    launch_menu()


if __name__ == "__main__":
    main()