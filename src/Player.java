import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Represents the player character in the game.
 * Handles player movement, combat, animations, and attributes such as health and mana.
 */
public class Player {
    private Rectangle bounds;
    private Rectangle hurtbox; // Added hurtbox for attack collision detection
    private int speedX = 0, speedY = 0;
    private final int GRAVITY = 1;
    private final int JUMP_STRENGTH = -15;
    private final int HIGH_JUMP_STRENGTH = -23; // 1.5x stronger jump for Shift+W/Space
    
    // HP and damage system
    private int maxHP = 100;
    private int currentHP = 100;
    private int attackDamage = 50;
    private int basicAttackDamage = 10; // Damage for basic attacks (K key)
    private boolean invulnerable = false;
    private int invulnerabilityTimer = 0;
    private final int INVULNERABILITY_DURATION = 30; // frames of invulnerability after being hit
    
    // UI elements
    private BufferedImage portraitImage;
    
    // Hit animation state
    private boolean isHit = false;
    private int hitAnimationTimer = 0;
    private final int HIT_ANIMATION_DURATION = 20; // frames to show hit animation
    
    // Mana system
    private int maxMana = 100;
    private int currentMana = 100;
    private final int ATTACK_MANA_COST = 50; // 50% of mana for attack
    private final int MANA_REGEN_RATE = 2; // Normal regeneration rate
    private final int CHARGING_MANA_REGEN_RATE = 1; // Regeneration rate while charging

    private BufferedImage spriteSheet;
    private int frameCount;
    private int currentFrame = 0;
    private int frameHeight;
    private int frameWidth;
    private int animationSpeed = 10; // Adjust for slower/faster animation
    private int animationCounter = 0;
    private int attackCount = 0;
    private boolean facingLeft = false; // Track the direction the player is facing
    private boolean isCharging = false; // Track if the player is charging
    private boolean isAttacking = false; // Track if the player is attacking
    private boolean isBasicAttacking = false; // Track if player is doing basic attack
    private int basicAttackCount = 0;
    
    // Store previous sprite info to return to after hit animation
    private String previousSpriteFile = "assets/Blue_witch/B_witch_idle.png";
    private int previousWidth = 21;
    private int previousHeight = 39;
    private int previousFrameCount = 6;

    /**
     * Constructor for the Player class.
     * Creates a player with the specified position, dimensions, and sprite.
     * 
     * @param x int X-coordinate of the player
     * @param y int Y-coordinate of the player
     * @param width int Width of the player sprite
     * @param height int Height of the player sprite
     * @param spriteFile String Path to the sprite sheet file
     * @param frameCount int Number of animation frames in the sprite sheet
     * @return void
     */
    public Player(int x, int y, int width, int height, String spriteFile, int frameCount) {
        int scaledWidth = width * 2;
        int scaledHeight = height * 2;
        bounds = new Rectangle(x, y, scaledWidth, scaledHeight + 20);
        hurtbox = new Rectangle(0, 0, 0, 0); // Initialize empty hurtbox
        this.frameCount = frameCount;
        
        // Store initial sprite info
        previousSpriteFile = spriteFile;
        previousWidth = width;
        previousHeight = height;
        previousFrameCount = frameCount;

        try {
            spriteSheet = ImageIO.read(new File(spriteFile));
            frameHeight = spriteSheet.getHeight() / frameCount;
            frameWidth = spriteSheet.getWidth();
            
            // Load portrait image for UI
            portraitImage = ImageIO.read(new File("assets/Blue_witch/B_witch.gif"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the player's state each frame.
     * Handles gravity, movement, animation, attack cooldowns, mana regeneration, and other state changes.
     * 
     * @return void
     */
    public void update() {
        speedY += GRAVITY;
        
        // Calculate new position
        int newX = bounds.x + speedX;
        int newY = bounds.y + speedY;
        
        // Prevent going out of left window bound
        if (newX < 0) {
            newX = 0;
        }
        
        // Prevent going out of right window bound (assuming window width is 800)
        if (newX + bounds.width > 800) {
            newX = 800 - bounds.width;
        }
        
        // Update position
        bounds.x = newX;
        bounds.y = newY;
        
        // Check if player has fallen off the screen
        if (bounds.y > 600) { // Assuming window height is 600
            // Player has fallen - set HP to 0 to trigger death
            currentHP = 0;
            // No need to reset position since the game will handle death
        }

        // Update animation frame
        animationCounter++;
        if (animationCounter >= animationSpeed) {
            currentFrame = (currentFrame + 1) % frameCount;

            if (isAttacking) {
                attackCount++;
                // Lock player in place during attack
                speedX = 0;
            }
            if (attackCount >= 9) {
                stopAttack();
                attackCount = 0;
            }

            if (isBasicAttacking) {
                basicAttackCount++;
            }
            if (basicAttackCount >= 3) { // Reduced from 6 to 3 frames for faster basic attacks
                stopBasicAttack();
                basicAttackCount = 0;
            }
            
            // Handle hit animation timing
            if (isHit) {
                hitAnimationTimer++;
                
                // Only lock movement during the first few frames of hit animation
                if (hitAnimationTimer < 10) {
                    speedX = 0;
                }

                if (hitAnimationTimer >= HIT_ANIMATION_DURATION) {
                    stopHitAnimation();
                }
            }

            animationCounter = 0;
        }

        // If charging or attacking (but not basic attacking), prevent movement
        if (isCharging || isAttacking) {
            speedX = 0;
        }
        
        // Regenerate mana only when charging
        if (isCharging) {
            regenerateMana(CHARGING_MANA_REGEN_RATE);
        } else {
            // Regenerate mana slowly even when not charging
            if (animationCounter == 0) {
                regenerateMana(1); // Slower regeneration rate
            }
        }

        // Update hurtbox position based on player position and direction
        updateHurtbox();
        
        // Update invulnerability timer
        if (invulnerable) {
            invulnerabilityTimer++;
            if (invulnerabilityTimer >= INVULNERABILITY_DURATION) {
                invulnerable = false;
                invulnerabilityTimer = 0;
            }
        }
    }

    /**
     * Updates the position of the player's attack hitbox (hurtbox).
     * Positions and sizes the hurtbox based on the player's facing direction and attack type.
     * 
     * @return void
     */
    private void updateHurtbox() {
        if (isAttacking || isBasicAttacking) {
            // Set hurtbox dimensions - adjust these values as needed
            int hurtboxWidth;
            int hurtboxHeight;
            if (isAttacking){
                hurtboxWidth = bounds.width * 4;
                hurtboxHeight = bounds.height;
            } else {
                // Make basic attack hurtbox larger
                hurtboxWidth = bounds.width;
                hurtboxHeight = bounds.height;
            }

            if (facingLeft) {
                // Position hurtbox to the left of player
                hurtbox.setBounds(
                    bounds.x - hurtboxWidth, // Position left of player
                    bounds.y + bounds.height / 4, // Center vertically
                    hurtboxWidth,
                    hurtboxHeight);
            } else {
                // Position hurtbox to the right of player
                hurtbox.setBounds(
                    bounds.x + bounds.width, // Position right of player
                    bounds.y + bounds.height / 4, // Center vertically
                    hurtboxWidth,
                    hurtboxHeight);
            }
        } else {
            // No hurtbox when not attacking
            hurtbox.setBounds(0, 0, 0, 0);
        }
    }

    /**
     * Checks for collision with a platform and adjusts player position accordingly.
     * Handles different collision cases (top, left, right) based on player's movement direction.
     * 
     * @param platform Platform The platform to check collision with
     * @return void
     */
    public void checkCollision(Platform platform) {
        if (bounds.intersects(platform.getBounds())) {
            Rectangle platformBounds = platform.getBounds();
            
            // Check collision with top of platform (landing)
            if (bounds.y + bounds.height - speedY <= platformBounds.y) {
                bounds.y = platformBounds.y - bounds.height;
                speedY = 0;
            }
            // Check collision with left side of platform (wall on right)
            else if (bounds.x + bounds.width - speedX <= platformBounds.x) {
                bounds.x = platformBounds.x - bounds.width;
                speedX = 0;
            }
            // Check collision with right side of platform (wall on left)
            else if (bounds.x - speedX >= platformBounds.x + platformBounds.width) {
                bounds.x = platformBounds.x + platformBounds.width;
                speedX = 0;
            }
        }
    }

    /**
     * Moves the player left.
     * Updates the player's horizontal speed and facing direction.
     * Does not move the player if they are in the middle of an attack.
     * 
     * @return void
     */
    public void moveLeft() {
        if (!isAttacking) { // Only check if not attacking, allow during basic attack
            speedX = -4;
            facingLeft = true;
        }
    }

    /**
     * Moves the player right.
     * Updates the player's horizontal speed and facing direction.
     * Does not move the player if they are in the middle of an attack.
     * 
     * @return void
     */
    public void moveRight() {
        if (!isAttacking) { // Only check if not attacking, allow during basic attack
            speedX = 4;
            facingLeft = false;
        }
    }

    /**
     * Makes the player jump with normal strength.
     * Only works if the player is on a platform (not midair) and not attacking.
     * 
     * @return void
     */
    public void jump() {
        // Only allow jumping if on a platform
        if (speedY == 0 && !isAttacking) { // Only check if not attacking, allow during basic attack
            speedY = JUMP_STRENGTH;
        }
    }

    /**
     * Makes the player jump with extra strength.
     * Only works if the player is on a platform (not midair) and not attacking.
     * 
     * @return void
     */
    public void highJump() {
        // Only allow jumping if on a platform
        if (speedY == 0 && !isAttacking) { // Only check if not attacking, allow during basic attack
            speedY = HIGH_JUMP_STRENGTH;
        }
    }

    /**
     * Stops the player's horizontal movement.
     * Sets horizontal speed to zero.
     * 
     * @return void
     */
    public void stop() {
        speedX = 0;
    }

    /**
     * Starts the charging animation and state.
     * Used for mana regeneration and preparing for an attack.
     * Changes player sprite to charging animation.
     * 
     * @return void
     */
    public void startCharging() {
        if (!isCharging) {
            isCharging = true;
            changeSprite(48, 48, "assets/Blue_witch/B_witch_charge.png", 5); // Charging sprite
        }
    }

    /**
     * Stops the charging state and returns to idle animation.
     * 
     * @return void
     */
    public void stopCharging() {
        if (isCharging) {
            isCharging = false;
            changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Return to idle sprite
        }
    }

    /**
     * Performs a super attack if the player has enough mana.
     * Changes sprite, consumes mana, and plays attack sound.
     * 
     * @return void
     */
    public void attack() {
        // Only allow attack if player has enough mana and isn't in hit state
        if (!isAttacking && !isBasicAttacking && currentMana >= ATTACK_MANA_COST) {
            isAttacking = true;
            // Consume mana for attack
            useMana(ATTACK_MANA_COST);
            changeSprite(104, 45, "assets/Blue_witch/B_witch_attack.png", 9); // Attack sprite
            currentFrame = 0; // Reset animation to first frame
            animationCounter = 0; // Reset animation counter to avoid immediate frame progression
            
            // Play super attack sound
            playSuperAttackSound();
        }
    }

    /**
     * Performs a basic attack that doesn't require mana.
     * Changes sprite and plays basic attack sound.
     * 
     * @return void
     */
    public void basicAttack() {
        if (!isAttacking && !isBasicAttacking) { // Only allow basic attack if not already attacking
            // Start basic attack
            isBasicAttacking = true;
            basicAttackCount = 0;
            changeSprite(50, 45, "assets/Blue_witch/B_witch_basic.png", 5); // Basic attack using idle sprite
            currentFrame = 0; // Reset animation to first frame
            animationCounter = 0; // Reset animation counter
            
            // Play basic attack sound
            playBasicAttackSound();
        }
    }

    /**
     * Stops the super attack animation and returns to idle state.
     * 
     * @return void
     */
    public void stopAttack() {
        if (isAttacking) {
            isAttacking = false;
            changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Return to idle sprite
        }
    }

    /**
     * Stops the basic attack animation and returns to idle state.
     * 
     * @return void
     */
    public void stopBasicAttack() {
        if (isBasicAttacking) {
            isBasicAttacking = false;
            changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Return to idle sprite
        }
    }
    
    /**
     * Starts the hit animation when the player takes damage.
     * Saves the current sprite state to return to after hit animation completes.
     * 
     * @return void
     */
    private void startHitAnimation() {
        if (!isHit) {
            // Save current sprite info before changing to hit animation if not already in hit sprite
            if (!isAttacking && !isBasicAttacking) {
                previousSpriteFile = "assets/Blue_witch/B_witch_idle.png";
                previousWidth = 21;
                previousHeight = 39;
                previousFrameCount = 6;
            }
            
            isHit = true;
            hitAnimationTimer = 0;
            
            // Change to hit animation sprite
            changeSprite(32, 48, "assets/Blue_witch/B_witch_take_damage.png", 3);
            currentFrame = 0; // Start from first frame
        }
    }
    
    /**
     * Stops the hit animation and restores the previous sprite state.
     * 
     * @return void
     */
    private void stopHitAnimation() {
        if (isHit) {
            isHit = false;
            hitAnimationTimer = 0;
            
            // Return to previous state if it was an attack
            if (isAttacking) {
                changeSprite(104, 45, "assets/Blue_witch/B_witch_attack.png", 9);
            } else if (isBasicAttacking) {
                changeSprite(50, 45, "assets/Blue_witch/B_witch_basic.png", 5);
            } else {
                // Otherwise go back to idle
                changeSprite(previousWidth, previousHeight, previousSpriteFile, previousFrameCount);
            }
            
            // Player can move again after hit animation ends, even while still invulnerable
        }
    }

    /**
     * Changes the player's sprite sheet to a new one.
     * Handles error cases gracefully if the new sprite cannot be loaded.
     * 
     * @param width int Width of the new sprite
     * @param height int Height of the new sprite
     * @param spriteFile String Path to the new sprite sheet file
     * @param frameCount int Number of animation frames in the new sprite sheet
     * @return void
     */
    public void changeSprite(int width, int height, String spriteFile, int frameCount) {
        try {
            // Store previous state in case loading fails
            BufferedImage oldSheet = spriteSheet;
            int oldFrameCount = this.frameCount;
            int oldFrameHeight = frameHeight;
            int oldFrameWidth = frameWidth;
            
            // Try to load new sprite
            BufferedImage newSheet = ImageIO.read(new File(spriteFile));
            
            // If successfully loaded, update sprite properties
            spriteSheet = newSheet;
            this.frameCount = frameCount;
            frameHeight = spriteSheet.getHeight() / frameCount;
            frameWidth = spriteSheet.getWidth();
            
            // Reset animation to avoid out-of-bounds frames
            currentFrame = 0;
            animationCounter = 0;
        } catch (IOException e) {
            // Log error but don't crash
            System.out.println("Error loading sprite: " + spriteFile);
            // Keep using the current sprite - don't change anything
        } catch (Exception e) {
            // Other potential errors (null pointer, array index, etc.)
            System.out.println("Error changing sprite: " + e.getMessage());
        }
    }

    /**
     * Draws the player sprite, hitbox, health bar, and mana bar.
     * Handles flipping sprites when facing left and positioning special sprites like attack animations.
     * 
     * @param g Graphics object used for drawing
     * @param showBounds boolean Whether to show hitboxes for debugging
     * @return void
     */
    public void draw(Graphics g, boolean showBounds) {
        if (spriteSheet != null) {
            try {
                // Make sure currentFrame doesn't exceed our frameCount
                int safeFrame = Math.min(currentFrame, frameCount - 1);
                
                // Add bounds checking for subimage extraction
                int y = safeFrame * frameHeight;
                if (y + frameHeight <= spriteSheet.getHeight() && frameWidth <= spriteSheet.getWidth()) {
                    BufferedImage currentSprite = spriteSheet.getSubimage(0, y, frameWidth, frameHeight);

                    // Scale the sprite by 3x
                    int scaledWidth = frameWidth * 3;
                    int scaledHeight = frameHeight * 3;

                    int spriteX, spriteY;
                    
                    // Exception for attacking sprites - special positioning
                    if (isAttacking) {
                        if (facingLeft) {
                            // When attacking and facing left, position the sprite so the right side aligns with center
                            spriteX = bounds.x + bounds.width - scaledWidth;
                        } else {
                            // When attacking and facing right, use the default left alignment
                            spriteX = bounds.x - 25;
                        }
                        spriteY = bounds.y - 20;
                    } else {
                        // Center other sprites
                        spriteX = bounds.x + (bounds.width - scaledWidth) / 2;
                        spriteY = bounds.y + (bounds.height - scaledHeight) / 2;
                    }

                    if (facingLeft) {
                        // Flip the sprite horizontally using an offscreen buffer
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
                // System.out.println("Error rendering sprite: " + e.getMessage());
            }
        }

        // Draw the player's hitbox if showBounds is true
        if (showBounds) {
            g.setColor(Color.RED);
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height); // Draw consistent hitbox

            // Only draw the hurtbox when debug mode (showBounds) is active
            if ((isAttacking || isBasicAttacking) && hurtbox.width > 0) {
                g.setColor(Color.GREEN);
                g.drawRect(hurtbox.x, hurtbox.y, hurtbox.width, hurtbox.height); // Draw hurtbox outline
            }
        }
        
        // Draw health and mana bars
        drawHealthBar(g);
        drawManaBar(g);
        drawProfileBorder(g);
    }

    /**
     * Draws the profile border image containing the player's portrait.
     * 
     * @param g Graphics object used for drawing
     * @return void
     */
    private void drawProfileBorder(Graphics g) {
        int profileX = 5;
        int profileY = 14;
        //Draw Image, from assets/ProfileBar.png at the top left of the screen
        try {
            BufferedImage profileBorder = ImageIO.read(new File("assets/ProfileBar.png"));
            g.drawImage(profileBorder, profileX, profileY, 232, 64, null); // Adjust size as needed
        } catch (IOException e) {
            System.out.println("Error loading profile border image: " + e.getMessage());
        }
    }
    
    /**
     * Draws the health bar at the top of the screen.
     * Displays the player's current health as a proportion of maximum health.
     * 
     * @param g Graphics object used for drawing
     * @return void
     */
    private void drawHealthBar(Graphics g) {
        int barWidth = 150;
        int barHeight = 15;
        int barX = 80; // Position at top left of screen, leaving space for portrait
        int barY = 30; // Position moved down by 10 pixels
        
        // Background (empty health)
        g.setColor(new Color(241, 181, 133));
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Foreground (current health)
        g.setColor(new Color(212, 103,108));
        int currentWidth = (int)(((double)currentHP / maxHP) * barWidth);
        g.fillRect(barX, barY, currentWidth, barHeight);
        
        // Border
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
        
        // Draw player portrait
        if (portraitImage != null) {
            g.drawImage(portraitImage, 10, 20, 50, 50, null); // Moved down by 10 pixels
        } else {
            // If image couldn't be loaded, draw a simple circle as placeholder
            g.setColor(Color.BLUE);
            g.fillOval(10, 20, 50, 50); // Moved down by 10 pixels
        }
    }
    
    /**
     * Draws the mana bar at the top of the screen.
     * Displays the player's current mana as a proportion of maximum mana.
     * 
     * @param g Graphics object used for drawing
     * @return void
     */
    private void drawManaBar(Graphics g) {
        int barWidth = 150;
        int barHeight = 15;
        int barX = 80; // Same X as health bar
        int barY = 60; // Position moved down by 10 pixels
        
        // Background (empty mana)
        g.setColor(new Color(241, 181, 133));
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Foreground (current mana)
        g.setColor(new Color(55, 23, 66));
        int currentWidth = (int)(((double)currentMana / maxMana) * barWidth);
        g.fillRect(barX, barY, currentWidth, barHeight);
        
        // Border
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
    }
    
    /**
     * Consumes mana for abilities.
     * Decreases the player's current mana by the specified amount.
     * 
     * @param amount int The amount of mana to consume
     * @return boolean True if there was enough mana to use, false otherwise
     */
    public boolean useMana(int amount) {
        if (currentMana >= amount) {
            currentMana -= amount;
            return true;
        }
        return false;
    }
    
    /**
     * Regenerates the player's mana.
     * Increases current mana by the specified amount, capped at maximum mana.
     * 
     * @param amount int The amount of mana to regenerate
     * @return void
     */
    public void regenerateMana(int amount) {
        currentMana += amount;
        if (currentMana > maxMana) {
            currentMana = maxMana;
        }
    }
    
    /**
     * Gets the player's current mana.
     * 
     * @return int The current mana
     */
    public int getCurrentMana() {
        return currentMana;
    }
    
    /**
     * Gets the player's maximum mana.
     * 
     * @return int The maximum mana
     */
    public int getMaxMana() {
        return maxMana;
    }
    
    /**
     * Checks if the player has enough mana for a super attack.
     * 
     * @return boolean True if the player has enough mana, false otherwise
     */
    public boolean hasEnoughManaForAttack() {
        return currentMana >= ATTACK_MANA_COST;
    }
    
    /**
     * Applies damage to the player.
     * Reduces health, triggers hit animation, and makes the player temporarily invulnerable.
     * Does nothing if the player is already invulnerable.
     * 
     * @param damage int The amount of damage to apply
     * @return void
     */
    public void takeDamage(int damage) {
        if (!invulnerable) {
            currentHP -= damage;
            if (currentHP < 0) {
                currentHP = 0;
            }
            
            // Start hit animation
            startHitAnimation();
            
            // Make player briefly invulnerable after taking damage
            invulnerable = true;
            invulnerabilityTimer = 0;
            
            // Play hurt sound
            playHurtSound();
        }
    }
    
    /**
     * Heals the player.
     * Increases health, capped at maximum health.
     * 
     * @param amount int The amount of health to restore
     * @return void
     */
    public void heal(int amount) {
        currentHP += amount;
        if (currentHP > maxHP) {
            currentHP = maxHP;
        }
    }
    
    /**
     * Checks if the player is dead.
     * 
     * @return boolean True if the player has zero or less health, false otherwise
     */
    public boolean isDead() {
        return currentHP <= 0;
    }
    
    /**
     * Gets the player's attack damage based on the current attack type.
     * 
     * @return int The damage value for the current attack, or 0 if not attacking
     */
    public int getAttackDamage() {
        if (isAttacking) {
            return attackDamage;
        } else if (isBasicAttacking) {
            return basicAttackDamage;
        }
        return 0; // No damage if not attacking
    }
    
    /**
     * Gets the player's current health.
     * 
     * @return int The current health
     */
    public int getCurrentHP() {
        return currentHP;
    }

    /**
     * Sets the player's current health.
     * 
     * @param currentHP int The new current health value
     * @return void
     */
    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }
    
    /**
     * Gets the player's maximum health.
     * 
     * @return int The maximum health
     */
    public int getMaxHP() {
        return maxHP;
    }
    
    /**
     * Checks if the player is currently invulnerable.
     * 
     * @return boolean True if the player is invulnerable, false otherwise
     */
    public boolean isInvulnerable() {
        return invulnerable;
    }

    /**
     * Checks if the player is currently performing a super attack.
     * 
     * @return boolean True if the player is attacking, false otherwise
     */
    public boolean isAttacking() {
        return isAttacking;
    }

    /**
     * Checks if the player is currently performing a basic attack.
     * 
     * @return boolean True if the player is performing a basic attack, false otherwise
     */
    public boolean isBasicAttacking() {
        return isBasicAttacking;
    }
    
    /**
     * Checks if the player is currently in hit animation.
     * 
     * @return boolean True if the player is in hit animation, false otherwise
     */
    public boolean isHit() {
        return isHit;
    }

    /**
     * Gets the player's attack hurtbox.
     * 
     * @return Rectangle The attack hurtbox rectangle
     */
    public Rectangle getHurtbox() {
        return hurtbox;
    }

    /**
     * Gets the player's bounding rectangle.
     * 
     * @return Rectangle The bounds of the player
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Gets the player's vertical speed.
     * 
     * @return int The vertical speed
     */
    public int getSpeedY() {
        return speedY;
    }

    /**
     * Sets the player's horizontal speed.
     * 
     * @param speedX int The new horizontal speed
     * @return void
     */
    public void setSpeedX(int speedX) {
        this.speedX = speedX;
    }

    /**
     * Sets the player's facing direction.
     * 
     * @param facingLeft boolean True to face left, false to face right
     * @return void
     */
    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
    }

    /**
     * Gets the player's sprite sheet.
     * Protected method used by child classes.
     * 
     * @return BufferedImage The current sprite sheet
     */
    protected BufferedImage getSpriteSheet() {
        return spriteSheet;
    }

    /**
     * Gets the player's current animation frame index.
     * Protected method used by child classes.
     * 
     * @return int The current frame index
     */
    protected int getCurrentFrame() {
        return currentFrame;
    }

    /**
     * Gets the height of each frame in the sprite sheet.
     * Protected method used by child classes.
     * 
     * @return int The frame height
     */
    protected int getFrameHeight() {
        return frameHeight;
    }

    /**
     * Gets the width of each frame in the sprite sheet.
     * Protected method used by child classes.
     * 
     * @return int The frame width
     */
    protected int getFrameWidth() {
        return frameWidth;
    }

    /**
     * Checks if the player is facing left.
     * Protected method used by child classes.
     * 
     * @return boolean True if the player is facing left, false if facing right
     */
    protected boolean isFacingLeft() {
        return facingLeft;
    }

    /**
     * Sets the player's position to specific coordinates.
     * 
     * @param x int X-coordinate for the player
     * @param y int Y-coordinate for the player
     * @return void
     */
    public void setPosition(int x, int y) {
        bounds.x = x;
        bounds.y = y;
    }

    /**
     * Plays the sound effect for basic attacks.
     * 
     * @return void
     */
    private void playBasicAttackSound() {
        try {
            AudioPlayer attackSound = new AudioPlayer("assets/sounds/basic_attack.wav");
            attackSound.play(false);
        } catch (Exception e) {
            System.out.println("Could not play basic attack sound: " + e.getMessage());
        }
    }
    
    /**
     * Plays the sound effect for super attacks.
     * 
     * @return void
     */
    private void playSuperAttackSound() {
        try {
            AudioPlayer attackSound = new AudioPlayer("assets/sounds/super_attack.wav");
            attackSound.play(false);
        } catch (Exception e) {
            System.out.println("Could not play super attack sound: " + e.getMessage());
        }
    }
    
    /**
     * Plays the sound effect for when the player takes damage.
     * 
     * @return void
     */
    private void playHurtSound() {
        try {
            AudioPlayer hurtSound = new AudioPlayer("assets/sounds/player_hurt.wav");
            hurtSound.play(false);
        } catch (Exception e) {
            System.out.println("Could not play player hurt sound: " + e.getMessage());
        }
    }
}
