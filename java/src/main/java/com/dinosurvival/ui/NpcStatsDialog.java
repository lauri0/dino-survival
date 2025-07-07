package com.dinosurvival.ui;

import com.dinosurvival.game.Game;
import com.dinosurvival.model.NPCAnimal;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Map;

/** Simple dialog showing NPC statistics. */
public class NpcStatsDialog extends JDialog {
    public NpcStatsDialog(JFrame parent, Game game, NPCAnimal npc) {
        super(parent, npc.getName() + " Stats", true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        add(new JScrollPane(panel), BorderLayout.CENTER);

        ImageIcon img = loadScaledIcon(imagePath(npc.getName()), 400, 250, !npc.isAlive());
        if (img != null) {
            JLabel imgLbl = new JLabel(img);
            imgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(imgLbl);
        }

        JLabel heading = new JLabel(npc.getName());
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(heading);

        label(panel, "Age: " + npc.getAge() + " turns");
        double hpMax = game.npcMaxHp(npc);
        iconLabel(panel, "/assets/icons/health.png",
                String.format("%.1f/%.1f", npc.getHp(), hpMax));
        iconLabel(panel, "/assets/icons/energy.png",
                String.format("%.0f%%", npc.getEnergy()));

        String abil = String.join(", ", npc.getAbilities());
        label(panel, "Abilities: " + (abil.isEmpty() ? "None" : abil));

        if (!npc.getHunts().isEmpty()) {
            label(panel, "Successful Hunts:");
            npc.getHunts().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> label(panel, "  " + e.getKey() + ": " + e.getValue()));
        } else {
            label(panel, "Successful Hunts: None");
        }

        label(panel, "Egg clusters eaten: " + npc.getEggClustersEaten());
        iconLabel(panel, "/assets/icons/weight.png",
                String.format("%.1f", npc.getWeight()));
        iconLabel(panel, "/assets/icons/attack.png",
                String.format("%.1f", game.npcEffectiveAttack(npc)));
        iconLabel(panel, "/assets/icons/speed.png",
                String.format("%.1f", game.npcEffectiveSpeed(npc)));

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel btn = new JPanel();
        btn.add(close);
        add(btn, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private static void label(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
    }

    private static void iconLabel(JPanel p, String iconPath, String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        ImageIcon icon = loadScaledIcon(iconPath, 20, 20, false);
        if (icon != null) {
            l.setIcon(icon);
        }
        p.add(l);
    }

    private static String imagePath(String name) {
        return "/assets/dinosaurs/" + name.toLowerCase().replace(" ", "_") + ".png";
    }

    private static ImageIcon loadScaledIcon(String path, int w, int h, boolean gray) {
        java.net.URL url = NpcStatsDialog.class.getResource(path);
        if (url == null) return null;
        try {
            BufferedImage img = ImageIO.read(url);
            if (gray) {
                java.awt.image.ColorConvertOp op = new java.awt.image.ColorConvertOp(
                        java.awt.color.ColorSpace.getInstance(java.awt.color.ColorSpace.CS_GRAY), null);
                img = op.filter(img, null);
            }
            double scaleW = (double) w / img.getWidth();
            double scaleH = (double) h / img.getHeight();
            double scale = Math.max(scaleW, scaleH);
            int newW = (int) Math.round(img.getWidth() * scale);
            int newH = (int) Math.round(img.getHeight() * scale);
            Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = resized.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();
            int x = Math.max(0, (newW - w) / 2);
            int y = Math.max(0, (newH - h) / 2);
            BufferedImage cropped = resized.getSubimage(x, y, w, h);
            return new ImageIcon(cropped);
        } catch (IOException ex) {
            return null;
        }
    }
}
