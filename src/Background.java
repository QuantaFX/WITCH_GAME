import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Background {
    private BufferedImage[] layers;
    private double[] scrollSpeeds; // Use double for smoother scrolling
    private double[] offsets; // Track the horizontal scroll offsets as double
    private String levelBackgroundPath;


    
    public Background(String levelBgPath) {
        layers = new BufferedImage[5];
        scrollSpeeds = new double[]{0.05, 0.1, 0.2, 0.4, 0}; // Slower speeds for each layer
        offsets = new double[5];
        this.levelBackgroundPath = levelBgPath;

        try {
            layers[0] = ImageIO.read(new File("assets/Clouds/Clouds 5/1.png")); // Farthest layer
            layers[1] = ImageIO.read(new File("assets/Clouds/Clouds 5/3.png"));
            layers[2] = ImageIO.read(new File("assets/Clouds/Clouds 5/4.png"));
            layers[3] = ImageIO.read(new File("assets/Clouds/Clouds 5/5.png")); // Closest layer
            // LAST LAYER IS THE LEVEL BACKGROUND WITH 0 SCROLL SPEED
            loadLevelBackground(levelBgPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void loadLevelBackground(String path) {
        try {
            File bgFile = new File(path);
            if (!bgFile.exists()) {
                // If the file doesn't exist with the provided path, try without "../" prefix
                if (path.startsWith("../")) {
                    String altPath = path.substring(3); // Remove "../" prefix
                    bgFile = new File(altPath);
                    if (bgFile.exists()) {
                        layers[4] = ImageIO.read(bgFile);
                        return;
                    }
                } else {
                    // Try with "../" prefix
                    bgFile = new File("../" + path);
                    if (bgFile.exists()) {
                        layers[4] = ImageIO.read(bgFile);
                        return;
                    }
                }
                System.err.println("Could not find background image at: " + path);
            } else {
                layers[4] = ImageIO.read(bgFile);
            }
        } catch (IOException e) {
            System.err.println("Error loading level background: " + path);
            e.printStackTrace();
        }
    }
    
    public void setLevelBackground(String path) {
        this.levelBackgroundPath = path;
        loadLevelBackground(path);
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
