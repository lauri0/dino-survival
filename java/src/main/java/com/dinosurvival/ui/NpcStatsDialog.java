package com.dinosurvival.ui;

import com.dinosurvival.game.Game;
import com.dinosurvival.model.NPCAnimal;

import javax.swing.*;
import java.awt.BorderLayout;

/** Simple dialog showing NPC statistics. */
public class NpcStatsDialog extends JDialog {
    public NpcStatsDialog(JFrame parent, Game game, NPCAnimal npc) {
        super(parent, npc.getName() + " Stats", true);
        JTextArea area = new JTextArea(10, 30);
        area.setEditable(false);
        area.setText(buildText(game, npc));
        add(new JScrollPane(area), BorderLayout.CENTER);
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        add(close, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private String buildText(Game game, NPCAnimal npc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(npc.getName()).append('\n');
        sb.append("Age: ").append(npc.getAge()).append(" turns\n");
        sb.append(String.format("Weight: %.1f\n", npc.getWeight()));
        sb.append(String.format("HP: %.1f/%.1f\n", npc.getHp(), game.npcMaxHp(npc)));
        sb.append(String.format("Energy: %.0f%%\n", npc.getEnergy()));
        sb.append(String.format("Attack: %.1f\n", game.npcEffectiveAttack(npc)));
        sb.append(String.format("Speed: %.1f\n", game.npcEffectiveSpeed(npc)));
        return sb.toString();
    }
}
