import tkinter as tk
from tkinter import messagebox
import random
import os
from dinosurvival.game import Game, DINO_STATS, calculate_catch_chance
from dinosurvival.settings import MORRISON, HELL_CREEK

SETTINGS = {
    "morrison": MORRISON,
    "hell_creek": HELL_CREEK,
}


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


def run_game_gui(setting, dinosaur_name: str) -> None:
    """Run the game using a graphical interface."""
    game = Game(setting, dinosaur_name)
    game._generate_encounters()

    root = tk.Tk()
    root.title("Dinosaur Survival")
    root.geometry("1400x800")
    root.minsize(1400, 800)

    # Preload biome images if available
    assets_dir = os.path.join(os.path.dirname(__file__), "assets", "biomes")
    biome_images: dict[str, tk.PhotoImage] = {}

    try:
        from PIL import Image, ImageTk  # type: ignore
    except Exception:
        Image = None  # type: ignore
        ImageTk = None  # type: ignore

    def load_scaled_image(path: str, width: int, height: int) -> tk.PhotoImage | None:
        if not os.path.exists(path):
            return None
        if Image and ImageTk:
            img = Image.open(path)
            scale = width / img.width
            resample = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
            resized = img.resize((width, int(img.height * scale)), resample)
            if resized.height > height:
                top = int((resized.height - height) / 2)
                resized = resized.crop((0, top, width, top + height))
            return ImageTk.PhotoImage(resized, master=root)
        return tk.PhotoImage(master=root, file=path)

    for tname in game.setting.terrains.keys():
        fname = f"{game.setting.formation.lower()}_{tname}.png"
        path = os.path.join(assets_dir, fname)
        img = load_scaled_image(path, 400, 250)
        if img:
            biome_images[tname] = img

    # Load player dinosaur image if available
    dino_image = None
    dino_image_path = DINO_STATS.get(dinosaur_name, {}).get("image")
    if dino_image_path:
        abs_path = os.path.join(os.path.dirname(__file__), dino_image_path)
        dino_image = load_scaled_image(abs_path, 400, 250)

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

    biome_image_label = tk.Label(biome_frame)
    biome_image_label.pack(pady=(0, 10))

    # Biome name and danger value on the same row
    biome_row = tk.Frame(biome_frame)
    biome_row.pack(pady=(0, 2))

    biome_var = tk.StringVar()
    biome_label = tk.Label(biome_row, textvariable=biome_var, font=("Helvetica", 16))
    biome_label.pack(side="left")

    danger_var = tk.StringVar()
    danger_label = tk.Label(biome_row, textvariable=danger_var, font=("Helvetica", 14))
    danger_label.pack(side="left", padx=(5, 0))

    # Player dinosaur image in the top middle
    dino_frame = tk.Frame(main, width=200)
    dino_frame.grid(row=0, column=1, sticky="nsew", padx=10, pady=10)
    dino_frame.grid_propagate(False)

    dino_image_label = tk.Label(dino_frame)
    if dino_image:
        dino_image_label.configure(image=dino_image)
        dino_image_label.image = dino_image
    dino_image_label.pack()

    def update_biome() -> None:
        terrain = game.map.terrain_at(game.x, game.y)
        label = terrain.name.capitalize()
        if game.map.has_nest(game.x, game.y):
            label += " (Nest)"
        biome_var.set(label)
        danger = game.map.danger_at(game.x, game.y)
        danger_var.set(f"(Danger: {danger:.0f})")
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

    # Movement buttons on the bottom right
    btn_frame = tk.Frame(main, width=200)
    btn_frame.grid(row=1, column=2, sticky="nsew", padx=10, pady=10)
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
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_victory_stats()

    move_buttons = {}
    move_buttons["north"] = tk.Button(
        btn_frame, text="North", width=12, height=2, command=lambda: perform("north")
    )
    move_buttons["south"] = tk.Button(
        btn_frame, text="South", width=12, height=2, command=lambda: perform("south")
    )
    move_buttons["east"] = tk.Button(
        btn_frame, text="East", width=12, height=2, command=lambda: perform("east")
    )
    move_buttons["west"] = tk.Button(
        btn_frame, text="West", width=12, height=2, command=lambda: perform("west")
    )
    move_buttons["stay"] = tk.Button(
        btn_frame, text="Stay", width=12, height=2, command=lambda: perform("stay")
    )
    move_buttons["drink"] = tk.Button(
        btn_frame, text="Drink", width=12, height=2, command=lambda: perform("drink")
    )

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
    encounter_list = tk.Frame(encounter_frame)
    encounter_list.pack(fill="both", expand=True)
    encounter_rows = []
    encounter_images: dict[str, tk.PhotoImage] = {}
    for _ in range(4):
        row = tk.Frame(encounter_list)
        img = tk.Label(row)
        info_frame = tk.Frame(row)
        name_lbl = tk.Label(info_frame, font=("Helvetica", 12), anchor="w")
        stats_lbl = tk.Label(info_frame, font=("Helvetica", 10), anchor="w")
        name_lbl.pack(anchor="w")
        stats_lbl.pack(anchor="w")
        btn = tk.Button(row, text="Hunt", width=4, height=3)
        img.grid(row=0, column=0, rowspan=2, sticky="w")
        info_frame.grid(row=0, column=1, sticky="w", padx=5)
        btn.grid(row=0, column=2, rowspan=2, sticky="e")
        row.grid_columnconfigure(1, weight=1)
        encounter_rows.append({"frame": row, "img": img, "name": name_lbl, "stats": stats_lbl, "btn": btn})

    def do_hunt(target_name: str, juvenile: bool) -> None:
        result = game.hunt_dinosaur(target_name, juvenile)
        append_output(result)
        update_biome()
        update_stats()
        update_drink_button()
        update_encounters()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_victory_stats()

    def do_collect_eggs() -> None:
        result = game.collect_eggs()
        append_output(result)
        update_stats()
        update_biome()
        update_map()
        update_encounters()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_victory_stats()

    def update_encounters() -> None:
        for slot in encounter_rows:
            slot["frame"].pack_forget()
        player_f = game.player.fierceness or 1
        player_s = game.player.speed or 1
        terrain = game.map.terrain_at(game.x, game.y).name
        boost = 0.0
        if terrain == "lake":
            boost = game.player.aquatic_boost
        elif terrain == "swamp":
            boost = game.player.aquatic_boost / 2
        player_s *= 1 + boost / 100.0
        entries = game.current_encounters
        for slot, (name, juvenile) in zip(encounter_rows, entries):
            if name.startswith("eggs:"):
                state = name.split(":", 1)[1]
                weight_map = {"small": 4, "medium": 10, "large": 20}
                slot["img"].configure(image="")
                slot["img"].image = None
                slot["name"].configure(text=f"Eggs ({state.capitalize()})")
                slot["stats"].configure(text=f"W:{weight_map.get(state, 0)}kg")
                slot["btn"].configure(command=do_collect_eggs)
                slot["frame"].pack(fill="x", pady=2, expand=True)
                continue

            stats = DINO_STATS[name]
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
            catch = calculate_catch_chance(rel_s)
            if juvenile:
                target_weight = (
                    stats.get("hatchling_weight", 0) + stats.get("adult_weight", 0)
                ) / 2
            else:
                target_weight = stats.get("adult_weight", 0)
            meat = target_weight * stats.get("carcass_food_value_modifier", 1.0)

            img_path = stats.get("image")
            img = None
            if img_path:
                abs_path = os.path.join(os.path.dirname(__file__), img_path)
                if name not in encounter_images:
                    encounter_images[name] = load_scaled_image(abs_path, 100, 63)
                img = encounter_images.get(name)
            if img:
                slot["img"].configure(image=img)
                slot["img"].image = img
            else:
                slot["img"].configure(image="")
                slot["img"].image = None

            slot["name"].configure(text=disp_name)
            slot["stats"].configure(
                text=(
                    f"F:{rel_f:.2f} S:{rel_s:.2f}"
                    f"({int(round(catch * 100))}%) M:{meat:.1f}kg"
                )
            )
            slot["btn"].configure(command=lambda n=name, j=juvenile: do_hunt(n, j))
            slot["frame"].pack(fill="x", pady=2, expand=True)

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
                if revealed and game.map.has_nest(x, y):
                    canvas.create_oval(
                        tile_size / 2 - 3,
                        tile_size / 2 - 3,
                        tile_size / 2 + 3,
                        tile_size / 2 + 3,
                        fill="black",
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
        turn_label.config(text=f"Turn: {game.turn_count}")

    # Bottom middle stats
    stats_frame = tk.Frame(main)
    stats_frame.grid(row=1, column=1, sticky="nsew", padx=10, pady=10)

    tk.Label(stats_frame, text=f"{dinosaur_name}", font=("Helvetica", 16)).pack()
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
    turn_label = tk.Label(stats_frame, font=("Helvetica", 16))
    turn_label.pack()
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

    shown_win = False

    def show_victory_stats() -> None:
        nonlocal shown_win
        if shown_win:
            return
        shown_win = True
        lines = [
            "Congratulations! You reached adult size!",
            f"Turns: {game.turn_count}",
            "",
            "Biome Visits:",
        ]
        for b, c in game.biome_turns.items():
            lines.append(f"- {b.capitalize()}: {c}")
        lines.append("")
        lines.append("Hunts:")
        for a, (att, kill) in game.hunt_stats.items():
            lines.append(f"- {a}: {kill}/{att}")
        messagebox.showinfo("Victory", "\n".join(lines))

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
