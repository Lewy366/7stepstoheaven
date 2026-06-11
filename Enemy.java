import java.awt.*;
import java.util.ArrayList;

public class Enemy {

    // Position und Größe
    float x, y;
    int width  = 40;
    int height = 40;

    // Patrol-Grenzen (in Pixel)
    float patrolLeft;
    float patrolRight;

    // Geschwindigkeit
    float speedX = 2f;
    final float CHASE_SPEED  = 3.5f;
    final float PATROL_SPEED = 2f;

    // Schwerkraft
    float velY  = 0f;
    final float GRAVITY  = 0.5f;
    final float MAX_FALL = 15f;

    boolean onGround = false;

    // Zustand
    enum State { PATROL, CHASE, ATTACK }
    State state = State.PATROL;

    // Angriffs-Animation
    int attackTimer    = 0;
    final int ATTACK_DURATION = 30; // Frames
    float swordAngle = 0f;

    // Farben
    Color bodyColor  = new Color(200, 40, 40);
    Color eyeColor   = Color.WHITE;
    Color pupilColor = Color.BLACK;

    public Enemy(float startX, float startY, float patrolLeft, float patrolRight) {
        this.x           = startX;
        this.y           = startY;
        this.patrolLeft  = patrolLeft;
        this.patrolRight = patrolRight;
    }

    public void update(ArrayList<Rectangle> tiles, Spieler spieler) {

        float playerCenterX = spieler.playerX + 25;
        float playerCenterY = spieler.playerY + 25;
        float enemyCenterX  = x + width  / 2f;
        float enemyCenterY  = y + height / 2f;

        boolean playerInPatrolZone =
            spieler.playerX + 50 >= patrolLeft &&
            spieler.playerX      <= patrolRight;

        // --- Zustand bestimmen ---
        if (playerInPatrolZone) {
            float dx = Math.abs(playerCenterX - enemyCenterX);
            float dy = Math.abs(playerCenterY - enemyCenterY);
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < 55) {
                state = State.ATTACK;
            } else {
                state = State.CHASE;
            }
        } else {
            state = State.PATROL;
        }

        // --- Horizontale Bewegung je nach Zustand ---
        if (state == State.PATROL) {
            x += speedX;

            if (x <= patrolLeft) {
                x = patrolLeft;
                speedX =  PATROL_SPEED;
            } else if (x + width >= patrolRight) {
                x = patrolRight - width;
                speedX = -PATROL_SPEED;
            }

        } else if (state == State.CHASE) {
            speedX = (playerCenterX > enemyCenterX) ? CHASE_SPEED : -CHASE_SPEED;
            x += speedX;

        } else if (state == State.ATTACK) {
            // Stehen bleiben, Schwert-Animation
            if (attackTimer < ATTACK_DURATION) {
                attackTimer++;
                swordAngle = (float) Math.sin(attackTimer * Math.PI / ATTACK_DURATION) * 90f;
            } else {
                attackTimer = 0;
            }
            speedX = (playerCenterX > enemyCenterX) ? PATROL_SPEED : -PATROL_SPEED; // Blickrichtung
        }

        // --- Horizontale Tile-Kollision ---
        Rectangle bounds = getBounds();
        for (Rectangle tile : tiles) {
            if (bounds.intersects(tile)) {
                if (speedX > 0) {
                    x = tile.x - width;
                } else {
                    x = tile.x + tile.width;
                }
                if (state == State.PATROL) speedX = -speedX;
                bounds = getBounds();
            }
        }

        // --- Schwerkraft ---
        velY = Math.min(velY + GRAVITY, MAX_FALL);
        y += velY;
        onGround = false;

        // --- Vertikale Tile-Kollision ---
        bounds = getBounds();
        for (Rectangle tile : tiles) {
            if (bounds.intersects(tile)) {
                if (velY > 0) {
                    y = tile.y - height;
                    velY = 0;
                    onGround = true;
                } else if (velY < 0) {
                    y = tile.y + tile.height;
                    velY = 0;
                }
                bounds = getBounds();
            }
        }
    }

    public void draw(Graphics2D g2) {
        int ix = (int) x;
        int iy = (int) y;

        // --- Schwert (nur bei ATTACK) ---
        if (state == State.ATTACK) {
            drawSword(g2, ix, iy);
        }

        // --- Körper ---
        Color currentBody = (state == State.CHASE || state == State.ATTACK)
            ? new Color(220, 80, 0)   // orange wenn aggressiv
            : bodyColor;

        g2.setColor(currentBody);
        g2.fillRoundRect(ix, iy, width, height, 10, 10);
        g2.setColor(currentBody.darker());
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(ix, iy, width, height, 10, 10);

        // --- Auge ---
        int eyeOffsetX = (speedX >= 0) ? width - 14 : 4;
        g2.setColor(eyeColor);
        g2.fillOval(ix + eyeOffsetX, iy + 8, 12, 12);
        int pupilX = (speedX >= 0) ? ix + eyeOffsetX + 4 : ix + eyeOffsetX + 1;
        g2.setColor(pupilColor);
        g2.fillOval(pupilX, iy + 11, 6, 6);

        // --- Mund (wütend bei Chase/Attack) ---
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        if (state == State.PATROL) {
            g2.drawLine(ix + 10, iy + height - 10, ix + width - 10, iy + height - 10);
        } else {
            // Wütender Mund (Zacken)
            g2.drawLine(ix + 8,  iy + height - 8,  ix + 16, iy + height - 12);
            g2.drawLine(ix + 16, iy + height - 12, ix + 24, iy + height - 8);
            g2.drawLine(ix + 24, iy + height - 8,  ix + 32, iy + height - 12);
        }

        g2.setStroke(new BasicStroke(1));
    }

    private void drawSword(Graphics2D g2, int ix, int iy) {
        Graphics2D sg = (Graphics2D) g2.create();

        // Drehpunkt: Schulter des Gegners
        int pivotX = (speedX >= 0) ? ix + width : ix;
        int pivotY = iy + 10;

        float angle = (speedX >= 0)
            ? (float) Math.toRadians(-45 + swordAngle)
            : (float) Math.toRadians(225 - swordAngle);

        sg.translate(pivotX, pivotY);
        sg.rotate(angle);

        // Klinge
        sg.setColor(new Color(200, 200, 220));
        sg.fillRect(0, -3, 35, 6);

        // Spitze
        int[] spitzeX = {35, 35, 48};
        int[] spitzeY = {-3,  3,  0};
        sg.fillPolygon(spitzeX, spitzeY, 3);

        // Griff
        sg.setColor(new Color(120, 80, 30));
        sg.fillRect(-10, -2, 10, 4);

        // Parierstange
        sg.setColor(Color.DARK_GRAY);
        sg.fillRect(-2, -7, 4, 14);

        sg.dispose();
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }
}