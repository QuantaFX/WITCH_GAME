import java.awt.*;

public class Platform {
    private Rectangle bounds;

    public Platform(int x, int y, int width, int height) {
        bounds = new Rectangle(x, y, width, height);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }
}
