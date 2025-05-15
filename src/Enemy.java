import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;
import java.util.ArrayList;

/**
 * Represents an enemy character in the game.
 * Extends the Player class to inherit movement and combat capabilities.
 * Includes AI for following and attacking the player.
 */
public class Enemy extends Player {
    // Static collection of enemy spawn positions from the level
    private static ArrayList<Point> enemySpawnPoints = new ArrayList<>();
    private static int currentSpawnIndex = 0;
    
    private Player target;
    private int followSpeed = 1;
    private int followDelay = 0;
    private final int MAX_FOLLOW_DELAY = 30;
    private final int Y_LEVEL_TOLERANCE = 2; // Tolerance for Y-level difference
    private int originalWidth; // Store original width for sprite rendering
    
    // Attack properties
    private int attackDamage = 10;
    private int attackCooldown = 0;
    private final int MAX_ATTACK_COOLDOWN = 60; // 1 second at 60 FPS
    private boolean canAttack = true;
    
    // Enemy health
    private int maxHP = 100; // Base enemy HP
    
    // Enemy state management
    private boolean frozen = false;
    private int frozenTimer = 0;
    private final int FROZEN_DURATION = 45; // 0.75 seconds at 60 FPS
    
    // Boss properties
    private boolean isBoss = false;
    private int spawnTimer = 0;
    private final int SPAWN_COOLDOWN = 5 * 60; // 5 seconds at 60 FPS
    
    // Track the current sprite state
    private String currentSpriteFile;
    private int currentWidth;
    private int currentFrameCount;

    // Track if this enemy has already been hit by the current attack
    private boolean hitByCurrentAttack = false;
    
    /**
     * Constructor for the Enemy class.
     * Creates an enemy with the specified position, dimensions, and sprite.
     * 
     * @param x int X-coordinate of the enemy
     * @param y int Y-coordinate of the enemy
     * @param width int Width of the enemy sprite
     * @param height int Height of the enemy sprite
     * @param spriteFile String Path to the sprite sheet file
     * @param frameCount int Number of animation frames in the sprite sheet
     * @return void
     */
    public Enemy(int x, int y, int width, int height, String spriteFile, int frameCount){
        // Use half width for the bounds/hitbox
        super(x, y, width - 18, height, spriteFile, frameCount);
        this.originalWidth = width;
        this.currentSpriteFile = spriteFile;
        this.currentWidth = width;
        this.currentFrameCount = frameCount;
    }
    
    /**
     * Sets the player as the target for this enemy to follow and attack.
     * 
     * @param player Player The player character to target
     * @return void
     */
    public void setTarget(Player player) {
        this.target = player;
    }
    
    /**
     * Moves the enemy left.
     * Overrides the Player class method to handle frozen state.
     * 
     * @return void
     */
    @Override
    public void moveLeft() {
        // Don't move if frozen
        if (frozen) return;
        
        setSpeedX(-followSpeed);
        setFacingLeft(true);
    }
    
    /**
     * Moves the enemy right.
     * Overrides the Player class method to handle frozen state.
     * 
     * @return void
     */
    @Override
    public void moveRight() {
        // Don't move if frozen
        if (frozen) return;
        
        setSpeedX(followSpeed);
        setFacingLeft(false);
    }
    
    /**
     * Handles enemy taking damage.
     * Overrides the Player class method to prevent sprite change and implement freezing.
     * 
     * @param damage int The amount of damage to apply
     * @return void
     */
    @Override
    public void takeDamage(int damage) {
        // Extract the HP reduction logic without calling super.takeDamage()
        // to avoid triggering the Player's startHitAnimation()

        int currentHP = getCurrentHP() - damage;
        if(isBoss){
            System.out.println("Enemy took damage: " + damage + ", current HP: " + currentHP);
        }
        if (currentHP < 0) {
            currentHP = 0;
        }

        setCurrentHP(currentHP);
        
        // Set the HP directly using reflection (not ideal, but a workaround)
        // We'll just use our freeze mechanics instead
        
        // Freeze enemy when taking damage without changing sprite
        freeze();
        
        // Mark this enemy as hit by the current attack
        hitByCurrentAttack = true;
        
        // Play enemy hurt sound
        playEnemyHurtSound();
    }
    
    /**
     * Resets the hit tracking flag when player starts a new attack.
     * Prevents the enemy from being hit multiple times by the same attack.
     * 
     * @return void
     */
    public void resetHitTracking() {
        hitByCurrentAttack = false;
    }
    
    /**
     * Checks if this enemy was already hit by the current player attack.
     * 
     * @return boolean True if the enemy was hit by the current attack, false otherwise
     */
    public boolean wasHitByCurrentAttack() {
        return hitByCurrentAttack;
    }
    
    /**
     * Freezes the enemy temporarily when hit.
     * Stops movement but doesn't change the sprite.
     * 
     * @return void
     */
    public void freeze() {
        frozen = true;
        frozenTimer = 0;
        setSpeedX(0); // Stop movement
        // No sprite change when frozen
    }
    
    /**
     * Updates the enemy's state.
     * Handles frozen state, following the player, attacking, and cooldowns.
     * 
     * @return void
     */
    @Override
    public void update() {
        // Update frozen state
        if (frozen) {
            frozenTimer++;
            if (frozenTimer >= FROZEN_DURATION) {
                frozen = false;
                // Do NOT change sprite when unfreezing
            }
        }
        
        // Boss spawn timer
        if (isBoss && !isDead()) {
            spawnTimer++;
        }
        
        if (!frozen) {
            if (target != null) {
                followTarget();
                attackPlayer();
            }
            
            // Update attack cooldown
            if (!canAttack) {
                attackCooldown++;
                if (attackCooldown >= MAX_ATTACK_COOLDOWN) {
                    canAttack = true;
                    // Return to idle sprite after attack cooldown
                    if (!frozen) {
                        changeSprite(36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
                        currentSpriteFile = "assets/Orc_Sprite/orc_idle.png";
                        currentWidth = 36;
                        currentFrameCount = 4;
                    }
                }
            }
        }
        
        // Call parent update which includes bounds checking
        super.update();
    }
    
    /**
     * Implements AI logic for following the player.
     * Moves toward the player when they are at approximately the same Y level.
     * 
     * @return void
     */
    public void followTarget() {
        if (target == null || frozen) return;
        
        followDelay++;
        if (followDelay < MAX_FOLLOW_DELAY) {
            return;
        }
        followDelay = 0;
        
        Rectangle targetBounds = target.getBounds();
        Rectangle myBounds = getBounds();
        
        // Check if player is at roughly the same Y level (with tolerance)
        int yDifference = Math.abs((myBounds.y + myBounds.height) - (targetBounds.y + targetBounds.height));
        
        if (yDifference <= Y_LEVEL_TOLERANCE) {
            // Player is at same level, follow them
            if (targetBounds.x < myBounds.x) {
                if(!isBoss){
                    changeSprite(47, 28, "assets/Orc_Sprite/orc_walk.png", 8);
                    currentSpriteFile = "assets/Orc_Sprite/orc_walk.png";
                    currentWidth = 47;
                    currentFrameCount = 8;
                }
                else {
                    changeSprite(47, 28, "assets/Boss_Sprite/boss_walk.png", 8);
                    currentSpriteFile = "assets/Boss_Sprite/boss_walk.png";
                    currentWidth = 47;
                    currentFrameCount = 8;
                }
                moveLeft();
            } else if (targetBounds.x > myBounds.x) {
                if(!isBoss){
                    changeSprite(47, 28, "assets/Orc_Sprite/orc_walk.png", 8);
                    currentSpriteFile = "assets/Orc_Sprite/orc_walk.png";
                    currentWidth = 47;
                    currentFrameCount = 8;
                }
                else {
                    changeSprite(47, 28, "assets/Boss_Sprite/boss_walk.png", 8);
                    currentSpriteFile = "assets/Boss_Sprite/boss_walk.png";
                    currentWidth = 47;
                    currentFrameCount = 8;
                }
                moveRight();
            } else {
                if(!isBoss){
                    changeSprite(36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
                    currentSpriteFile = "assets/Orc_Sprite/orc_idle.png";
                    currentWidth = 36;
                    currentFrameCount = 4;
                }
                else {
                    changeSprite(36, 28, "assets/Boss_Sprite/boss_idle.png", 4);
                    currentSpriteFile = "assets/Boss_Sprite/boss_idle.png";
                    currentWidth = 36;
                    currentFrameCount = 4;
                }
                stop();
            }
        } else {
            // Player is not at same level, stop following
            if(!isBoss){
                changeSprite(36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
                currentSpriteFile = "assets/Orc_Sprite/orc_idle.png";
                currentWidth = 36;
                currentFrameCount = 4;
            }
            else {
                changeSprite(36, 28, "assets/Boss_Sprite/boss_idle.png", 4);
                currentSpriteFile = "assets/Boss_Sprite/boss_idle.png";
                currentWidth = 36;
                currentFrameCount = 4;
            }
            stop();
        }
    }
    
    /**
     * Checks if the enemy is colliding with the player.
     * Used for determining when to attack the player.
     * 
     * @return boolean True if the enemy is colliding with the player, false otherwise
     */
    public boolean isCollidingWithPlayer() {
        if (target == null) return false;
        return getBounds().intersects(target.getBounds());
    }
    
    /**
     * Implements the enemy's attack logic.
     * Attacks the player when in close proximity and not on cooldown.
     * 
     * @return void
     */
    public void attackPlayer() {
        if (target == null || !canAttack || frozen) return;
        
        if (isCollidingWithPlayer() && !target.isInvulnerable()) {
            try {
                // Show attack animation
                if(!isBoss){
                    changeSprite(64, 28, "assets/Orc_Sprite/orc_hit.png", 6);
                    currentSpriteFile = "assets/Orc_Sprite/orc_hit.png";
                    currentWidth = 64;
                    currentFrameCount = 6;
                }
                else {
                    changeSprite(64, 28, "assets/Boss_Sprite/boss_bit.png", 6);
                    currentSpriteFile = "assets/BossSprite/boss_bit.png";
                    currentWidth = 64;
                    currentFrameCount = 6;
                }
            } catch (Exception e) {
                // If attack sprite fails to load, fall back to idle sprite
                changeSprite(36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
                currentSpriteFile = "assets/Orc_Sprite/orc_idle.png";
                currentWidth = 36;
                currentFrameCount = 4;
            }
            
            // Deal damage to player
            target.takeDamage(attackDamage);
            
            // Set cooldown
            canAttack = false;
            attackCooldown = 0;
        }
    }
    
    /**
     * Draws the enemy on the screen.
     * Handles different sprite sizes and orientations for enemies and bosses.
     * 
     * @param g Graphics object used for drawing
     * @param showBounds boolean Whether to show hitboxes for debugging
     * @return void
     */
    @Override
    public void draw(Graphics g, boolean showBounds) {
        if (getSpriteSheet() != null) {
            try {
                // Make sure currentFrame doesn't exceed frameCount
                int safeFrame = Math.min(getCurrentFrame(), getFrameCount());
                
                // Add bounds checking for subimage extraction
                int y = safeFrame * getFrameHeight();
                if (y + getFrameHeight() <= getSpriteSheet().getHeight() && 
                    getFrameWidth() <= getSpriteSheet().getWidth()) {
                    
                    BufferedImage currentSprite = getSpriteSheet().getSubimage(
                        0, y, getFrameWidth(), getFrameHeight()
                    );
    
                    // Scale the sprite by 3x (normal) or 6x (boss)
                    int scaleFactor = isBoss ? 6 : 3;
                    int scaledWidth = getFrameWidth() * scaleFactor;
                    int scaledHeight = getFrameHeight() * scaleFactor;
                    
                    Rectangle bounds = getBounds();
                    boolean facingLeft = isFacingLeft();
                    
                    int spriteX;
                    int spriteY = bounds.y + (bounds.height - scaledHeight) / 2;
                    
                    if (facingLeft) {
                        // When facing left, position sprite so right half aligns with hitbox
                        spriteX = bounds.x - scaledWidth/2 + 10;
                    } else {
                        // When facing right, position sprite so left half aligns with hitbox
                        spriteX = bounds.x - 10;
                    }
    
                    if (facingLeft) {
                        // Flip the sprite horizontally
                        BufferedImage flippedSprite = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g2d = flippedSprite.createGraphics();
                        g2d.drawImage(currentSprite, scaledWidth, 0, -scaledWidth, scaledHeight, null);
                        g2d.dispose();
    
                        g.drawImage(flippedSprite, spriteX, spriteY, scaledWidth, scaledHeight, null);
                    } else {
                        g.drawImage(currentSprite, spriteX, spriteY, scaledWidth, scaledHeight, null);
                    }
                }
            } catch (Exception e) {
                // Handle sprite rendering exceptions silently
            }
        }
        
        // Draw the hitbox if showBounds is true
        if (showBounds) {
            Rectangle bounds = getBounds();
            g.setColor(Color.RED);
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            
            // Draw the hurtbox if attacking and showBounds is true
            if (isAttacking() && getHurtbox().width > 0) {
                g.setColor(Color.GREEN);
                g.drawRect(getHurtbox().x, getHurtbox().y, getHurtbox().width, getHurtbox().height);
            }
        }
    }
    
    /**
     * Changes the enemy's sprite.
     * Overrides the Player class method to properly scale enemy sprites.
     * 
     * @param width int Width of the sprite
     * @param height int Height of the sprite
     * @param spriteFile String Path to the sprite sheet file
     * @param frameCount int Number of animation frames in the sprite sheet
     * @return void
     */
    @Override
    public void changeSprite(int width, int height, String spriteFile, int frameCount) {
        try {
            // Call the parent changeSprite method
            super.changeSprite(width/2, height, spriteFile, frameCount);
            this.originalWidth = width;
        } catch (Exception e) {
            // If sprite fails to load, just continue with current sprite
            System.out.println("Failed to load sprite: " + spriteFile);
        }
    }
    
    /**
     * Gets the number of frames in the current sprite sheet.
     * 
     * @return int The number of animation frames
     */
    protected int getFrameCount() {
        try {
            // This is a method that wasn't in the parent class, but we need it
            return getSpriteSheet().getHeight() / getFrameHeight();
        } catch (Exception e) {
            return 1; // Default to 1 if there's an error
        }
    }
    
    /**
     * Sets the attack damage for this enemy.
     * 
     * @param damage int The amount of damage this enemy deals
     * @return void
     */
    public void setAttackDamage(int damage) {
        this.attackDamage = damage;
    }
    
    /**
     * Gets the attack damage for this enemy.
     * Overrides the Player class method.
     * 
     * @return int The amount of damage this enemy deals
     */
    @Override
    public int getAttackDamage() {
        return attackDamage;
    }
    
    /**
     * Checks if the enemy is currently frozen.
     * 
     * @return boolean True if the enemy is frozen, false otherwise
     */
    public boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Plays a sound effect when the enemy is hurt.
     * 
     * @return void
     */
    private void playEnemyHurtSound() {
        try {
            AudioPlayer hurtSound = new AudioPlayer("assets/sounds/orc_hurt.wav");
            hurtSound.play(false);
        } catch (Exception e) {
            System.out.println("Could not play enemy hurt sound: " + e.getMessage());
        }
    }
    
    /**
     * Sets this enemy as a boss.
     * Bosses have increased health, damage, and size.
     * 
     * @param isBoss boolean Whether this enemy should be a boss
     * @return void
     */
    public void setBoss(boolean isBoss) {
        this.isBoss = isBoss;
        if (isBoss) {
            // ten the health
            setCurrentHP(getMaxHP() * 10);
            
            // Double attack damage
            this.attackDamage *= 2;
            
            // Double the scale for rendering
            this.originalWidth *= 2;
            this.currentWidth *= 2;
            
            // Scale the hitbox/bounds
            Rectangle bounds = getBounds();
            int newWidth = bounds.width * 2;
            int newHeight = bounds.height * 2;
            // Keep the bottom edge aligned with the original position
            int newY = bounds.y - (newHeight - bounds.height);
            bounds.setBounds(bounds.x, newY, newWidth, newHeight);
        }
    }
    
    /**
     * Checks if this enemy is a boss.
     * 
     * @return boolean True if this enemy is a boss, false otherwise
     */
    public boolean isBoss() {
        return isBoss;
    }
    
    /**
     * Adds a spawn point to the static collection of spawn points.
     * Used for boss enemies to spawn regular enemies.
     * 
     * @param x int X-coordinate of the spawn point
     * @param y int Y-coordinate of the spawn point
     * @return void
     */
    public static void addSpawnPoint(int x, int y) {
        enemySpawnPoints.add(new Point(x, y));
    }
    
    /**
     * Clears all spawn points from the static collection.
     * Used when loading a new level.
     * 
     * @return void
     */
    public static void clearSpawnPoints() {
        enemySpawnPoints.clear();
        currentSpawnIndex = 0;
    }
    
    /**
     * Creates a new minion enemy from a random spawn point.
     * Used by boss enemies to spawn regular enemies during battle.
     * 
     * @param platforms ArrayList<Platform> The list of platforms in the level
     * @return Enemy A new enemy instance, or null if spawning is not possible
     */
    public Enemy spawnMinion(ArrayList<Platform> platforms) {
        if (!isBoss || isDead() || enemySpawnPoints.isEmpty()) return null;
        
        // Get next spawn point in rotation
        Point spawnPoint = enemySpawnPoints.get(currentSpawnIndex);
        currentSpawnIndex = (currentSpawnIndex + 1) % enemySpawnPoints.size();
        
        // Create a new enemy at the spawn point
        Enemy minion = new Enemy(
            spawnPoint.x,
            spawnPoint.y,
            36, 28,
            "assets/Orc_Sprite/orc_idle.png", 
            4
        );
        
        return minion;
    }
    
    /**
     * Checks if the boss can spawn a minion.
     * Based on spawn timer and boss status.
     * 
     * @return boolean True if a minion can be spawned, false otherwise
     */
    public boolean canSpawnMinion() {
        if (!isBoss || isDead()) return false;
        if (spawnTimer >= SPAWN_COOLDOWN) {
            spawnTimer = 0;
            return true;
        }
        return false;
    }
    
    /**
     * Resets the spawn timer.
     * Called after spawning a minion.
     * 
     * @return void
     */
    public void resetSpawnTimer() {
        spawnTimer = 0;
    }
}

