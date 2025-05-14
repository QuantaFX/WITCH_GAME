import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Background {
    private BufferedImage[] layers;
    private double[] scrollSpeeds; // Use double for smoother scrolling
    private double[] offsets; // Track the horizontal scroll offsets as double

    public Background() {
        layers = new BufferedImage[4];
        scrollSpeeds = new double[]{0.05, 0.1, 0.2, 0.4}; // Slower speeds for each layer
        offsets = new double[4];

        try {
            layers[0] = ImageIO.read(new File("assets/Clouds/Clouds 5/1.png")); // Farthest layer
            layers[1] = ImageIO.read(new File("assets/Clouds/Clouds 5/3.png"));
            layers[2] = ImageIO.read(new File("assets/Clouds/Clouds 5/4.png"));
            layers[3] = ImageIO.read(new File("assets/Clouds/Clouds 5/5.png")); // Closest layer
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        for (int i = 0; i < offsets.length; i++) {
            offsets[i] -= scrollSpeeds[i]; // Move each layer
            if (offsets[i] <= -800) { // Reset offset when it scrolls out of view
                offsets[i] += 800;
            }
        }
    }

    public void draw(Graphics g) {
        for (int i = 0; i < layers.length; i++) {
            if (layers[i] != null) {
                // Draw the layer twice to create a seamless scrolling effect
                g.drawImage(layers[i], (int) offsets[i], 0, 800, 600, null);
                g.drawImage(layers[i], (int) offsets[i] + 800, 0, 800, 600, null);
            }
        }
    }
}
