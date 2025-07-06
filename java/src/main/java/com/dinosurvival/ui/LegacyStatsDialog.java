package com.dinosurvival.ui;

import com.dinosurvival.util.LogUtils;
import javax.swing.*;
import java.awt.BorderLayout;
import java.io.IOException;

/** Dialog that shows win/loss statistics for a dinosaur across games. */
public class LegacyStatsDialog extends JDialog {
    public LegacyStatsDialog(JFrame parent, String formation, String dino) {
        super(parent, "Legacy Stats", true);
        JTextArea area = new JTextArea(10, 30);
        area.setEditable(false);
        area.setText(loadStats(formation, dino));
        add(new JScrollPane(area), BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        add(close, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private String loadStats(String formation, String dino) {
        StringBuilder sb = new StringBuilder();
        try {
            int[] wl = LogUtils.getDinoGameStats(formation, dino);
            sb.append(String.format("Wins: %d\nLosses: %d\n", wl[0], wl[1]));
            int[] player = LogUtils.getPlayerStats();
            sb.append(String.format("Total Games: %d\n", player[0]));
            sb.append(String.format("Overall Wins: %d\n", player[1]));
        } catch (IOException ex) {
            sb.append("No statistics available.");
        }
        return sb.toString();
    }
}
