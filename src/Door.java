import java.awt.*;

/**
 * Represents a door in the game that can be used to transition between levels.
 * Doors can be active or inactive, and have a visual activation effect.
 */
public class Door {
    private Rectangle bounds;
    private int targetLevel;
    private boolean isActive = false;
    private boolean wasInactive = true; // Track if door just became active
    private int activationEffectTimer = 0;
    private final int ACTIVATION_EFFECT_DURATION = 60; // 1 second at 60 FPS
    private boolean activationSoundPlayed = false;
    
    /**
     * Constructor for the Door class with unspecified target level.
     * Creates a door with the specified position and dimensions.
     * 
     * @param x int X-coordinate of the door
     * @param y int Y-coordinate of the door
     * @param width int Width of the door
     * @param height int Height of the door
     * @return void
     */
    public Door(int x, int y, int width, int height) {
        this.bounds = new Rectangle(x, y, width, height);
        this.targetLevel = -1; // Default value, will be set by LevelManager
    }
    
    /**
     * Constructor for the Door class with a specified target level.
     * Creates a door with the specified position, dimensions, and target level.
     * 
     * @param x int X-coordinate of the door
     * @param y int Y-coordinate of the door
     * @param width int Width of the door
     * @param height int Height of the door
     * @param targetLevel int The level index this door leads to
     * @return void
     */
    public Door(int x, int y, int width, int height, int targetLevel) {
        this.bounds = new Rectangle(x, y, width, height);
        this.targetLevel = targetLevel;
    }
    
    /**
     * Returns the bounding rectangle of this door.
     * Used for collision detection with the player.
     * 
     * @return Rectangle The bounds of this door
     */
    public Rectangle getBounds() {
        return bounds;
    }
    
    /**
     * Returns the target level index for this door.
     * 
     * @return int The level index this door leads to
     */
    public int getTargetLevel() {
        return targetLevel;
    }
    
    /**
     * Sets the target level for this door.
     * 
     * @param targetLevel int The level index this door should lead to
     * @return void
     */
    public void setTargetLevel(int targetLevel) {
        this.targetLevel = targetLevel;
    }
    
    /**
     * Checks if the door is currently active.
     * Only active doors can be used to transition between levels.
     * 
     * @return boolean True if the door is active, false otherwise
     */
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Sets the active state of the door.
     * Initializes activation effect when the door becomes active.
     * 
     * @param active boolean Whether the door should be active
     * @return void
     */
    public void setActive(boolean active) {
        // Check if door is becoming active
        if (!isActive && active) {
            wasInactive = true;
            activationEffectTimer = 0;
            activationSoundPlayed = false;
        }
        this.isActive = active;
    }
    
    /**
     * Updates the door's state.
     * Handles activation effect and triggers activation sound when necessary.
     * 
     * @return void
     */
    public void update() {
        // Update activation effect timer
        if (isActive && wasInactive && activationEffectTimer < ACTIVATION_EFFECT_DURATION) {
            // Play activation sound once when door becomes active
            if (!activationSoundPlayed) {
                try {
                    // Use GamePanel's sound system instead of direct AudioPlayer
                    activationSoundPlayed = true;
                    // Sound effect will be handled by the game panel
                } catch (Exception e) {
                    System.out.println("Could not play door activation sound");
                    activationSoundPlayed = true; // Mark as played to avoid repeated errors
                }
            }
            
            activationEffectTimer++;
            if (activationEffectTimer >= ACTIVATION_EFFECT_DURATION) {
                wasInactive = false;
            }
        }
    }
    
    /**
     * Draws the door on the screen.
     * Only active doors are visible. Includes a glowing effect when the door is first activated.
     * 
     * @param g Graphics object used for drawing
     * @return void
     */
    public void draw(Graphics g) {
        if (isActive) {
            // Draw the door
            g.setColor(new Color(139, 69, 19)); // Brown color for door
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            
            // Draw door handle
            g.setColor(Color.YELLOW);
            g.fillOval(bounds.x + bounds.width - 10, bounds.y + bounds.height/2, 6, 6);
            
            // Draw door frame
            g.setColor(new Color(101, 67, 33)); // Darker brown for frame
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
            
            // Draw activation effect (glowing outline)
            if (wasInactive) {
                float effectIntensity = 1.0f - (float)activationEffectTimer / ACTIVATION_EFFECT_DURATION;
                Color glowColor = new Color(1.0f, 1.0f, 0.0f, effectIntensity);
                Graphics2D g2d = (Graphics2D) g;
                Stroke originalStroke = g2d.getStroke();
                g2d.setColor(glowColor);
                g2d.setStroke(new BasicStroke(4));
                g2d.drawRect(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4);
                g2d.setStroke(originalStroke);
            }
        }
    }
} 