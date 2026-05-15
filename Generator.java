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
    Boss boss;
    Portal portal;
    ArrayList<HealingItem> healingItems = new ArrayList<>();
    ArrayList<Projectile> projectiles = new ArrayList<>();

    // --- Camera ---
    int cameraX = 0;
    int cameraY = 0;

    // --- Seed ---
    long seed;

    // --- Background ---
    private Image backgroundImage;

    // --- UI Reference and Level Tracking ---
    private GameUI uiReference;
    private int currentLevel;
    private Difficulty difficulty;
    private boolean portalOpened = false;

    // --- Pause and Settings ---
    private boolean isPaused = false;
    private int selectedMenuOption = 0; // 0=Resume, 1=Controls, 2=Menu, 3=Quit
    private boolean inControlsMenu = false;
    private int selectedControlOption = 0; // 0=WASD, 1=Arrow Keys, etc. (placeholder)

    // Movement control options (placeholder for future expansion)
    private static final String[] MOVEMENT_CONTROLS = {
        "Move: A/D or Arrow Keys",
        "Jump: W/Space/Up",
        "Duck: S/Down",
        "Dash: Shift",
        "Melee: F/J",
        "Shoot: E/K"
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
    public Generator(Image background, int level, Difficulty difficulty, GameUI ui) {
        this.backgroundImage = background;
        this.currentLevel = level;
        this.difficulty = difficulty;
        this.uiReference = ui;

        // This panel is designed to be embedded inside GameUI.
        setFocusable(true);
        setDoubleBuffered(true);
        setPreferredSize(new Dimension(1920, 1080));

        seed = System.currentTimeMillis();
        map  = generateMap(MAP_ROWS, MAP_COLS, seed);
        buildMap();

        player = new Player(tiles, difficulty);
        setupLevelEntities();
        addKeyListener(player.keyAdapter);
        addKeyListener(this);

        timer.start();
        System.out.println("Level " + currentLevel + ": " + LEVEL_NAMES[currentLevel - 1]);
    }

    // -------------------------------------------------------
    // MAP GENERATOR
    // -------------------------------------------------------
    int[][] generateMap(int rows, int cols, long seed) {
        Random random = new Random(seed);
        int[][] newMap = new int[rows][cols];

        for (int row = rows - 3; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                newMap[row][col] = 1;
            }
        }

        int platformCount = random.nextInt(10) + 10;

        for (int i = 0; i < platformCount; i++) {
            int platformLength = random.nextInt(5) + 3;
            int row = random.nextInt(rows - 5) + 1;
            int col = random.nextInt(cols - platformLength);

            for (int j = 0; j < platformLength; j++) {
                newMap[row][col + j] = 1;
            }
        }

        int arenaStart = Math.max(8, cols - 11);
        for (int col = arenaStart; col < cols; col++) {
            newMap[rows - 4][col] = 0;
            newMap[rows - 5][col] = 0;
        }
        for (int col = arenaStart + 1; col < cols - 1; col++) {
            newMap[rows - 8][col] = 0;
        }
        for (int col = arenaStart + 2; col < cols - 2; col++) {
            newMap[rows - 9][col] = 0;
        }
        for (int col = arenaStart + 2; col < arenaStart + 6 && col < cols; col++) {
            newMap[rows - 10][col] = 1;
        }

        return newMap;
    }

    // -------------------------------------------------------
    // BUILD MAP
    // -------------------------------------------------------
    void buildMap() {
        tiles.clear();
        for (int row = 0; row < map.length; row++) {
            for (int col = 0; col < map[row].length; col++) {
                if (map[row][col] == 1) {
                    tiles.add(new Rectangle(
                        col * TILE_SIZE,
                        row * TILE_SIZE,
                        TILE_SIZE,
                        TILE_SIZE
                    ));
                }
            }
        }
    }

    // -------------------------------------------------------
    // NEW MAP
    // -------------------------------------------------------
    void regenerateMap() {
        seed = System.currentTimeMillis();
        map  = generateMap(MAP_ROWS, MAP_COLS, seed);
        buildMap();

        player.playerX   = 100;
        player.playerY   = 250;
        player.velocityY = 0;
        player.velocityX = 0;
        portalOpened = false;
        projectiles.clear();
        setupLevelEntities();

        System.out.println("New map! Seed: " + seed);
    }

    private void setupLevelEntities() {
        healingItems.clear();

        int floorY = (MAP_ROWS - 3) * TILE_SIZE;
        int bossX = (MAP_COLS - 7) * TILE_SIZE;
        boss = new Boss(bossX, floorY - 86, currentLevel, difficulty, tiles);
        portal = new Portal((MAP_COLS - 2) * TILE_SIZE, floorY - 100);

        int healAmount = 22 - difficulty.getLevel() * 3;
        healingItems.add(new HealingItem((MAP_COLS - 13) * TILE_SIZE, floorY - 38, healAmount));
        healingItems.add(new HealingItem((MAP_COLS - 9) * TILE_SIZE, floorY - 38, healAmount));
        if (difficulty == Difficulty.EASY) {
            healingItems.add(new HealingItem((MAP_COLS - 5) * TILE_SIZE, floorY - 38, healAmount));
        }
    }

    private void updateProjectiles() {
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update(tiles);

            if (projectile.isActive()) {
                if (projectile.isFromPlayer()) {
                    if (boss != null && !boss.isDead() && projectile.getBounds().intersects(boss.getBounds())) {
                        boss.takeDamage(projectile.getDamage());
                        projectile.deactivate();
                    }
                } else if (projectile.getBounds().intersects(player.getBounds())) {
                    player.hurt(projectile.getDamage());
                    projectile.deactivate();
                }
            }

            Rectangle bounds = projectile.getBounds();
            boolean outOfWorld = bounds.x < -200 || bounds.x > MAP_COLS * TILE_SIZE + 200
                    || bounds.y < -300 || bounds.y > MAP_ROWS * TILE_SIZE + 300;
            if (!projectile.isActive() || outOfWorld) {
                projectiles.remove(i);
            }
        }
    }

    // -------------------------------------------------------
    // GAME LOOP
    // -------------------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isPaused) {
            repaint();
            return;
        }

        player.update();

        if (player.startAttack() && boss != null && !boss.isDead()
                && player.getAttackBounds().intersects(boss.getBounds())) {
            boss.takeDamage(18 + currentLevel * 2);
        }

        Projectile playerShot = player.shootProjectile();
        if (playerShot != null) {
            projectiles.add(playerShot);
        }

        if (boss != null) {
            boss.update(player);
            Projectile bossShot = boss.fireProjectileIfReady(player);
            if (bossShot != null) {
                projectiles.add(bossShot);
            }
            if (boss.canDamagePlayer(player)) {
                player.hurt(boss.getCurrentDamage());
            }
            if (boss.isDead() && !portalOpened) {
                portalOpened = true;
                portal.setActive(true);
            }
        }

        for (HealingItem item : healingItems) {
            item.update();
            item.collectIfTouched(player);
        }

        updateProjectiles();

        if (player.playerY > MAP_ROWS * TILE_SIZE + 300) {
            player.hurt(player.getMaxHealth());
        }

        if (player.isDead()) {
            timer.stop();
            if (uiReference != null) {
                uiReference.showGameOver(currentLevel);
            }
            return;
        }

        if (portal != null) {
            portal.update();
        }

        if (portal != null && portal.canAdvance(player)) {
            timer.stop();
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

        if (portal != null) portal.draw(g2);
        for (HealingItem item : healingItems) item.draw(g2);
        for (Projectile projectile : projectiles) projectile.draw(g2);
        if (boss != null && !boss.isDead()) boss.draw(g2);
        player.draw(g2);

        g2.translate(cameraX, cameraY);

        // Draw level title at the top
        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("Monospaced", Font.BOLD, 32));
        String levelTitle = "LEVEL " + currentLevel + ": " + LEVEL_NAMES[currentLevel - 1];
        FontMetrics fm = g2.getFontMetrics();
        int titleX = (getWidth() - fm.stringWidth(levelTitle)) / 2;
        g2.drawString(levelTitle, titleX, 50);

        drawHud(g2);

        // Debug info at bottom left
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2.drawString("A/D move | W/SPACE jump | S duck | SHIFT dash | F melee | E shoot | R new map | ESC pause", 10, getHeight() - 10);

        // Draw pause menu if paused
        if (isPaused) {
            drawPauseMenu(g2);
        }
    }

    private void drawHud(Graphics2D g2) {
        int x = 24;
        int y = 78;
        drawBar(g2, x, y, 260, 18, player.getHealth(), player.getMaxHealth(),
                new Color(40, 220, 105), "PLAYER");

        if (boss != null && !boss.isDead()) {
            drawBar(g2, getWidth() - 344, y, 320, 18, boss.getHealth(), boss.getMaxHealth(),
                    new Color(230, 55, 70), "BOSS");
        } else {
            g2.setColor(new Color(90, 220, 255));
            g2.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2.drawString("Boss defeated. Enter the portal.", getWidth() - 370, y + 17);
        }
    }

    private void drawBar(Graphics2D g2, int x, int y, int w, int h, int value, int max, Color color, String label) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(x, y, w, h);
        int fill = max <= 0 ? 0 : (int) (w * Math.max(0, value) / (double) max);
        g2.setColor(color);
        g2.fillRect(x, y, fill, h);
        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, w, h);
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.drawString(label + " " + value + "/" + max, x + 8, y + 14);
    }

    // Draw the pause menu overlay
    private void drawPauseMenu(Graphics2D g2) {
        // Semi-transparent overlay
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
        
        // Title
        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("Monospaced", Font.BOLD, 48));
        String title = "PAUSED";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = centerX - fm.stringWidth(title) / 2;
        g2.drawString(title, titleX, centerY - 150);

        // Menu options
        int startY = centerY - 20;
        int spacing = 70;

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
        
        // Title
        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("Monospaced", Font.BOLD, 48));
        String title = "MOVEMENT CONTROLS";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = centerX - fm.stringWidth(title) / 2;
        g2.drawString(title, titleX, centerY - 150);

        // Control options
        int startY = centerY - 20;
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
        // Handle pause menu
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (inControlsMenu) {
                inControlsMenu = false;
            } else {
                isPaused = !isPaused;
            }
            repaint();
            return;
        }

        // Menu navigation
        if (isPaused) {
            if (inControlsMenu) {
                // Controls submenu navigation
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    selectedControlOption = (selectedControlOption - 1 + MOVEMENT_CONTROLS.length) % MOVEMENT_CONTROLS.length;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    selectedControlOption = (selectedControlOption + 1) % MOVEMENT_CONTROLS.length;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Apply selected control option (placeholder for future implementation)
                    System.out.println("Selected controls: " + MOVEMENT_CONTROLS[selectedControlOption]);
                }
            } else {
                // Main pause menu navigation
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

        // Game controls (when not paused)
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
