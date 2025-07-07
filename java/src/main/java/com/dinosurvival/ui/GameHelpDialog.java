package com.dinosurvival.ui;

import javax.swing.*;
import java.awt.BorderLayout;

/** Dialog showing general gameplay help. */
public class GameHelpDialog extends JDialog {
    public GameHelpDialog(JFrame parent) {
        super(parent, "Game Help", true);
        setLayout(new BorderLayout());
        JLabel heading = new JLabel("Game Help", SwingConstants.CENTER);
        heading.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        add(heading, BorderLayout.NORTH);
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setText(String.join("\n",
                "Use the movement buttons to travel north, south, east or west.",
                "Stay will skip a turn and Drink restores hydration when water is present.",
                "Threaten can scare prey away. Lay Eggs lets you deposit eggs once ready.",
                "Lay Eggs requires being fully grown with 80+ health and energy.",
                "There is a short waiting period before eggs can be laid again after doing so.",
                "Health, Energy, Hydration, Weight, Attack and Speed describe your dinosaur.",
                "Grow by hunting prey and once grown up lay eggs and hatch enough of them to win."));
        area.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
        add(area, BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        add(close, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }
}
