# Dino Survival

A simple text-based dinosaur survival game inspired by the BBC "Big Al" game. The player controls a juvenile carnivorous dinosaur that must hunt to survive and grow. Different geological settings define the available species and terrain types.

## Features

- Turn based actions: explore the map as a hatchling carnivorous dinosaur. Find water, hunt, grow, evade natural disasters and lay eggs to create the next generation.
- NPC animals are persistently simulated and move around the map in search of food. Carnivores hunt other animals while herbivores eat the various plants which grow in the game world.
- Noise based random terrain generation including forests, plains, swamps, woodlands, deserts and lakes that affect the type of prey encountered.
- Occasional natural disasters like volcanic eruptions and floods. Stay away from the lava while it is hot!
![dino_survival2](https://github.com/user-attachments/assets/2422b694-39cd-4f3d-a854-7f3e4c95fca5)

## Requirements

The game requires Python 3.8+. If the image scaling doesn't work and the game opens with a huge dinosaur image covering the window then you will need to install pillow: pip install pillow.

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


## Java Version

A simple Java implementation is included under the `java` directory. It uses Maven for builds.

### Building

```bash
mvn -f java/pom.xml package
```

### Running

The build creates a versioned jar under `java/target`. Run it using

```bash
java -jar java/target/dino-survival-0.1.0.jar
```
