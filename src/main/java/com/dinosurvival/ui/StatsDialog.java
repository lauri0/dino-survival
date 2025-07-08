package com.dinosurvival.ui;

import com.dinosurvival.game.Game;
import com.dinosurvival.model.DinosaurStats;
import javax.swing.*;
import java.awt.BorderLayout;

/** Simple dialog showing current player statistics. */
public class StatsDialog extends JDialog {
    public StatsDialog(JFrame parent, Game game) {
        super(parent, "Player Stats", true);
        JTextArea area = new JTextArea(10, 30);
        area.setEditable(false);
        area.setText(buildText(game));
        add(new JScrollPane(area), BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        add(close, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private String buildText(Game game) {
        DinosaurStats d = game.getPlayer();
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(d.getName()).append('\n');
        sb.append(String.format("Weight: %.1f/%.0f\n", d.getWeight(), d.getAdultWeight()));
        sb.append(String.format("HP: %.1f/%.1f\n", d.getHp(), d.getMaxHp()));
        sb.append(String.format("Energy: %.0f%%\n", d.getEnergy()));
        sb.append(String.format("Hydration: %.0f%%\n", d.getHydration()));
        sb.append(String.format("Attack: %.1f\n", d.getAttack()));
        sb.append(String.format("Speed: %.1f\n", d.getSpeed()));
        sb.append("Turn: ").append(game.getTurn()).append('\n');
        sb.append("Descendants: ").append(game.descendantCount());
        return sb.toString();
    }
}
