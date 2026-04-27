import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Platformer extends JPanel implements ActionListener, KeyListener {

    // --- Window ---
    Timer timer = new Timer(16, this);

    // --- Map ---
    int TILE_SIZE = 50;

    int[][] map = {
            {0,0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,0,0,0,0},
            {0,0,0,0,1,1,1,0,0,0,0,0},
            {0,0,0,0,0,0,0,0,1,0,0,0},
            {0,0,1,1,1,0,0,0,1,0,0,0},
            {0,0,0,0,0,0,1,1,1,0,0,0},
            {1,1,1,1,1,0,0,0,1,1,1,1,0,0,0,1,1,1,1,0,0,0,1,1,1,1,0,0,0,1,1,1,1,0,0,0,1,1,1,1},
            {0,0,0,0,0,0,1,1,1,0,0,0},
            {1,1,1,1,1,1,1,1,1,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1}
    };

    ArrayList<Rectangle> tiles = new ArrayList<>();

    // --- Player ---
    Rectangle player = new Rectangle(100, 100, 40, 40);
    double velX = 0;
    double velY = 0;

    boolean left, right, jumping;
    boolean onGround = false;

    public Platformer() {
        JFrame frame = new JFrame("Platformer Example");

        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(this);
        frame.addKeyListener(this);
        frame.setVisible(true);

        buildMap();

        timer.start();
    }

    void buildMap() {
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

    @Override
    public void actionPerformed(ActionEvent e) {
        updatePlayer();
        repaint();
    }

    void updatePlayer() {
        // --- Movement input ---
        if (left) velX = -5;
        else if (right) velX = 5;
        else velX = 0;

        // --- Gravity ---
        velY += 0.5;
        if (velY > 10) velY = 10;

        // --- Horizontal movement ---
        player.x += velX;
        for (Rectangle tile : tiles) {
            if (player.intersects(tile)) {
                if (velX > 0) {
                    player.x = tile.x - player.width;
                } else if (velX < 0) {
                    player.x = tile.x + tile.width;
                }
            }
        }

        // --- Vertical movement ---
        player.y += velY;
        onGround = false;

        for (Rectangle tile : tiles) {
            if (player.intersects(tile)) {
                if (velY > 0) {
                    player.y = tile.y - player.height;
                    velY = 0;
                    onGround = true;
                } else if (velY < 0) {
                    player.y = tile.y + tile.height;
                    velY = 0;
                }
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // draw tiles
        g.setColor(Color.GRAY);
        for (Rectangle tile : tiles) {
            g.fillRect(tile.x, tile.y, tile.width, tile.height);
        }

        // draw player
        g.setColor(Color.RED);
        g.fillRect(player.x, player.y, player.width, player.height);
    }

    // --- Controls ---
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) left = true;
        if (e.getKeyCode() == KeyEvent.VK_D) right = true;

        if (e.getKeyCode() == KeyEvent.VK_SPACE && onGround) {
            velY = -10;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_A) left = false;
        if (e.getKeyCode() == KeyEvent.VK_D) right = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        new Platformer();
    }
}