import tkinter as tk
from tkinter import messagebox
import random
import os
from dinosurvival.game import Game, DINO_STATS
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
    print("Type commands: north, south, east, west, hunt, drink, quit")
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
    root.geometry("1200x800")
    root.minsize(1200, 800)

    # Preload biome images if available
    assets_dir = os.path.join(os.path.dirname(__file__), "assets", "biomes")
    biome_images: dict[str, tk.PhotoImage] = {}

    try:
        from PIL import Image, ImageTk  # type: ignore
    except Exception:
        Image = None  # type: ignore
        ImageTk = None  # type: ignore

    for tname in game.setting.terrains.keys():
        fname = f"{game.setting.formation.lower()}_{tname}.png"
        path = os.path.join(assets_dir, fname)
        if os.path.exists(path):
            if Image and ImageTk:
                img = Image.open(path)
                target_w, target_h = 400, 250
                scale = target_w / img.width
                # Use LANCZOS resampling if available
                resample = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
                resized = img.resize((target_w, int(img.height * scale)), resample)
                if resized.height > target_h:
                    top = int((resized.height - target_h) / 2)
                    resized = resized.crop((0, top, target_w, top + target_h))
                biome_images[tname] = ImageTk.PhotoImage(resized, master=root)
            else:
                biome_images[tname] = tk.PhotoImage(master=root, file=path)

    main = tk.Frame(root)
    main.pack(fill="both", expand=True)

    main.grid_rowconfigure(0, weight=1)
    main.grid_rowconfigure(1, weight=0)
    main.grid_rowconfigure(2, weight=1)
    main.grid_columnconfigure(0, weight=0, minsize=200)
    main.grid_columnconfigure(1, weight=0, minsize=200)
    main.grid_columnconfigure(2, weight=1)

    # Biome information on the left
    biome_frame = tk.Frame(main, width=200)
    biome_frame.grid(row=0, column=0, sticky="nsew", padx=10, pady=10)
    biome_frame.grid_propagate(False)

    biome_var = tk.StringVar()
    biome_label = tk.Label(biome_frame, textvariable=biome_var, font=("Helvetica", 16))
    biome_label.pack(pady=(0, 2))

    danger_var = tk.StringVar()
    danger_label = tk.Label(biome_frame, textvariable=danger_var, font=("Helvetica", 14))
    danger_label.pack(pady=(0, 2))

    biome_image_label = tk.Label(biome_frame)
    biome_image_label.pack(pady=(0, 10))

    def update_biome() -> None:
        terrain = game.map.terrain_at(game.x, game.y)
        biome_var.set(f"Biome: {terrain.name}")
        danger = game.map.danger_at(game.x, game.y)
        danger_var.set(f"Danger: {danger:.0f}")
        img = biome_images.get(terrain.name)
        if img:
            biome_image_label.configure(image=img)
            biome_image_label.image = img
        else:
            biome_image_label.configure(image="")
            biome_image_label.image = None
        update_drink_button()

    def update_drink_button() -> None:
        terrain = game.map.terrain_at(game.x, game.y)
        state = "normal" if terrain.name == "lake" else "disabled"
        move_buttons["drink"].config(state=state)

    # Movement buttons between biome info and map
    btn_frame = tk.Frame(main, width=200)
    btn_frame.grid(row=0, column=1, sticky="nsew", padx=10, pady=10)
    btn_frame.grid_propagate(False)

    def perform(action: str) -> None:
        result = game.turn(action)
        append_output(result)
        update_biome()
        update_map()
        update_stats()
        update_encounters()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")

    move_buttons = {}
    move_buttons["north"] = tk.Button(btn_frame, text="North", width=8, command=lambda: perform("north"))
    move_buttons["south"] = tk.Button(btn_frame, text="South", width=8, command=lambda: perform("south"))
    move_buttons["east"] = tk.Button(btn_frame, text="East", width=8, command=lambda: perform("east"))
    move_buttons["west"] = tk.Button(btn_frame, text="West", width=8, command=lambda: perform("west"))
    move_buttons["stay"] = tk.Button(btn_frame, text="Stay", width=8, command=lambda: perform("stay"))
    move_buttons["drink"] = tk.Button(btn_frame, text="Drink", width=8, command=lambda: perform("drink"))

    move_buttons["north"].grid(row=0, column=1)
    move_buttons["drink"].grid(row=0, column=2)
    move_buttons["west"].grid(row=1, column=0)
    move_buttons["stay"].grid(row=1, column=1)
    move_buttons["east"].grid(row=1, column=2)
    move_buttons["south"].grid(row=2, column=1)

    # Bottom-left encounter display
    encounter_frame = tk.Frame(main, width=400)
    encounter_frame.grid(row=1, column=0, sticky="nsew", padx=10, pady=10)
    encounter_frame.grid_propagate(False)
    tk.Label(encounter_frame, text="Encounters", font=("Helvetica", 16)).pack()
    encounter_list = tk.Frame(encounter_frame)
    encounter_list.pack(fill="both", expand=True)

    def do_hunt(target_name: str, juvenile: bool) -> None:
        result = game.hunt_dinosaur(target_name, juvenile)
        append_output(result)
        update_stats()
        update_drink_button()
        update_encounters()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")

    def update_encounters() -> None:
        for w in encounter_list.winfo_children():
            w.destroy()
        terrain = game.map.terrain_at(game.x, game.y).name
        player_f = game.player.fierceness or 1
        player_s = game.player.speed or 1
        for name, stats in DINO_STATS.items():
            formations = stats.get("formations", [])
            if game.setting.formation not in formations:
                continue
            chance = stats.get("encounter_chance", {}).get(terrain, 0)
            if random.random() < chance:
                juvenile = random.random() < 0.5
                row = tk.Frame(encounter_list)
                row.pack(fill="x", pady=2, expand=True)
                if juvenile:
                    target_f = (
                        stats.get("hatchling_fierceness", 0)
                        + stats.get("adult_fierceness", 0)
                    ) / 2
                    target_s = (
                        stats.get("hatchling_speed", 0)
                        + stats.get("adult_speed", 0)
                    ) / 2
                    disp_name = f"{name} (J)"
                else:
                    target_f = stats.get("adult_fierceness", 0)
                    target_s = stats.get("adult_speed", 0)
                    disp_name = name

                rel_f = target_f / player_f
                rel_s = target_s / player_s
                info = f"{disp_name}  F:{rel_f:.2f} S:{rel_s:.2f}"
                tk.Label(
                    row,
                    text=info,
                    font=("Helvetica", 12),
                    width=28,
                    anchor="w",
                ).pack(side="left")
                tk.Button(
                    row,
                    text="Hunt",
                    width=7,
                    font=("Helvetica", 12),
                    command=lambda n=name, j=juvenile: do_hunt(n, j),
                ).pack(side="right")

    # Top-right map
    map_frame = tk.Frame(main)
    map_frame.grid(row=0, column=2, sticky="nsew", padx=10, pady=10)

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
            "swamp": "olivedrab",
            "woodlands": "palegreen",
            "badlands": "yellow",
            "lake": "blue",
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

    def color_for(val: float) -> str:
        if val <= 25:
            return "red"
        if val <= 50:
            return "orange"
        if val <= 75:
            return "goldenrod"
        return "green"

    def update_stats() -> None:
        health_label.config(
            text=f"Health: {game.player.health:.0f}%",
            fg=color_for(game.player.health),
        )
        energy_label.config(
            text=f"Energy: {game.player.energy:.0f}%",
            fg=color_for(game.player.energy),
        )
        hydration_label.config(
            text=f"Hydration: {game.player.hydration:.0f}%",
            fg=color_for(game.player.hydration),
        )
        pct = 0.0
        growth_range = game.player.adult_weight - game.player.hatchling_weight
        if growth_range > 0:
            pct = (
                (game.player.weight - game.player.hatchling_weight)
                / growth_range
            ) * 100
        weight_label.config(
            text=(
                f"Weight: {game.player.weight:.1f}kg/"
                f"{game.player.adult_weight:.0f}kg ({pct:.1f}%)"
            )
        )
        fierce_label.config(text=f"Fierceness: {game.player.fierceness:.1f}")
        speed_label.config(text=f"Speed: {game.player.speed:.1f}")

    # Bottom-right stats
    stats_frame = tk.Frame(main)
    stats_frame.grid(row=1, column=2, sticky="nsew", padx=10, pady=10)

    tk.Label(stats_frame, text=f"Dinosaur: {dinosaur_name}", font=("Helvetica", 16)).pack()
    health_label = tk.Label(stats_frame, font=("Helvetica", 16))
    health_label.pack()
    energy_label = tk.Label(stats_frame, font=("Helvetica", 16))
    energy_label.pack()
    hydration_label = tk.Label(stats_frame, font=("Helvetica", 16))
    hydration_label.pack()
    weight_label = tk.Label(stats_frame, font=("Helvetica", 16))
    weight_label.pack()
    fierce_label = tk.Label(stats_frame, font=("Helvetica", 16))
    fierce_label.pack()
    speed_label = tk.Label(stats_frame, font=("Helvetica", 16))
    speed_label.pack()
    tk.Button(stats_frame, text="Quit", width=10, command=root.destroy).pack(pady=10)

    # Bottom text output
    text_frame = tk.Frame(main)
    text_frame.grid(row=2, column=0, columnspan=3, sticky="nsew")
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
    update_stats()
    update_encounters()

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
