import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

/**
 * Main entry point for the Witch Game application.
 * Initializes and configures the game window.
 */
public class Main extends JFrame {
    private GamePanel gamePanel;
    
    /**
     * Entry point of the application.
     * Creates a new Main instance with a specified starting level from command line arguments.
     * 
     * @param args Command line arguments. The first argument, if provided, specifies the starting level (1-based).
     * @return void
     */
    public static void main(String[] args) {
        // Parse starting level from command line arguments
        int startLevel = 0; // Default to first level (index 0)
        
        if (args.length > 0) {
            try {
                // Convert from 1-based level number to 0-based index
                startLevel = Integer.parseInt(args[0]) - 1;
                if (startLevel < 0) {
                    System.out.println("Warning: Level number must be at least 1. Using level 1.");
                    startLevel = 0;
                }
            } catch (NumberFormatException e) {
                System.out.println("Warning: Invalid level number. Using level 1.");
                startLevel = 0;
            }
        }
        
        new Main(startLevel);
    }

    /**
     * Constructor for the Main class.
     * Sets up the JFrame window properties, initializes the game panel,
     * and configures window behaviors.
     * 
     * @param startLevel The initial level index (0-based) to start the game from
     * @return void
     */
    public Main(int startLevel) {
        setTitle("Witch Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        
        // Try to set an icon if it exists
        try {
            File iconFile = new File("assets/icon.png");
            if (iconFile.exists()) {
                Image icon = new ImageIcon("assets/icon.png").getImage();
                setIconImage(icon);
            }
        } catch (Exception e) {
            System.out.println("Could not set icon: " + e.getMessage());
        }
        
        gamePanel = new GamePanel(startLevel);
        add(gamePanel);
        
        // Add window listener to stop music when window closing
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gamePanel.stopMusic();
            }
        });
        
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
