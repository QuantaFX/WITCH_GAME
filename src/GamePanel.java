import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Iterator;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    private Thread gameThread;
    private boolean running = false;
    private boolean showBounds = false;
    private Player player;
    private ArrayList<Enemy> enemies;
    private ArrayList<Platform> platforms;
    private ArrayList<Heart> hearts; // Added hearts collection
    private Background background;
    private boolean isWindows;
    private AudioPlayer backgroundMusic; // Added audio player for background music
    private boolean musicEnabled = true; // Flag to track if music is enabled
    private boolean gameOver = false; // Flag to track if game is over
    private Random random = new Random(); // For random heart drops

    // Track if player is currently in an attack state
    private boolean playerWasAttacking = false;
    private boolean playerWasBasicAttacking = false;

    // Heart drop chance (50%)
    private final double HEART_DROP_CHANCE = 1;

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

        // Initialize hearts ArrayList
        hearts = new ArrayList<>();

        platforms = new ArrayList<>();
        platforms.add(new Platform(-35, 472, 874, 106));
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
        
        // Detect when player starts a new attack (either regular or basic)
        boolean playerIsAttacking = player.isAttacking();
        boolean playerIsBasicAttacking = player.isBasicAttacking();
        if ((playerIsAttacking && !playerWasAttacking) || (playerIsBasicAttacking && !playerWasBasicAttacking)) {
            // Player just started attacking, reset hit tracking for all enemies
            for (Enemy enemy : enemies) {
                enemy.resetHitTracking();
            }
        }
        playerWasAttacking = playerIsAttacking;
        playerWasBasicAttacking = playerIsBasicAttacking;
        
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
            
            // If player is attacking or doing basic attack, check if attack hits enemy
            if ((player.isAttacking() || player.isBasicAttacking()) && player.getHurtbox().intersects(enemy.getBounds())) {
                // Only damage enemy if it hasn't been hit by this attack yet
                if (!enemy.wasHitByCurrentAttack()) {
                    enemy.takeDamage(player.getAttackDamage());
                    
                    // Remove dead enemies
                    if (enemy.isDead()) {
                        // Chance to drop a heart (50%)
                        if (random.nextDouble() < HEART_DROP_CHANCE) {
                            // Create a heart at the enemy's position
                            Heart heart = new Heart(enemy.getBounds().x + enemy.getBounds().width/2, 
                                                   enemy.getBounds().y + enemy.getBounds().height/2);
                            hearts.add(heart);
                        }
                        enemyIterator.remove();
                        continue;
                    }
                }
            }
        }
        
        // Update hearts and check for player collection
        Iterator<Heart> heartIterator = hearts.iterator();
        while (heartIterator.hasNext()) {
            Heart heart = heartIterator.next();
            heart.update();
            
            // Check if player collects the heart
            if (player.getBounds().intersects(heart.getBounds())) {
                player.heal(heart.getHealAmount());
                heartIterator.remove();
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

        // Draw all hearts
        for (Heart heart : hearts) {
            heart.draw(g);
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
        String restartMessage = "Press R to restart";
        metrics = g.getFontMetrics();
        x = (getWidth() - metrics.stringWidth(restartMessage)) / 2;
        y = getHeight() / 2 + 50;
        g.drawString(restartMessage, x, y);
    }
    
    private void resetGame() {
        initGame();
        gameOver = false;
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

        // If player is attacking (but not basic attacking), ignore movement inputs
        // But allow movement during hit state (after the hit animation ends) and basic attacks
        if (player.isAttacking()) {
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
        } else if (key == KeyEvent.VK_L) {
            player.startCharging(); // Start charging mana
        } else if (key == KeyEvent.VK_J) {
            // Only attack if player has enough mana
            if (player.hasEnoughManaForAttack()) {
                player.attack(); // Start attack and consume mana
            }
        } else if (key == KeyEvent.VK_K) {
            player.basicAttack(); // Start basic attack using idle sprite
            
            // Reset hit tracking for all enemies when K is pressed
            for (Enemy enemy : enemies) {
                enemy.resetHitTracking();
            }
        } else if (key == KeyEvent.VK_M) {
            // Toggle music on/off
            toggleMusic();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        
        // Don't process movement key releases during attack animations (but allow during basic attacks)
        if (player.isAttacking()) {
            return;
        }
        
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_D) {
            // Return to normal sprite if not basic attacking
            if (!player.isBasicAttacking()) {
                player.changeSprite(21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Idle sprite
            }
            player.stop();
        } else if (key == KeyEvent.VK_L) {
            player.stopCharging(); // Stop charging mana
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
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
}
