package com.dinosurvival.ui;

import com.dinosurvival.game.Game;
import java.awt.BorderLayout;
import javax.swing.*;

public class GameWindow extends JFrame {
    private final JTextArea logArea = new JTextArea();
    private final Game game;

    public GameWindow(Game game) {
        super("Dino Survival");
        this.game = game;
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        add(new JScrollPane(logArea), BorderLayout.CENTER);
    }

    public void log(String msg) {
        logArea.append(msg + "\n");
    }
}
