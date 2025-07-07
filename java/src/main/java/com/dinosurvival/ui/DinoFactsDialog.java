package com.dinosurvival.ui;

import com.dinosurvival.game.Game;
import com.dinosurvival.model.DinosaurStats;
import com.dinosurvival.util.StatsLoader;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Dialog displaying facts about a dinosaur or critter. */
public class DinoFactsDialog extends JDialog {
    public DinoFactsDialog(JFrame parent, Game game, String name) {
        super(parent, name + " Facts", true);
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        add(new JScrollPane(panel), BorderLayout.CENTER);

        ImageIcon img = loadScaledIcon(imagePath(name), 400, 250);
        if (img != null) {
            JLabel imgLbl = new JLabel(img);
            imgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(imgLbl);
        }
        JLabel heading = new JLabel(name);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(heading);

        Object info = StatsLoader.getDinoStats().get(name);
        boolean isDino = true;
        if (info == null) {
            info = StatsLoader.getCritterStats().get(name);
            isDino = false;
        }
        if (info == null) {
            info = Map.of();
        }

        if (isDino) {
            DinosaurStats ds = (DinosaurStats) info;
            iconLabel(panel, "/assets/icons/weight.png", String.format("%.0f kg", ds.getAdultWeight()));
            iconLabel(panel, "/assets/icons/attack.png", String.format("%.0f", ds.getAdultAttack()));
            iconLabel(panel, "/assets/icons/health.png", String.format("%.0f", ds.getAdultHp()));
            iconLabel(panel, "/assets/icons/speed.png", String.format("%.0f", ds.getAdultSpeed()));
            iconLabel(panel, "/assets/icons/turn.png", "Energy drain: " + ds.getAdultEnergyDrain());
            String diet = String.join(", ", ds.getDiet().stream().map(Object::toString).toList());
            label(panel, "Diet: " + diet);
            if (!ds.getAbilities().isEmpty()) {
                label(panel, "Abilities: " + String.join(", ", ds.getAbilities()));
            }
        } else if (info instanceof Map<?,?> map) {
            iconLabel(panel, "/assets/icons/weight.png", String.format("%.0f kg", getDouble(map.get("adult_weight"))));
            iconLabel(panel, "/assets/icons/attack.png", String.format("%.0f", getDouble(map.get("attack"))));
            iconLabel(panel, "/assets/icons/health.png", String.format("%.0f", getDouble(map.get("hp"))));
            iconLabel(panel, "/assets/icons/speed.png", String.format("%.0f", getDouble(map.get("adult_speed"))));
            iconLabel(panel, "/assets/icons/turn.png", "Energy drain: " + getDouble(map.get("adult_energy_drain")));
            Object diet = map.get("diet");
            if (diet instanceof List<?> list && !list.isEmpty()) {
                label(panel, "Diet: " + String.join(", ", list.stream().map(Object::toString).toList()));
            }
            Object abilities = map.get("abilities");
            if (abilities instanceof List<?> list && !list.isEmpty()) {
                label(panel, "Abilities: " + String.join(", ", list.stream().map(Object::toString).toList()));
            }
        }

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel btn = new JPanel();
        btn.add(close);
        add(btn, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
    }

    private static void label(JPanel panel, String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(l);
    }

    private static void iconLabel(JPanel panel, String iconPath, String text) {
        JLabel l = new JLabel(text);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        ImageIcon icon = loadScaledIcon(iconPath, 20, 20);
        if (icon != null) {
            l.setIcon(icon);
        }
        panel.add(l);
    }

    private static String imagePath(String name) {
        String fname = name.toLowerCase().replace(" ", "_") + ".png";
        return "/assets/dinosaurs/" + fname;
    }

    private static double getDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o != null) {
            try {
                return Double.parseDouble(o.toString());
            } catch (NumberFormatException ignored) { }
        }
        return 0.0;
    }

    private static ImageIcon loadScaledIcon(String path, int width, int height) {
        java.net.URL url = DinoFactsDialog.class.getResource(path);
        if (url == null) {
            return null;
        }
        try {
            BufferedImage img = ImageIO.read(url);
            double scaleW = (double) width / img.getWidth();
            double scaleH = (double) height / img.getHeight();
            double scale = Math.max(scaleW, scaleH);
            int newW = (int) Math.round(img.getWidth() * scale);
            int newH = (int) Math.round(img.getHeight() * scale);
            Image scaled = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
            BufferedImage resized = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = resized.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();
            int x = Math.max(0, (newW - width) / 2);
            int y = Math.max(0, (newH - height) / 2);
            BufferedImage cropped = resized.getSubimage(x, y, width, height);
            return new ImageIcon(cropped);
        } catch (IOException ex) {
            return null;
        }
    }
}
