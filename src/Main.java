import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main extends JFrame {
    private GamePanel gamePanel;
    
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

    public Main(int startLevel) {
        setTitle("2D Platformer Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        
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
