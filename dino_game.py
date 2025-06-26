import tkinter as tk
from tkinter import messagebox
import random
import os
import dinosurvival.game as game_module
from dinosurvival.settings import MORRISON, HELL_CREEK
from dinosurvival.dinosaur import NPCAnimal
from dinosurvival.logging_utils import (
    append_game_log,
    update_hunter_log,
    load_hunter_stats,
    get_dino_game_stats,
    get_player_stats,
)

try:
    from PIL import Image, ImageTk, ImageOps  # type: ignore
except Exception:
    Image = None  # type: ignore
    ImageTk = None  # type: ignore


def load_scaled_image(path: str, width: int, height: int, master=None, grayscale: bool = False) -> tk.PhotoImage | None:
    if not os.path.exists(path):
        return None
    if Image and ImageTk:
        img = Image.open(path)
        if grayscale:
            img = ImageOps.grayscale(img)
        scale = width / img.width
        resample = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
        resized = img.resize((width, int(img.height * scale)), resample)
        if resized.height > height:
            top = int((resized.height - height) / 2)
            resized = resized.crop((0, top, width, top + height))
        return ImageTk.PhotoImage(resized, master=master)
    return tk.PhotoImage(master=master, file=path)

SETTINGS = {
    "morrison": MORRISON,
    "hell_creek": HELL_CREEK,
}


def display_legacy_stats(parent: tk.Widget, formation: str, dname: str) -> None:
    """Show legacy hunt stats with image and win rate."""
    data = load_hunter_stats()
    form = data.get(formation, {})
    dsection = form.get(dname, {})
    pairs = sorted(dsection.items(), key=lambda i: i[1], reverse=True)
    wins, losses = get_dino_game_stats(formation, dname)
    total_games = wins + losses
    win_rate = (wins / total_games * 100) if total_games else 0.0

    win = tk.Toplevel(parent)
    win.title("Dinosaur Stats")

    info = game_module.DINO_STATS.get(dname, {})
    img = None
    img_path = info.get("image")
    if img_path:
        abs_path = os.path.join(os.path.dirname(__file__), img_path)
        img = load_scaled_image(abs_path, 400, 250, master=win)
    if img:
        lbl = tk.Label(win, image=img)
        lbl.image = img
        lbl.pack()

    tk.Label(win, text=dname, font=("Helvetica", 18)).pack(pady=5)
    tk.Label(
        win,
        text=f"Games played: {total_games} ({win_rate:.0f}% win rate)",
        font=("Helvetica", 12),
    ).pack()

    if pairs:
        for prey, count in pairs:
            if count > 0:
                tk.Label(win, text=f"{prey}: {count}", font=("Helvetica", 12), anchor="w").pack(anchor="w")
    else:
        tk.Label(win, text="No recorded hunts.", font=("Helvetica", 12)).pack()

    tk.Button(win, text="Close", command=win.destroy).pack(pady=5)


def choose_dinosaur_gui(root: tk.Tk, setting, on_select, on_back=None) -> None:
    """Display a dinosaur selection menu inside an existing root window.

    ``on_back`` can be provided to return to a previous menu instead of
    closing the application.
    """

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

    # Images were previously shown on the dinosaur selection buttons, but these
    # buttons now only display text. The image loading logic is retained here in
    # comments for reference should graphical buttons be reintroduced in the
    # future.

    # try:
    #     from PIL import Image, ImageTk  # type: ignore
    # except Exception:
    #     Image = None  # type: ignore
    #     ImageTk = None  # type: ignore

    # def load_scaled_image(path: str, width: int, height: int) -> tk.PhotoImage | None:
    #     if not os.path.exists(path):
    #         return None
    #     if Image and ImageTk:
    #         img = Image.open(path)
    #         scale = width / img.width
    #         resample = getattr(getattr(Image, "Resampling", Image), "LANCZOS")
    #         resized = img.resize((width, int(img.height * scale)), resample)
    #         if resized.height > height:
    #             top = int((resized.height - height) / 2)
    #             resized = resized.crop((0, top, width, top + height))
    #         return ImageTk.PhotoImage(resized, master=root)
    #     return tk.PhotoImage(master=root, file=path)

    # images: dict[str, tk.PhotoImage] = {}

    def show_legacy(dname: str) -> None:
        display_legacy_stats(root, setting.formation, dname)

    def show_info(dname: str) -> None:
        info = game_module.DINO_STATS.get(dname, {})
        win = tk.Toplevel(root)
        win.title(f"{dname} Facts")
        img = None
        img_path = info.get("image")
        if img_path:
            abs_path = os.path.join(os.path.dirname(__file__), img_path)
            img = load_scaled_image(abs_path, 400, 250, master=win)
        if img:
            lbl = tk.Label(win, image=img)
            lbl.image = img
            lbl.pack()
        tk.Label(win, text=dname, font=("Helvetica", 18)).pack(pady=5)
        lines = []
        lines.append(f"Weight: {info.get('adult_weight', 0)} kg")
        lines.append(f"Fierceness: {info.get('adult_fierceness', 0)}")
        lines.append(f"Speed: {info.get('adult_speed', 0)}")
        lines.append(f"Energy drain per turn: {info.get('adult_energy_drain', 0)}")
        pref_biomes = ", ".join(info.get("preferred_biomes", []))
        lines.append(f"Preferred biomes: {pref_biomes}")
        diet_items = [d.value if hasattr(d, 'value') else d for d in info.get('diet', [])]
        diet = ", ".join(diet_items)
        lines.append(f"Diet: {diet}")
        interval = info.get("egg_laying_interval")
        if interval is not None:
            lines.append(f"Egg laying interval: {interval} turns")
        num_eggs = info.get("num_eggs")
        if num_eggs is not None:
            lines.append(f"Eggs laid at once: {num_eggs}")
        for line in lines:
            tk.Label(win, text=line, font=("Helvetica", 12), anchor="w", justify="left").pack(anchor="w")
        tk.Button(win, text="Close", command=win.destroy).pack(pady=5)

    for dino in setting.playable_dinos.keys():
        row = tk.Frame(frame)
        btn = tk.Button(
            row,
            text=dino,
            width=20,
            height=2,
            command=lambda d=dino: choose(d),
        )
        btn.pack(side="left", padx=5)
        tk.Button(row, text="Info", command=lambda d=dino: show_info(d)).pack(side="left")
        tk.Button(row, text="Dinosaur Stats", command=lambda d=dino: show_legacy(d)).pack(side="left")
        row.pack(pady=5)

    btn_row = tk.Frame(frame)
    btn_row.pack(pady=10)
    tk.Button(btn_row, text="Back", width=20, height=2, command=lambda: (frame.destroy(), on_back() if on_back else None)).pack(side="left", padx=5)
    tk.Button(btn_row, text="Quit", width=20, height=2, command=root.destroy).pack(side="left", padx=5)

    # This function does not start a new main loop; the provided root must already
    # be running. The window will be destroyed when a dinosaur is chosen.


def run_game_gui(setting, dinosaur_name: str) -> None:
    """Run the game using a graphical interface."""
    game = game_module.Game(setting, dinosaur_name)
    game._generate_encounters()

    root = tk.Tk()
    root.title("Dinosaur Survival")
    root.geometry("1700x1000")
    root.minsize(1700, 1000)

    tile_size = 20

    # Preload biome images if available
    assets_dir = os.path.join(os.path.dirname(__file__), "assets", "biomes")
    biome_images: dict[str, tk.PhotoImage] = {}


    for tname in game.setting.terrains.keys():
        fname = f"{game.setting.formation.lower().replace(' ', '_')}_{tname}.png"
        path = os.path.join(assets_dir, fname)
        img = load_scaled_image(path, 400, 250)
        if img:
            biome_images[tname] = img

    # Load player dinosaur images for different growth stages if available
    player_images: dict[str, tk.PhotoImage | None] = {"adult": None, "hatchling": None, "juvenile": None}
    dino_image_path = game_module.DINO_STATS.get(dinosaur_name, {}).get("image")
    if dino_image_path:
        abs_path = os.path.join(os.path.dirname(__file__), dino_image_path)
        base, ext = os.path.splitext(abs_path)
        player_images["adult"] = load_scaled_image(abs_path, 400, 250)
        player_images["hatchling"] = load_scaled_image(f"{base}_hatchling{ext}", 400, 250)
        player_images["juvenile"] = load_scaled_image(f"{base}_juvenile{ext}", 400, 250)

    main = tk.Frame(root)
    main.pack(fill="both", expand=True)

    main.grid_rowconfigure(0, weight=0)
    main.grid_rowconfigure(1, weight=1)
    main.grid_rowconfigure(2, weight=0)
    main.grid_columnconfigure(0, weight=0, minsize=200)
    main.grid_columnconfigure(1, weight=0, minsize=200)
    main.grid_columnconfigure(2, weight=1)
    main.grid_columnconfigure(3, weight=0, minsize=200)

    # Biome information (middle column after layout swap)
    biome_frame = tk.Frame(main, width=200)
    biome_frame.grid(row=0, column=1, sticky="nsew", padx=10, pady=10)
    biome_frame.grid_propagate(False)

    biome_image_label = tk.Label(biome_frame)
    biome_image_label.pack(pady=(0, 10))

    # Biome name
    biome_row = tk.Frame(biome_frame)
    biome_row.pack(pady=(0, 2))

    biome_var = tk.StringVar()
    biome_label = tk.Label(biome_row, textvariable=biome_var, font=("Helvetica", 16))
    biome_label.pack(side="left")

    # Player dinosaur image on the left after swap
    dino_frame = tk.Frame(main, width=200)
    dino_frame.grid(row=0, column=0, sticky="nsew", padx=10, pady=10)
    dino_frame.grid_propagate(False)

    dino_image_label = tk.Label(dino_frame)
    initial_stage = game.player_growth_stage().lower()
    key = initial_stage if initial_stage in ("hatchling", "juvenile") else "adult"
    img = player_images.get(key)
    if img:
        dino_image_label.configure(image=img)
        dino_image_label.image = img
    dino_image_label.pack()

    info_images: dict[str, tk.PhotoImage] = {}
    npc_images: dict[str, tk.PhotoImage] = {}

    def show_dino_facts(name: str = dinosaur_name) -> None:
        info = game_module.DINO_STATS.get(name)
        if info is None:
            info = game_module.CRITTER_STATS.get(name, {})
        win = tk.Toplevel(root)
        win.title(f"{name} Facts")
        img = info_images.get(name)
        img_path = info.get("image")
        if img_path and img is None:
            abs_path = os.path.join(os.path.dirname(__file__), img_path)
            info_images[name] = load_scaled_image(abs_path, 400, 250, master=win)
            img = info_images.get(name)

        header = tk.Frame(win)
        counts = game.population_history.get(name, [])
        if img:
            lbl = tk.Label(header, image=img)
            lbl.image = img
            lbl.pack(side="left", padx=5)

        if counts:
            width, height = 320, 200
            margin = 24
            top_pad = 10
            right_pad = 10
            canvas = tk.Canvas(
                header,
                width=width + margin + right_pad,
                height=height + margin + top_pad,
            )
            max_c = max(counts)
            max_c = max(max_c, 1)
            step = width / max(len(counts) - 1, 1)
            canvas.create_line(margin, top_pad, margin, top_pad + height)
            canvas.create_line(margin, top_pad + height, margin + width, top_pad + height)
            for i in range(1, len(counts)):
                x1 = margin + (i - 1) * step
                y1 = top_pad + height - counts[i - 1] / max_c * height
                x2 = margin + i * step
                y2 = top_pad + height - counts[i] / max_c * height
                canvas.create_line(x1, y1, x2, y2, fill="blue")
            canvas.create_text(margin - 5, top_pad + height, text="0", anchor="e", font=("Helvetica", 8))
            canvas.create_text(margin - 5, top_pad, text=str(max_c), anchor="e", font=("Helvetica", 8))
            turns = game.turn_history
            if turns:
                canvas.create_text(
                    margin,
                    top_pad + height + 10,
                    text=str(turns[0]),
                    anchor="n",
                    font=("Helvetica", 8),
                )
                canvas.create_text(
                    margin + width,
                    top_pad + height + 10,
                    text=str(turns[-1]),
                    anchor="n",
                    font=("Helvetica", 8),
                )
            canvas.create_text(
                margin + width / 2,
                top_pad + height + 20,
                text="Turn",
                font=("Helvetica", 10),
            )
            canvas.create_text(
                10,
                top_pad + height / 2,
                text="Population",
                angle=90,
                font=("Helvetica", 10),
            )
            canvas.pack(side="left", padx=5)

        if img or counts:
            header.pack()

        tk.Label(win, text=name, font=("Helvetica", 18)).pack(pady=5)
        lines = []
        lines.append(f"Weight: {info.get('adult_weight', 0)} kg")
        lines.append(f"Fierceness: {info.get('adult_fierceness', 0)}")
        lines.append(f"Speed: {info.get('adult_speed', 0)}")
        lines.append(f"Energy drain per turn: {info.get('adult_energy_drain', 0)}")
        pref_biomes = ", ".join(info.get("preferred_biomes", []))
        lines.append(f"Preferred biomes: {pref_biomes}")
        diet_items = [d.value if hasattr(d, 'value') else d for d in info.get('diet', [])]
        diet = ", ".join(diet_items)
        lines.append(f"Diet: {diet}")
        interval = info.get("egg_laying_interval")
        if interval is not None:
            lines.append(f"Egg laying interval: {interval} turns")
        num_eggs = info.get("num_eggs")
        if num_eggs is not None:
            lines.append(f"Eggs laid at once: {num_eggs}")
        for line in lines:
            tk.Label(win, text=line, font=("Helvetica", 12), anchor="w", justify="left").pack(anchor="w")
        tk.Button(win, text="Close", command=win.destroy).pack(pady=5)

    def show_legacy_stats() -> None:
        display_legacy_stats(root, game.setting.formation, dinosaur_name)

    def show_player_stats() -> None:
        games, wins, hunts, turns = get_player_stats()
        win_rate = (wins / games * 100) if games else 0.0
        win = tk.Toplevel(root)
        win.title("Player Stats")
        tk.Label(win, text="Player Stats", font=("Helvetica", 18)).pack(pady=5)
        lines = [
            f"Games played: {games} ({win_rate:.0f}% win rate)",
            f"Successful hunts: {hunts}",
            f"Lifetime turns: {turns}",
        ]
        for l in lines:
            tk.Label(win, text=l, font=("Helvetica", 12), anchor="w").pack(anchor="w")
        tk.Button(win, text="Close", command=win.destroy).pack(pady=5)

    def show_npc_stats(npc: NPCAnimal) -> None:
        stats = game_module.DINO_STATS.get(npc.name)
        if stats is None:
            stats = game_module.CRITTER_STATS.get(npc.name, {})
        win = tk.Toplevel(root)
        win.title(f"{npc.name} Stats")
        img = npc_images.get(npc.name)
        img_path = stats.get("image")
        if img_path and img is None:
            abs_path = os.path.join(os.path.dirname(__file__), img_path)
            npc_images[npc.name] = load_scaled_image(abs_path, 400, 250, master=win, grayscale=not npc.alive)
            img = npc_images.get(npc.name)
        if img:
            lbl = tk.Label(win, image=img)
            lbl.image = img
            lbl.pack()
        tk.Label(win, text=npc.name, font=("Helvetica", 18)).pack(pady=5)
        tk.Label(win, text=f"Age: {npc.age} turns", font=("Helvetica", 12), anchor="w").pack(anchor="w")
        tk.Label(win, text=f"Health: {npc.health:.0f}%", font=("Helvetica", 12), anchor="w").pack(anchor="w")
        tk.Label(win, text=f"Energy: {npc.energy:.0f}%", font=("Helvetica", 12), anchor="w").pack(anchor="w")
        abil = "None"
        if "ambush" in npc.abilities:
            bonus = min(npc.ambush_streak, 3) * 5
            abil = f"Ambush ({npc.ambush_streak} stacks) +{bonus}% speed"
        tk.Label(win, text=f"Abilities: {abil}", font=("Helvetica", 12), anchor="w").pack(anchor="w")
        sep = tk.Frame(win, height=2, bd=1, relief="sunken")
        sep.pack(fill="x", pady=5)
        if npc.hunts:
            tk.Label(win, text="Successful Hunts:", font=("Helvetica", 12), anchor="w").pack(anchor="w")
            for prey, count in sorted(npc.hunts.items()):
                tk.Label(win, text=f"  {prey}: {count}", font=("Helvetica", 12), anchor="w").pack(anchor="w")
        else:
            tk.Label(win, text="Successful Hunts: None", font=("Helvetica", 12), anchor="w").pack(anchor="w")
        tk.Label(win, text=f"Egg clusters eaten: {npc.egg_clusters_eaten}", font=("Helvetica", 12), anchor="w").pack(anchor="w")
        sep = tk.Frame(win, height=2, bd=1, relief="sunken")
        sep.pack(fill="x", pady=5)
        tk.Label(
            win,
            text=f"Weight: {npc.weight:.1f}/{stats.get('adult_weight', 0)}",
            font=("Helvetica", 12),
            anchor="w",
        ).pack(anchor="w")
        fierce = game._stat_from_weight(npc.weight, stats, "hatchling_fierceness", "adult_fierceness")
        tk.Label(
            win,
            text=f"Fierceness: {fierce:.1f}/{stats.get('adult_fierceness', 0)}",
            font=("Helvetica", 12),
            anchor="w",
        ).pack(anchor="w")
        speed = game.npc_effective_speed(npc, stats)
        tk.Label(
            win,
            text=f"Speed: {speed:.1f}/{stats.get('adult_speed', 0)}",
            font=("Helvetica", 12),
            anchor="w",
        ).pack(anchor="w")
        tk.Button(win, text="Close", command=win.destroy).pack(pady=5)

    button_row = tk.Frame(dino_frame)
    tk.Button(button_row, text="Info", command=show_dino_facts).pack(side="left", padx=2)
    tk.Button(button_row, text="Dinosaur Stats", command=show_legacy_stats).pack(side="left", padx=2)
    tk.Button(button_row, text="Player Stats", command=show_player_stats).pack(side="left", padx=2)
    button_row.pack(pady=5)

    def update_biome() -> None:
        terrain = game.map.terrain_at(game.x, game.y)
        label = terrain.name.capitalize()
        if game.map.has_nest(game.x, game.y):
            label += " (Nest)"
        label += f" ({game.x},{game.y})"
        biome_var.set(label)
        img = biome_images.get(terrain.name)
        if img:
            biome_image_label.configure(image=img)
            biome_image_label.image = img
        else:
            biome_image_label.configure(image="")
            biome_image_label.image = None
        update_drink_button()
        update_lay_button()

    def update_drink_button() -> None:
        terrain = game.map.terrain_at(game.x, game.y)
        state = "normal" if terrain.name == "lake" else "disabled"
        move_buttons["drink"].config(state=state)

    def update_lay_button() -> None:
        state = "normal" if game._can_player_lay_eggs() else "disabled"
        move_buttons["lay"].config(state=state)

    # Movement buttons (middle column after swap)
    btn_frame = tk.Frame(main, width=200)
    btn_frame.grid(row=1, column=1, sticky="nsew", padx=10, pady=10)
    btn_frame.grid_propagate(False)
    # Buttons were previously centered using the place geometry manager which
    # caused the plant list to overlap them when it grew. Pack the container at
    # the top instead so the plants appear underneath without covering the
    # movement controls.
    btn_container = tk.Frame(btn_frame)
    btn_container.pack(pady=5)
    for i in range(3):
        btn_container.grid_columnconfigure(i, weight=1)
    for i in range(3):
        btn_container.grid_rowconfigure(i, weight=1)

    def perform(action: str) -> None:
        result = game.turn(action)
        append_output(result)
        update_biome()
        update_map()
        update_stats()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    move_buttons = {}
    move_buttons["lay"] = tk.Button(
        btn_container,
        text="Lay Eggs",
        width=12,
        height=2,
        command=lambda: do_lay_eggs(),
    )
    move_buttons["north"] = tk.Button(
        btn_container, text="North", width=12, height=2, command=lambda: perform("north")
    )
    move_buttons["south"] = tk.Button(
        btn_container, text="South", width=12, height=2, command=lambda: perform("south")
    )
    move_buttons["east"] = tk.Button(
        btn_container, text="East", width=12, height=2, command=lambda: perform("east")
    )
    move_buttons["west"] = tk.Button(
        btn_container, text="West", width=12, height=2, command=lambda: perform("west")
    )
    move_buttons["stay"] = tk.Button(
        btn_container, text="Stay", width=12, height=2, command=lambda: perform("stay")
    )
    move_buttons["drink"] = tk.Button(
        btn_container, text="Drink", width=12, height=2, command=lambda: perform("drink")
    )
    move_buttons["threaten"] = tk.Button(
        btn_container,
        text="Threaten",
        width=12,
        height=2,
        command=lambda: do_threaten(),
    )

    move_buttons["north"].grid(row=0, column=1)
    move_buttons["drink"].grid(row=0, column=2)
    move_buttons["lay"].grid(row=0, column=0)
    move_buttons["west"].grid(row=1, column=0)
    move_buttons["stay"].grid(row=1, column=1)
    move_buttons["east"].grid(row=1, column=2)
    move_buttons["south"].grid(row=2, column=1)
    move_buttons["threaten"].grid(row=2, column=2)

    # Encounter display in the center right
    encounter_frame = tk.Frame(main, width=500)
    encounter_frame.grid(row=1, column=2, rowspan=4, sticky="nsew", padx=10, pady=10)
    encounter_frame.grid_propagate(False)

    encounter_canvas = tk.Canvas(encounter_frame)
    encounter_scroll = tk.Scrollbar(
        encounter_frame, orient="vertical", command=encounter_canvas.yview
    )
    encounter_canvas.configure(yscrollcommand=encounter_scroll.set)
    encounter_scroll.pack(side="right", fill="y")
    encounter_canvas.pack(side="left", fill="both", expand=True)

    encounter_list = tk.Frame(encounter_canvas)
    encounter_window = encounter_canvas.create_window(
        (0, 0), window=encounter_list, anchor="nw"
    )

    def _resize_encounter(event) -> None:
        encounter_canvas.configure(scrollregion=encounter_canvas.bbox("all"))
        encounter_canvas.itemconfigure(encounter_window, width=event.width)

    encounter_list.bind("<Configure>", _resize_encounter)

    encounter_images: dict[str, tk.PhotoImage] = {}
    encounter_rows: list[dict] = []

    # Plant display under movement buttons
    plant_list = tk.Frame(btn_frame)
    plant_list.pack(fill="x", side="bottom")
    plant_rows = []
    plant_images: dict[str, tk.PhotoImage] = {}

    # Population tracker on the right
    population_frame = tk.Frame(main, width=200)
    population_frame.grid(row=0, column=3, rowspan=4, sticky="nsew", padx=10, pady=10)
    population_frame.grid_propagate(False)
    population_list = tk.Frame(population_frame)
    population_list.pack(fill="both", expand=True)
    population_images: dict[str, tk.PhotoImage] = {}

    def do_hunt(npc_id: int) -> None:
        result = game.hunt_npc(npc_id)
        append_output(result)
        update_biome()
        update_stats()
        update_drink_button()
        update_lay_button()
        update_lay_button()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    def do_mate(npc_id: int) -> None:
        result = game.mate(npc_id)
        append_output(result)
        update_biome()
        update_stats()
        update_drink_button()
        update_lay_button()
        update_lay_button()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    def do_pack_up(juvenile: bool) -> None:
        result = game.pack_up(juvenile)
        append_output(result)
        update_biome()
        update_stats()
        update_drink_button()
        update_lay_button()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    def do_leave_pack() -> None:
        result = game.leave_pack()
        append_output(result)
        update_biome()
        update_stats()
        update_drink_button()
        update_lay_button()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    def do_collect_eggs() -> None:
        result = game.collect_eggs()
        append_output(result)
        update_stats()
        update_biome()
        update_map()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    def do_dig_burrow() -> None:
        result = game.dig_burrow()
        append_output(result)
        update_stats()
        update_biome()
        update_map()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    def do_lay_eggs() -> None:
        result = game.lay_eggs()
        append_output(result)
        update_stats()
        update_biome()
        update_map()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    def do_threaten() -> None:
        result = game.threaten()
        append_output(result)
        update_biome()
        update_stats()
        update_drink_button()
        update_lay_button()
        update_encounters()
        update_plants()
        if "Game Over" in result:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Game Over", "You have perished!")
        if game.won:
            for b in move_buttons.values():
                b.config(state="disabled")
            show_final_stats("Victory", "Congratulations! Your lineage lives on!")

    def update_encounters() -> None:
        for child in encounter_list.winfo_children():
            child.destroy()
        encounter_rows.clear()
        player_f = game.effective_fierceness() or 1
        player_s = game.player_effective_speed() or 1
        entries = game.current_encounters
        for entry in entries:
            row = tk.Frame(encounter_list)
            img = tk.Label(row)
            info_frame = tk.Frame(row)
            # Allow a bit more space so long names and stats are fully visible
            name_lbl = tk.Label(
                info_frame, font=("Helvetica", 12), anchor="w", width=30
            )
            stats_lbl = tk.Label(
                info_frame, font=("Helvetica", 10), anchor="w", width=28
            )
            name_lbl.pack(anchor="w")
            stats_lbl.pack(anchor="w")
            info_btn = tk.Button(row, text="Stats", width=4, height=3)
            btn = tk.Button(row, text="Hunt", width=4, height=3)
            img.grid(row=0, column=0, rowspan=2, sticky="w")
            info_frame.grid(row=0, column=1, sticky="w", padx=5)
            info_btn.grid(row=0, column=2, rowspan=2, sticky="e")
            btn.grid(row=0, column=3, rowspan=2, sticky="e")
            row.grid_columnconfigure(1, weight=1)
            slot = {"frame": row, "img": img, "name": name_lbl, "stats": stats_lbl, "btn": btn, "info": info_btn}
            encounter_rows.append(slot)
            if entry.burrow:
                b = entry.burrow
                key = "burrow"
                if key not in encounter_images:
                    path = os.path.join(os.path.dirname(__file__), "assets/other/burrow.png")
                    encounter_images[key] = load_scaled_image(path, 100, 63)
                bimg = encounter_images.get(key)
                if bimg:
                    slot["img"].configure(image=bimg)
                    slot["img"].image = bimg
                else:
                    slot["img"].configure(image="")
                status = "Full" if b.full else "Empty"
                slot["name"].configure(text=f"Burrow ({status})")
                slot["stats"].configure(text=f"Dig:{b.progress:.0f}%")
                slot["btn"].configure(command=do_dig_burrow, text="Dig")
                slot["info"].grid_remove()
                slot["frame"].pack(fill="x", pady=2, expand=True)
                continue
            if entry.eggs:
                cluster = entry.eggs
                slot["img"].configure(image="")
                slot["img"].image = None
                slot["name"].configure(
                    text=f"{cluster.species} Eggs ({cluster.number})"
                )
                slot["stats"].configure(
                    text=(
                        f"W:{cluster.weight:.1f}kg "
                        f"Hatch:{cluster.turns_until_hatch}"
                    )
                )
                slot["btn"].configure(command=do_collect_eggs, text="Eat")
                slot["info"].grid_remove()
                slot["frame"].pack(fill="x", pady=2, expand=True)
                continue

            npc = entry.npc
            if npc is None:
                continue
            stats = game_module.DINO_STATS.get(npc.name)
            if stats is None:
                stats = game_module.CRITTER_STATS.get(npc.name, {})
            disp_name = f"{npc.name} ({npc.id})"
            disp_name = f"{disp_name} W:{npc.weight:.1f}kg"

            target_f = game._stat_from_weight(
                npc.weight, stats, "hatchling_fierceness", "adult_fierceness"
            )
            target_f *= npc.health / 100.0
            target_s = game.npc_effective_speed(npc, stats)
            rel_f = target_f / player_f
            rel_s = target_s / player_s
            catch = game_module.calculate_catch_chance(rel_s)

            img_path = stats.get("image")
            img = None
            if img_path:
                abs_path = os.path.join(os.path.dirname(__file__), img_path)
                key = npc.name if npc.alive else f"{npc.name}_dead"
                if key not in encounter_images:
                    encounter_images[key] = load_scaled_image(abs_path, 100, 63, grayscale=not npc.alive)
                img = encounter_images.get(key)
            if img:
                slot["img"].configure(image=img)
                slot["img"].image = img
            else:
                slot["img"].configure(image="")
                slot["img"].image = None

            slot["name"].configure(text=disp_name)
            slot["stats"].configure(
                text=(
                    f"F:{rel_f:.2f} S:{rel_s:.2f} "
                    f"({int(round(catch * 100))}%) "
                    f"E:{npc.energy:.0f}% "
                )
            )
            label = "Hunt" if npc.alive else "Eat"
            slot["btn"].configure(command=lambda i=npc.id: do_hunt(i), text=label)
            slot["info"].configure(command=lambda n=npc: show_npc_stats(n))
            slot["info"].grid()
            slot["frame"].pack(fill="x", pady=2, expand=True)
        update_population()

    def update_plants() -> None:
        for child in plant_list.winfo_children():
            child.destroy()
        plant_rows.clear()
        for plant in game.current_plants:
            row = tk.Frame(plant_list)
            img = tk.Label(row)
            info_frame = tk.Frame(row)
            name_lbl = tk.Label(info_frame, font=("Helvetica", 12), anchor="w")
            weight_lbl = tk.Label(info_frame, font=("Helvetica", 10), anchor="w")
            name_lbl.pack(anchor="w")
            weight_lbl.pack(anchor="w")
            img.grid(row=0, column=0, rowspan=2, sticky="w")
            info_frame.grid(row=0, column=1, sticky="w", padx=5)
            row.grid_columnconfigure(1, weight=1)
            slot = {"frame": row, "img": img, "name": name_lbl, "weight": weight_lbl}
            plant_rows.append(slot)
            stats = game_module.PLANT_STATS.get(plant.name)
            img_path = stats.image if stats else None
            pimg = None
            if img_path:
                abs_path = os.path.join(os.path.dirname(__file__), img_path)
                if plant.name not in plant_images:
                    plant_images[plant.name] = load_scaled_image(abs_path, 100, 63)
                pimg = plant_images.get(plant.name)
            if pimg:
                slot["img"].configure(image=pimg)
                slot["img"].image = pimg
            slot["name"].configure(text=plant.name)
            slot["weight"].configure(text=f"W:{plant.weight:.1f}kg")
            slot["frame"].pack(fill="x", pady=2, expand=True)

    # Top-right map
    map_frame = tk.Frame(main)
    map_frame.grid(row=0, column=2, sticky="nsew", padx=10, pady=10)

    map_tiles = []
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
            "swamp": "olivedrab",
            "woodlands": "palegreen",
            "badlands": "yellow",
            "lake": "blue",
            "mountain": "darkgray",
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
                if revealed and game.map.has_burrow(x, y):
                    canvas.create_polygon(
                        tile_size / 2,
                        tile_size / 2 - 4,
                        tile_size / 2 - 4,
                        tile_size / 2 + 4,
                        tile_size / 2 + 4,
                        tile_size / 2 + 4,
                        fill="brown",
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
        stage = game.player_growth_stage()
        name_var.set(f"{dinosaur_name} \u2640 ({stage})")
        img_key = stage.lower() if stage.lower() in ("hatchling", "juvenile") else "adult"
        img = player_images.get(img_key)
        if img:
            dino_image_label.configure(image=img)
            dino_image_label.image = img
        elif player_images.get("adult"):
            dino_image_label.configure(image=player_images["adult"])
            dino_image_label.image = player_images["adult"]
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
        adj_f = game.player.fierceness * (game.player.health / 100.0)
        fierce_label.config(text=f"Fierceness: {adj_f:.1f}")
        speed_label.config(text=f"Speed: {game.player_effective_speed():.1f}")
        desc_label.config(
            text=f"Alive descendants: {game.descendant_count()}"
        )
        turn_label.config(text=f"Turn: {game.turn_count}")
        update_lay_button()

    def update_population() -> None:
        for child in population_list.winfo_children():
            child.destroy()
        counts, total = game.population_stats()
        if total <= 0:
            return
        for name, count in sorted(counts.items(), key=lambda x: x[1], reverse=True):
            pct = count / total * 100
            row = tk.Frame(population_list)
            img_lbl = tk.Label(row)
            img_path = game_module.DINO_STATS.get(name, {}).get("image")
            if img_path is None:
                img_path = game_module.CRITTER_STATS.get(name, {}).get("image")
            img = None
            if img_path:
                abs_path = os.path.join(os.path.dirname(__file__), img_path)
                if name not in population_images:
                    population_images[name] = load_scaled_image(abs_path, 40, 25)
                img = population_images.get(name)
            if img:
                img_lbl.configure(image=img)
                img_lbl.image = img
            img_lbl.grid(row=0, column=0, sticky="w")
            tk.Label(row, text=f"{name}", font=("Helvetica", 10)).grid(row=0, column=1, sticky="w", padx=5)
            tk.Label(row, text=f"{count} ({pct:.1f}%)", font=("Helvetica", 10)).grid(row=0, column=2, sticky="w")
            tk.Button(row, text="Info", width=4, command=lambda n=name: show_dino_facts(n)).grid(row=0, column=3, sticky="e")
            row.grid_columnconfigure(1, weight=1)
            row.pack(fill="x", pady=2)

    # Left stats
    stats_frame = tk.Frame(main)
    stats_frame.grid(row=1, column=0, sticky="nsew", padx=10, pady=10)

    name_var = tk.StringVar()
    tk.Label(stats_frame, textvariable=name_var, font=("Helvetica", 16)).pack()
    health_label = tk.Label(stats_frame, font=("Helvetica", 14), anchor="w")
    health_label.pack(anchor="w")
    energy_label = tk.Label(stats_frame, font=("Helvetica", 14), anchor="w")
    energy_label.pack(anchor="w")
    hydration_label = tk.Label(stats_frame, font=("Helvetica", 14), anchor="w")
    hydration_label.pack(anchor="w")
    weight_label = tk.Label(stats_frame, font=("Helvetica", 14), anchor="w")
    weight_label.pack(anchor="w")
    fierce_label = tk.Label(stats_frame, font=("Helvetica", 14), anchor="w")
    fierce_label.pack(anchor="w")
    speed_label = tk.Label(stats_frame, font=("Helvetica", 14), anchor="w")
    speed_label.pack(anchor="w")
    desc_label = tk.Label(stats_frame, font=("Helvetica", 14), anchor="w")
    desc_label.pack(anchor="w")
    turn_label = tk.Label(stats_frame, font=("Helvetica", 14), anchor="w")
    turn_label.pack(anchor="w")
    tk.Button(stats_frame, text="Quit", width=10, command=root.destroy).pack(pady=10)

    # Bottom left text output
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

    shown_stats = False

    def show_final_stats(title: str, header: str) -> None:
        nonlocal shown_stats
        if shown_stats:
            return
        shown_stats = True
        append_game_log(game.setting.formation, dinosaur_name, game.turn_count, game.player.weight, game.won)
        update_hunter_log(game.setting.formation, dinosaur_name, game.hunt_stats)
        lines = [
            header,
            f"Turns: {game.turn_count}",
            "",
            "Biome Visits:",
        ]
        for b in game.setting.terrains.keys():
            c = game.biome_turns.get(b, 0)
            lines.append(f"- {b.capitalize()}: {c}")
        lines.append("")
        lines.append("Hunts:")
        for a, (att, kill) in sorted(
            game.hunt_stats.items(), key=lambda i: i[1][1], reverse=True
        ):
            lines.append(f"- {a}: {kill}/{att}")
        messagebox.showinfo(title, "\n".join(lines))

    update_biome()
    update_map()
    update_stats()
    update_encounters()
    update_plants()
    update_population()

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

        choose_dinosaur_gui(root, setting, on_select, on_back=show_start_menu)

    show_start_menu()
    root.mainloop()

    if selection["setting"] and selection["dino"]:
        run_game_gui(selection["setting"], selection["dino"])


def main():
    launch_menu()


if __name__ == "__main__":
    main()
