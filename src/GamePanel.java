import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    private Thread gameThread;
    private boolean running = false;
    private boolean showBounds = false;
    private Player player;
    private ArrayList<Platform> platforms;
    private Background background;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);

        initGame();
        startGame();

        addKeyListener(this);
        setFocusable(true);
    }

    private void initGame() {
        player = new Player(100, 300, 21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Idle sprite
        platforms = new ArrayList<>();
        platforms.add(new Platform(50, 550, 700, 20));
        platforms.add(new Platform(200, 450, 100, 20));
        platforms.add(new Platform(400, 350, 150, 20));
        background = new Background();
    }

    private void startGame() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (running) {
            updateGame();
            repaint();
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGame() {
        background.update(); // Update the background for parallax scrolling
        player.update();
        for (Platform platform : platforms) {
            player.checkCollision(platform);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        background.draw(g);
        for (Platform platform : platforms) {
            platform.draw(g);
        }
        player.draw(g, showBounds);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT) {
            player.changeSprite(21, 41, "assets/Blue_witch/B_witch_run.png", 8); // Running sprite
            player.moveLeft();
        } else if (key == KeyEvent.VK_RIGHT) {
            player.changeSprite(21, 41, "assets/Blue_witch/B_witch_run.png", 8);
            player.moveRight();
        } else if (key == KeyEvent.VK_SPACE) {
            player.jump();
        } else if (key == KeyEvent.VK_F3) {
            showBounds = !showBounds;
        } else if (key == KeyEvent.VK_X) {
            player.startCharging(); // Start charging
        } else if (key == KeyEvent.VK_Z) {
            player.attack(); // Start attack
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
            // Return to normal sprite
            player.changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Idle sprite
            player.stop();
        } else if (key == KeyEvent.VK_X) {
            player.stopCharging(); // Stop charging
        } else if (key == KeyEvent.VK_Z) {
            player.stopAttack(); // Stop attack
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}
