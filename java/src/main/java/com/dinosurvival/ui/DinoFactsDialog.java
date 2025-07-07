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
        java.util.List<Integer> counts = game.getPopulationHistory(name);
        JPanel header = new JPanel();
        header.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        boolean haveHeader = false;
        if (img != null) {
            JLabel imgLbl = new JLabel(img);
            header.add(imgLbl);
            haveHeader = true;
        }
        if (counts != null && !counts.isEmpty()) {
            header.add(new PopGraphPanel(counts, game.getTurnHistory()));
            haveHeader = true;
        }
        if (haveHeader) {
            panel.add(header);
        }
        JLabel heading = new JLabel(name);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 18f));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
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

    private static class PopGraphPanel extends JPanel {
        private final java.util.List<Integer> counts;
        private final java.util.List<Integer> turns;
        PopGraphPanel(java.util.List<Integer> counts, java.util.List<Integer> turns) {
            this.counts = counts;
            this.turns = turns;
            setPreferredSize(new Dimension(354, 234));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D graphics2D = (Graphics2D) graphics;
            int width = 320;
            int height = 200;
            int margin = 24;
            int topPad = 10;
            int maxCount = 1;
            for (int count : counts) {
                if (count > maxCount) maxCount = count;
            }
            double step = counts.size() > 1 ? (double) width / (counts.size() - 1) : width;
            graphics2D.setColor(Color.BLACK);
            graphics2D.drawLine(margin, topPad, margin, topPad + height);
            graphics2D.drawLine(margin, topPad + height, margin + width, topPad + height);
            graphics2D.setColor(Color.BLUE);
            for (int countsIndex = 1; countsIndex < counts.size(); countsIndex++) {
                int x1 = (int) Math.round(margin + (countsIndex - 1) * step);
                int y1 = (int) Math.round(topPad + height - counts.get(countsIndex - 1) * height / (double) maxCount);
                int x2 = (int) Math.round(margin + countsIndex * step);
                int y2 = (int) Math.round(topPad + height - counts.get(countsIndex) * height / (double) maxCount);
                graphics2D.drawLine(x1, y1, x2, y2);
            }
            graphics2D.setColor(Color.BLACK);
            graphics2D.drawString("0", margin - 20, topPad + height);
            graphics2D.drawString(String.valueOf(maxCount), margin - 20, topPad + 5);
            if (!turns.isEmpty()) {
                graphics2D.drawString(String.valueOf(turns.get(0)), margin, topPad + height + 15);
                graphics2D.drawString(String.valueOf(turns.get(turns.size() - 1)), margin + width - 20, topPad + height + 15);
            }
            graphics2D.drawString("Turn", margin + width / 2 - 15, topPad + height + 18);
            Graphics2D populationTextGraphics = (Graphics2D) graphics2D.create();
            populationTextGraphics.rotate(-Math.PI / 2);
            populationTextGraphics.drawString("Population", -(topPad + height / 2 + 20), margin - 10);
            populationTextGraphics.dispose();
        }
    }
}
