package com.dinosurvival.ui;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Terrain;
import com.dinosurvival.model.Plant;
import com.dinosurvival.model.PlantStats;
import com.dinosurvival.game.EncounterEntry;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
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
    private final Map<String, ImageIcon> plantImages = new HashMap<>();
    private final JPanel plantList = new JPanel();
    private final JPanel encounterList = new JPanel();
    private final Map<String, ImageIcon> npcImages = new HashMap<>();
    private boolean encounterSortAsc = true;

    private static final int TILE_SIZE = 22;

    private final JButton northButton = new JButton("North");
    private final JButton southButton = new JButton("South");
    private final JButton eastButton = new JButton("East");
    private final JButton westButton = new JButton("West");
    private final JButton stayButton = new JButton("Stay");
    private final JButton drinkButton = new JButton("Drink");
    private final JButton threatenButton = new JButton("Threaten");
    private final JButton layButton = new JButton("Lay Eggs");

    private ImageIcon loadScaledIcon(String path, int width, int height) {
        java.net.URL url = getClass().getResource(path);
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
        dinoPanel.setPreferredSize(new Dimension(400, 250));
        dinoPanel.add(dinoImageLabel, BorderLayout.CENTER);
        String dName = game.getPlayer().getName();
        String imgPath = "/assets/dinosaurs/" + dName.toLowerCase().replace(" ", "_") + ".png";
        ImageIcon dIcon = loadScaledIcon(imgPath, 400, 250);
        if (dIcon != null) {
            dinoImageLabel.setIcon(dIcon);
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
        biomePanel.setPreferredSize(new Dimension(400, 250));
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
        JPanel btns = new JPanel(new GridLayout(3, 3, 5, 5));
        btns.add(layButton);
        btns.add(northButton);
        btns.add(drinkButton);
        btns.add(westButton);
        btns.add(stayButton);
        btns.add(eastButton);
        btns.add(new JLabel());
        btns.add(southButton);
        btns.add(threatenButton);
        btnFrame.add(btns, BorderLayout.NORTH);
        plantList.setLayout(new BoxLayout(plantList, BoxLayout.Y_AXIS));
        btnFrame.add(new JScrollPane(plantList), BorderLayout.CENTER);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 1;
        main.add(btnFrame, c);

        // Map (row 0, column 2)
        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.fill = GridBagConstraints.NONE;
        main.add(mapPanel, c);
        c.fill = GridBagConstraints.BOTH;

        // Encounter list (row 1, column 2)
        JPanel encounterPanel = new JPanel(new BorderLayout());
        JLabel encLabel = new JLabel("Encounters");
        JButton sortBtn = new JButton("Sort");
        sortBtn.addActionListener(e -> {
            encounterSortAsc = !encounterSortAsc;
            updateEncounterList();
        });
        JPanel encHeader = new JPanel(new BorderLayout());
        encHeader.add(encLabel, BorderLayout.WEST);
        encHeader.add(sortBtn, BorderLayout.EAST);
        encounterPanel.add(encHeader, BorderLayout.NORTH);
        encounterList.setLayout(new BoxLayout(encounterList, BoxLayout.Y_AXIS));
        JScrollPane encounterScroll = new JScrollPane(encounterList);
        encounterPanel.add(encounterScroll, BorderLayout.CENTER);
        c.gridx = 2;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        main.add(encounterPanel, c);

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
        northButton.addActionListener(e -> doAction(() -> game.moveNorth(), "Moved north"));
        southButton.addActionListener(e -> doAction(() -> game.moveSouth(), "Moved south"));
        eastButton.addActionListener(e -> doAction(() -> game.moveEast(), "Moved east"));
        westButton.addActionListener(e -> doAction(() -> game.moveWest(), "Moved west"));
        stayButton.addActionListener(e -> doAction(() -> game.rest(), "Stayed"));
        drinkButton.addActionListener(e -> doAction(() -> game.drink(), "Drink"));
        threatenButton.addActionListener(e -> doAction(() -> game.threaten(), "Threaten"));
        layButton.addActionListener(e -> doAction(() -> game.layEggs(), "Lay eggs"));

        buildMap();
        refreshAll();
        pack();
        setMinimumSize(new Dimension(1700, 1000));
        setSize(1700, 1000);
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
                lbl.setPreferredSize(new Dimension(TILE_SIZE, TILE_SIZE));
                mapPanel.add(lbl);
                mapCells[y][x] = lbl;
            }
        }
        int prefW = gmap.getWidth() * TILE_SIZE;
        int prefH = gmap.getHeight() * TILE_SIZE;
        mapPanel.setPreferredSize(new Dimension(prefW, prefH));
        mapPanel.setMinimumSize(new Dimension(prefW, prefH));
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
        updateActionButtons();
        updatePlantList();
        updateEncounterList();
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
            icon = loadScaledIcon("/assets/biomes/" + t.getName() + ".png", 400, 250);
            if (icon != null) {
                biomeImages.put(t.getName(), icon);
            }
        }
        biomeLabel.setIcon(icon);
        biomeLabel.setText(t.getName());
    }

    private void updateDinoImage() {
        String dName = game.getPlayer().getName();
        String path = "/assets/dinosaurs/" + dName.toLowerCase().replace(" ", "_") + ".png";
        ImageIcon icon = loadScaledIcon(path, 400, 250);
        if (icon != null) {
            dinoImageLabel.setIcon(icon);
        }
        dinoImageLabel.setText(dName);
    }

    private void updateActionButtons() {
        Terrain t = game.getMap().terrainAt(game.getPlayerX(), game.getPlayerY());
        drinkButton.setEnabled(t == Terrain.LAKE);
        layButton.setEnabled(game.playerCanLayEggs());
    }

    private void updatePlantList() {
        plantList.removeAll();
        for (Plant p : game.getCurrentPlants()) {
            JPanel row = new JPanel();
            row.setLayout(new FlowLayout(FlowLayout.LEFT));
            JLabel img = new JLabel();
            PlantStats stats = StatsLoader.getPlantStats().get(p.getName());
            if (stats != null) {
                ImageIcon icon = plantImages.get(p.getName());
                if (icon == null) {
                    String path = stats.getImage();
                    if (path != null && !path.isEmpty()) {
                        icon = loadScaledIcon("/" + path, 100, 63);
                        if (icon != null) {
                            plantImages.put(p.getName(), icon);
                        }
                    }
                }
                if (icon != null) {
                    img.setIcon(icon);
                }
            }
            JLabel name = new JLabel(p.getName());
            JLabel weight = new JLabel(String.format("W:%.1fkg", p.getWeight()));
            row.add(img);
            row.add(name);
            row.add(weight);
            plantList.add(row);
        }
        plantList.revalidate();
        plantList.repaint();
    }

    private double entryWeight(EncounterEntry e) {
        if (e.getNpc() != null) {
            return e.getNpc().getWeight();
        } else if (e.getEggs() != null) {
            return e.getEggs().getWeight();
        }
        return 0.0;
    }

    private void updateEncounterList() {
        encounterList.removeAll();
        java.util.List<EncounterEntry> entries = new java.util.ArrayList<>(game.getCurrentEncounters());
        entries.sort(java.util.Comparator.comparingDouble(this::entryWeight));
        if (!encounterSortAsc) {
            java.util.Collections.reverse(entries);
        }
        for (EncounterEntry e : entries) {
            JPanel row = new JPanel(new BorderLayout());
            JLabel img = new JLabel();
            JPanel info = new JPanel();
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
            row.add(img, BorderLayout.WEST);
            row.add(info, BorderLayout.CENTER);
            JButton actBtn = new JButton();
            JButton statsBtn = new JButton("Stats");
            JPanel btns = new JPanel();
            btns.setLayout(new GridLayout(2,1));
            btns.add(statsBtn);
            btns.add(actBtn);
            row.add(btns, BorderLayout.EAST);

            if (e.getBurrow() != null) {
                ImageIcon bIcon = loadScaledIcon("/assets/other/burrow.png", 100, 70);
                if (bIcon != null) img.setIcon(bIcon);
                info.add(new JLabel("Burrow" + (e.getBurrow().isFull() ? " (Full)" : " (Empty)")));
                info.add(new JLabel(String.format("Dig: %.0f%%", e.getBurrow().getProgress())));
                actBtn.setText("Dig");
                actBtn.addActionListener(ev -> doAction(() -> game.digBurrow(), "Dig"));
                statsBtn.setVisible(false);
            } else if (e.getEggs() != null) {
                info.add(new JLabel(e.getEggs().getSpecies() + " Eggs"));
                info.add(new JLabel(String.format("W: %.1fkg", e.getEggs().getWeight())));
                actBtn.setText("Eat");
                actBtn.addActionListener(ev -> doAction(() -> game.collectEggs(), "Eat eggs"));
                statsBtn.setVisible(false);
            } else if (e.getNpc() != null) {
                NPCAnimal npc = e.getNpc();
                String name = npc.getName();
                ImageIcon icon = npcImages.get(name);
                if (icon == null) {
                    String path = "/assets/dinosaurs/" + name.toLowerCase().replace(" ", "_") + ".png";
                    icon = loadScaledIcon(path, 100, 70);
                    if (icon != null) {
                        npcImages.put(name, icon);
                    }
                }
                if (icon != null) img.setIcon(icon);
                info.add(new JLabel(name + " (" + npc.getId() + ")"));
                info.add(new JLabel(String.format("A: %.1f  HP: %.1f/%.1f", game.npcEffectiveAttack(npc), npc.getHp(), game.npcMaxHp(npc))));
                info.add(new JLabel(String.format("S: %.1f  W: %.1fkg", game.npcEffectiveSpeed(npc), npc.getWeight())));
                actBtn.setText(npc.isAlive() ? "Attack" : "Eat");
                actBtn.addActionListener(ev -> doAction(() -> game.huntNpc(npc.getId()), "Hunt"));
                statsBtn.addActionListener(ev -> new NpcStatsDialog(this, game, npc).setVisible(true));
            }

            encounterList.add(row);
        }
        encounterList.revalidate();
        encounterList.repaint();
    }
}
