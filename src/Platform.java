import java.awt.*;

/**
 * Represents a platform in the game that the player can stand on.
 * Platforms define collision areas for the player and other game elements.
 */
public class Platform {
    private Rectangle bounds;

    /**
     * Constructor for the Platform class.
     * Creates a platform with the specified position and dimensions.
     * 
     * @param x int X-coordinate of the top-left corner
     * @param y int Y-coordinate of the top-left corner
     * @param width int Width of the platform
     * @param height int Height of the platform
     * @return void
     */
    public Platform(int x, int y, int width, int height) {
        bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Returns the bounding rectangle of this platform.
     * Used for collision detection with the player and other game elements.
     * 
     * @return Rectangle The bounds of this platform
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Draws the platform on the screen.
     * Currently disabled (commented out).
     * 
     * @param g Graphics object used for drawing
     * @return void
     */
    public void draw(Graphics g) {
       // g.setColor(Color.GREEN);
        //g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
