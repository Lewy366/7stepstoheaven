import java.awt.*;
import java.util.ArrayList;

public class Boss {
    private final Difficulty difficulty;
    private final int level;
    private final ArrayList<Rectangle> tiles;

    int x;
    int y;
    int width = 78;
    int height = 86;

    private int maxHealth;
    private int health;
    private int contactDamage;
    private int attackDamage;
    private int attackCooldownFrames;
    private int rangedCooldownFrames;
    private int windupFrames;
    private int attackFrames;
    private int rangedWindupFrames;
    private boolean rangedShotReady;
    private int hurtFlashFrames;
    private double velocityX;
    private double velocityY;
    private boolean facingRight;

    public Boss(int x, int y, int level, Difficulty difficulty, ArrayList<Rectangle> tiles) {
        this.x = x;
        this.y = y;
        this.level = level;
        this.difficulty = difficulty;
        this.tiles = tiles;
        maxHealth = 95 + level * 18 + difficulty.getLevel() * 35;
        health = maxHealth;
        contactDamage = 7 + difficulty.getLevel() * 3;
        attackDamage = 12 + difficulty.getLevel() * 5;
    }

    public void update(Player player) {
        if (isDead()) return;

        if (attackCooldownFrames > 0) attackCooldownFrames--;
        if (rangedCooldownFrames > 0) rangedCooldownFrames--;
        if (hurtFlashFrames > 0) hurtFlashFrames--;

        Rectangle playerBounds = player.getBounds();
        int bossCenter = x + width / 2;
        int playerCenter = playerBounds.x + playerBounds.width / 2;
        facingRight = playerCenter > bossCenter;

        int distance = Math.abs(playerCenter - bossCenter);
        double speed = 2.1 + difficulty.getLevel() * 0.75 + level * 0.12;

        rangedShotReady = false;

        if (rangedWindupFrames > 0) {
            rangedWindupFrames--;
            velocityX *= 0.8;
            if (rangedWindupFrames == 0) {
                rangedShotReady = true;
            }
        } else if (windupFrames > 0) {
            windupFrames--;
            velocityX *= 0.82;
            if (windupFrames == 0) {
                attackFrames = 13 + difficulty.getLevel() * 2;
                velocityX = facingRight ? speed * 4.5 : -speed * 4.5;
            }
        } else if (attackFrames > 0) {
            attackFrames--;
        } else if (distance < 130 && attackCooldownFrames == 0) {
            windupFrames = Math.max(10, 28 - difficulty.getLevel() * 5);
            attackCooldownFrames = Math.max(34, 82 - difficulty.getLevel() * 13 - level * 2);
        } else if (distance < 650 && rangedCooldownFrames == 0) {
            rangedWindupFrames = Math.max(14, 34 - difficulty.getLevel() * 4);
            rangedCooldownFrames = Math.max(45, 110 - difficulty.getLevel() * 17 - level * 3);
        } else {
            if (distance > 58) {
                velocityX += facingRight ? 0.38 : -0.38;
            } else {
                velocityX *= 0.78;
            }
            if (velocityX > speed) velocityX = speed;
            if (velocityX < -speed) velocityX = -speed;
        }

        velocityY += 1.0;
        if (velocityY > 18) velocityY = 18;

        moveX();
        moveY();
    }

    private void moveX() {
        x += Math.round(velocityX);
        Rectangle bounds = getBounds();
        for (Rectangle tile : tiles) {
            if (bounds.intersects(tile)) {
                if (velocityX > 0) {
                    x = tile.x - width;
                } else if (velocityX < 0) {
                    x = tile.x + tile.width;
                }
                velocityX = 0;
                bounds = getBounds();
            }
        }
    }

    private void moveY() {
        y += Math.round(velocityY);
        Rectangle bounds = getBounds();
        for (Rectangle tile : tiles) {
            if (bounds.intersects(tile)) {
                if (velocityY > 0) {
                    y = tile.y - height;
                    velocityY = 0;
                } else if (velocityY < 0) {
                    y = tile.y + tile.height;
                    velocityY = 0;
                }
                bounds = getBounds();
            }
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public Rectangle getAttackBounds() {
        int range = 70 + difficulty.getLevel() * 8;
        int attackX = facingRight ? x + width : x - range;
        return new Rectangle(attackX, y + 18, range, height - 28);
    }

    public boolean canDamagePlayer(Player player) {
        if (isDead()) return false;
        if (getBounds().intersects(player.getBounds())) return true;
        return attackFrames > 0 && getAttackBounds().intersects(player.getBounds());
    }

    public int getCurrentDamage() {
        return attackFrames > 0 ? attackDamage : contactDamage;
    }

    public Projectile fireProjectileIfReady(Player player) {
        if (!rangedShotReady || isDead()) return null;
        rangedShotReady = false;

        Rectangle playerBounds = player.getBounds();
        double originX = facingRight ? x + width : x - 24;
        double originY = y + 32;
        double targetX = playerBounds.x + playerBounds.width / 2.0;
        double targetY = playerBounds.y + playerBounds.height / 2.0;
        double dx = targetX - originX;
        double dy = targetY - originY;
        double length = Math.max(1, Math.sqrt(dx * dx + dy * dy));
        double speed = 6.5 + difficulty.getLevel() * 1.2 + level * 0.18;
        return new Projectile(originX, originY, dx / length * speed, dy / length * speed,
                24, 24, 8 + difficulty.getLevel() * 4, false, 150);
    }

    public void takeDamage(int damage) {
        health -= damage;
        hurtFlashFrames = 8;
        velocityX += facingRight ? -5 : 5;
        if (health < 0) health = 0;
    }

    public boolean isDead() {
        return health <= 0;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Rectangle body = getBounds();

        if (hurtFlashFrames > 0) {
            g2.setColor(new Color(255, 235, 235));
        } else if (windupFrames > 0 || rangedWindupFrames > 0) {
            g2.setColor(new Color(255, 95, 80));
        } else {
            g2.setColor(new Color(165, 42, 56));
        }
        g2.fillRect(body.x, body.y, body.width, body.height);

        g2.setColor(new Color(70, 0, 18));
        g2.fillRect(body.x + 10, body.y + 12, body.width - 20, 12);

        g2.setColor(Color.YELLOW);
        int eyeX = facingRight ? body.x + 52 : body.x + 18;
        g2.fillRect(eyeX, body.y + 30, 9, 9);

        if (attackFrames > 0 || windupFrames > 0) {
            Rectangle attack = getAttackBounds();
            g2.setColor(new Color(255, 80, 80, attackFrames > 0 ? 155 : 70));
            g2.fillRect(attack.x, attack.y, attack.width, attack.height);
            g2.setColor(new Color(255, 180, 150));
            g2.drawRect(attack.x, attack.y, attack.width, attack.height);
        }

        if (rangedWindupFrames > 0) {
            int orbX = facingRight ? body.x + body.width + 8 : body.x - 28;
            g2.setColor(new Color(255, 90, 40, 180));
            g2.fillOval(orbX, body.y + 26, 24, 24);
            g2.setColor(Color.WHITE);
            g2.drawOval(orbX, body.y + 26, 24, 24);
        }
    }
}
