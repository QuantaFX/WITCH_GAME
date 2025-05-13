import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main extends JFrame {
    private GamePanel gamePanel;
    
    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        setTitle("2D Platformer Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);
        
        gamePanel = new GamePanel();
        add(gamePanel);
        
        // Add window listener to stop music when window is closed
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
