import java.awt.Rectangle;

public class Enemy extends Player {
    private Player target;
    private int followSpeed = 1;
    private int followDelay = 0;
    private final int MAX_FOLLOW_DELAY = 30;
    private final int Y_LEVEL_TOLERANCE = 2; // Tolerance for Y-level difference
    
    public Enemy(int x, int y, int width, int height, String spriteFile, int frameCount){
        super(x, y, width, height, spriteFile, frameCount);
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
    }
}
