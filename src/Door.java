import java.awt.*;

public class Door {
    private Rectangle bounds;
    private int targetLevel;
    private boolean isActive = true;
    
    public Door(int x, int y, int width, int height) {
        this.bounds = new Rectangle(x, y, width, height);
        this.targetLevel = -1; // Default value, will be set by LevelManager
    }
    
    public Door(int x, int y, int width, int height, int targetLevel) {
        this.bounds = new Rectangle(x, y, width, height);
        this.targetLevel = targetLevel;
    }
    
    public Rectangle getBounds() {
        return bounds;
    }
    
    public int getTargetLevel() {
        return targetLevel;
    }
    
    public void setTargetLevel(int targetLevel) {
        this.targetLevel = targetLevel;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    public void draw(Graphics g) {
        if (isActive) {
            g.setColor(new Color(139, 69, 19)); // Brown color for door
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            
            // Draw door handle
            g.setColor(Color.YELLOW);
            g.fillOval(bounds.x + bounds.width - 10, bounds.y + bounds.height/2, 6, 6);
            
            // Draw door frame
            g.setColor(new Color(101, 67, 33)); // Darker brown for frame
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }
} 