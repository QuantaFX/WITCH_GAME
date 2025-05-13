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

    public Player(int x, int y, int width, int height, String spriteFile, int frameCount) {
        int scaledWidth = width * 3;
        int scaledHeight = height * 3;
        bounds = new Rectangle(x, y, scaledWidth, scaledHeight);
        hurtbox = new Rectangle(0, 0, 0, 0); // Initialize empty hurtbox
        this.frameCount = frameCount;

        try {
            spriteSheet = ImageIO.read(new File(spriteFile));
            frameHeight = spriteSheet.getHeight() / frameCount;
            frameWidth = spriteSheet.getWidth();
        } catch (IOException e) {
            System.out.println("hi");
            e.printStackTrace();
        }
    }

    public void update() {
        speedY += GRAVITY;
        bounds.x += speedX;
        bounds.y += speedY;

        // Update animation frame
        animationCounter++;
        if (animationCounter >= animationSpeed) {
            currentFrame = (currentFrame + 1) % frameCount;

            if (isAttacking) {
                attackCount++;
            }
            if (attackCount >= 9) {
                stopAttack();
                attackCount = 0;
            }

            if (isBasicAttacking) {
                basicAttackCount++;
            }
            if (basicAttackCount >= 6) { // Assuming basic attack has 6 frames
                stopBasicAttack();
                basicAttackCount = 0;
            }

            animationCounter = 0;
        }

        // If charging or attacking, prevent movement
        if (isCharging || isAttacking || isBasicAttacking) {
            speedX = 0;
        }

        // Update hurtbox position based on player position and direction
        updateHurtbox();
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
                hurtboxWidth = bounds.width / 2;
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
            if (bounds.y + bounds.height - speedY <= platform.getBounds().y) {
                bounds.y = platform.getBounds().y - bounds.height;
                speedY = 0;
            }
        }
    }

    public void moveLeft() {
        speedX = -5;
        facingLeft = true; // Set direction to left
    }

    public void moveRight() {
        speedX = 5;
        facingLeft = false; // Set direction to right
    }

    public void jump() {
        if (speedY == 0) {
            speedY = JUMP_STRENGTH;
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
        if (!isAttacking && !isBasicAttacking) {
            isAttacking = true;
            changeSprite(104, 45, "assets/Blue_witch/B_witch_attack.png", 9); // Attack sprite
            currentFrame = 0; // Reset animation to first frame
            animationCounter = 0; // Reset animation counter to avoid immediate frame progression
        }
    }

    public void basicAttack() {
        if (!isBasicAttacking && !isAttacking) {
            isBasicAttacking = true;
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

    public void changeSprite(int width, int height, String spriteFile, int frameCount) {
        this.frameCount = frameCount;

        try {
            spriteSheet = ImageIO.read(new File(spriteFile));
            frameHeight = spriteSheet.getHeight() / frameCount;
            frameWidth = spriteSheet.getWidth();
        } catch (IOException e) {
            System.out.println("hii");
            e.printStackTrace();
        }

        // The hitbox (bounds) remains unchanged, so no adjustments to `bounds` are made here.
    }

    public void draw(Graphics g, boolean showBounds) {
        if (spriteSheet != null) {
            BufferedImage currentSprite = spriteSheet.getSubimage(0, currentFrame * frameHeight, frameWidth, frameHeight);

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
                    spriteX = bounds.x;
                }
                spriteY = bounds.y;
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
    }

    // Add this getter method to check if player is attacking
    public boolean isAttacking() {
        return isAttacking || isBasicAttacking; // Either type of attack counts
    }

    // Add getter for basic attack state
    public boolean isBasicAttacking() {
        return isBasicAttacking;
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
}
