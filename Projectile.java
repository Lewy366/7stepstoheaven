import java.awt.*;
import java.util.ArrayList;

public class Projectile {
    private double x;
    private double y;
    private final double velocityX;
    private final double velocityY;
    private final int width;
    private final int height;
    private final int damage;
    private final boolean fromPlayer;
    private boolean active = true;
    private int lifeFrames;

    public Projectile(double x, double y, double velocityX, double velocityY,
                      int width, int height, int damage, boolean fromPlayer, int lifeFrames) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.width = width;
        this.height = height;
        this.damage = damage;
        this.fromPlayer = fromPlayer;
        this.lifeFrames = lifeFrames;
    }

    public void update(ArrayList<Rectangle> tiles) {
        if (!active) return;
        x += velocityX;
        y += velocityY;
        lifeFrames--;

        Rectangle bounds = getBounds();
        for (Rectangle tile : tiles) {
            if (bounds.intersects(tile)) {
                active = false;
                return;
            }
        }

        if (lifeFrames <= 0) active = false;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    public int getDamage() {
        return damage;
    }

    public boolean isFromPlayer() {
        return fromPlayer;
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }

    public void draw(Graphics g) {
        if (!active) return;
        Graphics2D g2 = (Graphics2D) g;
        Rectangle bounds = getBounds();
        if (fromPlayer) {
            g2.setColor(new Color(120, 230, 255));
            g2.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            g2.setColor(Color.WHITE);
            g2.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        } else {
            g2.setColor(new Color(255, 80, 45));
            g2.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            g2.setColor(new Color(255, 220, 120));
            g2.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }
}
