import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;

/**
 * Main gameplay panel for one level.
 * It owns the generated map, player, boss, pickups, projectiles, camera,
 * pause menu, and frame-by-frame game loop.
 */
public class Generator extends JPanel implements ActionListener, KeyListener, MouseListener {

    // --- Timer ---
    // Roughly 60 frames per second; actionPerformed is the game loop.
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
    // Camera offset is subtracted from world coordinates during drawing.
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
    private int selectedControlOption = 0;
    private boolean waitingForControlKey = false;

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
        // Each level instance gets a fresh random map layout.
        map  = generateMap(MAP_ROWS, MAP_COLS, seed);
        buildMap();

        player = new Player(tiles, difficulty, getWorldWidth(), getWorldHeight());
        setupLevelEntities();
        addKeyListener(player.keyAdapter);
        addKeyListener(this);
        addMouseListener(this);

        timer.start();
        System.out.println("Level " + currentLevel + ": " + LEVEL_NAMES[currentLevel - 1]);
    }

    // -------------------------------------------------------
    // MAP GENERATOR
    // -------------------------------------------------------
    int[][] generateMap(int rows, int cols, long seed) {
        Random random = new Random(seed);
        int[][] newMap = new int[rows][cols];
        int floorRow = rows - 3;
        int arenaStart = Math.max(8, cols - 12);

        // Start with a solid floor at the bottom of the world.
        for (int row = floorRow; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                newMap[row][col] = 1;
            }
        }

        // Keep the spawn area and boss arena open so important entities are reachable.
        clearArea(newMap, 0, 0, 6, floorRow);
        clearArea(newMap, arenaStart, 0, cols - arenaStart, floorRow);

        // Add randomized mid-level platforms between the spawn and boss arena.
        int platformRow = floorRow - 5 - random.nextInt(3);
        for (int col = 5; col < arenaStart - 5; col += 5 + random.nextInt(3)) {
            int length = 3 + random.nextInt(3);
            platformRow += random.nextInt(5) - 2;
            platformRow = Math.max(4, Math.min(floorRow - 5, platformRow));
            addPlatform(newMap, platformRow, col, Math.min(length, arenaStart - col - 2));
        }

        int bridgeRow = floorRow - 7;
        // A few fixed bridge platforms create a more reliable route across the map.
        for (int col = 8; col < arenaStart - 8; col += 10) {
            addPlatform(newMap, bridgeRow, col, 4);
        }

        // Final cleanup removes accidental blocks from key open areas.
        clearArea(newMap, 0, 0, 5, floorRow);
        clearArea(newMap, arenaStart, 0, cols - arenaStart, floorRow);
        clearArea(newMap, cols - 4, 0, 4, floorRow);
        return newMap;
    }

    private void addPlatform(int[][] targetMap, int row, int startCol, int length) {
        // Map value 1 means solid tile; 0 means empty space.
        if (length <= 0 || row < 0 || row >= targetMap.length) return;
        int endCol = Math.min(targetMap[row].length, startCol + length);
        for (int col = Math.max(0, startCol); col < endCol; col++) {
            targetMap[row][col] = 1;
        }
    }

    private void clearArea(int[][] targetMap, int startCol, int startRow, int width, int height) {
        // Clamp the requested rectangle so clearing never indexes outside the map.
        int endRow = Math.min(targetMap.length, startRow + height);
        for (int row = Math.max(0, startRow); row < endRow; row++) {
            int endCol = Math.min(targetMap[row].length, startCol + width);
            for (int col = Math.max(0, startCol); col < endCol; col++) {
                targetMap[row][col] = 0;
            }
        }
    }

    // -------------------------------------------------------
    // BUILD MAP
    // -------------------------------------------------------
    void buildMap() {
        tiles.clear();
        // Convert the tile grid into Rectangle hitboxes used by collision code.
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

    private void setupLevelEntities() {
        healingItems.clear();

        // Boss, portal, and pickups are placed in the arena at the far right.
        int floorY = (MAP_ROWS - 3) * TILE_SIZE;
        int arenaStart = Math.max(8, MAP_COLS - 12);
        int bossX = (MAP_COLS - 7) * TILE_SIZE;
        boss = new Boss(bossX, floorY - 86, currentLevel, difficulty, tiles,
                getWorldWidth(), getWorldHeight(), arenaStart * TILE_SIZE, getWorldWidth());
        portal = new Portal((MAP_COLS - 2) * TILE_SIZE, floorY - 100);

        int healAmount = 22 - difficulty.getLevel() * 3;
        healingItems.add(new HealingItem((MAP_COLS - 13) * TILE_SIZE, floorY - 38, healAmount));
        healingItems.add(new HealingItem((MAP_COLS - 9) * TILE_SIZE, floorY - 38, healAmount));
        if (difficulty == Difficulty.EASY) {
            healingItems.add(new HealingItem((MAP_COLS - 5) * TILE_SIZE, floorY - 38, healAmount));
        }
    }

    private void updateProjectiles() {
        // Iterate backward so projectiles can be safely removed during the loop.
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = projectiles.get(i);
            projectile.update(tiles);

            if (projectile.isActive()) {
                // A projectile can only damage the opposite side.
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
            // Remove old shots that have traveled far outside the playable space.
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
            // Repaint keeps the pause menu responsive without advancing gameplay.
            repaint();
            return;
        }

        player.update();

        // Resolve player attacks immediately after player movement/input.
        if (player.startAttack() && boss != null && !boss.isDead()
                && player.getAttackBounds().intersects(boss.getBounds())) {
            boss.takeDamage(18 + currentLevel * 2);
        }

        Projectile playerShot = player.shootProjectile();
        if (playerShot != null) {
            projectiles.add(playerShot);
        }

        if (boss != null) {
            // Boss updates may produce a projectile, then its body/attack can hurt the player.
            boss.update(player);
            Projectile bossShot = boss.fireProjectileIfReady(player);
            if (bossShot != null) {
                projectiles.add(bossShot);
            }
            if (boss.canDamagePlayer(player)) {
                player.hurt(boss.getCurrentDamage());
            }
            if (boss.isDead() && !portalOpened) {
                // The portal opens exactly once after the boss dies.
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
            // Falling far below the map is treated as lethal.
            player.hurt(player.getMaxHealth());
        }

        if (player.isDead()) {
            // GameUI handles screen changes; Generator just reports the result.
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
            // Touching an active portal advances to the next level.
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

        // Center the camera on the player, then clamp it to the map edges.
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

        // Everything drawn after this translate is in world coordinates.
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

        // Return to screen coordinates for HUD and menus.
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
        g2.drawString(Controls.getShortSummary(), 10, getHeight() - 10);

        // Draw pause menu if paused
        if (isPaused) {
            drawPauseMenu(g2);
        }
    }

    private void drawHud(Graphics2D g2) {
        // Player health is always shown; boss health is replaced by portal guidance on death.
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
        // Shared rectangle bar renderer for player and boss health.
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
        int optionCount = Controls.getActionCount() + 1;
        
        // Title
        g2.setColor(new Color(255, 200, 0));
        g2.setFont(new Font("Monospaced", Font.BOLD, 48));
        String title = "CHANGE COMMANDS";
        FontMetrics fm = g2.getFontMetrics();
        int titleX = centerX - fm.stringWidth(title) / 2;
        g2.drawString(title, titleX, centerY - 150);

        // Control options
        int startY = centerY - 70;
        int spacing = 46;

        for (int i = 0; i < optionCount; i++) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 22));
            
            if (i == selectedControlOption) {
                g2.setColor(new Color(58, 58, 255));
                g2.fillRect(centerX - 260, startY + i * spacing - 28, 520, 42);
                g2.setColor(new Color(10, 255, 110));
            } else {
                g2.setColor(new Color(232, 232, 255));
            }

            String option;
            if (i < Controls.getActionCount()) {
                option = Controls.getActionName(i) + ": " + Controls.getBindingText(i);
                if (waitingForControlKey && i == selectedControlOption) {
                    option = Controls.getActionName(i) + ": press key/click";
                }
            } else {
                option = "Reset to Defaults";
            }
            fm = g2.getFontMetrics();
            int optionX = centerX - fm.stringWidth(option) / 2;
            g2.drawString(option, optionX, startY + i * spacing);
        }

        g2.setColor(new Color(106, 106, 154));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        String help = waitingForControlKey
                ? "Press a new key or mouse button for this command, or ESC to cancel"
                : "Use UP/DOWN to select, ENTER to change, ESC to go back";
        fm = g2.getFontMetrics();
        g2.drawString(help, centerX - fm.stringWidth(help) / 2, getHeight() - 40);
    }

    // -------------------------------------------------------
    // CONTROLS
    // -------------------------------------------------------
    @Override
    public void keyPressed(KeyEvent e) {
        if (isPaused && inControlsMenu && waitingForControlKey) {
            // In capture mode, the next key either becomes the binding or cancels capture.
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                waitingForControlKey = false;
            } else if (selectedControlOption < Controls.getActionCount()) {
                Controls.setBinding(selectedControlOption, e.getKeyCode());
                player.resetInputState();
                waitingForControlKey = false;
            }
            repaint();
            return;
        }

        // Handle pause menu
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            // ESC backs out of controls first, otherwise toggles the pause overlay.
            if (inControlsMenu) {
                inControlsMenu = false;
                waitingForControlKey = false;
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
                    int optionCount = Controls.getActionCount() + 1;
                    selectedControlOption = (selectedControlOption - 1 + optionCount) % optionCount;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    int optionCount = Controls.getActionCount() + 1;
                    selectedControlOption = (selectedControlOption + 1) % optionCount;
                    repaint();
                } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (selectedControlOption < Controls.getActionCount()) {
                        // ENTER on an action starts listening for the replacement binding.
                        waitingForControlKey = true;
                    } else {
                        Controls.resetDefaults();
                        player.resetInputState();
                    }
                    repaint();
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

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        if (!isPaused) {
            // During gameplay, mouse input is forwarded to the player's input adapter.
            player.mouseAdapter.mousePressed(e);
            return;
        }

        if (inControlsMenu && waitingForControlKey) {
            // Mouse buttons can be used as bindings just like keyboard keys.
            if (selectedControlOption < Controls.getActionCount()) {
                Controls.setMouseBinding(selectedControlOption, e.getButton());
                player.resetInputState();
            }
            waitingForControlKey = false;
            repaint();
            return;
        }

        if (inControlsMenu) {
            int clickedOption = getControlsMenuOptionAt(e.getX(), e.getY());
            if (clickedOption >= 0) {
                selectedControlOption = clickedOption;
                if (clickedOption < Controls.getActionCount()) {
                    waitingForControlKey = true;
                } else {
                    Controls.resetDefaults();
                    player.resetInputState();
                }
                repaint();
            }
            return;
        }

        int clickedOption = getPauseMenuOptionAt(e.getX(), e.getY());
        if (clickedOption >= 0) {
            selectedMenuOption = clickedOption;
            handleMenuSelection();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (!isPaused) {
            player.mouseAdapter.mouseReleased(e);
        }
    }
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    private int getPauseMenuOptionAt(int mouseX, int mouseY) {
        // These rectangles mirror the positions used in drawMainPauseMenu().
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int startY = centerY - 20;
        int spacing = 70;
        for (int i = 0; i < 4; i++) {
            Rectangle optionBounds = new Rectangle(centerX - 180, startY + i * spacing - 25, 360, 50);
            if (optionBounds.contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private int getControlsMenuOptionAt(int mouseX, int mouseY) {
        // These rectangles mirror the positions used in drawControlsMenu().
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int startY = centerY - 70;
        int spacing = 46;
        int optionCount = Controls.getActionCount() + 1;
        for (int i = 0; i < optionCount; i++) {
            Rectangle optionBounds = new Rectangle(centerX - 260, startY + i * spacing - 28, 520, 42);
            if (optionBounds.contains(mouseX, mouseY)) {
                return i;
            }
        }
        return -1;
    }

    private int getWorldWidth() {
        return MAP_COLS * TILE_SIZE;
    }

    private int getWorldHeight() {
        return MAP_ROWS * TILE_SIZE;
    }
}
