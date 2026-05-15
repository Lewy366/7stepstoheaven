import java.awt.*;

public class HealingItem {
    private final Rectangle bounds;
    private final int amount;
    private boolean collected;
    private int pulse;

    public HealingItem(int x, int y, int amount) {
        this.bounds = new Rectangle(x, y, 34, 34);
        this.amount = amount;
    }

    public void update() {
        pulse = (pulse + 1) % 60;
    }

    public boolean collectIfTouched(Player player) {
        if (collected || !bounds.intersects(player.getBounds())) return false;
        player.heal(amount);
        collected = true;
        return true;
    }

    public boolean isCollected() {
        return collected;
    }

    public void draw(Graphics g) {
        if (collected) return;
        Graphics2D g2 = (Graphics2D) g;
        int glow = 40 + (int) (Math.sin(pulse / 60.0 * Math.PI * 2) * 20);
        g2.setColor(new Color(40, 255, 135, 80 + glow));
        g2.fillOval(bounds.x - 6, bounds.y - 6, bounds.width + 12, bounds.height + 12);
        g2.setColor(new Color(20, 210, 95));
        g2.fillRect(bounds.x + 11, bounds.y + 3, 12, 28);
        g2.fillRect(bounds.x + 3, bounds.y + 11, 28, 12);
        g2.setColor(Color.WHITE);
        g2.drawRect(bounds.x + 11, bounds.y + 3, 12, 28);
        g2.drawRect(bounds.x + 3, bounds.y + 11, 28, 12);
    }
}
