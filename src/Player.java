import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Player {
    private Rectangle bounds;
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

    public Player(int x, int y, int width, int height, String spriteFile, int frameCount) {
        int scaledWidth = width*3;
        int scaledHeight = height*3;
        bounds = new Rectangle(x, y, scaledWidth, scaledHeight);
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

            if(isAttacking){
                attackCount++;
            }
            if(attackCount >= 9){
                stopAttack();
                attackCount = 0;
            }
            animationCounter = 0;
        }

        // If charging or attacking, prevent movement
        if (isCharging || isAttacking) {
            speedX = 0;
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
        if (!isAttacking) {
            isAttacking = true;
            changeSprite(104, 45, "assets/Blue_witch/B_witch_attack.png", 9); // Attack sprite
            currentFrame = 0; // Reset animation to first frame
            animationCounter = 0; // Reset animation counter to avoid immediate frame progression
        }
    }

    public void stopAttack() {
        if (isAttacking) {
            isAttacking = false;
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

            int spriteX = bounds.x + (bounds.width - scaledWidth) / 2; // Center the sprite horizontally
            int spriteY = bounds.y + (bounds.height - scaledHeight) / 2; // Center the sprite vertically

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
        if (showBounds) {
            g.setColor(Color.RED);
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height); // Draw consistent hitbox
        }
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
}

