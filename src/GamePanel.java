import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Iterator;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    private Thread gameThread;
    private boolean running = false;
    private boolean showBounds = false;
    private Player player;
    private ArrayList<Enemy> enemies;
    private ArrayList<Platform> platforms;
    private Background background;
    private boolean isWindows;
    private AudioPlayer backgroundMusic; // Added audio player for background music
    private boolean musicEnabled = true; // Flag to track if music is enabled
    private boolean gameOver = false; // Flag to track if game is over

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        
        // Check the OS
        isWindows = System.getProperty("os.name").toLowerCase().contains("win");

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
        
        // Initialize background music
        // Note: Using WAV format as it's natively supported by Java Sound API
        // You can convert MP3 files to WAV using online converters or tools
        String musicFile = "assets/music/sorcer_chill_wave.wav";
        
        // Check if music file exists
        File file = new File(musicFile);
        if (file.exists()) {
            backgroundMusic = new AudioPlayer(musicFile);
            backgroundMusic.play(true); // Start playing in a loop
        } else {
            System.out.println("Background music file not found: " + musicFile);
            System.out.println("Please place a WAV audio file at: " + musicFile);
        }
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
        if (gameOver) {
            return; // Don't update if game is over
        }
        
        background.update(); // Update the background for parallax scrolling
        player.update();
        
        // Check if player is dead
        if (player.isDead()) {
            gameOver = true;
            return;
        }
        
        // Update all enemies and check for player attack collisions
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.update();
            
            // If player is attacking, check if attack hits enemy
            if (player.isAttacking() && player.getHurtbox().intersects(enemy.getBounds())) {
                enemy.takeDamage(player.getAttackDamage());
                
                // Remove dead enemies
                if (enemy.isDead()) {
                    enemyIterator.remove();
                    continue;
                }
            }
        }
        
        // Check platform collisions
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
        
        // Draw game over screen if needed
        if (gameOver) {
            drawGameOver(g);
        }
    }
    
    // Draw game over screen
    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200)); // Semi-transparent black
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        FontMetrics metrics = g.getFontMetrics();
        String message = "GAME OVER";
        int x = (getWidth() - metrics.stringWidth(message)) / 2;
        int y = getHeight() / 2;
        g.drawString(message, x, y);
        
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        metrics = g.getFontMetrics();
        message = "Press R to restart";
        x = (getWidth() - metrics.stringWidth(message)) / 2;
        y = getHeight() / 2 + 50;
        g.drawString(message, x, y);
    }
    
    // Reset the game
    private void resetGame() {
        gameOver = false;
        initGame();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Handle game over state
        if (gameOver) {
            if (key == KeyEvent.VK_R) {
                resetGame();
            }
            return;
        }
        
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
        } else if (key == KeyEvent.VK_M) {
            // Toggle music on/off
            toggleMusic();
        }
    }
    
    // Add method to toggle music
    private void toggleMusic() {
        if (backgroundMusic != null) {
            if (musicEnabled) {
                backgroundMusic.stop();
                musicEnabled = false;
            } else {
                backgroundMusic.play(true);
                musicEnabled = true;
            }
        }
    }
    
    // Add method to stop music (useful when game closing)
    public void stopMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.stop();
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
}
