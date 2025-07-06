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
    private final Game game;
    private JLabel[][] mapCells;
    private final Map<String, ImageIcon> biomeImages = new HashMap<>();

    public GameWindow(Game game) {
        super("Dino Survival");
        this.game = game;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        mapPanel.setLayout(new GridLayout(game.getMap().getHeight(), game.getMap().getWidth()));
        add(mapPanel, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        add(right, BorderLayout.EAST);

        JPanel btns = new JPanel(new GridLayout(4, 2, 5, 5));
        JButton north = new JButton("North");
        JButton south = new JButton("South");
        JButton east = new JButton("East");
        JButton west = new JButton("West");
        JButton stay = new JButton("Stay");
        JButton drink = new JButton("Drink");
        JButton threaten = new JButton("Threaten");
        JButton lay = new JButton("Lay Eggs");
        btns.add(north); btns.add(south); btns.add(east); btns.add(west);
        btns.add(stay); btns.add(drink); btns.add(threaten); btns.add(lay);
        right.add(btns);

        JButton stats = new JButton("Stats");
        stats.addActionListener(e -> new StatsDialog(this, game).setVisible(true));
        JButton legacy = new JButton("Legacy Stats");
        legacy.addActionListener(e -> new LegacyStatsDialog(this, "Morrison", game.getPlayer().getName()).setVisible(true));
        JButton quit = new JButton("Quit");
        quit.addActionListener(e -> dispose());
        JPanel misc = new JPanel();
        misc.add(stats); misc.add(legacy); misc.add(quit);
        right.add(misc);

        JScrollPane scroll = new JScrollPane(logArea);
        logArea.setEditable(false);
        scroll.setPreferredSize(new Dimension(200, 150));
        add(scroll, BorderLayout.SOUTH);

        add(biomeLabel, BorderLayout.NORTH);

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
                lbl.setPreferredSize(new Dimension(20, 20));
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
}
