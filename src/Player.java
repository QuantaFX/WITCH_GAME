import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    // Method to update hurtbox position
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
                    hurtboxHeight
                );
            } else {
                // Position hurtbox to the right of player
                hurtbox.setBounds(
                    bounds.x + bounds.width, // Position right of player
                    bounds.y + bounds.height / 4, // Center vertically
                    hurtboxWidth,
                    hurtboxHeight
                );
            }
        } else {
            // If not attacking, set hurtbox to zero size
            hurtbox.setBounds(0, 0, 0, 0);
        }
    }

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

    public void moveLeft() {
        if (!isAttacking) { // Only check if not attacking, allow during basic attack
            speedX = -4;
            facingLeft = true;
        }
    }

    public void moveRight() {
        if (!isAttacking) { // Only check if not attacking, allow during basic attack
            speedX = 4;
            facingLeft = false;
        }
    }

    public void jump() {
        // Only allow jumping if on a platform
        if (speedY == 0 && !isAttacking) { // Only check if not attacking, allow during basic attack
            speedY = JUMP_STRENGTH;
        }
    }

    public void highJump() {
        // Only allow jumping if on a platform
        if (speedY == 0 && !isAttacking) { // Only check if not attacking, allow during basic attack
            speedY = HIGH_JUMP_STRENGTH;
        }
    }

    public void stop() {
        speedX = 0;
    }

    public void startCharging() {
        if (!isCharging) {
            isCharging = true;
            changeSprite(48, 48, "assets/Blue_witch/B_witch_charge.png", 5); // Charging sprite
        }
    }

    public void stopCharging() {
        if (isCharging) {
            isCharging = false;
            changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Return to idle sprite
        }
    }

    public void attack() {
        // Only allow attack if player has enough mana and isn't in hit state
        if (!isAttacking && !isBasicAttacking && currentMana >= ATTACK_MANA_COST) {
            isAttacking = true;
            // Consume mana for attack
            useMana(ATTACK_MANA_COST);
            changeSprite(104, 45, "assets/Blue_witch/B_witch_attack.png", 9); // Attack sprite
            currentFrame = 0; // Reset animation to first frame
            animationCounter = 0; // Reset animation counter to avoid immediate frame progression
        }
    }

    public void basicAttack() {
        if (!isAttacking && !isBasicAttacking) { // Only allow basic attack if not already attacking
            // Start basic attack
            isBasicAttacking = true;
            basicAttackCount = 0;
            changeSprite(50, 45, "assets/Blue_witch/B_witch_basic.png", 5); // Basic attack using idle sprite
            currentFrame = 0; // Reset animation to first frame
            animationCounter = 0; // Reset animation counter
        }
    }

    public void stopAttack() {
        if (isAttacking) {
            isAttacking = false;
            changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Return to idle sprite
        }
    }

    public void stopBasicAttack() {
        if (isBasicAttacking) {
            isBasicAttacking = false;
            changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Return to idle sprite
        }
    }
    
    // Start hit animation
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
    
    // Stop hit animation and restore previous sprite
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
    }
    
    // Method to draw the health bar above the player
    private void drawHealthBar(Graphics g) {
        int barWidth = bounds.width;
        int barHeight = 5;
        int barX = bounds.x;
        int barY = bounds.y - 12;
        
        // Background (empty health)
        g.setColor(Color.RED);
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Foreground (current health)
        g.setColor(Color.GREEN);
        int currentWidth = (int)(((double)currentHP / maxHP) * barWidth);
        g.fillRect(barX, barY, currentWidth, barHeight);
        
        // Border
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
    }
    
    // Method to draw the mana bar below the health bar
    private void drawManaBar(Graphics g) {
        int barWidth = bounds.width;
        int barHeight = 5;
        int barX = bounds.x;
        int barY = bounds.y - 6; // Position just below health bar
        
        // Background (empty mana)
        g.setColor(Color.GRAY);
        g.fillRect(barX, barY, barWidth, barHeight);
        
        // Foreground (current mana)
        g.setColor(Color.BLUE);
        int currentWidth = (int)(((double)currentMana / maxMana) * barWidth);
        g.fillRect(barX, barY, currentWidth, barHeight);
        
        // Border
        g.setColor(Color.BLACK);
        g.drawRect(barX, barY, barWidth, barHeight);
    }
    
    // Method to use mana
    public boolean useMana(int amount) {
        if (currentMana >= amount) {
            currentMana -= amount;
        }
        return false;
    }
    
    // Method to regenerate mana
    public void regenerateMana(int amount) {
        currentMana += amount;
        if (currentMana > maxMana) {
            currentMana = maxMana;
        }
    }
    
    // Method to get current mana
    public int getCurrentMana() {
        return currentMana;
    }
    
    // Method to get max mana
    public int getMaxMana() {
        return maxMana;
    }
    
    // Method to check if player has enough mana for an attack
    public boolean hasEnoughManaForAttack() {
        return currentMana >= ATTACK_MANA_COST;
    }
    
    // Method to take damage - now triggers hit animation
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
        }
    }
    
    // Method to heal
    public void heal(int amount) {
        currentHP += amount;
        if (currentHP > maxHP) {
            currentHP = maxHP;
        }
    }
    
    // Check if player is dead
    public boolean isDead() {
        return currentHP <= 0;
    }
    
    // Get attack damage based on attack type
    public int getAttackDamage() {
        if (isAttacking) {
            return attackDamage;
        } else if (isBasicAttacking) {
            return basicAttackDamage;
        }
        return 0; // No damage if not attacking
    }
    
    // Get current HP
    public int getCurrentHP() {
        return currentHP;
    }

    // Set current HP
    public void setCurrentHP(int currentHP) {
        this.currentHP = currentHP;
    }
    
    // Get max HP
    public int getMaxHP() {
        return maxHP;
    }
    
    // Check if player is invulnerable
    public boolean isInvulnerable() {
        return invulnerable;
    }

    // Add this getter method to check if player is attacking
    public boolean isAttacking() {
        return isAttacking;
    }

    // Add getter for basic attack state
    public boolean isBasicAttacking() {
        return isBasicAttacking;
    }
    
    // Add getter for hit state
    public boolean isHit() {
        return isHit;
    }

    public Rectangle getHurtbox() {
        return hurtbox;
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public int getSpeedY() {
        return speedY;
    }

    public void setSpeedX(int speedX) {
        this.speedX = speedX;
    }

    public void setFacingLeft(boolean facingLeft) {
        this.facingLeft = facingLeft;
    }

    // Add getters for protected fields needed by Enemy class
    protected BufferedImage getSpriteSheet() {
        return spriteSheet;
    }

    protected int getCurrentFrame() {
        return currentFrame;
    }

    protected int getFrameHeight() {
        return frameHeight;
    }

    protected int getFrameWidth() {
        return frameWidth;
    }

    protected boolean isFacingLeft() {
        return facingLeft;
    }

    public void setPosition(int x, int y) {
        bounds.x = x;
        bounds.y = y;
    }
}
