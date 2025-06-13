# Dino Survival

A simple text-based dinosaur survival game inspired by the BBC "Big Al" game. The player controls a juvenile carnivorous dinosaur that must hunt to survive and grow. Different geological settings define the available species and terrain types.

## Features

- Turn based actions: move or hunt on each turn.
- Random terrain including forests, plains, swamps, woodlands, badlands and lakes that affects the type of prey encountered.
- Danger increases with hunting and slowly decays, reducing animal encounters on highly hunted tiles.
- Collecting eggs no longer raises danger, allowing opportunistic meals without scaring off prey.
- Support for multiple settings such as the Morrison Formation or Hell Creek.

## Requirements

The game only requires Python 3.8+.

## Running the Game

```bash
python dino_game.py
```

Running the script opens a full-screen menu with buttons for the available
geological settings. After selecting **Morrison** or **Hell Creek**, another
menu appears allowing you to pick a dinosaur. Once a dinosaur is chosen the
window clears and a new interface appears. Use the direction buttons to move
between squares or stay put and watch the text box at the bottom for game
updates. The **Quit** button in the stats panel exits the program.
The stats panel also includes **Player Stats** alongside **Info**, **Game Stats**
and **Legacy Stats**. This shows your cumulative games played, win rate,
successful hunts and total turns across all sessions.

Each dinosaur's base attributes are defined in `dinosurvival/dino_stats.yaml`.
