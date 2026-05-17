import java.awt.*;

/**
 * Exit portal for a level.
 * It stays inactive until the boss is defeated, then touching it advances.
 */
public class Portal {
    private final Rectangle bounds;
    private boolean active;
    private int pulse;

    public Portal(int x, int y) {
        bounds = new Rectangle(x, y, 70, 100);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void update() {
        // Only animate once the portal is available to the player.
        if (active) pulse = (pulse + 1) % 90;
    }

    public boolean canAdvance(Player player) {
        // A portal that is drawn but inactive should not end the level.
        return active && bounds.intersects(player.getBounds());
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        if (!active) {
            // Inactive portals are drawn as an outline so players know where to go later.
            g2.setColor(new Color(70, 70, 85, 100));
            g2.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
            return;
        }

        // Active portals pulse by changing transparency over time.
        int alpha = 120 + (int) (Math.sin(pulse / 90.0 * Math.PI * 2) * 55);
        g2.setColor(new Color(90, 220, 255, alpha));
        g2.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
        g2.setColor(new Color(255, 245, 120, 190));
        g2.drawOval(bounds.x + 8, bounds.y + 10, bounds.width - 16, bounds.height - 20);
        g2.setColor(Color.WHITE);
        g2.drawString("NEXT", bounds.x + 17, bounds.y + bounds.height + 18);
    }
}
