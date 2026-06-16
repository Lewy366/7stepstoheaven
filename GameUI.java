import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class GameUI extends JFrame {

    // Main colors used throughout the menu and game UI.
    private static final Color BG_DARK = new Color(10, 10, 18);
    private static final Color ACCENT_BLUE = new Color(58, 58, 255);
    private static final Color GREEN_START = new Color(10, 255, 110);
    private static final Color RED_QUIT = new Color(255, 58, 92);
    private static final Color YELLOW = new Color(255, 200, 0);
    private static final Color TEXT_LIGHT = new Color(232, 232, 255);
    private static final Color TEXT_MUTED = new Color(106, 106, 154);
    private static final Color TEXT_WHITE = new Color(255, 255, 255);

    // Colors match the order of the Difficulty enum: easy, normal, hard.
    private static final Color[] DIFF_COLORS = { GREEN_START, YELLOW, RED_QUIT };

    // The difficulty currently selected on the main menu.
    private Difficulty selectedDifficulty = Difficulty.NORMAL;

    // CardLayout switches between the menu screen and the game screen.
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    // Panels used while the actual game is running.
    private JPanel gameContainer;
    private JPanel gameScreen;
    private JPanel infoPanel;
    private Generator gamePanel;
    private int currentLevel = 1;

    // Labels that update when the player chooses a difficulty.
    private JLabel gameInfoLabel;
    private JLabel gameStarsLabel;
    private JLabel gameDifficultyLabel;

    // Sets up the main window, background panel, and the two main screens.
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
        mainPanel = new JPanel(cardLayout);
        mainPanel.setOpaque(false);

        mainPanel.add(createMainMenu(), "MENU");

        setContentPane(new BackgroundPanel());
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    // Builds the first screen the player sees: title, difficulty choices, start,
    // and quit buttons.
    private JPanel createMainMenu() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        JLabel version = createLabel("Version 1.0", 24, TEXT_WHITE, Font.BOLD);
        JLabel title = createLabel("7StepsToHeaven", 30, TEXT_WHITE, Font.BOLD);
        JLabel subtitle = createLabel("Get to heaven", 19, TEXT_WHITE, Font.PLAIN);
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sectionLabel = createLabel("Difficulty", 20, TEXT_LIGHT, Font.PLAIN);
        sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel difficultyRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        difficultyRow.setOpaque(false);
        difficultyRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descriptionLabel = createLabel(selectedDifficulty.getDescription(), 16, TEXT_WHITE, Font.BOLD);
        JLabel starsLabel = createLabel(selectedDifficulty.getStars(), 22,
                DIFF_COLORS[selectedDifficulty.getLevel() - 1], Font.BOLD);
        JLabel statsLabel = createLabel(buildStatsText(selectedDifficulty), 14, TEXT_WHITE, Font.PLAIN);
        descriptionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        starsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Create one toggle button per difficulty and keep only one selected.
        ButtonGroup group = new ButtonGroup();
        for (int i = 0; i < Difficulty.values().length; i++) {
            Difficulty difficulty = Difficulty.values()[i];
            Color color = DIFF_COLORS[i];
            JToggleButton toggleButton = createToggleButton(difficulty.getDisplayName(), color);
            if (difficulty == selectedDifficulty) toggleButton.setSelected(true);
            group.add(toggleButton);
            difficultyRow.add(toggleButton);
            toggleButton.addActionListener(e -> {
                selectedDifficulty = difficulty;
                descriptionLabel.setText(difficulty.getDescription());
                starsLabel.setText(difficulty.getStars());
                starsLabel.setForeground(color);
                statsLabel.setText(buildStatsText(difficulty));
            });
        }

        JButton startButton = createButton(">  Start Game", GREEN_START);
        JButton quitButton = createButton("X  Quit", RED_QUIT);
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        quitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Copy the selected difficulty into the game info labels before starting.
        startButton.addActionListener(e -> {
            Color difficultyColor = DIFF_COLORS[selectedDifficulty.getLevel() - 1];
            gameDifficultyLabel.setText(selectedDifficulty.getDisplayName().toUpperCase());
            gameDifficultyLabel.setForeground(difficultyColor);
            gameStarsLabel.setText(selectedDifficulty.getStars());
            gameStarsLabel.setForeground(difficultyColor);
            gameInfoLabel.setText(buildStatsText(selectedDifficulty));
            startGame();
            cardLayout.show(mainPanel, "GAME");
        });
        quitButton.addActionListener(e -> showQuitDialog());

        panel.add(version);
        panel.add(Box.createVerticalStrut(4));
        panel.add(title);
        panel.add(Box.createVerticalStrut(6));
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(26));
        panel.add(createPixelDivider());
        panel.add(Box.createVerticalStrut(18));
        panel.add(sectionLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(difficultyRow);
        panel.add(Box.createVerticalStrut(10));
        panel.add(starsLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(descriptionLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(statsLabel);
        panel.add(Box.createVerticalStrut(18));
        panel.add(createPixelDivider());
        panel.add(Box.createVerticalStrut(18));
        panel.add(startButton);
        panel.add(Box.createVerticalStrut(12));
        panel.add(quitButton);

        return panel;
    }

    // Creates the game panel if needed, places it on screen, and gives it
    // keyboard focus so movement controls work immediately.
    private void startGame() {
        this.dispose();
        MainGame.main(new String[0]);
    }

    // Changes the window background image to match the current level.
    private void updateBackgroundForLevel(int level) {
        Component contentPane = getContentPane();
        if (contentPane instanceof BackgroundPanel) {
            ((BackgroundPanel) contentPane).setBackgroundForLevel(level);
            contentPane.repaint();
        }
    }

    // Called by Generator when the player reaches the end of a level.
    // It loops back to level 1 after level 7.
    public void advanceToNextLevel() {
        currentLevel++;
        if (currentLevel > 7) {
            currentLevel = 1;
        }

        updateBackgroundForLevel(currentLevel);
        Image nextBackground = loadBackgroundForLevel(currentLevel);

        gamePanel = new Generator(nextBackground, currentLevel, this);
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

    // Looks for a level background in the project folder and Background folder.
    private Image loadBackgroundForLevel(int level) {
        try {
            String[] possiblePaths = {
                "MainMenu" + ".png",
                "Background/" + "MainMenu" + ".png",
                System.getProperty("user.dir") + "/Background/" + "MainMenu" + ".png"
            };

            for (String path : possiblePaths) {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    ImageIcon icon = new ImageIcon(path);
                    Image image = icon.getImage();
                    if (image != null && image.getWidth(null) > 0) {
                        System.out.println("Loaded level " + level + " background from: " + path);
                        return image;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading background for level " + level + ": " + e.getMessage());
        }
        return null;
    }

    // Shows a custom confirmation dialog before closing the game window.
    private void showQuitDialog() {
        JDialog dialog = new JDialog(this, "Quit?", true);
        dialog.setUndecorated(true);
        dialog.setSize(320, 200);
        dialog.setLocationRelativeTo(this);

        JPanel dialogPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(18, 18, 30));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(RED_QUIT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 16, 16));
                g2.dispose();
            }
        };
        dialogPanel.setOpaque(false);
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 24, 30));

        JLabel question = createLabel("Are you sure you want to quit?", 12, TEXT_LIGHT, Font.BOLD);
        question.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton yesButton = createButton("Yes", RED_QUIT);
        JButton noButton = createButton("No", TEXT_MUTED);
        yesButton.setPreferredSize(new Dimension(110, 44));
        noButton.setPreferredSize(new Dimension(110, 44));

        yesButton.addActionListener(e -> { dialog.dispose(); dispose(); });
        noButton.addActionListener(e -> dialog.dispose());

        buttonRow.add(yesButton);
        buttonRow.add(noButton);
        dialogPanel.add(question);
        dialogPanel.add(Box.createVerticalStrut(28));
        dialogPanel.add(buttonRow);

        dialog.setContentPane(dialogPanel);
        dialog.setVisible(true);
    }

    // Formats the selected difficulty's values into one compact label.
    private String buildStatsText(Difficulty difficulty) {
        return "Lives: " + difficulty.getLives()
             + "  |  Speed: " + difficulty.getSpeed()
             + "  |  Bonus: +" + difficulty.getTimeBonus() + "s";
    }

    // Creates a label with the game's shared font and color style.
    private JLabel createLabel(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Monospaced", style, size));
        label.setForeground(color);
        return label;
    }

    // Creates a transparent button with a colored pixel-style outline.
    private JButton createButton(String text, Color color) {
        JButton button = new JButton(text) {
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
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(new Font("Monospaced", Font.BOLD, 40));
        button.setForeground(color);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(380, 50));
        button.setMaximumSize(new Dimension(380, 50));
        return button;
    }

    // Creates the selectable difficulty buttons on the main menu.
    private JToggleButton createToggleButton(String text, Color color) {
        JToggleButton toggleButton = new JToggleButton(text) {
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
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        toggleButton.setFont(new Font("Monospaced", Font.BOLD, 20));
        toggleButton.setForeground(color);
        toggleButton.setOpaque(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setBorderPainted(false);
        toggleButton.setFocusPainted(false);
        toggleButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleButton.setPreferredSize(new Dimension(150, 50));
        return toggleButton;
    }

    // Small decorative divider used to split menu sections.
    private JPanel createPixelDivider() {
        JPanel divider = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(ACCENT_BLUE);
                int x = 0;
                while (x < getWidth()) {
                    g.fillRect(x, 0, 6, 2);
                    x += 10;
                }
            }
        };
        divider.setOpaque(false);
        divider.setMaximumSize(new Dimension(180, 4));
        divider.setPreferredSize(new Dimension(180, 4));
        divider.setAlignmentX(Component.CENTER_ALIGNMENT);
        return divider;
    }

    // Custom content pane that paints the level background, scanlines, and corner
    // decorations behind the menu/game screens.
    class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        BackgroundPanel() {
            setOpaque(true);
            setBackground(BG_DARK);
            loadBackgroundImage();
        }

        // Gives Generator access to the currently displayed background image.
        public Image getBackgroundImage() {
            return backgroundImage;
        }

        // Loads and displays the background for a specific level number.
        public void setBackgroundForLevel(int level) {
            backgroundImage = loadImageForLevel(level);
            repaint();
        }

        // Tries each supported background path and returns the first valid image.
        private Image loadImageForLevel(int level) {
            String[] possiblePaths = {
                "MainMenu" + ".png",
                "Background/" + "MainMenu" + ".png",
                System.getProperty("user.dir") + "/Background/" + "MainMenu" + ".png"
            };
            for (String path : possiblePaths) {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    ImageIcon icon = new ImageIcon(path);
                    Image image = icon.getImage();
                    if (image != null && image.getWidth(null) > 0) return image;
                }
            }
            return null;
        }

        // Starts the UI with the level 1 background.
        private void loadBackgroundImage() {
            backgroundImage = loadImageForLevel(1);
            if (backgroundImage == null) {
                System.out.println("Background image not found for level 1");
            }
        }

        // Draws the background image first, then overlays scanlines and corners.
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            if (backgroundImage != null) {
                g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }

            g2.setColor(new Color(0, 0, 0, 40));
            for (int y = 0; y < getHeight(); y += 4) g2.drawLine(0, y + 3, getWidth(), y + 3);
            int size = 20;
            int margin = 20;
            g2.setColor(ACCENT_BLUE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(margin, margin, margin + size, margin);
            g2.drawLine(margin, margin, margin, margin + size);
            g2.drawLine(getWidth() - margin - size, margin, getWidth() - margin, margin);
            g2.drawLine(getWidth() - margin, margin, getWidth() - margin, margin + size);
            g2.drawLine(margin, getHeight() - margin, margin + size, getHeight() - margin);
            g2.drawLine(margin, getHeight() - margin - size, margin, getHeight() - margin);
            g2.drawLine(getWidth() - margin - size, getHeight() - margin, getWidth() - margin, getHeight() - margin);
            g2.drawLine(getWidth() - margin, getHeight() - margin - size, getWidth() - margin, getHeight() - margin);
            g2.dispose();
        }
    }
}
