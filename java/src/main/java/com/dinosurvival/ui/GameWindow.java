package com.dinosurvival.ui;

import com.dinosurvival.game.Game;
import com.dinosurvival.game.Terrain;
import com.dinosurvival.model.Plant;
import com.dinosurvival.model.PlantStats;
import com.dinosurvival.game.EncounterEntry;
import com.dinosurvival.model.NPCAnimal;
import com.dinosurvival.util.StatsLoader;
import com.dinosurvival.ui.DinoFactsDialog;
import com.dinosurvival.ui.GameHelpDialog;
import com.dinosurvival.ui.EncounterHelpDialog;
import com.dinosurvival.ui.LegacyStatsDialog;
import com.dinosurvival.ui.StatsDialog;
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
    private final JPanel populationList = new JPanel();
    private final Map<String, ImageIcon> populationImages = new HashMap<>();
    private final JLabel weatherIconLabel = new JLabel();
    private final JLabel weatherNameLabel = new JLabel();
    private final JLabel weatherEffectLabel = new JLabel();
    private final Map<String, ImageIcon> weatherIcons = new HashMap<>();
    private boolean encounterSortAsc = true;

    // Stats sidebar components
    private final Map<String, ImageIcon> statIcons = new HashMap<>();
    private final JLabel nameLabel = new JLabel();
    private final JLabel attackLabel = new JLabel();
    private final JLabel hpValueLabel = new JLabel();
    private final JProgressBar hpBar = new JProgressBar(0, 100);
    private final JProgressBar energyBar = new JProgressBar(0, 100);
    private final JProgressBar hydrationBar = new JProgressBar(0, 100);
    private final JLabel weightLabel = new JLabel();
    private final JLabel speedLabel = new JLabel();
    private final JLabel descendantLabel = new JLabel();
    private final JLabel turnLabel = new JLabel();

    private static final int TILE_SIZE = 22;

    private final JButton northButton = new JButton("North");
    private final JButton southButton = new JButton("South");
    private final JButton eastButton = new JButton("East");
    private final JButton westButton = new JButton("West");
    private final JButton stayButton = new JButton("Stay");
    private final JButton drinkButton = new JButton("Drink");
    private final JButton threatenButton = new JButton("Threaten");
    private final JButton layButton = new JButton("Lay Eggs");
    private final JButton infoButton = new JButton("Info");
    private final JButton playerStatsButton = new JButton("Player Stats");
    private final JButton dinoStatsButton = new JButton("Dinosaur Stats");
    private final JButton helpButton = new JButton("Help");

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

    private String formatBiomeName(String name) {
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            sb.append(Character.toUpperCase(parts[i].charAt(0)))
              .append(parts[i].substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
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
        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
        infoRow.add(infoButton);
        infoRow.add(dinoStatsButton);
        infoRow.add(playerStatsButton);
        infoRow.add(helpButton);
        dinoPanel.add(infoRow, BorderLayout.SOUTH);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        main.add(dinoPanel, c);

        // Stats sidebar (row 1, column 0)
        JPanel statsPanel = new JPanel();
        statsPanel.setPreferredSize(new Dimension(200, 200));
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 16f));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(nameLabel);

        JPanel attackRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel attackIcon = new JLabel(loadScaledIcon("/assets/icons/attack.png", 20, 20));
        attackRow.add(attackIcon);
        attackRow.add(attackLabel);
        attackRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(attackRow);

        JPanel hpRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel hpIcon = new JLabel(loadScaledIcon("/assets/icons/health.png", 20, 20));
        hpRow.add(hpIcon);
        hpRow.add(hpValueLabel);
        hpBar.setPreferredSize(new Dimension(100, 15));
        hpBar.setStringPainted(true);
        hpRow.add(hpBar);
        hpRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(hpRow);

        JPanel energyRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel energyIcon = new JLabel(loadScaledIcon("/assets/icons/energy.png", 20, 20));
        energyRow.add(energyIcon);
        energyBar.setPreferredSize(new Dimension(100, 15));
        energyBar.setStringPainted(true);
        energyRow.add(energyBar);
        energyRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(energyRow);

        JPanel hydrationRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel hydIcon = new JLabel(loadScaledIcon("/assets/icons/hydration.png", 20, 20));
        hydrationRow.add(hydIcon);
        hydrationBar.setPreferredSize(new Dimension(100, 15));
        hydrationBar.setStringPainted(true);
        hydrationRow.add(hydrationBar);
        hydrationRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(hydrationRow);

        JPanel weightRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel weightIcon = new JLabel(loadScaledIcon("/assets/icons/weight.png", 20, 20));
        weightRow.add(weightIcon);
        weightRow.add(weightLabel);
        weightRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(weightRow);

        JPanel speedRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel speedIcon = new JLabel(loadScaledIcon("/assets/icons/speed.png", 20, 20));
        speedRow.add(speedIcon);
        speedRow.add(speedLabel);
        speedRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(speedRow);

        JPanel descRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel descIcon = new JLabel(loadScaledIcon("/assets/icons/descendant.png", 20, 20));
        descRow.add(descIcon);
        descRow.add(descendantLabel);
        descRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(descRow);

        JPanel turnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel turnIcon = new JLabel(loadScaledIcon("/assets/icons/turn.png", 20, 20));
        turnRow.add(turnIcon);
        turnRow.add(turnLabel);
        turnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(turnRow);

        JButton quit = new JButton("Quit");
        quit.addActionListener(e -> dispose());
        quit.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.add(Box.createVerticalStrut(5));
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
        JButton encHelp = new JButton("Help");
        encHelp.addActionListener(e -> new EncounterHelpDialog(this).setVisible(true));
        JPanel encHeader = new JPanel(new BorderLayout());
        encHeader.add(encLabel, BorderLayout.WEST);
        JPanel encBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        encBtns.add(encHelp);
        encBtns.add(sortBtn);
        encHeader.add(encBtns, BorderLayout.EAST);
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
        weatherPanel.setLayout(new BoxLayout(weatherPanel, BoxLayout.Y_AXIS));
        weatherIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        weatherNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        weatherNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        weatherEffectLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        weatherPanel.add(weatherIconLabel);
        weatherPanel.add(weatherNameLabel);
        weatherPanel.add(weatherEffectLabel);
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        main.add(weatherPanel, c);

        // Population tracker (row 1, column 3)
        JPanel popPanel = new JPanel(new BorderLayout());
        popPanel.setPreferredSize(new Dimension(200, 200));
        JLabel popLabel = new JLabel("Global Population");
        popPanel.add(popLabel, BorderLayout.NORTH);
        populationList.setLayout(new BoxLayout(populationList, BoxLayout.Y_AXIS));
        JScrollPane popScroll = new JScrollPane(populationList);
        popPanel.add(popScroll, BorderLayout.CENTER);
        c.gridx = 3;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 1;
        main.add(popPanel, c);

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
        infoButton.addActionListener(e -> new DinoFactsDialog(this, game, game.getPlayer().getName()).setVisible(true));
        playerStatsButton.addActionListener(e -> new StatsDialog(this, game).setVisible(true));
        dinoStatsButton.addActionListener(e -> new LegacyStatsDialog(this, game.getFormation(), game.getPlayer().getName()).setVisible(true));
        helpButton.addActionListener(e -> new GameHelpDialog(this).setVisible(true));

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
        refreshMap();
        updateBiomeImage();
        updateDinoImage();
        updateStatsPanel();
        updateActionButtons();
        updatePlantList();
        updateEncounterList();
        updateWeatherPanel();
        updatePopulationList();
        checkGameEnd();
    }

    private void disableActionButtons() {
        northButton.setEnabled(false);
        southButton.setEnabled(false);
        eastButton.setEnabled(false);
        westButton.setEnabled(false);
        stayButton.setEnabled(false);
        drinkButton.setEnabled(false);
        threatenButton.setEnabled(false);
        layButton.setEnabled(false);
    }

    private void checkGameEnd() {
        if (game.getPlayer().getHp() <= 0 || game.hasWon()) {
            disableActionButtons();
        }
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
        updateStatsPanel();
        updateActionButtons();
        updatePlantList();
        updateEncounterList();
        updateWeatherPanel();
        updatePopulationList();
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
        int px = game.getPlayerX();
        int py = game.getPlayerY();
        Terrain t = game.getMap().terrainAt(px, py);
        ImageIcon icon = biomeImages.get(t.getName());
        if (icon == null) {
            icon = loadScaledIcon("/assets/biomes/" + t.getName() + ".png", 400, 250);
            if (icon != null) {
                biomeImages.put(t.getName(), icon);
            }
        }
        biomeLabel.setIcon(icon);
        String label = formatBiomeName(t.getName());
        if (game.getMap().hasNest(px, py)) {
            label += " (Nest)";
        }
        label += " (" + px + "," + py + ")";
        biomeLabel.setText(label);
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

    private void updateStatsPanel() {
        var p = game.getPlayer();
        nameLabel.setText(p.getName());
        attackLabel.setText(String.format("%.1f", game.playerEffectiveAttack()));

        double maxHp = p.getMaxHp();
        double hpPct = maxHp > 0 ? p.getHp() / maxHp * 100.0 : 0.0;
        hpValueLabel.setText(String.format("%.1f/%.1f", p.getHp(), maxHp));
        hpBar.setValue((int) Math.round(hpPct));
        hpBar.setString(String.format("%.0f%%", hpPct));

        energyBar.setValue((int) Math.round(p.getEnergy()));
        energyBar.setString(String.format("%.0f%%", p.getEnergy()));

        hydrationBar.setValue((int) Math.round(p.getHydration()));
        hydrationBar.setString(String.format("%.0f%%", p.getHydration()));

        double hatch = p.getHatchlingWeight();
        double adult = p.getAdultWeight();
        double pct = adult - hatch > 0 ? (p.getWeight() - hatch) / (adult - hatch) * 100.0 : 100.0;
        weightLabel.setText(String.format("%.1fkg/%.0fkg (%.1f%%)", p.getWeight(), adult, pct));

        speedLabel.setText(String.format("%.1f", game.playerEffectiveSpeed()));

        descendantLabel.setText(String.valueOf(game.descendantCount()));
        turnLabel.setText(String.valueOf(game.getTurn()));
    }

    private void updateWeatherPanel() {
        var w = game.getWeather();
        weatherNameLabel.setText(w.getName());
        ImageIcon icon = weatherIcons.get(w.getName());
        if (icon == null && w.getIcon() != null && !w.getIcon().isEmpty()) {
            icon = loadScaledIcon("/" + w.getIcon(), 128, 128);
            if (icon != null) {
                weatherIcons.put(w.getName(), icon);
            }
        }
        weatherIconLabel.setIcon(icon);
        java.util.List<String> effects = new java.util.ArrayList<>();
        if (Math.abs(w.getPlayerHydrationMult() - 1.0) > 0.01) {
            effects.add(String.format("Hydration loss x%.2g", w.getPlayerHydrationMult()));
        }
        if (Math.abs(w.getPlayerEnergyMult() - 1.0) > 0.01) {
            effects.add(String.format("Energy loss x%.2g", w.getPlayerEnergyMult()));
        }
        if (w.getFloodChance() > 0) {
            effects.add(String.format("Flood chance %d%%", (int) (w.getFloodChance() * 100)));
        }
        if (effects.isEmpty()) {
            weatherEffectLabel.setText("");
        } else {
            weatherEffectLabel.setText("<html>" + String.join("<br>", effects) + "</html>");
        }
    }

    private void updatePopulationList() {
        populationList.removeAll();
        java.util.Map<String, Integer> counts = game.populationStats();
        int total = 0;
        for (int c : counts.values()) {
            total += c;
        }
        java.util.List<java.util.Map.Entry<String, Integer>> list =
                new java.util.ArrayList<>(counts.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        for (java.util.Map.Entry<String, Integer> e : list) {
            String name = e.getKey();
            int count = e.getValue();
            double pct = total > 0 ? count * 100.0 / total : 0.0;
            JPanel row = new JPanel(new BorderLayout());
            JLabel img = new JLabel();
            ImageIcon icon = populationImages.get(name);
            if (icon == null) {
                String path = "/assets/dinosaurs/" + name.toLowerCase().replace(" ", "_") + ".png";
                icon = loadScaledIcon(path, 40, 25);
                if (icon != null) {
                    populationImages.put(name, icon);
                }
            }
            if (icon != null) {
                img.setIcon(icon);
            }
            row.add(img, BorderLayout.WEST);
            row.add(new JLabel(name + " " + count + String.format(" (%.1f%%)", pct)), BorderLayout.CENTER);
            JButton infoBtn = new JButton("Info");
            infoBtn.addActionListener(ev -> new NpcStatsDialog(this, game, sampleNpc(name)).setVisible(true));
            row.add(infoBtn, BorderLayout.EAST);
            populationList.add(row);
        }
        populationList.revalidate();
        populationList.repaint();
    }

    private NPCAnimal sampleNpc(String name) {
        NPCAnimal npc = new NPCAnimal();
        npc.setName(name);
        var ds = StatsLoader.getDinoStats().get(name);
        if (ds != null) {
            npc.setWeight(ds.getAdultWeight());
            npc.setMaxHp(ds.getAdultHp());
            npc.setHp(npc.getMaxHp());
            npc.setAttack(ds.getAdultAttack());
            npc.setSpeed(ds.getAdultSpeed());
        } else {
            java.util.Map<String, Object> map = StatsLoader.getCritterStats().get(name);
            if (map != null) {
                Object w = map.get("adult_weight");
                if (w instanceof Number n) npc.setWeight(n.doubleValue());
                Object hp = map.get("hp");
                if (hp instanceof Number n) {
                    npc.setMaxHp(n.doubleValue());
                    npc.setHp(n.doubleValue());
                }
                Object atk = map.get("attack");
                if (atk instanceof Number n) npc.setAttack(n.doubleValue());
                Object sp = map.get("adult_speed");
                if (sp instanceof Number n) npc.setSpeed(n.doubleValue());
            }
        }
        return npc;
    }
}
