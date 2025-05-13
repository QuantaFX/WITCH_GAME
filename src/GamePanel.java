import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    private Thread gameThread;
    private boolean running = false;
    private boolean showBounds = false;
    private Player player;
    private ArrayList<Enemy> enemies;
    private ArrayList<Platform> platforms;
    private Background background;
    private MP3Player backgroundMusic;

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);

        initGame();
        startGame();

        addKeyListener(this);
        setFocusable(true);
        
        // Add shutdown hook to stop music when game closes
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (backgroundMusic != null) {
                backgroundMusic.stop();
            }
        }));
    }

    private void initGame() {
        player = new Player(100, 300, 21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Idle sprite
        
        // Initialize enemies ArrayList
        enemies = new ArrayList<>();
        
        // Add multiple enemies at different positions
        Enemy enemy1 = new Enemy(600, 300, 36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
        enemy1.setTarget(player);
        enemies.add(enemy1);
        
        Enemy enemy2 = new Enemy(400, 300, 36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
        enemy2.setTarget(player);
        enemies.add(enemy2);
        
        Enemy enemy3 = new Enemy(300, 200, 36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
        enemy3.setTarget(player);
        enemies.add(enemy3);
        
        platforms = new ArrayList<>();
        platforms.add(new Platform(50, 550, 700, 20));
        platforms.add(new Platform(200, 450, 100, 20));
        platforms.add(new Platform(400, 350, 150, 20));
        background = new Background();
        
        // Initialize and play background music
        backgroundMusic = new MP3Player("assets/music/sorcera_normal.mp3");
        backgroundMusic.play(true); // Loop the music
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
        
        // Update all enemies
        for (Enemy enemy : enemies) {
            enemy.update();
        }
        
        for (Platform platform : platforms) {
            player.checkCollision(platform);
            
            // Check collision for each enemy
            for (Enemy enemy : enemies) {
                enemy.checkCollision(platform);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        background.draw(g);
        for (Platform platform : platforms) {
            platform.draw(g);
        }
        
        // Draw all enemies
        for (Enemy enemy : enemies) {
            enemy.draw(g, showBounds);
        }
        
        player.draw(g, showBounds);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        // If player is attacking, ignore movement inputs
        if (player.isAttacking()) {
            if (key == KeyEvent.VK_J) {
                // Allow re-triggering attack
                player.attack();
            }
            return;
        }
        
        if (key == KeyEvent.VK_A) {
            player.changeSprite(21, 41, "assets/Blue_witch/B_witch_run.png", 8); // Running sprite
            player.moveLeft();
        } else if (key == KeyEvent.VK_D) {
            player.changeSprite(21, 41, "assets/Blue_witch/B_witch_run.png", 8);
            player.moveRight();
        } else if (key == KeyEvent.VK_SPACE || key == KeyEvent.VK_W) {
            player.jump();
        } else if (key == KeyEvent.VK_F3) {
            showBounds = !showBounds;
        } else if (key == KeyEvent.VK_K) {
            player.startCharging(); // Start charging
        } else if (key == KeyEvent.VK_J) {
            player.attack(); // Start attack
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_D) {
            // Return to normal sprite
            player.changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Idle sprite
            player.stop();
        } else if (key == KeyEvent.VK_K) {
            player.stopCharging(); // Stop charging
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    // Add method to stop music
    public void stopMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
    }
}
