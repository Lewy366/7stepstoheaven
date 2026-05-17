import java.awt.*;
import java.util.ArrayList;

/**
 * Level boss enemy.
 * Handles simple platform-aware movement, melee/ranged attacks, health, and drawing.
 */
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
    // Frame counters control attack timing without needing separate timer objects.
    private int attackCooldownFrames;
    private int rangedCooldownFrames;
    private int windupFrames;
    private int attackFrames;
    private int rangedWindupFrames;
    private boolean rangedShotReady;
    private int slamCooldownFrames;
    private int slamWindupFrames;
    private int slamFlashFrames;
    private boolean shockwaveReady;
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
        // Boss stats scale with both the level number and selected difficulty.
        maxHealth = 100 + level * 30 + difficulty.getLevel() * 40;
        health = maxHealth;
        contactDamage = 7 + difficulty.getLevel() * 3 + level;
        attackDamage = 12 + difficulty.getLevel() * 5 + level * 2;
    }

    public void update(Player player) {
        if (isDead()) return;

        // Count down temporary states before choosing this frame's action.
        if (attackCooldownFrames > 0) attackCooldownFrames--;
        if (rangedCooldownFrames > 0) rangedCooldownFrames--;
        if (slamCooldownFrames > 0) slamCooldownFrames--;
        if (slamFlashFrames > 0) slamFlashFrames--;
        if (hurtFlashFrames > 0) hurtFlashFrames--;

        // Aim every action at the player's current center point.
        Rectangle playerBounds = player.getBounds();
        int bossCenter = x + width / 2;
        int playerCenter = playerBounds.x + playerBounds.width / 2;
        facingRight = playerCenter > bossCenter;

        int distance = Math.abs(playerCenter - bossCenter);
        boolean enraged = isEnraged();
        double speed = 2.1 + difficulty.getLevel() * 0.75 + level * 0.22 + (enraged ? 0.75 : 0);
        int direction = facingRight ? 1 : -1;
        boolean safeTowardPlayer = canMoveInDirection(direction);
        boolean safeAwayFromPlayer = canMoveInDirection(-direction);
        boolean playerAbove = playerBounds.y + playerBounds.height < y - 18;

        rangedShotReady = false;
        shockwaveReady = false;

        if (slamWindupFrames > 0) {
            // Higher-level bosses can stomp to send shockwaves across the arena.
            slamWindupFrames--;
            velocityX *= 0.72;
            if (slamWindupFrames == 0) {
                shockwaveReady = true;
                slamFlashFrames = 14;
            }
        } else if (rangedWindupFrames > 0) {
            // During ranged windup the boss slows down, then fires on the final frame.
            rangedWindupFrames--;
            velocityX *= 0.8;
            if (rangedWindupFrames == 0) {
                rangedShotReady = true;
            }
        } else if (windupFrames > 0) {
            // Melee windup telegraphs a dash attack before it launches.
            windupFrames--;
            velocityX *= 0.82;
            if (!safeTowardPlayer) {
                // Cancel the dash if it would run into a wall or off a platform.
                windupFrames = 0;
                attackCooldownFrames = 24;
                velocityX = 0;
            }
            if (windupFrames == 0) {
                // Attack frames keep the hitbox active while the boss lunges.
                attackFrames = 13 + difficulty.getLevel() * 2;
                velocityX = safeTowardPlayer ? direction * speed * 4.5 : 0;
            }
        } else if (attackFrames > 0) {
            attackFrames--;
        } else if (distance < 130 && attackCooldownFrames == 0) {
            // Prefer melee when close enough.
            windupFrames = Math.max(8, 28 - difficulty.getLevel() * 5 - level / 2);
            attackCooldownFrames = Math.max(28, 82 - difficulty.getLevel() * 13 - level * 4 - (enraged ? 12 : 0));
        } else if (level >= 5 && onGround && distance < 430 && slamCooldownFrames == 0) {
            // Late-game bosses add area control so standing still near them is dangerous.
            slamWindupFrames = Math.max(16, 44 - difficulty.getLevel() * 4 - level);
            slamCooldownFrames = Math.max(80, 185 - difficulty.getLevel() * 22 - level * 9);
        } else if ((distance < 650 || !safeTowardPlayer) && rangedCooldownFrames == 0) {
            // Use ranged attacks when the player is reachable but not ideal for melee.
            rangedWindupFrames = Math.max(11, 34 - difficulty.getLevel() * 4 - level / 2);
            rangedCooldownFrames = Math.max(36, 110 - difficulty.getLevel() * 17 - level * 6 - (enraged ? 10 : 0));
        } else {
            // Normal movement: back up if too close, chase if too far, otherwise slow down.
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
                // Hop up toward platforms when the player is above the boss.
                velocityY = -14;
                onGround = false;
            }
        }

        if (!canMoveInDirection(velocityX > 0 ? 1 : -1)) {
            // A final safety check keeps the boss from walking off ledges.
            velocityX = 0;
        }

        // Gravity applies after choosing movement, then collision resolves both axes.
        velocityY += 1.0;
        if (velocityY > 18) velocityY = 18;

        int oldX = x;
        moveX();
        moveY();

        if (Math.abs(x - oldX) < 1 && Math.abs(velocityX) > 0.2) {
            stuckFrames++;
            if (stuckFrames > 18 && onGround) {
                // If movement is blocked for a while, jump to try to escape.
                velocityY = -12 - Math.min(3, level / 2);
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
                // Push the boss to the near side of the wall it hit.
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
                // Falling into a tile means the boss has landed.
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
        // Respect the arena limits passed in from the level generator.
        if ((direction < 0 && x <= minX) || (direction > 0 && x + width >= maxX)) {
            return false;
        }
        Rectangle nextBody = new Rectangle(x + direction * 10, y, width, height);
        for (Rectangle tile : tiles) {
            // Do not intentionally walk into solid terrain.
            if (nextBody.intersects(tile)) {
                return false;
            }
        }
        return hasGroundAhead(direction);
    }

    private boolean hasGroundAhead(int direction) {
        // A downward probe checks for floor just beyond the boss's feet.
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
        // minX/maxX confine the boss to the arena section of the map.
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
        // The melee rectangle is placed on the side the boss is facing.
        int range = 70 + difficulty.getLevel() * 8;
        int attackX = facingRight ? x + width : x - range;
        return new Rectangle(attackX, y + 18, range, height - 28);
    }

    public boolean canDamagePlayer(Player player) {
        if (isDead()) return false;
        // Touching the body deals contact damage; active melee frames deal attack damage.
        if (getBounds().intersects(player.getBounds())) return true;
        return attackFrames > 0 && getAttackBounds().intersects(player.getBounds());
    }

    public int getCurrentDamage() {
        return attackFrames > 0 ? attackDamage : contactDamage;
    }

    public ArrayList<Projectile> fireProjectilesIfReady(Player player) {
        ArrayList<Projectile> shots = new ArrayList<>();
        if (isDead()) return shots;

        if (shockwaveReady) {
            shockwaveReady = false;
            double originY = y + height - 28;
            double speed = 8.5 + level * 0.35 + difficulty.getLevel() * 0.45;
            int damage = 10 + level + difficulty.getLevel() * 4;
            shots.add(createProjectile(x - 32, originY, -1, 0, speed, 34, 20, damage, 110));
            shots.add(createProjectile(x + width - 2, originY, 1, 0, speed, 34, 20, damage, 110));
        }

        if (!rangedShotReady) return shots;
        rangedShotReady = false;

        // Aim the projectile directly at the player's center at the moment of firing.
        Rectangle playerBounds = player.getBounds();
        double originX = facingRight ? x + width : x - 24;
        double originY = y + 32;
        double targetX = playerBounds.x + playerBounds.width / 2.0;
        double targetY = playerBounds.y + playerBounds.height / 2.0;
        double dx = targetX - originX;
        double dy = targetY - originY;
        double baseAngle = Math.atan2(dy, dx);
        double speed = 6.5 + difficulty.getLevel() * 1.2 + level * 0.32;
        int damage = 8 + difficulty.getLevel() * 4 + level;

        if (level >= 6) {
            // Final levels fire a five-shot fan.
            for (double offset : new double[]{ -0.34, -0.17, 0, 0.17, 0.34 }) {
                shots.add(createProjectileAtAngle(originX, originY, baseAngle + offset, speed, 22, 22, damage, 150));
            }
        } else if (level >= 3) {
            // Middle levels fire a three-shot spread.
            for (double offset : new double[]{ -0.22, 0, 0.22 }) {
                shots.add(createProjectileAtAngle(originX, originY, baseAngle + offset, speed, 23, 23, damage, 150));
            }
        } else {
            shots.add(createProjectileAtAngle(originX, originY, baseAngle, speed, 24, 24, damage, 150));
        }

        return shots;
    }

    private Projectile createProjectileAtAngle(double originX, double originY, double angle,
                                               double speed, int width, int height,
                                               int damage, int lifeFrames) {
        return createProjectile(originX, originY, Math.cos(angle), Math.sin(angle),
                speed, width, height, damage, lifeFrames);
    }

    private Projectile createProjectile(double originX, double originY, double unitX, double unitY,
                                        double speed, int width, int height,
                                        int damage, int lifeFrames) {
        return new Projectile(originX, originY, unitX * speed, unitY * speed,
                width, height, damage, false, lifeFrames);
    }

    public void takeDamage(int damage) {
        health -= damage;
        hurtFlashFrames = 8;
        // Knockback pushes the boss away from the direction it was facing.
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

    private boolean isEnraged() {
        // After half health, bosses move and attack faster as a second phase.
        return level >= 4 && health <= maxHealth / 2;
    }

    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Rectangle body = getBounds();

        // Body color communicates current state: hurt, windup, or idle.
        if (hurtFlashFrames > 0) {
            g2.setColor(new Color(255, 235, 235));
        } else if (slamWindupFrames > 0) {
            g2.setColor(new Color(255, 150, 35));
        } else if (windupFrames > 0 || rangedWindupFrames > 0) {
            g2.setColor(new Color(255, 95, 80));
        } else if (isEnraged()) {
            g2.setColor(new Color(205, 34, 82));
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
            // Telegraph and active melee range are both drawn, with stronger opacity when active.
            Rectangle attack = getAttackBounds();
            g2.setColor(new Color(255, 80, 80, attackFrames > 0 ? 155 : 70));
            g2.fillRect(attack.x, attack.y, attack.width, attack.height);
            g2.setColor(new Color(255, 180, 150));
            g2.drawRect(attack.x, attack.y, attack.width, attack.height);
        }

        if (rangedWindupFrames > 0) {
            // The charging orb warns that a projectile is about to be fired.
            int orbX = facingRight ? body.x + body.width + 8 : body.x - 28;
            g2.setColor(new Color(255, 90, 40, 180));
            g2.fillOval(orbX, body.y + 26, 24, 24);
            g2.setColor(Color.WHITE);
            g2.drawOval(orbX, body.y + 26, 24, 24);
        }

        if (slamWindupFrames > 0 || slamFlashFrames > 0) {
            // Stomp warning/impact line shows where shockwaves will start.
            int alpha = slamWindupFrames > 0 ? 100 : 180;
            g2.setColor(new Color(255, 180, 45, alpha));
            g2.fillRect(body.x - 22, body.y + body.height - 10, body.width + 44, 12);
            g2.setColor(Color.WHITE);
            g2.drawRect(body.x - 22, body.y + body.height - 10, body.width + 44, 12);
        }
    }
}
