import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

public class Generator extends JPanel implements ActionListener, KeyListener {

    // --- Timer ---
    Timer timer = new Timer(16, this);

    // --- Map ---
    int TILE_SIZE = 50;
    int MAP_ROWS  = 21;
    int MAP_COLS  = 38;

    int[][] map;
    ArrayList<Rectangle> tiles = new ArrayList<>();

    // --- Player ---
    Player player;

    // --- Camera ---
    int cameraX = 0;
    int cameraY = 0;

    // --- Seed ---
    long seed;

    // --- Background ---
    private Image backgroundImage;

    // --- UI Reference and Level Tracking ---
    private GameUI uiReference;
    private LevelChanger levelChanger;

    // --- Pause and Settings ---
    private boolean isPaused = false;
    private int selectedMenuOption = 0; // 0=Resume, 1=Controls, 2=Menu, 3=Quit
    private boolean inControlsMenu = false;
    private int selectedControlOption = 0;

    // Movement control options (placeholder for future expansion)
    private static final String[] MOVEMENT_CONTROLS = {
        "WASD",
        "Arrow Keys",
        "Placeholder 3",
        "Placeholder 4"
    };

    // Level names for the 7 levels (Hell to Heaven)
    private static final String[] LEVEL_NAMES = {
        "The Inferno",      // Level 1
        "The Abyss",        // Level 2
        "Purgatory",        // Level 3
        "The Mortal Realm", // Level 4
        "The Ascent",       // Level 5
        "Paradise",         // Level 6
        "The Heavens"       // Level 7
    };

    // Constructor that accepts background image, level, and UI reference from GameUI
    public Generator(Image background, int level, GameUI ui) {
        this.backgroundImage = background;
        this.levelChanger = new LevelChanger();
        this.levelChanger.currentlevel = level;
        this.uiReference = ui;

        setFocusable(true);
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(1920, 1080));

        seed = System.currentTimeMillis();
        // Fix 2: was calling nonexistent generateMap(); use generateMap() which now calls mapgen() internally
        map = generateMap(MAP_ROWS, MAP_COLS, seed);
        buildMap();

        player = new Player(tiles);
        addKeyListener(player.keyAdapter);
        addKeyListener(this);

        timer.start();
        System.out.println("Level " + (int)levelChanger.currentlevel + ": " + LEVEL_NAMES[(int)levelChanger.currentlevel - 1]);
    }

    // -------------------------------------------------------
    // MAP GENERATOR
    // Fix 2: renamed mapgen() -> generateMap() and added parameters to match call sites
    // -------------------------------------------------------
    public int[][] generateMap(int rows, int cols, long mapSeed) {
        map = new int[rows][cols];

        // Clear
        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[i].length; j++) {
                map[i][j] = 0;
            }
        }

        // Border walls
        for (int r = 0; r < map.length; r++) {
            for (int c = 0; c < map[r].length; c++) {
                if (c == 0) {
                    map[r][c] = 1;
                } else if (c == map[r].length - 1) {
                    map[r][c] = 1;
                } else if (r == 0) {
                    map[r][c] = 0;
                } else if (r == map.length - 1) {
                    map[r][c] = 1;
                } else {
                    map[r][c] = 0;
                }
            }
        }

        // Platform generation
        Random rand = new Random(mapSeed);
        // Fix 3: was "currentlevel % 1 != 0" (always false) and wrong case.
        //        Changed to "currentLevel % 2 != 0" for odd levels = normal platforms,
        //        even levels = boss room. Adjust the condition to suit your design.
        if (((int)levelChanger.currentlevel) % 2 != 0) {
            for (int i = map.length - 5; i > 1; i -= 4) {
                int gapSize  = rand.nextInt(4) + 4;
                int gapStart = rand.nextInt(map[i].length - 2 - gapSize) + 1;

                int gapSize2  = rand.nextInt(4) + 4;
                int gapStart2 = rand.nextInt(map[i].length - 2 - gapSize2) + 1;

                for (int j = 1; j < map[i].length - 1; j++) {
                    boolean inGap  = j >= gapStart  && j < gapStart  + gapSize;
                    boolean inGap2 = j >= gapStart2 && j < gapStart2 + gapSize2;
                    map[i][j] = (inGap || inGap2) ? 0 : 1;
                }
            }
        } else {
            // Boss room — add your boss-room logic here
        }

        return map;
    }

    // -------------------------------------------------------
    // BUILD MAP
    // Fix 4: was referencing undefined variables "dieMap" and "g1"
    //        Changed to "map" and "TILE_SIZE"
    // -------------------------------------------------------
    void buildMap() {
        tiles.clear();
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col] == 1) {
                    tiles.add(new Rectangle(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE));
                }
            }
        }
    }
    // -------------------------------------------------------
    // GAME LOOP
    // -------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        player.update();

        // Check if player has reached the right edge of the map to advance to next level
        if (player.playerX > MAP_COLS * TILE_SIZE) {
            timer.stop();
            levelChanger.nextlevel();
            if (uiReference != null) {
                uiReference.advanceToNextLevel();
            }
            return;
        }

        updateCamera();
        repaint();
    }

    // -------------------------------------------------------
    // CAMERA
    // -------------------------------------------------------
    void updateCamera() {
        int viewW = Math.max(1, getWidth());
        int viewH = Math.max(1, getHeight());

        cameraX = player.playerX + 25 - viewW / 2;
        cameraY = player.playerY + 25 - viewH / 2;

        cameraX = Math.max(0, Math.min(cameraX, MAP_COLS * TILE_SIZE - viewW));
        cameraY = Math.max(0, Math.min(cameraY, MAP_ROWS * TILE_SIZE - viewH));
    }

    // -------------------------------------------------------
    // RENDERING
    // -------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw background image if available, otherwise use fallback color
        if (backgroundImage != null) {
            g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            g2.setColor(new Color(30, 30, 60));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        g2.translate(-cameraX, -cameraY);

        for (Rectangle tile : tiles) {
            g2.setColor(Color.GRAY);
            g2.fillRect(tile.x, tile.y, tile.width, tile.height);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(tile.x, tile.y, tile.width, tile.height);
        }

        player.draw(g2);

        g2.translate(cameraX, cameraY);

        // Draw level title at the top
        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("Monospaced", Font.BOLD, 32));
        String levelTitle = "LEVEL " + (int)levelChanger.currentlevel + ": " + LEVEL_NAMES[(int)levelChanger.currentlevel - 1];
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (getWidth() - fm.stringWidth(levelTitle)) / 2;
        g2.drawString(levelTitle, titleX, 50);

        // Debug info at bottom left
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2.drawString("Seed: " + seed + "  |  [R] = new map  |  [ESC] = Pause", 10, getHeight() - 10);

        // Draw pause menu if paused
        if (isPaused) {
            drawPauseMenu(g2);
        }
    }

    // Draw the pause menu overlay
    private void drawPauseMenu(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        if (inControlsMenu) {
            drawControlsMenu(g2, centerX, centerY);
        } else {
            drawMainPauseMenu(g2, centerX, centerY);
        }
    }

    private void drawMainPauseMenu(Graphics2D g2, int centerX, int centerY) {
        String[] options = { "Resume Game", "Controls", "Back to Menu", "Quit to Desktop" };

        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("Monospaced", Font.BOLD, 48));
        String title = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = centerX - fm.stringWidth(title) / 2;
        g2.drawString(title, titleX, centerY - 150);

        int startY   = centerY - 20;
        int spacing  = 70;

        for (int i = 0; i < options.length; i++) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 24));

            if (i == selectedMenuOption) {
                g2.setColor(new Color(58, 58, 255));
                g2.fillRect(centerX - 180, startY + i * spacing - 25, 360, 50);
                g2.setColor(new Color(10, 255, 110));
            } else {
                g2.setColor(new Color(232, 232, 255));
            }

            fm = g2.getFontMetrics();
            int optionX = centerX - fm.stringWidth(options[i]) / 2;
            g2.drawString(options[i], optionX, startY + i * spacing);
        }

        g2.setColor(new Color(106, 106, 154));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2.drawString("Use UP/DOWN arrows to navigate, ENTER to select", centerX - 200, getHeight() - 40);
    }

    private void drawControlsMenu(Graphics2D g2, int centerX, int centerY) {
        String[] controls = MOVEMENT_CONTROLS;

        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("Monospaced", Font.BOLD, 48));
        String title = "MOVEMENT CONTROLS";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = centerX - fm.stringWidth(title) / 2;
        g2.drawString(title, titleX, centerY - 150);

        int startY  = centerY - 20;
        int spacing = 70;

        for (int i = 0; i < controls.length; i++) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 24));

            if (i == selectedControlOption) {
                g2.setColor(new Color(58, 58, 255));
                g2.fillRect(centerX - 200, startY + i * spacing - 25, 400, 50);
                g2.setColor(new Color(10, 255, 110));
            } else {
                g2.setColor(new Color(232, 232, 255));
            }

            fm = g2.getFontMetrics();
            int optionX = centerX - fm.stringWidth(controls[i]) / 2;
            g2.drawString(controls[i], optionX, startY + i * spacing);
        }

        g2.setColor(new Color(106, 106, 154));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2.drawString("Use UP/DOWN to select, ENTER to confirm, ESC to go back", centerX - 250, getHeight() - 40);
    }

    // -------------------------------------------------------
    // CONTROLS
    // -------------------------------------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (inControlsMenu) {
                inControlsMenu = false;
            } else {
                isPaused = !isPaused;
            }
            repaint();
            return;
        }

        if (isPaused) {
            if (inControlsMenu) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    selectedControlOption = (selectedControlOption - 1 + MOVEMENT_CONTROLS.length) % MOVEMENT_CONTROLS.length;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    selectedControlOption = (selectedControlOption + 1) % MOVEMENT_CONTROLS.length;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    System.out.println("Selected controls: " + MOVEMENT_CONTROLS[selectedControlOption]);
                }
            } else {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    selectedMenuOption = (selectedMenuOption - 1 + 4) % 4;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    selectedMenuOption = (selectedMenuOption + 1) % 4;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleMenuSelection();
                }
            }
            return;
        }

        if (e.getKeyCode() == KeyEvent.VK_R) regenerateMap();
    }

    private void handleMenuSelection() {
        switch (selectedMenuOption) {
            case 0: // Resume Game
                isPaused = false;
                repaint();
                break;
            case 1: // Controls
                inControlsMenu = true;
                selectedControlOption = 0;
                repaint();
                break;
            case 2: // Back to Menu
                timer.stop();
                if (uiReference != null) {
                    uiReference.returnToMainMenu();
                }
                break;
            case 3: // Quit to Desktop
                System.exit(0);
                break;
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}