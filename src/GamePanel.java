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
import javax.swing.ImageIcon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.Desktop;
import java.io.IOException;

public class GamePanel extends JPanel implements Runnable, KeyListener {
    // Game state enum to handle different states
    public enum GameState {
        MENU,       // Main menu
        PLAYING,    // Actively playing the game
        CONTROLS,   // Viewing controls screen
        PAUSED,     // Game paused
        GAME_OVER,  // Game over state
        COMPLETED,  // Game completed state
        CUTSCENE    // Cutscene state
    }
    
    private GameState currentState = GameState.MENU;
    private int menuSelection = 0; // 0: Start, 1: Controls, 2: Exit
    
    private int pauseMenuSelection = 0; // 0: Resume, 1: Help/Controls, 2: Exit
    
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
    
    // Game completion flag
    private boolean gameCompleted = false;
    
    // Level management
    private LevelManager levelManager;
    private boolean levelCompleted = false;
    private int levelCompletedTimer = 0;
    private final int LEVEL_TRANSITION_TIME = 120; // 2 seconds at 60 FPS

    // Track if player is currently in an attack state
    private boolean playerWasAttacking = false;
    private boolean playerWasBasicAttacking = false;

    // Heart drop chance (50%)
    private final double HEART_DROP_CHANCE = 0.5;
    
    // Starting level index (0-based)
    private int startLevel = 0;

    private boolean isPlayingBossMusic = false; // Track whether we're playing boss music

    private Image menuBackground;
    private Image sorcera;
    private Image[] playButtons;
    private Image[] helpButtons;
    private Image[] exitButtons;
    private Image menuBar; // Menu bar background for buttons

    // Button images for controls screen
    private Image[] controlButtons;

    // Button sprites for controls screen
    private BufferedImage[][] controlButtonFrames; // Store all frames for each button
    private int[] buttonCurrentFrame;              // Current frame for each button
    private int buttonAnimCounter = 0;             // Animation counter for buttons
    private final int BUTTON_ANIM_SPEED = 10;      // Speed of button animation

    // For rolling credits
    private int creditsScrollPosition;
    private boolean creditsInitialized = false;
    private final int CREDITS_SCROLL_SPEED = 1;

    // Cutscene-related variables
    private String currentCutscene;
    private boolean cutsceneFinished = false;
    private JPanel videoPanel; // Panel to display video content
    private Timer cutsceneTimer;
    private File[] cutsceneFrameFiles; // Store file references instead of loaded images
    private int currentCutsceneFrame = 0;
    private int totalCutsceneFrames = 0;
    private final int CUTSCENE_FRAME_DELAY = 200; // ms between frames
    private long cutsceneStartTime;
    private Image currentFrameImage; // Only store the current frame in memory
    private AudioPlayer cutsceneAudio; // Audio for cutscene
    private boolean cutsceneAudioStarted = false; // Track if audio has started

    public GamePanel() {
        this(0); // Default to first level (index 0)
    }
    
    public GamePanel(int startLevel) {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        
        // Store the starting level
        this.startLevel = startLevel;
        
        // Set initial game state
        this.currentState = GameState.MENU;

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
        
        // Load the current level (which will also initialize the music)
        loadCurrentLevel();

        // Load menu images
        try {
            menuBackground = new ImageIcon("assets/Menu/Menu_BG.png").getImage();
            
            // Load and scale Sorcera logo to half size
            Image originalSorcera = new ImageIcon("assets/Menu/Sorcera.png").getImage();
            int newWidth = originalSorcera.getWidth(null) / 2;
            int newHeight = originalSorcera.getHeight(null) / 2;
            sorcera = originalSorcera.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            
            // Load menu bar image
            menuBar = new ImageIcon("assets/Menu/MenuBar.png").getImage();
            
            // Load button images
            playButtons = new Image[3];
            helpButtons = new Image[3];
            exitButtons = new Image[3];
            
            for (int i = 1; i <= 3; i++) {
                // Load original button images
                Image originalPlayBtn = new ImageIcon("assets/Menu/Play_btn/Play_btn" + i + ".png").getImage();
                Image originalHelpBtn = new ImageIcon("assets/Menu/Help_btn/Help_btn" + i + ".png").getImage();
                Image originalExitBtn = new ImageIcon("assets/Menu/Exit_btn/Exit_btn" + i + ".png").getImage();
                
                // Scale them to half size
                playButtons[i-1] = originalPlayBtn.getScaledInstance(
                    originalPlayBtn.getWidth(null) / 2,
                    originalPlayBtn.getHeight(null) / 2,
                    Image.SCALE_SMOOTH);
                
                helpButtons[i-1] = originalHelpBtn.getScaledInstance(
                    originalHelpBtn.getWidth(null) / 2, 
                    originalHelpBtn.getHeight(null) / 2,
                    Image.SCALE_SMOOTH);
                
                exitButtons[i-1] = originalExitBtn.getScaledInstance(
                    originalExitBtn.getWidth(null) / 2,
                    originalExitBtn.getHeight(null) / 2,
                    Image.SCALE_SMOOTH);
            }
            
            // Load control button sprites (for controls screen)
            String[] controlKeys = {"K", "J", "L", "A", "D", "W"};
            controlButtons = new Image[controlKeys.length];
            
            // We'll load control buttons on demand in the drawControlsScreen method
            // via the loadControlButtons method to properly handle the sprite states
        } catch (Exception e) {
            System.out.println("Error loading menu images: " + e.getMessage());
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
        
        // Only change music when entering or leaving a boss level
        boolean isBossLevel = level.hasBoss();
        
        // Check if we need to change the music
        if (isBossLevel != isPlayingBossMusic) {
            // We need to change music
            if (backgroundMusic != null) {
                backgroundMusic.stop();
            }
            
            String musicFile = isBossLevel ? "assets/music/sorcera_boss.wav" : "assets/music/sorcer_chill_wave.wav";
            
            // Check if music file exists
            File file = new File(musicFile);
            if (file.exists()) {
                backgroundMusic = new AudioPlayer(musicFile);
                if (musicEnabled) {
                    backgroundMusic.play(true); // Start playing in a loop
                }
                isPlayingBossMusic = isBossLevel; // Update our tracking variable
            } else {
                System.out.println("Music file not found: " + musicFile);
            }
        } else if (backgroundMusic == null) {
            // Initialize music for the first time
            String musicFile = isBossLevel ? "assets/music/sorcera_boss.wav" : "assets/music/sorcer_chill_wave.wav";
            File file = new File(musicFile);
            if (file.exists()) {
                backgroundMusic = new AudioPlayer(musicFile);
                if (musicEnabled) {
                    backgroundMusic.play(true);
                }
                isPlayingBossMusic = isBossLevel;
            }
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
        // Handle game states
        switch (currentState) {
            case MENU:
                // No updates needed for menu
                break;
                
            case PLAYING:
                updateGamePlay();
                break;
                
            case CONTROLS:
                // No updates needed for controls screen
                break;
                
            case GAME_OVER:
                // No updates needed for game over
                break;
                
            case COMPLETED:
                // No updates needed for completed
                break;
                
            case PAUSED:
                // No updates needed when paused
                break;
                
            case CUTSCENE:
                // No updates needed for cutscene
                break;
        }
    }
    
    private void updateGamePlay() {
        if (gameOver) {
            currentState = GameState.GAME_OVER;
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
                    currentState = GameState.COMPLETED;
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
            currentState = GameState.GAME_OVER;
            return;
        }
        
        // Temporary list to hold new enemies to avoid ConcurrentModificationException
        ArrayList<Enemy> newEnemies = new ArrayList<>();
        
        // Update all enemies and check for player attack collisions
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy enemy = enemyIterator.next();
            enemy.update();
            
            // Check if boss should spawn minions
            if (enemy.isBoss() && enemy.canSpawnMinion()) {
                Enemy minion = enemy.spawnMinion(platforms);
                if (minion != null) {
                    minion.setTarget(player);
                    // Instead of adding directly to the enemies list, add to newEnemies
                    newEnemies.add(minion);
                    enemy.resetSpawnTimer();
                }
            }
            
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
                        if (enemies.isEmpty() && newEnemies.isEmpty()) {
                            Level currentLevel = levelManager.getCurrentLevel();
                            if (currentLevel != null) {
                                // If we have a door, activate it
                                if (currentLevel.hasDoor()) {
                                    Door door = currentLevel.getDoor();
                                    if (!door.isActive()) {
                                        door.setActive(true);
                                        playDoorActivationSound();
                                    }
                                }
                                
                                // If this was the final level and had a boss, mark game as completed
                                if (currentLevel.hasBoss() && currentLevel.getLevelNumber() == levelManager.getLevels().size()) {
                                    gameCompleted = true;
                                    currentState = GameState.COMPLETED;
                                }
                            }
                        }
                        
                        continue;
                    }
                }
            }
        }
        
        // Now add all the new enemies after iteration is complete
        if (!newEnemies.isEmpty()) {
            enemies.addAll(newEnemies);
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
        
        switch (currentState) {
            case MENU:
                drawMenu(g);
                break;
                
            case PLAYING:
                drawGamePlay(g);
                
                // Draw level transition screen
                if (levelCompleted) {
                    drawLevelTransition(g);
                }
                break;
                
            case CONTROLS:
                drawControlsScreen(g);
                break;
                
            case GAME_OVER:
                drawGamePlay(g);
                drawGameOver(g);
                break;
                
            case COMPLETED:
                drawGamePlay(g);
                drawGameCompleted(g);
                break;
                
            case PAUSED:
                drawGamePlay(g);
                drawPauseScreen(g);
                break;
                
            case CUTSCENE:
                drawCutscene(g);
                break;
        }
    }
    
    private void drawGamePlay(Graphics g) {
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
        
        // Draw level info
        drawLevelInfo(g);
    }
    
    private void drawMenu(Graphics g) {
        // Draw the menu background
        if (menuBackground != null) {
            g.drawImage(menuBackground, 0, 0, getWidth(), getHeight(), null);
        } else {
            // Fallback to black background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Draw logo image at the top
        if (sorcera != null) {
            int logoX = (getWidth() - sorcera.getWidth(null)) / 2;
            g.drawImage(sorcera, logoX, 80, null);
        }
        
        // Draw menu button images
        int startY = 250;  // Moved up from 300 to better position smaller buttons
        int spacing = 100;  // Increased from 50 to add more vertical padding between buttons
        
        // Draw menu bar behind buttons
        if (menuBar != null) {
            // Position menu bar to cover all buttons with some padding
            int menuBarX = (getWidth() - menuBar.getWidth(null)) / 2;
            int menuBarY = startY - 80; // Moved down by another 20px
            
            // Use original image dimensions
            g.drawImage(menuBar, menuBarX, menuBarY, null);
        }
        
        for (int i = 0; i < 3; i++) {
            Image buttonImage = null;
            
            switch (i) {
                case 0: // Start
                    buttonImage = playButtons != null ? playButtons[menuSelection == i ? 1 : 0] : null;
                    break;
                case 1: // Controls/Help
                    buttonImage = helpButtons != null ? helpButtons[menuSelection == i ? 1 : 0] : null;
                    break;
                case 2: // Exit
                    buttonImage = exitButtons != null ? exitButtons[menuSelection == i ? 1 : 0] : null;
                    break;
            }
            
            if (buttonImage != null) {
                int buttonX = (getWidth() - buttonImage.getWidth(null)) / 2;
                int buttonY = startY + i * spacing;
                g.drawImage(buttonImage, buttonX, buttonY, null);
            }
        }
    }
    
    private void drawControlsScreen(Graphics g) {
        // Draw menu background instead of black background
        if (menuBackground != null) {
            g.drawImage(menuBackground, 0, 0, getWidth(), getHeight(), null);
        } else {
            // Fallback to black background
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Draw title
        g.setColor(new Color(255, 215, 0));
        g.setFont(new Font("Arial", Font.BOLD, 40));
        String title = "CONTROLS";
        FontMetrics metrics = g.getFontMetrics();
        int titleX = (getWidth() - metrics.stringWidth(title)) / 2;
        g.drawString(title, titleX, 100);
        
        // Draw control instructions with button sprites
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 20));
        
        // Control keys and descriptions
        String[][] controls = {
            {"K", "Basic Attack"},
            {"J", "Super Attack"},
            {"L", "Charge"},
            {"A", "Move Left"},
            {"D", "Move Right"},
            {"W", "Jump"},
            {"Space", "Jump (alternative)"},
        };
        
        // Load button sprites if not loaded yet
        if (controlButtonFrames == null) {
            loadControlButtonSprites(controls);
        }
        
        // Update animation counter
        buttonAnimCounter++;
        if (buttonAnimCounter >= BUTTON_ANIM_SPEED) {
            buttonAnimCounter = 0;
            // Update frame for each button
            for (int i = 0; i < buttonCurrentFrame.length; i++) {
                buttonCurrentFrame[i] = (buttonCurrentFrame[i] + 1) % 4; // Cycle through 4 frames
            }
        }
        
        int startY = 160;      // Move up slightly
        int lineHeight = 50;   // Reduce spacing between items
        int buttonX = 250;     // Keep same X position
        int textX = 350;       // Move text closer to button
        float buttonScale = 1.5f; // Scale factor for buttons
        
        for (int i = 0; i < controls.length; i++) {
            // Draw key button sprite (animated)
            if (controlButtonFrames != null && controlButtonFrames[i] != null) {
                // Get current frame for this button
                BufferedImage currentFrame = controlButtonFrames[i][buttonCurrentFrame[i]];
                if (currentFrame != null) {
                    // Calculate scaled dimensions
                    int origWidth = currentFrame.getWidth();
                    int origHeight = currentFrame.getHeight();
                    int scaledWidth = (int)(origWidth * buttonScale);
                    int scaledHeight = (int)(origHeight * buttonScale);
                    
                    // Calculate position with scaling taken into account
                    int y = startY + i * lineHeight - scaledHeight/2; // Center vertically
                    
                    // Draw scaled button
                    g.drawImage(currentFrame, 
                                buttonX - scaledWidth/2, 
                                y, 
                                scaledWidth, 
                                scaledHeight, 
                                null);
                }
            }
            
            // Draw description
            g.setColor(Color.WHITE);
            int y = startY + i * lineHeight + 10; // Adjusted text position to align with button
            g.drawString("- " + controls[i][1], textX, y);
        }
        
        // Draw back instruction
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        String backInstruction = "Press ESC to return to menu";
        metrics = g.getFontMetrics();
        int backX = (getWidth() - metrics.stringWidth(backInstruction)) / 2;
        g.drawString(backInstruction, backX, getHeight() - 50);
    }
    
    private void loadControlButtonSprites(String[][] controls) {
        try {
            int numControls = controls.length;
            controlButtonFrames = new BufferedImage[numControls][4]; // 4 frames per button
            buttonCurrentFrame = new int[numControls]; // Current frame for each button
            
            for (int i = 0; i < numControls; i++) {
                String key = controls[i][0];
                buttonCurrentFrame[i] = 0; // Initialize to first frame
                
                try {
                    // Load the button sprite sheet
                    File buttonFile = new File("assets/Buttons/" + key + ".png");
                    if (buttonFile.exists()) {
                        BufferedImage fullSprite = ImageIO.read(buttonFile);
                        
                        // Each button sprite has 4 states vertically stacked
                        int totalHeight = fullSprite.getHeight();
                        int frameHeight = totalHeight / 4; // Divide by 4 states
                        int frameWidth = fullSprite.getWidth();
                        
                        // Extract all 4 frames from the sprite sheet (vertically)
                        for (int frame = 0; frame < 4; frame++) {
                            controlButtonFrames[i][frame] = fullSprite.getSubimage(
                                0, frame * frameHeight, frameWidth, frameHeight);
                        }
                    } else {
                        System.out.println("Button sprite not found: " + buttonFile.getAbsolutePath());
                    }
                } catch (IOException e) {
                    System.out.println("Error loading button sprite for " + key + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error loading control button sprites: " + e.getMessage());
        }
    }
    
    private void drawPauseScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 40));
        String message = "PAUSED";
        FontMetrics metrics = g.getFontMetrics();
        int x = (getWidth() - metrics.stringWidth(message)) / 2;
        int y = 150;
        g.drawString(message, x, y);
        
        // Draw menu buttons (Resume, Help, Exit)
        int startY = 230;
        int spacing = 100;
        
        // Draw menu bar behind buttons
        if (menuBar != null) {
            // Position menu bar to cover all buttons with some padding
            int menuBarX = (getWidth() - menuBar.getWidth(null)) / 2;
            int menuBarY = startY - 80; // Moved down by another 20px
            
            // Use original image dimensions
            g.drawImage(menuBar, menuBarX, menuBarY, null);
        }
        
        for (int i = 0; i < 3; i++) {
            Image buttonImage = null;
            
            switch (i) {
                case 0: // Resume
                    buttonImage = playButtons != null ? playButtons[pauseMenuSelection == i ? 1 : 0] : null;
                    break;
                case 1: // Controls
                    buttonImage = helpButtons != null ? helpButtons[pauseMenuSelection == i ? 1 : 0] : null;
                    break;
                case 2: // Exit
                    buttonImage = exitButtons != null ? exitButtons[pauseMenuSelection == i ? 1 : 0] : null;
                    break;
            }
            
            if (buttonImage != null) {
                int buttonX = (getWidth() - buttonImage.getWidth(null)) / 2;
                int buttonY = startY + i * spacing;
                g.drawImage(buttonImage, buttonX, buttonY, null);
            }
        }
    }
    
    private void drawLevelInfo(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        
        // Draw level number on the right side of the screen
        String levelText = "Level: " + (levelManager.getCurrentLevelIndex() + 1);
        FontMetrics metrics = g.getFontMetrics();
        int levelTextWidth = metrics.stringWidth(levelText);
        g.drawString(levelText, getWidth() - levelTextWidth - 20, 30);
        
        // Show message about killing enemies if door exists but is inactive
        Level currentLevel = levelManager.getCurrentLevel();
        if (currentLevel != null && currentLevel.hasDoor() && !currentLevel.getDoor().isActive() && !enemies.isEmpty()) {
            g.setColor(new Color(255, 200, 0)); // Gold/yellow color
            g.setFont(new Font("Arial", Font.BOLD, 14));
            String message = "Kill all enemies to activate the door!";
            metrics = g.getFontMetrics();
            int x = (getWidth() - metrics.stringWidth(message)) / 2;
            g.drawString(message, x, 50);
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
    
    // Draw game completed screen
    private void drawGameCompleted(Graphics g) {
        g.setColor(new Color(0, 0, 0, 200)); // Semi-transparent black
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Initialize credits position if needed
        if (!creditsInitialized) {
            creditsScrollPosition = getHeight();
            creditsInitialized = true;
        }
        
        // Update credits position (roll up)
        creditsScrollPosition -= CREDITS_SCROLL_SPEED;
        
        // Reset credits when they roll past the top
        if (creditsScrollPosition < -800) { // Approximate height of all credits content
            creditsScrollPosition = getHeight();
        }
        
        // Draw rolling credits
        drawRollingCredits(g, creditsScrollPosition);
    }
    
    private void drawRollingCredits(Graphics g, int startY) {
        // Set up variables for positioning
        int centerX = getWidth() / 2;
        int currentY = startY;
        int lineSpacing = 40;
        
        // Use anti-aliasing for smoother text
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw Sorcera logo at the top of credits
        if (sorcera != null) {
            int logoX = centerX - sorcera.getWidth(null) / 2;
            g.drawImage(sorcera, logoX, currentY, null);
            currentY += sorcera.getHeight(null) + lineSpacing;
        } else {
            currentY += 100; // Space if logo not available
        }
        
        // Draw "Credits" title
        g.setColor(new Color(255, 215, 0)); // Gold color
        g.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics titleMetrics = g.getFontMetrics();
        g.drawString("Credits", centerX - titleMetrics.stringWidth("Credits") / 2, currentY);
        currentY += lineSpacing * 2;
        
        // Draw team members
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        FontMetrics nameMetrics = g.getFontMetrics();
        
        String[] names = {
            "Archie Junio",
            "Von Uyvico",
            "Nathan Palanas"
        };
        
        String[] roles = {
            "Game Developer / Graphics Artist",
            "Game Developer / Music & Video",
            "Game Developer / Level Editor"
        };
        
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < names.length; i++) {
            g.setColor(Color.WHITE);
            g.drawString(names[i], centerX - nameMetrics.stringWidth(names[i]) / 2, currentY);
            currentY += lineSpacing;
            
            g.setColor(Color.LIGHT_GRAY);
            g.setFont(new Font("Arial", Font.ITALIC, 18));
            FontMetrics roleMetrics = g.getFontMetrics();
            g.drawString(roles[i], centerX - roleMetrics.stringWidth(roles[i]) / 2, currentY);
            
            currentY += lineSpacing * 2;
            g.setFont(new Font("Arial", Font.BOLD, 24));
        }
        
        // Draw footer text
        currentY += lineSpacing;
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        FontMetrics footerMetrics = g.getFontMetrics();
        
        String[] footerText = {
            "All rights reserved.",
            "To god be the glory.",
            "Audited Version 3.4.0"
        };
        
        for (String line : footerText) {
            g.drawString(line, centerX - footerMetrics.stringWidth(line) / 2, currentY);
            currentY += lineSpacing;
        }
    }
    
    private void resetGame() {
        // Stop the current background music before initializing a new game
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic = null; // Set to null so loadCurrentLevel will start music again
        }
        
        // Stop cutscene audio if still playing
        if (cutsceneAudio != null) {
            cutsceneAudio.stop();
            cutsceneAudio = null;
        }
        
        // Clean up cutscene resources
        if (cutsceneTimer != null && cutsceneTimer.isRunning()) {
            cutsceneTimer.stop();
            cutsceneTimer = null;
        }
        cutsceneFrameFiles = null;
        currentFrameImage = null;
        currentCutsceneFrame = 0;
        totalCutsceneFrames = 0;
        cutsceneAudioStarted = false;
        
        // Suggest garbage collection to free memory
        System.gc();
        
        initGame();
        gameOver = false;
        gameCompleted = false;
        levelCompleted = false;
        levelCompletedTimer = 0;
        menuSelection = 0;
        pauseMenuSelection = 0;
        creditsInitialized = false;
        cutsceneFinished = false;
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        boolean isShiftDown = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
        
        switch (currentState) {
            case MENU:
                handleMenuKeyPress(key);
                break;
                
            case PLAYING:
                if (key == KeyEvent.VK_ESCAPE) {
                    currentState = GameState.PAUSED;
                    return;
                }
                
                handleGameplayKeyPress(key, isShiftDown);
                break;
                
            case CONTROLS:
                if (key == KeyEvent.VK_ESCAPE) {
                    currentState = GameState.MENU;
                }
                break;
                
            case PAUSED:
                if (key == KeyEvent.VK_ESCAPE) {
                    currentState = GameState.PLAYING;
                } else {
                    handlePauseMenuKeyPress(key);
                }
                break;
                
            case GAME_OVER:
                if (key == KeyEvent.VK_R) {
                    resetGame();
                    currentState = GameState.PLAYING;
                } else if (key == KeyEvent.VK_ESCAPE) {
                    resetGame();
                    currentState = GameState.MENU;
                }
                break;
                
            case COMPLETED:
                if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER) {
                    resetGame();
                    currentState = GameState.MENU;
                }
                break;
                
            case CUTSCENE:
                // Handle cutscene key presses
                handleCutsceneKeyPress(key);
                break;
        }
    }
    
    private void handleMenuKeyPress(int key) {
        if (key == KeyEvent.VK_UP) {
            menuSelection = (menuSelection - 1 + 3) % 3; // Wrap around to bottom
        } else if (key == KeyEvent.VK_DOWN) {
            menuSelection = (menuSelection + 1) % 3; // Wrap around to top
        } else if (key == KeyEvent.VK_ENTER) {
            switch (menuSelection) {
                case 0: // Start Game
                    // Play intro cutscene before starting the game
                    playCutscene("assets/Cutscenes/Start.mp4");
                    break;
                case 1: // Controls
                    currentState = GameState.CONTROLS;
                    break;
                case 2: // Exit
                    System.exit(0);
                    break;
            }
        }
    }
    
    private void playMenuSound(String soundPath) {
        try {
            AudioPlayer soundEffect = new AudioPlayer(soundPath);
            soundEffect.play(false);
        } catch (Exception e) {
            // If sound file doesn't exist, just continue silently
            System.out.println("Could not play menu sound: " + e.getMessage());
        }
    }
    
    private void handleGameplayKeyPress(int key, boolean isShiftDown) {
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
        
        if (currentState != GameState.PLAYING) {
            return;
        }
        
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

    private void handlePauseMenuKeyPress(int key) {
        if (key == KeyEvent.VK_UP) {
            pauseMenuSelection = (pauseMenuSelection - 1 + 3) % 3; // Wrap around to bottom
        } else if (key == KeyEvent.VK_DOWN) {
            pauseMenuSelection = (pauseMenuSelection + 1) % 3; // Wrap around to top
        } else if (key == KeyEvent.VK_ENTER) {
            switch (pauseMenuSelection) {
                case 0: // Resume Game
                    currentState = GameState.PLAYING;
                    break;
                case 1: // Controls
                    currentState = GameState.CONTROLS;
                    break;
                case 2: // Exit
                    resetGame();
                    currentState = GameState.MENU;
                    break;
            }
        }
    }

    private void drawCutscene(Graphics g) {
        // Black background during cutscene
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // If we have a current frame, draw it
        if (currentFrameImage != null) {
            // Start audio when first frame is visible (only once)
            if (!cutsceneAudioStarted && cutsceneAudio != null) {
                cutsceneAudio.play(true); // Loop the audio
                cutsceneAudioStarted = true;
                System.out.println("Starting cutscene audio with first frame");
            }
            
            // Calculate position to center the frame
            int frameWidth = currentFrameImage.getWidth(null);
            int frameHeight = currentFrameImage.getHeight(null);
            
            // Scale the frame if it's too large for the screen
            if (frameWidth > getWidth() || frameHeight > getHeight()) {
                double scaleX = (double) getWidth() / frameWidth;
                double scaleY = (double) getHeight() / frameHeight;
                double scale = Math.min(scaleX, scaleY) * 0.9; // Scale to 90% of available space
                
                frameWidth = (int) (frameWidth * scale);
                frameHeight = (int) (frameHeight * scale);
            }
            
            int x = (getWidth() - frameWidth) / 2;
            int y = (getHeight() - frameHeight) / 2;
            
            // Draw the frame scaled
            g.drawImage(currentFrameImage, x, y, frameWidth, frameHeight, null);
        } else if (cutsceneFrameFiles == null || totalCutsceneFrames == 0) {
            // No frames available - show instructions
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 16));
            String[] instructions = {
                "No video frames found!",
                "",
                "To prepare video frames:",
                "1. Extract frames from your MP4 using FFmpeg:",
                "   ffmpeg -i assets/Cutscenes/Start.mp4 -vf fps=24 assets/Cutscenes/Start_frames/frame_%04d.png",
                "",
                "2. Make sure the frames are in the Start_frames directory"
            };
            
            int y = getHeight() / 2 - (instructions.length * 25) / 2;
            for (String line : instructions) {
                FontMetrics metrics = g.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(line)) / 2;
                g.drawString(line, x, y);
                y += 25;
            }
        }
        
        // Draw a message to show cutscene is playing and frame count
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 16));
        String message = "Cutscene playing... Press ESC to skip";
        String frameInfo = "Frame: " + (currentCutsceneFrame + 1) + " / " + totalCutsceneFrames;
        
        FontMetrics metrics = g.getFontMetrics();
        int x = (getWidth() - metrics.stringWidth(message)) / 2;
        int y = getHeight() - 30;
        g.drawString(message, x, y);
        
        // Only show frame info if in debug mode or F3 is pressed
        if (showBounds) {
            x = 10;
            y = 30;
            g.drawString(frameInfo, x, y);
        }
        
        // Check if cutscene should be ended
        if (cutsceneFinished) {
            endCutscene();
        }
    }

    private void handleCutsceneKeyPress(int key) {
        if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            endCutscene();
        }
    }
    
    // Play a cutscene 
    private void playCutscene(String resourcePath) {
        currentCutscene = resourcePath;
        cutsceneFinished = false;
        cutsceneAudioStarted = false;
        currentState = GameState.CUTSCENE;
        cutsceneStartTime = System.currentTimeMillis();
        
        // Stop any playing music
        if (backgroundMusic != null) {
            backgroundMusic.stop();
        }
        
        // Initialize cutscene audio but don't play it yet
        try {
            cutsceneAudio = new AudioPlayer("assets/music/cutscene_audio.wav");
            // Audio will be played when first frame is visible
        } catch (Exception e) {
            System.out.println("Error loading cutscene audio: " + e.getMessage());
        }
        
        // For MP4 files, we need to use the frames extracted from the video
        if (resourcePath.toLowerCase().endsWith(".mp4")) {
            // Look for a directory with the same name as the video but without the extension
            String framesDirPath = resourcePath.substring(0, resourcePath.lastIndexOf('.')) + "_frames";
            loadCutsceneFrames(framesDirPath);
            
            if (cutsceneFrameFiles == null || cutsceneFrameFiles.length == 0) {
                System.out.println("No video frames found for: " + resourcePath);
                System.out.println("Please extract frames from the video to: " + framesDirPath);
                endCutscene();
            } else {
                startCutsceneAnimation();
            }
        } else {
            // Load image-based cutscene frames directly from directory
            loadCutsceneFrames(resourcePath);
            startCutsceneAnimation();
        }
    }
    
    private void loadCutsceneFrames(String basePath) {
        try {
            // Check if basePath is a directory
            File dir = new File(basePath);
            if (dir.exists() && dir.isDirectory()) {
                // Get all image files from the directory but don't load them yet
                File[] imageFiles = dir.listFiles((d, name) -> 
                    name.toLowerCase().endsWith(".png") || 
                    name.toLowerCase().endsWith(".jpg") ||
                    name.toLowerCase().endsWith(".jpeg"));
                
                if (imageFiles != null && imageFiles.length > 0) {
                    // Sort files by name for proper sequence
                    java.util.Arrays.sort(imageFiles, (a, b) -> a.getName().compareTo(b.getName()));
                    
                    System.out.println("Found " + imageFiles.length + " cutscene frames in: " + basePath);
                    
                    // Store only the file references, not the loaded images
                    cutsceneFrameFiles = imageFiles;
                    totalCutsceneFrames = imageFiles.length;
                    currentCutsceneFrame = 0;
                    
                    // Load just the first frame
                    if (totalCutsceneFrames > 0) {
                        currentFrameImage = new ImageIcon(cutsceneFrameFiles[0].getPath()).getImage();
                    }
                } else {
                    System.out.println("No image files found in cutscene directory: " + basePath);
                    cutsceneFrameFiles = null;
                    totalCutsceneFrames = 0;
                }
            } else {
                System.out.println("Cutscene path not found or not a directory: " + basePath);
                cutsceneFrameFiles = null;
                totalCutsceneFrames = 0;
            }
        } catch (Exception e) {
            System.out.println("Error loading cutscene frames: " + e.getMessage());
            e.printStackTrace();
            cutsceneFrameFiles = null;
            totalCutsceneFrames = 0;
        }
    }
    
    private void startCutsceneAnimation() {
        if (cutsceneFrameFiles != null && totalCutsceneFrames > 0) {
            // For smoother animation, use a proper frame rate
            // 60fps = ~16.7ms between frames
            int frameRate = 60;  
            int frameDelay = 1000 / frameRate;
            
            System.out.println("Starting cutscene animation with " + totalCutsceneFrames + 
                              " frames at " + frameRate + " fps");
            
            // Start timer to advance frames
            cutsceneTimer = new Timer(frameDelay, e -> {
                // Load the next frame on demand
                currentCutsceneFrame++;
                if (currentCutsceneFrame < totalCutsceneFrames) {
                    // Load the current frame and release the previous one
                    try {
                        currentFrameImage = new ImageIcon(cutsceneFrameFiles[currentCutsceneFrame].getPath()).getImage();
                    } catch (Exception ex) {
                        System.out.println("Error loading frame " + currentCutsceneFrame + ": " + ex.getMessage());
                    }
                } else {
                    // End cutscene when all frames have been shown
                    cutsceneFinished = true;
                    currentFrameImage = null; // Release the last frame
                }
                repaint(); // Request repaint after each frame change for smooth animation
            });
            cutsceneTimer.start();
        } else {
            endCutscene();
        }
    }
    
    private void endCutscene() {
        // Stop animation timer if running
        if (cutsceneTimer != null && cutsceneTimer.isRunning()) {
            cutsceneTimer.stop();
            cutsceneTimer = null;
        }
        
        // Stop cutscene audio
        if (cutsceneAudio != null) {
            cutsceneAudio.stop();
            cutsceneAudio = null;
        }
        
        // Clear cutscene resources
        cutsceneFrameFiles = null;
        currentFrameImage = null;
        totalCutsceneFrames = 0;
        
        // Suggest garbage collection
        System.gc();
        
        // Start game after cutscene
        resetGame();
        currentState = GameState.PLAYING;
    }
}
