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
    private boolean onGround;
    private int stuckFrames;
    private int worldWidth;
    private int worldHeight;
    private int minX;
    private int maxX;

    public Boss(int x, int y, int level, Difficulty difficulty, ArrayList<Rectangle> tiles,
                int worldWidth, int worldHeight, int minX, int maxX) {
        this.x = x;
        this.y = y;
        this.level = level;
        this.difficulty = difficulty;
        this.tiles = tiles;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.minX = minX;
        this.maxX = maxX;
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
        int direction = facingRight ? 1 : -1;
        boolean safeTowardPlayer = canMoveInDirection(direction);
        boolean safeAwayFromPlayer = canMoveInDirection(-direction);
        boolean playerAbove = playerBounds.y + playerBounds.height < y - 18;

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
            if (!safeTowardPlayer) {
                windupFrames = 0;
                attackCooldownFrames = 24;
                velocityX = 0;
            }
            if (windupFrames == 0) {
                attackFrames = 13 + difficulty.getLevel() * 2;
                velocityX = safeTowardPlayer ? direction * speed * 4.5 : 0;
            }
        } else if (attackFrames > 0) {
            attackFrames--;
        } else if (distance < 130 && attackCooldownFrames == 0) {
            windupFrames = Math.max(10, 28 - difficulty.getLevel() * 5);
            attackCooldownFrames = Math.max(34, 82 - difficulty.getLevel() * 13 - level * 2);
        } else if ((distance < 650 || !safeTowardPlayer) && rangedCooldownFrames == 0) {
            rangedWindupFrames = Math.max(14, 34 - difficulty.getLevel() * 4);
            rangedCooldownFrames = Math.max(45, 110 - difficulty.getLevel() * 17 - level * 3);
        } else {
            if (distance < 70 && safeAwayFromPlayer) {
                velocityX += -direction * 0.46;
            } else if (distance > 78 && safeTowardPlayer) {
                velocityX += direction * 0.38;
            } else {
                velocityX *= 0.78;
            }
            if (velocityX > speed) velocityX = speed;
            if (velocityX < -speed) velocityX = -speed;

            if (playerAbove && onGround && Math.abs(playerCenter - bossCenter) < 180) {
                velocityY = -14;
                onGround = false;
            }
        }

        if (!canMoveInDirection(velocityX > 0 ? 1 : -1)) {
            velocityX = 0;
        }

        velocityY += 1.0;
        if (velocityY > 18) velocityY = 18;

        int oldX = x;
        moveX();
        moveY();

        if (Math.abs(x - oldX) < 1 && Math.abs(velocityX) > 0.2) {
            stuckFrames++;
            if (stuckFrames > 18 && onGround) {
                velocityY = -12;
                stuckFrames = 0;
            }
        } else {
            stuckFrames = 0;
        }
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
        clampXToWorld();
    }

    private void moveY() {
        y += Math.round(velocityY);
        Rectangle bounds = getBounds();
        onGround = false;
        for (Rectangle tile : tiles) {
            if (bounds.intersects(tile)) {
                if (velocityY > 0) {
                    y = tile.y - height;
                    velocityY = 0;
                    onGround = true;
                } else if (velocityY < 0) {
                    y = tile.y + tile.height;
                    velocityY = 0;
                }
                bounds = getBounds();
            }
        }
        clampYToWorld();
    }

    private boolean canMoveInDirection(int direction) {
        if (direction == 0) return true;
        if ((direction < 0 && x <= minX) || (direction > 0 && x + width >= maxX)) {
            return false;
        }
        Rectangle nextBody = new Rectangle(x + direction * 10, y, width, height);
        for (Rectangle tile : tiles) {
            if (nextBody.intersects(tile)) {
                return false;
            }
        }
        return hasGroundAhead(direction);
    }

    private boolean hasGroundAhead(int direction) {
        int probeX = direction > 0 ? x + width + 6 : x - 24;
        Rectangle probe = new Rectangle(probeX, y + height + 2, 24, 70);
        for (Rectangle tile : tiles) {
            if (probe.intersects(tile)) {
                return true;
            }
        }
        return false;
    }

    private void clampXToWorld() {
        int leftLimit = Math.max(0, minX);
        int rightLimit = Math.max(leftLimit, Math.min(worldWidth, maxX) - width);
        if (x < leftLimit) {
            x = leftLimit;
            velocityX = Math.max(0, velocityX);
        } else if (x > rightLimit) {
            x = rightLimit;
            velocityX = Math.min(0, velocityX);
        }
    }

    private void clampYToWorld() {
        int maxY = Math.max(0, worldHeight - height);
        if (y < 0) {
            y = 0;
            velocityY = Math.max(0, velocityY);
        } else if (y > maxY) {
            y = maxY;
            velocityY = 0;
            onGround = true;
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
