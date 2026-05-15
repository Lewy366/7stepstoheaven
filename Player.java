import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

public class Player {

    static final int STANDING_WIDTH = 50;
    static final int STANDING_HEIGHT = 50;
    static final int DUCK_HEIGHT = 32;

    int playerX = 100;
    int playerY = 250;

    double velocityX = 0;
    double velocityY = 0;

    double moveAcceleration = 1.0;
    double groundFriction = 0.78;
    double airFriction = 0.92;
    double maxMoveSpeed = 7.0;
    double gravity = 1.0;
    double maxFallSpeed = 20;

    boolean onGround = false;
    boolean leftPressed = false;
    boolean rightPressed = false;
    boolean duckPressed = false;
    boolean facingRight = true;

    private boolean jumpHeld = false;
    private boolean dashPressed = false;
    private boolean attackPressed = false;
    private boolean shootPressed = false;

    private int coyoteFrames = 0;
    private int jumpBufferFrames = 0;
    private int jumpsUsed = 0;
    private int dashCooldownFrames = 0;
    private int dashFrames = 0;
    private int attackCooldownFrames = 0;
    private int attackFrames = 0;
    private int shootCooldownFrames = 0;
    private int invulnerableFrames = 0;

    private int maxHealth;
    private int health;
    private int worldWidth;
    private int worldHeight;

    ArrayList<Rectangle> tiles;

    KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (Controls.matchesKey(Controls.MOVE_LEFT, key)) leftPressed = true;
            if (Controls.matchesKey(Controls.MOVE_RIGHT, key)) rightPressed = true;
            if (Controls.matchesKey(Controls.DUCK, key)) duckPressed = true;
            if (Controls.matchesKey(Controls.JUMP, key) && !jumpHeld) {
                jumpBufferFrames = 8;
                jumpHeld = true;
            }
            if (Controls.matchesKey(Controls.DASH, key) && dashCooldownFrames == 0) dashPressed = true;
            if (Controls.matchesKey(Controls.MELEE, key)) attackPressed = true;
            if (Controls.matchesKey(Controls.SHOOT, key)) shootPressed = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            int key = e.getKeyCode();
            if (Controls.matchesKey(Controls.MOVE_LEFT, key)) leftPressed = false;
            if (Controls.matchesKey(Controls.MOVE_RIGHT, key)) rightPressed = false;
            if (Controls.matchesKey(Controls.DUCK, key)) duckPressed = false;
            if (Controls.matchesKey(Controls.JUMP, key)) jumpHeld = false;
        }
    };

    MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
            int button = e.getButton();
            if (Controls.matchesMouse(Controls.MOVE_LEFT, button)) leftPressed = true;
            if (Controls.matchesMouse(Controls.MOVE_RIGHT, button)) rightPressed = true;
            if (Controls.matchesMouse(Controls.DUCK, button)) duckPressed = true;
            if (Controls.matchesMouse(Controls.JUMP, button) && !jumpHeld) {
                jumpBufferFrames = 8;
                jumpHeld = true;
            }
            if (Controls.matchesMouse(Controls.DASH, button) && dashCooldownFrames == 0) dashPressed = true;
            if (Controls.matchesMouse(Controls.MELEE, button)) attackPressed = true;
            if (Controls.matchesMouse(Controls.SHOOT, button)) shootPressed = true;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            int button = e.getButton();
            if (Controls.matchesMouse(Controls.MOVE_LEFT, button)) leftPressed = false;
            if (Controls.matchesMouse(Controls.MOVE_RIGHT, button)) rightPressed = false;
            if (Controls.matchesMouse(Controls.DUCK, button)) duckPressed = false;
            if (Controls.matchesMouse(Controls.JUMP, button)) jumpHeld = false;
        }
    };

    public Player(ArrayList<Rectangle> tiles, Difficulty difficulty, int worldWidth, int worldHeight) {
        this.tiles = tiles;
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        maxHealth = 110 - difficulty.getLevel() * 10;
        health = maxHealth;
        maxMoveSpeed = 6.5 + difficulty.getSpeed() * 0.45;
    }

    public void setWorldBounds(int worldWidth, int worldHeight) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        clampToWorld();
    }

    public void update() {
        tickTimers();
        updateHorizontalMovement();
        tryJump();
        tryDash();

        if (dashFrames > 0) {
            dashFrames--;
        } else {
            velocityY += gravity;
            if (velocityY > maxFallSpeed) velocityY = maxFallSpeed;
        }

        moveX();
        moveY();

        if (onGround) {
            coyoteFrames = 8;
            jumpsUsed = 0;
        } else if (coyoteFrames > 0) {
            coyoteFrames--;
        }
    }

    public void resetInputState() {
        leftPressed = false;
        rightPressed = false;
        duckPressed = false;
        jumpHeld = false;
        dashPressed = false;
        attackPressed = false;
        shootPressed = false;
    }

    private void tickTimers() {
        if (jumpBufferFrames > 0) jumpBufferFrames--;
        if (dashCooldownFrames > 0) dashCooldownFrames--;
        if (attackCooldownFrames > 0) attackCooldownFrames--;
        if (attackFrames > 0) attackFrames--;
        if (shootCooldownFrames > 0) shootCooldownFrames--;
        if (invulnerableFrames > 0) invulnerableFrames--;
    }

    private void updateHorizontalMovement() {
        if (leftPressed) {
            velocityX -= moveAcceleration;
            facingRight = false;
        }
        if (rightPressed) {
            velocityX += moveAcceleration;
            facingRight = true;
        }

        if (!leftPressed && !rightPressed) {
            velocityX *= onGround ? groundFriction : airFriction;
            if (Math.abs(velocityX) < 0.1) velocityX = 0;
        }

        double speedCap = duckPressed && onGround ? maxMoveSpeed * 0.45 : maxMoveSpeed;
        if (velocityX > speedCap) velocityX = speedCap;
        if (velocityX < -speedCap) velocityX = -speedCap;
    }

    private void tryJump() {
        if (jumpBufferFrames == 0) return;

        if (onGround || coyoteFrames > 0) {
            velocityY = -18;
            onGround = false;
            coyoteFrames = 0;
            jumpsUsed = 1;
            jumpBufferFrames = 0;
        } else if (jumpsUsed < 2) {
            velocityY = -16;
            jumpsUsed++;
            jumpBufferFrames = 0;
        }
    }

    private void tryDash() {
        if (!dashPressed) return;
        dashPressed = false;
        dashFrames = 10;
        dashCooldownFrames = 36;
        velocityY = 0;
        velocityX = facingRight ? 18 : -18;
    }

    private void moveX() {
        playerX += Math.round(velocityX);

        Rectangle player = getBounds();
        for (Rectangle tile : tiles) {
            if (player.intersects(tile)) {
                if (velocityX > 0) {
                    playerX = tile.x - getWidth();
                } else if (velocityX < 0) {
                    playerX = tile.x + tile.width;
                }
                velocityX = 0;
                player = getBounds();
            }
        }
        clampXToWorld();
    }

    private void moveY() {
        playerY += Math.round(velocityY);

        onGround = false;
        Rectangle player = getBounds();
        for (Rectangle tile : tiles) {
            if (player.intersects(tile)) {
                if (velocityY > 0) {
                    playerY = tile.y - getHeight();
                    velocityY = 0;
                    onGround = true;
                } else if (velocityY < 0) {
                    playerY = tile.y + tile.height;
                    velocityY = 0;
                }
                player = getBounds();
            }
        }
        clampYToWorld();
    }

    private void clampToWorld() {
        clampXToWorld();
        clampYToWorld();
    }

    private void clampXToWorld() {
        if (worldWidth <= 0) return;
        int maxX = Math.max(0, worldWidth - getWidth());
        if (playerX < 0) {
            playerX = 0;
            velocityX = Math.max(0, velocityX);
        } else if (playerX > maxX) {
            playerX = maxX;
            velocityX = Math.min(0, velocityX);
        }
    }

    private void clampYToWorld() {
        if (worldHeight <= 0) return;
        Rectangle bounds = getBounds();
        int minY = -(STANDING_HEIGHT - getHeight());
        int maxY = Math.max(minY, worldHeight - bounds.height - (STANDING_HEIGHT - getHeight()));
        if (playerY < minY) {
            playerY = minY;
            velocityY = Math.max(0, velocityY);
        } else if (playerY > maxY) {
            playerY = maxY;
            velocityY = 0;
            onGround = true;
        }
    }

    public boolean startAttack() {
        if (attackPressed && attackCooldownFrames == 0) {
            attackPressed = false;
            attackFrames = 10;
            attackCooldownFrames = 24;
            return true;
        }
        attackPressed = false;
        return false;
    }

    public Rectangle getAttackBounds() {
        int range = 46;
        int x = facingRight ? playerX + getWidth() : playerX - range;
        return new Rectangle(x, playerY + 8, range, getHeight() - 12);
    }

    public Projectile shootProjectile() {
        if (!shootPressed || shootCooldownFrames > 0) {
            shootPressed = false;
            return null;
        }
        shootPressed = false;
        shootCooldownFrames = 28;
        int direction = facingRight ? 1 : -1;
        Rectangle body = getBounds();
        int startX = facingRight ? body.x + body.width : body.x - 18;
        return new Projectile(startX, body.y + body.height / 2 - 7, direction * 13, 0,
                18, 14, 12, true, 80);
    }

    public Rectangle getBounds() {
        int height = getHeight();
        return new Rectangle(playerX, playerY + STANDING_HEIGHT - height, getWidth(), height);
    }

    public int getWidth() {
        return STANDING_WIDTH;
    }

    public int getHeight() {
        return duckPressed && onGround ? DUCK_HEIGHT : STANDING_HEIGHT;
    }

    public void hurt(int damage) {
        if (invulnerableFrames > 0) return;
        health -= damage;
        invulnerableFrames = 50;
        velocityX = facingRight ? -8 : 8;
        velocityY = -8;
        if (health < 0) health = 0;
    }

    public void heal(int amount) {
        health = Math.min(maxHealth, health + amount);
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

        if (invulnerableFrames % 8 < 4) {
            g2.setColor(new Color(55, 100, 255));
        } else {
            g2.setColor(new Color(120, 170, 255));
        }
        g2.fillRect(body.x, body.y, body.width, body.height);

        g2.setColor(Color.WHITE);
        int eyeX = facingRight ? body.x + 34 : body.x + 10;
        g2.fillRect(eyeX, body.y + 10, 6, 6);

        if (attackFrames > 0) {
            Rectangle attack = getAttackBounds();
            g2.setColor(new Color(120, 210, 255, 140));
            g2.fillRect(attack.x, attack.y, attack.width, attack.height);
            g2.setColor(new Color(210, 245, 255));
            g2.drawRect(attack.x, attack.y, attack.width, attack.height);
        }
    }
}
