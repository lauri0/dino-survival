package com.dinosurvival.ui;

import com.dinosurvival.util.StatsLoader;
import java.util.Map;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.*;

/** Dialog used to choose formation and dinosaur before starting the game. */
public class SetupDialog extends JDialog {
    private String formation;
    private String dinosaur;
    private final JComboBox<String> formationBox = new JComboBox<>(new String[]{"Morrison", "Hell Creek"});
    private final JComboBox<String> dinoBox = new JComboBox<>();
    private static final Map<String, String[]> PLAYABLE_DINOS = Map.of(
            "Morrison",
            new String[]{"Allosaurus", "Ceratosaurus", "Torvosaurus", "Ornitholestes"},
            "Hell Creek",
            new String[]{"Tyrannosaurus", "Acheroraptor", "Pectinodon"}
    );

    public SetupDialog(JFrame parent) {
        super(parent, "New Game", true);
        setLayout(new BorderLayout());
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Formation:"));
        panel.add(formationBox);
        panel.add(new JLabel("Dinosaur:"));
        panel.add(dinoBox);
        add(panel, BorderLayout.CENTER);
        JButton ok = new JButton("Start");
        ok.addActionListener(e -> {
            formation = (String) formationBox.getSelectedItem();
            dinosaur = (String) dinoBox.getSelectedItem();
            dispose();
        });
        add(ok, BorderLayout.SOUTH);
        formationBox.addActionListener(e -> loadDinos());
        loadDinos();
        pack();
        setLocationRelativeTo(parent);
    }

    private void loadDinos() {
        String sel = (String) formationBox.getSelectedItem();
        try {
            StatsLoader.load(Path.of("dinosurvival"), sel);
        } catch (IOException ex) {
            // ignore
        }
        String[] dinos = PLAYABLE_DINOS.getOrDefault(sel, new String[0]);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(dinos);
        dinoBox.setModel(model);
        if (model.getSize() > 0) {
            dinoBox.setSelectedIndex(0);
        }
    }

    public String getFormation() {
        return formation;
    }

    public String getDinosaur() {
        return dinosaur;
    }
}
