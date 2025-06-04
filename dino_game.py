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


def run_game_gui(setting, dinosaur_name: str) -> None:
    """Run the game using a graphical interface."""
    game = Game(setting, dinosaur_name)

    root = tk.Tk()
    root.title("Dinosaur Survival")

    main = tk.Frame(root)
    main.pack(fill="both", expand=True)

    main.grid_rowconfigure(0, weight=1)
    main.grid_rowconfigure(1, weight=0)
    main.grid_rowconfigure(2, weight=1)
    main.grid_columnconfigure(0, weight=0)
    main.grid_columnconfigure(1, weight=1)

    # Top-left controls and biome
    control_frame = tk.Frame(main)
    control_frame.grid(row=0, column=0, sticky="nsew", padx=10, pady=10)

    biome_var = tk.StringVar()
    biome_label = tk.Label(control_frame, textvariable=biome_var, font=("Helvetica", 16))
    biome_label.pack(pady=(0, 10))

    def update_biome() -> None:
        terrain = game.map.terrain_at(game.x, game.y)
        biome_var.set(f"Biome: {terrain.name}")

    # Movement buttons
    btn_frame = tk.Frame(control_frame)
    btn_frame.pack()

    def perform(action: str) -> None:
        if action == "stay":
            result = "Stayed put"
        else:
            result = game.turn(action)
        append_output(result)
        update_biome()
        update_map()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")

    move_buttons = {}
    move_buttons["north"] = tk.Button(btn_frame, text="North", width=8, command=lambda: perform("north"))
    move_buttons["south"] = tk.Button(btn_frame, text="South", width=8, command=lambda: perform("south"))
    move_buttons["east"] = tk.Button(btn_frame, text="East", width=8, command=lambda: perform("east"))
    move_buttons["west"] = tk.Button(btn_frame, text="West", width=8, command=lambda: perform("west"))
    move_buttons["stay"] = tk.Button(btn_frame, text="Stay", width=8, command=lambda: perform("stay"))

    move_buttons["north"].grid(row=0, column=1)
    move_buttons["west"].grid(row=1, column=0)
    move_buttons["stay"].grid(row=1, column=1)
    move_buttons["east"].grid(row=1, column=2)
    move_buttons["south"].grid(row=2, column=1)

    # Top-right map
    map_frame = tk.Frame(main)
    map_frame.grid(row=0, column=1, sticky="nsew", padx=10, pady=10)

    map_tiles = []
    tile_size = 20
    for y in range(game.map.height):
        row = []
        for x in range(game.map.width):
            c = tk.Canvas(
                map_frame,
                width=tile_size,
                height=tile_size,
                highlightthickness=0,
                bd=0,
            )
            c.grid(row=y, column=x, padx=1, pady=1)
            row.append(c)
        map_tiles.append(row)

    def update_map() -> None:
        color_map = {
            "forest": "green",
            "plains": "yellowgreen",
            "floodplain": "tan",
        }
        for y, r in enumerate(map_tiles):
            for x, canvas in enumerate(r):
                terrain = game.map.terrain_at(x, y)
                revealed = game.map.is_revealed(x, y)
                color = color_map.get(terrain.name, "white") if revealed else "gray"
                canvas.delete("all")
                canvas.create_rectangle(
                    0,
                    0,
                    tile_size,
                    tile_size,
                    fill=color,
                    outline="black",
                )
                if (x, y) == (game.x, game.y):
                    canvas.create_rectangle(
                        2,
                        2,
                        tile_size - 2,
                        tile_size - 2,
                        outline="black",
                        width=2,
                    )

    # Bottom-right stats
    stats_frame = tk.Frame(main)
    stats_frame.grid(row=1, column=1, sticky="nsew", padx=10, pady=10)

    tk.Label(stats_frame, text=f"Dinosaur: {dinosaur_name}", font=("Helvetica", 16)).pack()
    tk.Button(stats_frame, text="Quit", width=10, command=root.destroy).pack(pady=10)

    # Bottom text output
    text_frame = tk.Frame(main)
    text_frame.grid(row=2, column=0, columnspan=2, sticky="nsew")
    text_frame.grid_rowconfigure(0, weight=1)
    text_frame.grid_columnconfigure(0, weight=1)

    output = tk.Text(text_frame, height=10, state="disabled")
    output.grid(row=0, column=0, sticky="nsew")
    scrollbar = tk.Scrollbar(text_frame, command=output.yview)
    scrollbar.grid(row=0, column=1, sticky="ns")
    output.configure(yscrollcommand=scrollbar.set)

    def append_output(text: str) -> None:
        output.configure(state="normal")
        output.insert(tk.END, text + "\n")
        output.see(tk.END)
        output.configure(state="disabled")

    update_biome()
    update_map()

    root.mainloop()


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
        run_game_gui(selection["setting"], selection["dino"])


def main():
    launch_menu()


if __name__ == "__main__":
    main()
