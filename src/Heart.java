import java.awt.*;

/**
 * Represents a heart pickup item in the game that restores player health.
 * Hearts have a floating animation and a distinctive heart shape.
 */
public class Heart {
    private Rectangle bounds;
    private final int HEAL_AMOUNT = 30;
    private final int SIZE = 15;
    private final Color HEART_COLOR = new Color(255, 0, 80);
    private final Color HEART_OUTLINE = new Color(200, 0, 60);
    
    // Animation properties
    private int floatOffset = 0;
    private int floatDirection = 1;
    private final int MAX_FLOAT_OFFSET = 5;
    
    /**
     * Constructor for the Heart class.
     * Creates a heart pickup item at the specified position.
     * 
     * @param x int X-coordinate of the heart
     * @param y int Y-coordinate of the heart
     * @return void
     */
    public Heart(int x, int y) {
        bounds = new Rectangle(x, y, SIZE, SIZE);
    }
    
    /**
     * Updates the heart's state.
     * Handles the floating animation by updating the vertical offset.
     * 
     * @return void
     */
    public void update() {
        // Simple floating animation
        floatOffset += floatDirection;
        if (floatOffset >= MAX_FLOAT_OFFSET || floatOffset <= -MAX_FLOAT_OFFSET) {
            floatDirection *= -1;
        }
    }
    
    /**
     * Draws the heart on the screen.
     * Renders a heart shape using basic geometric primitives.
     * 
     * @param g Graphics object used for drawing
     * @return void
     */
    public void draw(Graphics g) {
        int x = bounds.x;
        int y = bounds.y + floatOffset;
        
        // Draw heart shape
        g.setColor(HEART_COLOR);
        
        // Draw a simple heart shape (two circles and a triangle)
        int halfSize = SIZE / 2;
        
        // Fill the heart
        g.fillArc(x, y, halfSize, halfSize, 0, 180);
        g.fillArc(x + halfSize, y, halfSize, halfSize, 0, 180);
        int[] xPoints = {x, x + SIZE / 2, x + SIZE};
        int[] yPoints = {y + halfSize / 2, y + SIZE, y + halfSize / 2};
        g.fillPolygon(xPoints, yPoints, 3);
        
        // Draw the outline
        g.setColor(HEART_OUTLINE);
        g.drawArc(x, y, halfSize, halfSize, 0, 180);
        g.drawArc(x + halfSize, y, halfSize, halfSize, 0, 180);
        g.drawPolyline(xPoints, yPoints, 3);
    }
    
    /**
     * Returns the bounding rectangle of this heart.
     * Used for collision detection with the player.
     * 
     * @return Rectangle The bounds of this heart
     */
    public Rectangle getBounds() {
        return bounds;
    }
    
    /**
     * Returns the amount of health this heart restores when collected.
     * 
     * @return int The healing amount
     */
    public int getHealAmount() {
        return HEAL_AMOUNT;
    }
} 