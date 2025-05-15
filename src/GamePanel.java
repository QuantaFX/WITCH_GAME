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
    
    // Level management
    private LevelManager levelManager;
    private boolean levelCompleted = false;
    private int levelCompletedTimer = 0;
    private final int LEVEL_TRANSITION_TIME = 120; // 2 seconds at 60 FPS

    // Track if player is currently in an attack state
    private boolean playerWasAttacking = false;
    private boolean playerWasBasicAttacking = false;

    // Heart drop chance (50%)
    private final double HEART_DROP_CHANCE = 1;
    
    // Starting level index (0-based)
    private int startLevel = 0;

    public GamePanel() {
        this(0); // Default to first level (index 0)
    }
    
    public GamePanel(int startLevel) {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        
        // Store the starting level
        this.startLevel = startLevel;

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
        // Initialize player with default position (will be overridden by level data)
        player = new Player(100, 300, 21, 39, "assets/Blue_witch/B_witch_idle.png", 6); // Idle sprite

        // Initialize collections
        enemies = new ArrayList<>();
        platforms = new ArrayList<>();
        hearts = new ArrayList<>();
        
        // Initialize level manager and load specified level
        levelManager = new LevelManager(player);
        
        // Set the current level to the specified starting level
        if (startLevel > 0) {
            boolean success = levelManager.loadLevel(startLevel);
            if (!success) {
                System.out.println("Failed to load level " + (startLevel + 1) + ". Using level 1 instead.");
            } else {
                System.out.println("Starting at level " + (startLevel + 1));
            }
        }
        
        loadCurrentLevel();



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
    
    private void loadCurrentLevel() {
        Level level = levelManager.getCurrentLevel();
        if (level == null) {
            System.out.println("Error: No level loaded");
            return;
        }
        
        // Clear existing game objects
        platforms.clear();
        enemies.clear();
        hearts.clear();
        
        // Load platforms
        platforms.addAll(level.getPlatforms());
        
        // Load enemies
        for (Enemy enemy : level.getEnemies()) {
            enemy.setTarget(player);
            enemies.add(enemy);
        }
        
        // Set player position
        Point startPos = level.getPlayerStartPosition();
        System.out.println("GamePanel - Setting player position to: " + startPos.x + ", " + startPos.y);
        player.setPosition(startPos.x, startPos.y);
        System.out.println("GamePanel - Player position after setting: " + player.getBounds().x + ", " + player.getBounds().y);
        
        // Load background
        String bgPath = level.getBackgroundPath();
        if (bgPath != null && !bgPath.isEmpty()) {
            if (background == null) {
                background = new Background(bgPath);
            } else {
                background.setLevelBackground(bgPath);
            }
        }
        
        // Ensure door is initially inactive if there are enemies
        if (level.hasDoor()) {
            Door door = level.getDoor();
            door.setActive(enemies.isEmpty());
        }
        
        levelCompleted = false;
        levelCompletedTimer = 0;
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
        
        // Handle level transition
        if (levelCompleted) {
            levelCompletedTimer++;
            if (levelCompletedTimer >= LEVEL_TRANSITION_TIME) {
                if (levelManager.moveToNextLevel()) {
                    loadCurrentLevel();
                } else {
                    // No more levels, game completed
                    gameOver = true;
                }
            }
            return;
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
                        
                        // Check if all enemies are killed and activate the door if so
                        if (enemies.isEmpty()) {
                            Level currentLevel = levelManager.getCurrentLevel();
                            if (currentLevel != null && currentLevel.hasDoor()) {
                                Door door = currentLevel.getDoor();
                                if (!door.isActive()) {
                                    door.setActive(true);
                                    playDoorActivationSound();
                                }
                            }
                        }
                        
                        continue;
                    }
                }
            }
        }
        
        // Check if all enemies are killed and activate the door if so
        Level currentLevel = levelManager.getCurrentLevel();
        if (currentLevel != null && currentLevel.hasDoor()) {
            Door door = currentLevel.getDoor();
            // Only activate the door if there are no enemies left
            if (!door.isActive() && enemies.isEmpty()) {
                door.setActive(true);
                playDoorActivationSound();
            }
            // Update door animation
            door.update();
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
        
        // Check door collision for level transition
        if (currentLevel != null && currentLevel.hasDoor()) {
            Door door = currentLevel.getDoor();
            if (door.isActive() && player.getBounds().intersects(door.getBounds())) {
                levelCompleted = true;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        background.draw(g);
        
        // Draw all platforms
        for (Platform platform : platforms) {
            platform.draw(g);
        }
        
        // Draw door if exists in current level
        Level currentLevel = levelManager.getCurrentLevel();
        if (currentLevel != null && currentLevel.hasDoor()) {
            currentLevel.getDoor().draw(g);
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
        
        // Draw level transition screen
        if (levelCompleted) {
            drawLevelTransition(g);
        }
        
        // Draw game over screen if needed
        if (gameOver) {
            drawGameOver(g);
        }
        
        // Draw level info
        drawLevelInfo(g);
    }
    
    private void drawLevelInfo(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Level: " + (levelManager.getCurrentLevelIndex() + 1), 10, 20);
        
        // Show message about killing enemies if door exists but is inactive
        Level currentLevel = levelManager.getCurrentLevel();
        if (currentLevel != null && currentLevel.hasDoor() && !currentLevel.getDoor().isActive() && !enemies.isEmpty()) {
            g.setColor(new Color(255, 200, 0)); // Gold/yellow color
            g.setFont(new Font("Arial", Font.BOLD, 14));
            String message = "Kill all enemies to activate the door!";
            FontMetrics metrics = g.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(message)) / 2;
            g.drawString(message, x, 40);
        }
    }
    
    private void drawLevelTransition(Graphics g) {
        float alpha = Math.min(1.0f, (float)levelCompletedTimer / LEVEL_TRANSITION_TIME);
        g.setColor(new Color(0, 0, 0, alpha));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if (alpha > 0.5f) {
            g.setColor(new Color(1.0f, 1.0f, 1.0f, (alpha - 0.5f) * 2));
            g.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics metrics = g.getFontMetrics();
            String message = "Level " + (levelManager.getCurrentLevelIndex() + 2);
            int x = (getWidth() - metrics.stringWidth(message)) / 2;
            int y = getHeight() / 2;
            g.drawString(message, x, y);
        }
    }
    
    // Draw game over screen
    private void drawGameOver(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200)); // Semi-transparent black
        g.fillRect(0, 0, getWidth(), getHeight());
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 50));
        FontMetrics metrics = g.getFontMetrics();
        
        String message;
        if (levelManager.getCurrentLevelIndex() >= levelManager.getLevels().size() - 1 && enemies.isEmpty()) {
            message = "YOU WIN!";
        } else {
            // Check if player fell off the screen
            if (player.getBounds().y > 600) {
                message = "YOU FELL!";
            } else {
                message = "GAME OVER";
            }
        }
        
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
        boolean isShiftDown = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
        
        // Handle game over state
        if (gameOver) {
            if (key == KeyEvent.VK_R) {
                resetGame();
            }
            return;
        }
        
        // Don't process inputs during level transition
        if (levelCompleted) {
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
            if (isShiftDown) {
                player.highJump(); // High jump with Shift+Space or Shift+W
            } else {
                player.jump(); // Normal jump
            }
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

    private void playDoorActivationSound() {
        try {
            AudioPlayer doorSound = new AudioPlayer("assets/sounds/door_unlock.wav");
            doorSound.play(false);
        } catch (Exception e) {
            System.out.println("Could not play door activation sound: " + e.getMessage());
        }
    }
}
