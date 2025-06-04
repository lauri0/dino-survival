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


def choose_dinosaur_gui(root: tk.Tk, setting, on_select) -> None:
    """Display a dinosaur selection menu inside an existing root window."""

    frame = tk.Frame(root)
    frame.pack(expand=True)

    tk.Label(
        frame,
        text=f"Select a Dinosaur ({setting.name})",
        font=("Helvetica", 24),
    ).pack(pady=20)

    def choose(d):
        on_select(d)
        root.destroy()

    for dino in setting.playable_dinos.keys():
        tk.Button(
            frame,
            text=dino,
            width=20,
            height=2,
            command=lambda d=dino: choose(d),
        ).pack(pady=10)

    tk.Button(frame, text="Quit", width=20, height=2, command=root.destroy).pack(pady=10)

    # This function does not start a new main loop; the provided root must already
    # be running. The window will be destroyed when a dinosaur is chosen.


def run_game(setting, dinosaur_name: str | None = None):
    """Run the text-based portion of the game for the given setting."""
    if dinosaur_name is None:
        dino_name = choose_dinosaur(setting)
    else:
        dino_name = dinosaur_name
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

    selection: dict[str, str | None] = {"setting": None, "dino": None}

    def show_start_menu():
        frame = tk.Frame(root)
        frame.pack(expand=True)

        tk.Label(frame, text="Dinosaur Survival", font=("Helvetica", 24)).pack(pady=20)

        tk.Button(
            frame,
            text="Morrison",
            width=20,
            height=2,
            command=lambda: show_dino_menu(MORRISON, frame),
        ).pack(pady=10)
        tk.Button(
            frame,
            text="Hell Creek",
            width=20,
            height=2,
            command=lambda: show_dino_menu(HELL_CREEK, frame),
        ).pack(pady=10)
        tk.Button(frame, text="Quit", width=20, height=2, command=root.destroy).pack(pady=10)

    def show_dino_menu(setting, prev_frame):
        prev_frame.destroy()

        def on_select(dino):
            selection["setting"] = setting
            selection["dino"] = dino

        choose_dinosaur_gui(root, setting, on_select)

    show_start_menu()
    root.mainloop()

    if selection["setting"] and selection["dino"]:
        run_game(selection["setting"], selection["dino"])


def main():
    launch_menu()


if __name__ == "__main__":
    main()