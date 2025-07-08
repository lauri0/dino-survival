package com.dinosurvival.ui;

import javax.swing.*;
import java.awt.BorderLayout;

/** Dialog explaining the encounter list columns. */
public class EncounterHelpDialog extends JDialog {
    public EncounterHelpDialog(JFrame parent) {
        super(parent, "Encounter Help", true);
        setLayout(new BorderLayout());
        JLabel heading = new JLabel("Encounters", SwingConstants.CENTER);
        heading.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        add(heading, BorderLayout.NORTH);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setText(String.join("\n",
                "The encounters list displays animals or nests in your current cell.",
                "W: prey weight, A: attack power, HP: health points,",
                "S: speed relative to you (%) and E: energy available.",
                "Higher attack deals more damage while higher speed",
                "makes prey harder to catch."));
        area.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        add(area, BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        add(close, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }
}
