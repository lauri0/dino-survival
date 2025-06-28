# Dino Survival

A simple text-based dinosaur survival game inspired by the BBC "Big Al" game. The player controls a juvenile carnivorous dinosaur that must hunt to survive and grow. Different geological settings define the available species and terrain types.

## Features

- Turn based actions: explore the map as a hatchling carnivorous dinosaur. Find water, hunt, grow, evade natural disasters and lay eggs to create the next generation.
- NPC animals are persistently simulated and move around the map in search of food. Carnivores hunt other animals while herbivores eat the various plants which grow in the game world.
- Noise based random terrain generation including forests, plains, swamps, woodlands, desert and lakes that affect the type of prey encountered.
- Occasional natural disasters like volcanic eruptions and floods.
![dino_survival](https://github.com/user-attachments/assets/e18ef895-6505-4440-9653-15dc9b8826be)

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
The stats panel also includes **Player Stats** alongside **Info** and
**Dinosaur Stats**. Player Stats show your cumulative games played, win rate,
successful hunts and total turns across every dinosaur you've played.

Each dinosaur's base attributes are defined in `dinosurvival/dino_stats_morrison.yaml`.

