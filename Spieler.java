import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Spieler {

    int playerX = 100;
    int playerY = 250;

    int speed = 5;

    double velocityY = 0;
    double gravity = 1.0;
    double maxFallSpeed = 20;

    boolean isJumping = false;
    boolean onGround = false;

    boolean leftPressed = false;
    boolean rightPressed = false;

    // Tiles werden von Platformer übergeben
    ArrayList<Rectangle> tiles;

    // KeyAdapter wird von Platformer an den JFrame gehängt
    KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_A) leftPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_D) rightPressed = true;
            if (e.getKeyCode() == KeyEvent.VK_SPACE && !isJumping) {
                velocityY = -18; // Sprung (original)
                isJumping = true;
            }
        }
        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_A) leftPressed = false;
            if (e.getKeyCode() == KeyEvent.VK_D) rightPressed = false;
        }
    };

    public Spieler(ArrayList<Rectangle> tiles) {
        this.tiles = tiles;
    }

    public void update() {

        // Links / Rechts Bewegung (original)
        if (leftPressed)  playerX -= speed;
        if (rightPressed) playerX += speed;

        // Horizontale Tile-Kollision
        Rectangle player = new Rectangle(playerX, playerY, 50, 50);
        for (Rectangle tile : tiles) {
            if (player.intersects(tile)) {
                if (rightPressed) {
                    playerX = tile.x - 50;
                } else if (leftPressed) {
                    playerX = tile.x + tile.width;
                }
                player = new Rectangle(playerX, playerY, 50, 50);
            }
        }

        // Schwerkraft mit Beschleunigung (original)
        velocityY += gravity;
        if (velocityY > maxFallSpeed) velocityY = maxFallSpeed;

        playerY += velocityY;

        // Vertikale Tile-Kollision
        onGround = false;
        player = new Rectangle(playerX, playerY, 50, 50);
        for (Rectangle tile : tiles) {
            if (player.intersects(tile)) {
                if (velocityY > 0) {
                    playerY = tile.y - 50;
                    velocityY = 0;
                    isJumping = false;
                    onGround = true;
                } else if (velocityY < 0) {
                    playerY = tile.y + tile.height;
                    velocityY = 0;
                }
                player = new Rectangle(playerX, playerY, 50, 50);
            }
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(playerX, playerY, 50, 50);
    }
}
