package com.dinosurvival.ui;

import com.dinosurvival.game.Game;

public class Main {
    public static void main(String[] args) {
        Game game = new Game();
        game.start();
        java.awt.EventQueue.invokeLater(() -> {
            GameWindow win = new GameWindow(game);
            win.setVisible(true);
            win.log("Game started");
        });
    }
}
