import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.Rectangle;

public class Enemy extends Player {
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
    
    // Enemy state management
    private boolean frozen = false;
    private int frozenTimer = 0;
    private final int FROZEN_DURATION = 45; // 0.75 seconds at 60 FPS
    
    public Enemy(int x, int y, int width, int height, String spriteFile, int frameCount){
        // Use half width for the bounds/hitbox
        super(x, y, width - 18, height, spriteFile, frameCount);
        this.originalWidth = width;
    }
    
    public void setTarget(Player player) {
        this.target = player;
    }
    
    @Override
    public void moveLeft() {
        // Don't move if frozen
        if (frozen) return;
        
        setSpeedX(-followSpeed);
        setFacingLeft(true);
    }
    
    @Override
    public void moveRight() {
        // Don't move if frozen
        if (frozen) return;
        
        setSpeedX(followSpeed);
        setFacingLeft(false);
    }
    
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
                changeSprite(47, 28, "assets/Orc_Sprite/orc_walk.png", 8);
                moveLeft();
            } else if (targetBounds.x > myBounds.x) {
                changeSprite(47, 28, "assets/Orc_Sprite/orc_walk.png", 8);
                moveRight();
            } else {
                changeSprite(36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
                stop();
            }
        } else {
            // Player is not at same level, stop following
            changeSprite(36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
            stop();
        }
    }
    
    // Check if this enemy is colliding with the player
    public boolean isCollidingWithPlayer() {
        if (target == null) return false;
        return getBounds().intersects(target.getBounds());
    }
    
    // Attack the player
    public void attackPlayer() {
        if (target == null || !canAttack || frozen) return;
        
        if (isCollidingWithPlayer() && !target.isInvulnerable()) {
            try {
                // Show attack animation
                changeSprite(64, 28, "assets/Orc_Sprite/orc_hit.png", 6);
            } catch (Exception e) {
                // If attack sprite fails to load, fall back to idle sprite
                changeSprite(36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
            }
            
            // Deal damage to player
            target.takeDamage(attackDamage);
            
            // Set cooldown
            canAttack = false;
            attackCooldown = 0;
        }
    }
    
    // Freeze enemy when hit by player
    public void freeze() {
        frozen = true;
        frozenTimer = 0;
        setSpeedX(0); // Stop movement
    }
    
    @Override
    public void takeDamage(int damage) {
        super.takeDamage(damage);
        freeze(); // Freeze enemy when taking damage
    }
    
    @Override
    public void update() {
        // Update frozen state
        if (frozen) {
            frozenTimer++;
            if (frozenTimer >= FROZEN_DURATION) {
                frozen = false;
                // Return to idle sprite after being frozen
                changeSprite(36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
            }
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
                    }
                }
            }
        }
        
        super.update();
    }
    
    @Override
    public void draw(Graphics g, boolean showBounds) {
        if (getSpriteSheet() != null) {
            BufferedImage currentSprite = getSpriteSheet().getSubimage(
                0, getCurrentFrame() * getFrameHeight(), getFrameWidth(), getFrameHeight()
            );

            // Scale the sprite by 3x
            int scaledWidth = getFrameWidth() * 3;
            int scaledHeight = getFrameHeight() * 3;
            
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
    
    // Override changeSprite to ensure our width settings are preserved
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
    
    // Set attack damage
    public void setAttackDamage(int damage) {
        this.attackDamage = damage;
    }
    
    // Get attack damage
    @Override
    public int getAttackDamage() {
        return attackDamage;
    }
    
    // Check if enemy is frozen
    public boolean isFrozen() {
        return frozen;
    }
}
