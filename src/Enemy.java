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
        setSpeedX(-followSpeed);
        setFacingLeft(true);
    }
    
    @Override
    public void moveRight() {
        setSpeedX(followSpeed);
        setFacingLeft(false);
    }
    
    public void followTarget() {
        if (target == null) return;
        
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
                changeSprite(36, 28, "assets/Orc_Sprite/orc_run.png", 4);
                moveLeft();
            } else if (targetBounds.x > myBounds.x) {
                changeSprite(36, 28, "assets/Orc_Sprite/orc_run.png", 4);
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
    
    @Override
    public void update() {
        if (target != null) {
            followTarget();
        }
        super.update();
        
        // Adjust hitbox position based on facing direction after movement update
        Rectangle bounds = getBounds();
        boolean facingLeft = isFacingLeft();
        
        // For sprite rendering, we'll handle this in the draw method
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
        // Call the parent changeSprite method
        super.changeSprite(width/2, height, spriteFile, frameCount);
        this.originalWidth = width;
    }
}
