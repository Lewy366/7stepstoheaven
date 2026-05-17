import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

/**
 * Top-level window and screen manager.
 * It builds the main menu, embeds the active Generator panel, and receives
 * callbacks when the player dies, returns to menu, or advances levels.
 */
public class GameUI extends JFrame {

    // --- Colors ---
    private static final Color BG_DARK     = new Color(10, 10, 18);
    private static final Color ACCENT_BLUE = new Color(58, 58, 255);
    private static final Color GREEN_START = new Color(10, 255, 110);
    private static final Color RED_QUIT    = new Color(255, 58, 92);
    private static final Color YELLOW      = new Color(255, 200, 0);
    private static final Color TEXT_LIGHT  = new Color(232, 232, 255);
    private static final Color TEXT_MUTED  = new Color(106, 106, 154);

    // Color per difficulty level (index matches Difficulty.getLevel()-1)
    private static final Color[] DIFF_COLORS = { GREEN_START, YELLOW, RED_QUIT };

    private Difficulty selectedDifficulty = Difficulty.NORMAL;

    private final CardLayout cardLayout;
    private final JPanel     mainPanel;

    // Game embed container and generator panel
    private JPanel  gameContainer;
    private JPanel  gameScreen;
    private JPanel  infoPanel;
    private Generator gamePanel;
    private int currentLevel = 1;
    private Image currentBackgroundImage;

    // Game-screen labels updated on start
    // These labels live outside Generator and show the selected run settings.
    private JLabel gameInfoLabel;
    private JLabel gameStarsLabel;
    private JLabel gameDiffLabel;
    private JLabel gameOverStatsLabel;

    public GameUI() {
        setTitle("7StepsToHeaven");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1920, 1080);
        setLocationRelativeTo(null);
        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        cardLayout = new CardLayout();
        // CardLayout swaps between MENU, GAME, and GAME_OVER without creating new windows.
        mainPanel  = new JPanel(cardLayout);
        mainPanel.setOpaque(false);

        mainPanel.add(createMainMenu(),    "MENU");
        mainPanel.add(createGameView(), "GAME");
        mainPanel.add(createGameOverView(), "GAME_OVER");

        setContentPane(new BackgroundPanel());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    private JPanel createMainMenu() {
        // Main menu lets the player choose difficulty before creating a level.
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JLabel version = createLabel("Version 1.0", 10, TEXT_MUTED, Font.PLAIN);
        JLabel title = createLabel("7StepsToHeaven", 28, TEXT_LIGHT, Font.BOLD);
        JLabel sub     = createLabel("Get to heaven", 12, TEXT_MUTED, Font.PLAIN);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sectionLabel = createLabel("Difficulty", 9, TEXT_MUTED, Font.PLAIN);
        sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel diffRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        diffRow.setOpaque(false);
        diffRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descriptionLabel = createLabel(selectedDifficulty.getDescription(), 11, TEXT_MUTED, Font.PLAIN);
        JLabel starsLabel = createLabel(selectedDifficulty.getStars(), 16,
                DIFF_COLORS[selectedDifficulty.getLevel() - 1], Font.PLAIN);
        JLabel statsLabel = createLabel(buildStatsText(selectedDifficulty), 10, TEXT_MUTED, Font.PLAIN);
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        starsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < Difficulty.values().length; i++) {
            Difficulty s = Difficulty.values()[i];
            Color c = DIFF_COLORS[i];
            JToggleButton tb = createToggleButton(s.getDisplayName(), c);
            if (s == selectedDifficulty) tb.setSelected(true);
            group.add(tb);
            diffRow.add(tb);
            tb.addActionListener(e -> {
                // Keep all difficulty preview text synced with the selected toggle.
                selectedDifficulty = s;
                descriptionLabel.setText(s.getDescription());
                starsLabel.setText(s.getStars());
                starsLabel.setForeground(c);
                statsLabel.setText(buildStatsText(s));
            });
        }

        JButton btnStart = createButton("Start Game", GREEN_START);
        JButton btnControls = createButton("Controls", YELLOW);
        JButton btnQuit = createButton("Quit", RED_QUIT);
        btnStart.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnControls.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnQuit.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnStart.addActionListener(e -> {
            // Copy difficulty info into the side panel before the game goes fullscreen.
            Color dc = DIFF_COLORS[selectedDifficulty.getLevel() - 1];
            gameDiffLabel.setText(selectedDifficulty.getDisplayName().toUpperCase());
            gameDiffLabel.setForeground(dc);
            gameStarsLabel.setText(selectedDifficulty.getStars());
            gameStarsLabel.setForeground(dc);
            gameInfoLabel.setText(buildStatsText(selectedDifficulty));
            startGame();
            cardLayout.show(mainPanel, "GAME");
        });
        btnControls.addActionListener(e -> showControlsDialog());
        btnQuit.addActionListener(e -> showQuitDialog());

        panel.add(version);
        panel.add(Box.createVerticalStrut(4));
        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sub);
        panel.add(Box.createVerticalStrut(26));
        panel.add(createPixelDivider());
        panel.add(Box.createVerticalStrut(18));
        panel.add(sectionLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(diffRow);
        panel.add(Box.createVerticalStrut(10));
        panel.add(starsLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(descriptionLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(statsLabel);
        panel.add(Box.createVerticalStrut(18));
        panel.add(createPixelDivider());
        panel.add(Box.createVerticalStrut(18));
        panel.add(btnStart);
        panel.add(Box.createVerticalStrut(12));
        panel.add(btnControls);
        panel.add(Box.createVerticalStrut(12));
        panel.add(btnQuit);

        return panel;
    }

    private JPanel createGameView() {
        // This panel is the actual game screen with the generator game on the left
        // and gameplay information panel on the right.
        gameScreen = new JPanel(new BorderLayout());
        gameScreen.setOpaque(false);

        // Container where the actual Generator game panel will be embedded.
        gameContainer = new JPanel(new BorderLayout());
        gameContainer.setOpaque(false);
        gameContainer.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel placeholder = createLabel("Click Start Game to load the level.", 18, TEXT_MUTED, Font.PLAIN);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        gameContainer.add(placeholder, BorderLayout.CENTER);

        infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(50, 40, 50, 60));

        JLabel runningLabel = createLabel("-- GAME RUNNING --", 10, GREEN_START, Font.PLAIN);
        runningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        gameDiffLabel = createLabel("NORMAL", 13, YELLOW, Font.BOLD);
        gameDiffLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        gameStarsLabel = createLabel(Difficulty.NORMAL.getStars(), 18, YELLOW, Font.PLAIN);
        gameStarsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        gameInfoLabel = createLabel(buildStatsText(Difficulty.NORMAL), 10, TEXT_MUTED, Font.PLAIN);
        gameInfoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton btnStop = createButton("Stop Game", RED_QUIT);
        btnStop.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnStop.addActionListener(e -> showQuitDialog());

        infoPanel.add(runningLabel);
        infoPanel.add(Box.createVerticalStrut(26));
        infoPanel.add(createPixelDivider());
        infoPanel.add(Box.createVerticalStrut(26));
        infoPanel.add(gameDiffLabel);
        infoPanel.add(Box.createVerticalStrut(6));
        infoPanel.add(gameStarsLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(gameInfoLabel);
        infoPanel.add(Box.createVerticalStrut(16));
        infoPanel.add(createPixelDivider());
        infoPanel.add(Box.createVerticalStrut(26));
        infoPanel.add(btnStop);

        gameScreen.add(gameContainer, BorderLayout.CENTER);
        gameScreen.add(infoPanel, BorderLayout.EAST);
        return gameScreen;
    }

    private void startGame() {
        currentLevel = 1;
        updateBackgroundForLevel(currentLevel);
        // Nulling the panel forces startCurrentLevel to create a fresh run.
        gamePanel = null;
        startCurrentLevel();
    }

    private void startCurrentLevel() {
        if (gamePanel == null) {
            // Create the generator panel and pass the background image and UI reference
            Image backgroundImage = ((BackgroundPanel) getContentPane()).getBackgroundImage();
            gamePanel = new Generator(backgroundImage, currentLevel, selectedDifficulty, this);
            gamePanel.setPreferredSize(new Dimension(getWidth(), getHeight()));
            gamePanel.setFocusable(true);
        }

        gameContainer.removeAll();
        gameContainer.add(gamePanel, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();

        // Hide the info panel for fullscreen effect
        infoPanel.setVisible(false);

        // Enter fullscreen
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Give keyboard focus to the game panel so player controls work immediately.
        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.requestFocusInWindow();
            }
        });
    }

    private JPanel createGameOverView() {
        // This card is shown when Generator reports that the player died.
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(120, 60, 80, 60));

        JLabel title = createLabel("GAME OVER", 34, RED_QUIT, Font.BOLD);
        JLabel permadeath = createLabel("No saves. No second chances. Start from the beginning.", 13, TEXT_LIGHT, Font.PLAIN);
        gameOverStatsLabel = createLabel("Reached level 1", 12, TEXT_MUTED, Font.PLAIN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        permadeath.setAlignmentX(Component.CENTER_ALIGNMENT);
        gameOverStatsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton retry = createButton("Restart Run", GREEN_START);
        JButton menu = createButton("Main Menu", YELLOW);
        JButton quit = createButton("Quit", RED_QUIT);
        retry.setAlignmentX(Component.CENTER_ALIGNMENT);
        menu.setAlignmentX(Component.CENTER_ALIGNMENT);
        quit.setAlignmentX(Component.CENTER_ALIGNMENT);

        retry.addActionListener(e -> {
            // Restart keeps the selected difficulty and begins again at level 1.
            Color dc = DIFF_COLORS[selectedDifficulty.getLevel() - 1];
            gameDiffLabel.setText(selectedDifficulty.getDisplayName().toUpperCase());
            gameDiffLabel.setForeground(dc);
            gameStarsLabel.setText(selectedDifficulty.getStars());
            gameStarsLabel.setForeground(dc);
            gameInfoLabel.setText(buildStatsText(selectedDifficulty));
            startGame();
            cardLayout.show(mainPanel, "GAME");
        });
        menu.addActionListener(e -> returnToMainMenu());
        quit.addActionListener(e -> dispose());

        panel.add(title);
        panel.add(Box.createVerticalStrut(18));
        panel.add(permadeath);
        panel.add(Box.createVerticalStrut(8));
        panel.add(gameOverStatsLabel);
        panel.add(Box.createVerticalStrut(26));
        panel.add(createPixelDivider());
        panel.add(Box.createVerticalStrut(24));
        panel.add(retry);
        panel.add(Box.createVerticalStrut(12));
        panel.add(menu);
        panel.add(Box.createVerticalStrut(12));
        panel.add(quit);
        return panel;
    }

    // Callback method called by Generator when player wants to return to main menu
    public void returnToMainMenu() {
        if (gamePanel != null) {
            // Stop the old timer before removing the game panel.
            gamePanel.timer.stop();
            gamePanel = null;
        }
        currentLevel = 1;
        updateBackgroundForLevel(1);
        gameContainer.removeAll();
        JLabel placeholder = createLabel("Click Start Game to load the level.", 18, TEXT_MUTED, Font.PLAIN);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        gameContainer.add(placeholder, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();
        infoPanel.setVisible(true);
        setExtendedState(JFrame.NORMAL);
        cardLayout.show(mainPanel, "MENU");
    }

    public void showGameOver(int reachedLevel) {
        if (gamePanel != null) {
            // The dead run is discarded so retry starts from a clean Generator.
            gamePanel.timer.stop();
            gamePanel = null;
        }
        currentLevel = 1;
        updateBackgroundForLevel(1);
        if (gameOverStatsLabel != null) {
            gameOverStatsLabel.setText("Reached level " + reachedLevel + " on " + selectedDifficulty.getDisplayName());
        }
        gameContainer.removeAll();
        JLabel placeholder = createLabel("Click Start Game to load the level.", 18, TEXT_MUTED, Font.PLAIN);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        gameContainer.add(placeholder, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();
        infoPanel.setVisible(true);
        setExtendedState(JFrame.NORMAL);
        cardLayout.show(mainPanel, "GAME_OVER");
    }

    // Update the BackgroundPanel background image for the given level
    private void updateBackgroundForLevel(int level) {
        Component cp = getContentPane();
        if (cp instanceof BackgroundPanel) {
            ((BackgroundPanel) cp).setBackgroundForLevel(level);
            cp.repaint();
        }
    }

    // Callback method called by Generator when player reaches end of map
    public void advanceToNextLevel() {
        currentLevel++;
        if (currentLevel > 7) {
            // Finishing level 7 completes the run.
            JOptionPane.showMessageDialog(this, "You reached heaven. Run complete.");
            returnToMainMenu();
            return;
        }

        // Update the GameUI background panel to match the new level
        updateBackgroundForLevel(currentLevel);

        // Load the next background image
        Image nextBackground = loadBackgroundForLevel(currentLevel);
        
        // Create a new generator for the next level
        // A new Generator resets the map, boss, portal, player, and projectiles.
        gamePanel = new Generator(nextBackground, currentLevel, selectedDifficulty, this);
        gamePanel.setPreferredSize(new Dimension(getWidth(), getHeight()));
        gamePanel.setFocusable(true);

        gameContainer.removeAll();
        gameContainer.add(gamePanel, BorderLayout.CENTER);
        gameContainer.revalidate();
        gameContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            if (gamePanel != null) {
                gamePanel.requestFocusInWindow();
            }
        });
    }

    // Load background image for a specific level
    private Image loadBackgroundForLevel(int level) {
        try {
            // Try relative paths first, then an absolute working-directory path.
            String[] possiblePaths = {
                level + ".png",
                "Background/" + level + ".png",
                System.getProperty("user.dir") + "/Background/" + level + ".png"
            };

            for (String path : possiblePaths) {
                java.io.File f = new java.io.File(path);
                if (f.exists()) {
                    ImageIcon icon = new ImageIcon(path);
                    Image img = icon.getImage();
                    if (img != null && img.getWidth(null) > 0) {
                        System.out.println("Loaded level " + level + " background from: " + path);
                        return img;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading background for level " + level + ": " + e.getMessage());
        }
        return null;
    }

    private void showQuitDialog() {
        // Custom undecorated dialog keeps the pixel-style menu look consistent.
        JDialog dialog = new JDialog(this, "Quit?", true);
        dialog.setUndecorated(true);
        dialog.setSize(320, 200);
        dialog.setLocationRelativeTo(this);

        JPanel dlgPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 18, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(RED_QUIT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 16, 16));
                g2.dispose();
            }
        };
        dlgPanel.setOpaque(false);
        dlgPanel.setLayout(new BoxLayout(dlgPanel, BoxLayout.Y_AXIS));
        dlgPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 24, 30));

        JLabel question = createLabel("Really quit the game?", 12, TEXT_LIGHT, Font.BOLD);
        question.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton yes = createButton("Yes", RED_QUIT);
        JButton no = createButton("No", TEXT_MUTED);
        yes.setPreferredSize(new Dimension(110, 44));
        no.setPreferredSize(new Dimension(110, 44));

        yes.addActionListener(e -> { dialog.dispose(); dispose(); });
        no.addActionListener(e -> dialog.dispose());

        btnRow.add(yes);
        btnRow.add(no);
        dlgPanel.add(question);
        dlgPanel.add(Box.createVerticalStrut(28));
        dlgPanel.add(btnRow);

        dialog.setContentPane(dlgPanel);
        dialog.setVisible(true);
    }

    private void showControlsDialog() {
        // Main-menu control editor; in-game controls are edited from Generator's pause menu.
        JDialog dialog = new JDialog(this, "Controls", true);
        dialog.setUndecorated(true);
        dialog.setSize(460, 560);
        dialog.setLocationRelativeTo(this);

        JPanel dlgPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 18, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(YELLOW);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth()-2, getHeight()-2, 16, 16));
                g2.dispose();
            }
        };
        dlgPanel.setOpaque(false);
        dlgPanel.setLayout(new BoxLayout(dlgPanel, BoxLayout.Y_AXIS));
        dlgPanel.setBorder(BorderFactory.createEmptyBorder(26, 34, 24, 34));

        JLabel title = createLabel("CHANGE COMMANDS", 16, YELLOW, Font.BOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        dlgPanel.add(title);
        dlgPanel.add(Box.createVerticalStrut(18));

        JButton[] bindingButtons = new JButton[Controls.getActionCount()];
        for (int i = 0; i < Controls.getActionCount(); i++) {
            int action = i;
            JPanel row = new JPanel(new BorderLayout(16, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(380, 42));

            JLabel actionLabel = createLabel(Controls.getActionName(action), 12, TEXT_LIGHT, Font.BOLD);
            actionLabel.setHorizontalAlignment(SwingConstants.LEFT);

            JButton keyButton = createButton(Controls.getBindingText(action), ACCENT_BLUE);
            keyButton.setPreferredSize(new Dimension(150, 38));
            keyButton.setMaximumSize(new Dimension(150, 38));
            keyButton.addActionListener(e -> {
                // Capture one input, then refresh all labels because bindings are global.
                captureControlKey(dialog, action);
                for (int j = 0; j < bindingButtons.length; j++) {
                    bindingButtons[j].setText(Controls.getBindingText(j));
                }
            });
            bindingButtons[action] = keyButton;

            row.add(actionLabel, BorderLayout.CENTER);
            row.add(keyButton, BorderLayout.EAST);
            dlgPanel.add(row);
            dlgPanel.add(Box.createVerticalStrut(8));
        }

        dlgPanel.add(Box.createVerticalStrut(10));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        JButton reset = createButton("Reset", TEXT_MUTED);
        JButton done = createButton("Done", GREEN_START);
        reset.setPreferredSize(new Dimension(130, 42));
        done.setPreferredSize(new Dimension(130, 42));
        reset.addActionListener(e -> {
            Controls.resetDefaults();
            for (int i = 0; i < bindingButtons.length; i++) {
                bindingButtons[i].setText(Controls.getBindingText(i));
            }
        });
        done.addActionListener(e -> dialog.dispose());
        btnRow.add(reset);
        btnRow.add(done);
        dlgPanel.add(btnRow);

        dialog.setContentPane(dlgPanel);
        dialog.setVisible(true);
    }

    private void captureControlKey(JDialog parent, int action) {
        // Modal mini-dialog grabs exactly one key press or mouse click for rebinding.
        JDialog capture = new JDialog(parent, "Press Input", true);
        capture.setUndecorated(true);
        capture.setSize(320, 150);
        capture.setLocationRelativeTo(parent);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(18, 18, 30));
        panel.setBorder(BorderFactory.createLineBorder(ACCENT_BLUE, 2));

        JLabel label = createLabel("Press key or click for " + Controls.getActionName(action), 12, TEXT_LIGHT, Font.BOLD);
        panel.add(label, BorderLayout.CENTER);
        JLabel hint = createLabel("ESC cancels", 10, TEXT_MUTED, Font.PLAIN);
        panel.add(hint, BorderLayout.SOUTH);

        capture.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ESCAPE) {
                    Controls.setKeyBinding(action, e.getKeyCode());
                }
                capture.dispose();
            }
        });
        MouseAdapter mouseCapture = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                Controls.setMouseBinding(action, e.getButton());
                capture.dispose();
            }
        };
        capture.addMouseListener(mouseCapture);
        panel.addMouseListener(mouseCapture);
        capture.setContentPane(panel);
        SwingUtilities.invokeLater(capture::requestFocusInWindow);
        capture.setVisible(true);
    }

    private String buildStatsText(Difficulty s) {
        // Compact text reused in both menu and in-game info panels.
        return "Lives: " + s.getLives()
             + "  |  Speed: " + s.getSpeed()
             + "  |  Bonus: +" + s.getTimeBonus() + "s";
    }

    private JLabel createLabel(String text, int size, Color color, int style) {
        // Centralizes the arcade-style font and centered alignment.
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Monospaced", style, size));
        l.setForeground(color);
        return l;
    }

    private JButton createButton(String text, Color color) {
        // Custom painting gives every button the same outline/hover/pressed behavior.
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())
                    g2.setColor(color.darker().darker());
                else if (getModel().isRollover())
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
                else
                    g2.setColor(new Color(0, 0, 0, 0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Monospaced", Font.BOLD, 11));
        btn.setForeground(color);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(220, 46));
        btn.setMaximumSize(new Dimension(220, 46));
        return btn;
    }

    private JToggleButton createToggleButton(String text, Color color) {
        // Difficulty buttons share the same visual style as normal menu buttons.
        JToggleButton tb = new JToggleButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected())
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 40));
                else if (getModel().isRollover())
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 15));
                else
                    g2.setColor(new Color(0, 0, 0, 0));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(isSelected() ? color : TEXT_MUTED);
                g2.setStroke(new BasicStroke(isSelected() ? 2f : 1f));
                g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tb.setFont(new Font("Monospaced", Font.BOLD, 10));
        tb.setForeground(color);
        tb.setOpaque(false);
        tb.setContentAreaFilled(false);
        tb.setBorderPainted(false);
        tb.setFocusPainted(false);
        tb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        tb.setPreferredSize(new Dimension(100, 38));
        return tb;
    }

    private JPanel createPixelDivider() {
        // Small repeated blocks separate menu sections without using image assets.
        JPanel d = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(ACCENT_BLUE);
                int x = 0;
                while (x < getWidth()) { g.fillRect(x, 0, 6, 2); x += 10; }
            }
        };
        d.setOpaque(false);
        d.setMaximumSize(new Dimension(180, 4));
        d.setPreferredSize(new Dimension(180, 4));
        d.setAlignmentX(Component.CENTER_ALIGNMENT);
        return d;
    }

    // Background with scanlines and corner markers
    class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        BackgroundPanel() {
            setOpaque(true);
            setBackground(BG_DARK);
            loadBackgroundImage();
        }

        // Public getter so the generator can access the background image
        public Image getBackgroundImage() {
            return backgroundImage;
        }

        // Switch the background to the image for the given level number
        public void setBackgroundForLevel(int level) {
            backgroundImage = loadImageForLevel(level);
            repaint();
        }

        private Image loadImageForLevel(int level) {
            // Same lookup strategy as loadBackgroundForLevel, used for the outer frame.
            String[] possiblePaths = {
                level + ".png",
                "Background/" + level + ".png",
                System.getProperty("user.dir") + "/Background/" + level + ".png"
            };
            for (String path : possiblePaths) {
                java.io.File f = new java.io.File(path);
                if (f.exists()) {
                    ImageIcon icon = new ImageIcon(path);
                    Image img = icon.getImage();
                    if (img != null && img.getWidth(null) > 0) return img;
                }
            }
            return null;
        }

        private void loadBackgroundImage() {
            backgroundImage = loadImageForLevel(1);
            if (backgroundImage == null) {
                System.out.println("Background image not found for level 1");
            }
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            if (backgroundImage != null) {
                g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }

            // Scanlines and corner brackets sit over the level image for a retro UI frame.
            g2.setColor(new Color(0, 0, 0, 40));
            for (int y = 0; y < getHeight(); y += 4) g2.drawLine(0, y+3, getWidth(), y+3);
            int s = 20, m = 20;
            g2.setColor(ACCENT_BLUE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(m, m, m+s, m);
            g2.drawLine(m, m, m, m+s);
            g2.drawLine(getWidth()-m-s, m, getWidth()-m, m);
            g2.drawLine(getWidth()-m, m, getWidth()-m, m+s);
            g2.drawLine(m, getHeight()-m, m+s, getHeight()-m);
            g2.drawLine(m, getHeight()-m-s, m, getHeight()-m);
            g2.drawLine(getWidth()-m-s, getHeight()-m, getWidth()-m, getHeight()-m);
            g2.drawLine(getWidth()-m, getHeight()-m-s, getWidth()-m, getHeight()-m);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameUI::new);
    }
}
