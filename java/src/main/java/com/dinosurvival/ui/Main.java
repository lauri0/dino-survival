package com.dinosurvival.ui;

import com.dinosurvival.game.Game;

public class Main {
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> {
            SetupDialog setup = new SetupDialog(null);
            setup.setVisible(true);
            String formation = setup.getFormation();
            String dino = setup.getDinosaur();
            Game game = new Game();
            if (formation != null && dino != null) {
                game.start(formation, dino);
            } else {
                game.start();
            }
            GameWindow win = new GameWindow(game);
            win.setVisible(true);
            win.log("Game started");
        });
    }
}
