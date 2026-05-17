import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;

/**
 * Player character physics, input state, combat, health, and rendering.
 * Generator owns the game loop and calls update/draw each frame.
 */
public class Player {

    // The player's collision box gets shorter while ducking, but keeps the same width.
    static final int STANDING_WIDTH = 50;
    static final int STANDING_HEIGHT = 50;
    static final int DUCK_HEIGHT = 32;
    static final int TRIPLE_SHOT_COOLDOWN_FRAMES = 625;

    // Top-left position of the standing body box in world coordinates.
    int playerX = 100;
    int playerY = 250;

    // Movement is stored as velocity so acceleration, friction, and knockback feel smooth.
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
    private boolean tripleShotPressed = false;

    private int coyoteFrames = 0;
    private int jumpBufferFrames = 0;
    private int jumpsUsed = 0;
    private int dashCooldownFrames = 0;
    private int dashFrames = 0;
    private int attackCooldownFrames = 0;
    private int attackFrames = 0;
    private int shootCooldownFrames = 0;
    private int tripleShotCooldownFrames = 0;
    private int invulnerableFrames = 0;

    private int maxHealth;
    private int health;
    private double aimX = playerX + 300;
    private double aimY = playerY + STANDING_HEIGHT / 2.0;
    // World bounds stop the player from leaving the generated level.
    private int worldWidth;
    private int worldHeight;

    ArrayList<Rectangle> tiles;

    // Keyboard state is kept on the player so movement works independently of menus.
    KeyAdapter keyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            int key = e.getKeyCode();
            if (Controls.matchesKey(Controls.MOVE_LEFT, key)) leftPressed = true;
            if (Controls.matchesKey(Controls.MOVE_RIGHT, key)) rightPressed = true;
            if (Controls.matchesKey(Controls.DUCK, key)) duckPressed = true;
            if (Controls.matchesKey(Controls.JUMP, key) && !jumpHeld) {
                // Buffering makes a jump still happen if the key was pressed slightly early.
                jumpBufferFrames = 8;
                jumpHeld = true;
            }
            if (Controls.matchesKey(Controls.DASH, key) && dashCooldownFrames == 0) dashPressed = true;
            if (Controls.matchesKey(Controls.MELEE, key)) attackPressed = true;
            if (Controls.matchesKey(Controls.SHOOT, key)) shootPressed = true;
            if (Controls.matchesKey(Controls.TRIPLE_SHOT, key)) tripleShotPressed = true;
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

    // Mouse input uses the same action system, which allows any action to be rebound to a button.
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
            if (Controls.matchesMouse(Controls.TRIPLE_SHOT, button)) tripleShotPressed = true;
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
        // Higher difficulty lowers health; selected difficulty speed makes movement snappier.
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
        // Update order matters: input changes velocity, then collision resolves movement.
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
            // Coyote time lets the player jump a few frames after leaving a ledge.
            coyoteFrames = 8;
            jumpsUsed = 0;
        } else if (coyoteFrames > 0) {
            coyoteFrames--;
        }
    }

    public void resetInputState() {
        // Used after rebinding/menu changes so old held inputs do not remain stuck on.
        leftPressed = false;
        rightPressed = false;
        duckPressed = false;
        jumpHeld = false;
        dashPressed = false;
        attackPressed = false;
        shootPressed = false;
        tripleShotPressed = false;
    }

    private void tickTimers() {
        // All cooldowns count down in frames because the Swing timer runs every 16 ms.
        if (jumpBufferFrames > 0) jumpBufferFrames--;
        if (dashCooldownFrames > 0) dashCooldownFrames--;
        if (attackCooldownFrames > 0) attackCooldownFrames--;
        if (attackFrames > 0) attackFrames--;
        if (shootCooldownFrames > 0) shootCooldownFrames--;
        if (tripleShotCooldownFrames > 0) tripleShotCooldownFrames--;
        if (invulnerableFrames > 0) invulnerableFrames--;
    }

    private void updateHorizontalMovement() {
        // Holding left/right accelerates; releasing both applies friction.
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

        // Ducking on the ground intentionally limits horizontal speed.
        double speedCap = duckPressed && onGround ? maxMoveSpeed * 0.45 : maxMoveSpeed;
        if (velocityX > speedCap) velocityX = speedCap;
        if (velocityX < -speedCap) velocityX = -speedCap;
    }

    private void tryJump() {
        if (jumpBufferFrames == 0) return;

        if (onGround || coyoteFrames > 0) {
            // First jump can come from the ground or the coyote-time window.
            velocityY = -18;
            onGround = false;
            coyoteFrames = 0;
            jumpsUsed = 1;
            jumpBufferFrames = 0;
        } else if (jumpsUsed < 2) {
            // One extra air jump is allowed.
            velocityY = -16;
            jumpsUsed++;
            jumpBufferFrames = 0;
        }
    }

    private void tryDash() {
        if (!dashPressed) return;
        // Dash is a short burst that cancels vertical movement for a few frames.
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
                // Resolve horizontal collisions by placing the player beside the wall hit.
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
                // Resolve vertical collisions and mark grounded only when landing from above.
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
        // Ducking changes the collision height, so the clamp compensates for that offset.
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
            // A melee attack is active for a small number of frames, then goes on cooldown.
            attackPressed = false;
            attackFrames = 10;
            attackCooldownFrames = 24;
            return true;
        }
        attackPressed = false;
        return false;
    }

    public Rectangle getAttackBounds() {
        // Attack hitbox extends from the side the player is facing.
        int range = 46;
        int x = facingRight ? playerX + getWidth() : playerX - range;
        return new Rectangle(x, playerY + 8, range, getHeight() - 12);
    }

    public void setAimPoint(int worldX, int worldY) {
        aimX = worldX;
        aimY = worldY;
        Rectangle body = getBounds();
        facingRight = aimX >= body.x + body.width / 2.0;
    }

    public Projectile shootProjectile() {
        if (!shootPressed || shootCooldownFrames > 0) {
            shootPressed = false;
            return null;
        }
        // Shooting consumes the press immediately so one click/key press creates one shot.
        shootPressed = false;
        shootCooldownFrames = 28;
        return createAimedProjectile(0, 13, 18, 14, 12, 80);
    }

    public ArrayList<Projectile> shootTripleProjectiles() {
        ArrayList<Projectile> shots = new ArrayList<>();
        if (!tripleShotPressed || tripleShotCooldownFrames > 0) {
            tripleShotPressed = false;
            return shots;
        }

        // Triple shot fires a tight spread and then waits ten seconds before recharging.
        tripleShotPressed = false;
        tripleShotCooldownFrames = TRIPLE_SHOT_COOLDOWN_FRAMES;
        shootCooldownFrames = Math.max(shootCooldownFrames, 12);
        shots.add(createAimedProjectile(-0.22, 13.5, 18, 14, 10, 80));
        shots.add(createAimedProjectile(0, 14.0, 18, 14, 10, 80));
        shots.add(createAimedProjectile(0.22, 13.5, 18, 14, 10, 80));
        return shots;
    }

    private Projectile createAimedProjectile(double angleOffset, double speed,
                                             int projectileWidth, int projectileHeight,
                                             int damage, int lifeFrames) {
        Rectangle body = getBounds();
        double centerX = body.x + body.width / 2.0;
        double centerY = body.y + body.height / 2.0;
        double dx = aimX - centerX;
        double dy = aimY - centerY;

        if (Math.abs(dx) + Math.abs(dy) < 0.001) {
            dx = facingRight ? 1 : -1;
            dy = 0;
        }

        double angle = Math.atan2(dy, dx) + angleOffset;
        double unitX = Math.cos(angle);
        double unitY = Math.sin(angle);
        facingRight = unitX >= 0;

        double startX = centerX + unitX * (body.width / 2.0 + 2) - projectileWidth / 2.0;
        double startY = centerY + unitY * (body.height / 2.0 + 2) - projectileHeight / 2.0;
        return new Projectile(startX, startY, unitX * speed, unitY * speed,
                projectileWidth, projectileHeight, damage, true, lifeFrames);
    }

    public Rectangle getBounds() {
        // The Y offset keeps the feet in the same place when ducking lowers the body.
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
        // After taking damage, temporary invulnerability prevents instant repeated hits.
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

        // Flicker during invulnerability to show damage immunity.
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
            // Draw the active melee hitbox so combat range is visible.
            Rectangle attack = getAttackBounds();
            g2.setColor(new Color(120, 210, 255, 140));
            g2.fillRect(attack.x, attack.y, attack.width, attack.height);
            g2.setColor(new Color(210, 245, 255));
            g2.drawRect(attack.x, attack.y, attack.width, attack.height);
        }
    }
}
