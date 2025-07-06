package com.dinosurvival.ui;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Terrain;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/** Main game window showing the map, controls and log output. */
public class GameWindow extends JFrame {
    private final JTextArea logArea = new JTextArea();
    private final JPanel mapPanel = new JPanel();
    private final JLabel biomeLabel = new JLabel();
    private final JLabel dinoImageLabel = new JLabel();
    private final Game game;
    private JLabel[][] mapCells;
    private final Map<String, ImageIcon> biomeImages = new HashMap<>();

    public GameWindow(Game game) {
        super("Dino Survival");
        this.game = game;
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel main = new JPanel(new GridBagLayout());
        setContentPane(main);

        mapPanel.setLayout(new GridLayout(game.getMap().getHeight(), game.getMap().getWidth()));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.BOTH;

        // Dinosaur image (row 0, column 0)
        dinoImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel dinoPanel = new JPanel(new BorderLayout());
        dinoPanel.setPreferredSize(new Dimension(200, 250));
        dinoPanel.add(dinoImageLabel, BorderLayout.CENTER);
        String dName = game.getPlayer().getName();
        String imgPath = "/assets/dinosaurs/" + dName.toLowerCase().replace(" ", "_") + ".png";
        java.net.URL dUrl = getClass().getResource(imgPath);
        if (dUrl != null) {
            dinoImageLabel.setIcon(new ImageIcon(dUrl));
        }
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        main.add(dinoPanel, c);

        // Stats and misc buttons under the dinosaur image (row 1, column 0)
        JPanel statsPanel = new JPanel();
        statsPanel.setPreferredSize(new Dimension(200, 200));
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        JButton stats = new JButton("Stats");
        stats.addActionListener(e -> new StatsDialog(this, game).setVisible(true));
        JButton legacy = new JButton("Legacy Stats");
        legacy.addActionListener(e -> new LegacyStatsDialog(this, "Morrison", game.getPlayer().getName()).setVisible(true));
        JButton quit = new JButton("Quit");
        quit.addActionListener(e -> dispose());
        statsPanel.add(stats);
        statsPanel.add(legacy);
        statsPanel.add(quit);
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 1;
        main.add(statsPanel, c);

        // Biome display (row 0, column 1)
        JPanel biomePanel = new JPanel(new BorderLayout());
        biomePanel.setPreferredSize(new Dimension(200, 250));
        biomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        biomePanel.add(biomeLabel, BorderLayout.CENTER);
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        main.add(biomePanel, c);

        // Movement buttons and plant list (row 1, column 1)
        JPanel btnFrame = new JPanel(new BorderLayout());
        btnFrame.setPreferredSize(new Dimension(200, 200));
        JPanel btns = new JPanel(new GridLayout(4, 2, 5, 5));
        JButton north = new JButton("North");
        JButton south = new JButton("South");
        JButton east = new JButton("East");
        JButton west = new JButton("West");
        JButton stay = new JButton("Stay");
        JButton drink = new JButton("Drink");
        JButton threaten = new JButton("Threaten");
        JButton lay = new JButton("Lay Eggs");
        btns.add(north);
        btns.add(south);
        btns.add(east);
        btns.add(west);
        btns.add(stay);
        btns.add(drink);
        btns.add(threaten);
        btns.add(lay);
        btnFrame.add(btns, BorderLayout.NORTH);
        JPanel plantList = new JPanel();
        btnFrame.add(plantList, BorderLayout.CENTER);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 1;
        main.add(btnFrame, c);

        // Map (row 0, column 2)
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 1;
        c.weighty = 0;
        main.add(mapPanel, c);

        // Encounter list (row 1, column 2)
        JTextArea encounterArea = new JTextArea();
        encounterArea.setEditable(false);
        JScrollPane encounterScroll = new JScrollPane(encounterArea);
        c.gridx = 2;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        main.add(encounterScroll, c);

        // Weather info (row 0, column 3)
        JPanel weatherPanel = new JPanel();
        weatherPanel.setPreferredSize(new Dimension(200, 250));
        JLabel weatherLabel = new JLabel();
        JLabel weatherInfo = new JLabel();
        weatherLabel.setHorizontalAlignment(SwingConstants.CENTER);
        weatherPanel.setLayout(new BoxLayout(weatherPanel, BoxLayout.Y_AXIS));
        weatherPanel.add(weatherLabel);
        weatherPanel.add(weatherInfo);
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        main.add(weatherPanel, c);

        // Population tracker (row 1, column 3)
        JTextArea popArea = new JTextArea();
        popArea.setEditable(false);
        JScrollPane popScroll = new JScrollPane(popArea);
        popScroll.setPreferredSize(new Dimension(200, 200));
        c.gridx = 3;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 1;
        main.add(popScroll, c);

        // Log area at the bottom spanning two columns
        JScrollPane scroll = new JScrollPane(logArea);
        logArea.setEditable(false);
        scroll.setPreferredSize(new Dimension(400, 150));
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.weightx = 0;
        c.weighty = 0;
        main.add(scroll, c);
        c.gridwidth = 1;

        // Wire button actions
        north.addActionListener(e -> doAction(() -> game.moveNorth(), "Moved north"));
        south.addActionListener(e -> doAction(() -> game.moveSouth(), "Moved south"));
        east.addActionListener(e -> doAction(() -> game.moveEast(), "Moved east"));
        west.addActionListener(e -> doAction(() -> game.moveWest(), "Moved west"));
        stay.addActionListener(e -> doAction(() -> game.rest(), "Stayed"));
        drink.addActionListener(e -> doAction(() -> game.drink(), "Drink"));
        threaten.addActionListener(e -> doAction(() -> game.threaten(), "Threaten"));
        lay.addActionListener(e -> doAction(() -> game.layEggs(), "Lay eggs"));

        buildMap();
        refreshAll();
        pack();
        setLocationRelativeTo(null);
    }

    private void doAction(Runnable r, String msg) {
        r.run();
        log(msg);
        refreshAll();
    }

    public void log(String msg) {
        logArea.append(msg + "\n");
    }

    private void buildMap() {
        com.dinosurvival.game.Map gmap = game.getMap();
        mapCells = new JLabel[gmap.getHeight()][gmap.getWidth()];
        mapPanel.removeAll();
        for (int y = 0; y < gmap.getHeight(); y++) {
            for (int x = 0; x < gmap.getWidth(); x++) {
                JLabel lbl = new JLabel(" ", SwingConstants.CENTER);
                lbl.setOpaque(true);
                lbl.setPreferredSize(new Dimension(22, 22));
                mapPanel.add(lbl);
                mapCells[y][x] = lbl;
            }
        }
    }

    private Color colorForTerrain(Terrain t) {
        return switch (t) {
            case FOREST -> new Color(0, 100, 0);
            case PLAINS -> new Color(173, 255, 47);
            case SWAMP -> new Color(107, 142, 35);
            case WOODLANDS -> new Color(50, 205, 50);
            case DESERT -> Color.YELLOW;
            case TOXIC_BADLANDS -> new Color(128, 128, 0);
            case LAKE -> Color.BLUE;
            case MOUNTAIN -> new Color(210, 180, 140);
            case VOLCANO, VOLCANO_ERUPTING -> Color.BLACK;
            case LAVA -> Color.RED;
            case SOLIDIFIED_LAVA_FIELD -> Color.DARK_GRAY;
            default -> Color.GRAY;
        };
    }

    private void refreshAll() {
        refreshMap();
        updateBiomeImage();
        updateDinoImage();
    }

    private void refreshMap() {
        com.dinosurvival.game.Map gmap = game.getMap();
        for (int y = 0; y < gmap.getHeight(); y++) {
            for (int x = 0; x < gmap.getWidth(); x++) {
                JLabel lbl = mapCells[y][x];
                boolean reveal = gmap.isRevealed(x, y);
                Color c = reveal ? colorForTerrain(gmap.terrainAt(x, y)) : Color.GRAY;
                lbl.setBackground(c);
                lbl.setText((game.getPlayerX() == x && game.getPlayerY() == y) ? "P" : " ");
            }
        }
        mapPanel.revalidate();
        mapPanel.repaint();
    }

    private void updateBiomeImage() {
        Terrain t = game.getMap().terrainAt(game.getPlayerX(), game.getPlayerY());
        ImageIcon icon = biomeImages.get(t.getName());
        if (icon == null) {
            java.net.URL url = getClass().getResource("/assets/biomes/" + t.getName() + ".png");
            if (url != null) {
                icon = new ImageIcon(url);
                biomeImages.put(t.getName(), icon);
            }
        }
        biomeLabel.setIcon(icon);
        biomeLabel.setText(t.getName());
    }

    private void updateDinoImage() {
        String dName = game.getPlayer().getName();
        String path = "/assets/dinosaurs/" + dName.toLowerCase().replace(" ", "_") + ".png";
        java.net.URL url = getClass().getResource(path);
        if (url != null) {
            dinoImageLabel.setIcon(new ImageIcon(url));
        }
        dinoImageLabel.setText(dName);
    }
}
