import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.awt.Point;

/**
 * Manages the loading, storage, and transitioning between game levels.
 * Handles level data loading from XML files and provides methods to access and change levels.
 */
public class LevelManager {
    private ArrayList<Level> levels;
    private int currentLevelIndex;
    private Player player;
    
    /**
     * Constructor for the LevelManager class.
     * Initializes the level list, loads all levels, and sets up the player at the starting position.
     * 
     * @param player Player The player character to position at each level
     * @return void
     */
    public LevelManager(Player player) {
        this.levels = new ArrayList<>();
        this.currentLevelIndex = 0;
        this.player = player;
        loadAllLevels();
        
        // Explicitly set player position for the first level
        if (!levels.isEmpty()) {
            Level firstLevel = levels.get(0);
            Point startPos = firstLevel.getPlayerStartPosition();
            System.out.println("LevelManager constructor - Setting initial player position to: " + 
                              startPos.x + ", " + startPos.y);
            player.setPosition(startPos.x, startPos.y);
        }
    }
    
    /**
     * Loads all level files into memory.
     * Sets up each door to point to the next level in sequence.
     * 
     * @return void
     */
    private void loadAllLevels() {
        // Load levels from level 1 to 10
        for (int i = 1; i <= 10; i++) {
            Level level = loadLevelFromFile("levels/lvl_" + i + ".level");
            if (level != null) {
                levels.add(level);
            }
        }
        
        // Set target levels for doors (each door leads to the next level)
        for (int i = 0; i < levels.size() - 1; i++) {
            Level currentLevel = levels.get(i);
            if (currentLevel.hasDoor()) {
                currentLevel.getDoor().setTargetLevel(i + 1);
            }
        }
    }
    
    /**
     * Loads a single level from an XML file.
     * Parses all level elements including platforms, enemies, player start position, and door.
     * 
     * @param filePath String Path to the level XML file
     * @return Level A fully initialized Level object, or null if loading failed
     */
    private Level loadLevelFromFile(String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("Level file not found: " + filePath);
                return null;
            }
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            
            // Get level number from filename
            String fileName = file.getName();
            int levelNumber = Integer.parseInt(fileName.substring(4, fileName.indexOf(".level")));
            
            Level level = new Level(levelNumber);
            
            // Parse level background if exists
            Element rootElement = doc.getDocumentElement();
            if (rootElement.hasAttribute("bg")) {
                String bgPath = rootElement.getAttribute("bg");
                level.setBackgroundPath(bgPath);
                System.out.println("Level " + levelNumber + " - Background path: " + bgPath);
            }
            
            // Parse platforms
            NodeList platformNodes = doc.getElementsByTagName("platform");
            for (int i = 0; i < platformNodes.getLength(); i++) {
                Element platformElement = (Element) platformNodes.item(i);
                
                int x = Integer.parseInt(platformElement.getAttribute("x"));
                int y = Integer.parseInt(platformElement.getAttribute("y"));
                int width = Integer.parseInt(platformElement.getAttribute("width"));
                int height = Integer.parseInt(platformElement.getAttribute("height"));
                
                Platform platform = new Platform(x, y, width, height);
                level.addPlatform(platform);
            }
            
            // Clear existing enemy spawn points
            Enemy.clearSpawnPoints();
            
            // Parse enemies
            NodeList enemyNodes = doc.getElementsByTagName("enemy");
            for (int i = 0; i < enemyNodes.getLength(); i++) {
                Element enemyElement = (Element) enemyNodes.item(i);
                
                int x = Integer.parseInt(enemyElement.getAttribute("x"));
                int y = Integer.parseInt(enemyElement.getAttribute("y"));
                
                // Store this position as a spawn point for bosses to use
                Enemy.addSpawnPoint(x, y);
                
                // Check if this enemy is a boss
                boolean isBoss = enemyElement.hasAttribute("boss") && 
                                 enemyElement.getAttribute("boss").equals("true");
                
                Enemy enemy = new Enemy(x, y, 36, 28, "assets/Orc_Sprite/orc_idle.png", 4);
                
                if (isBoss) {
                    // Set boss properties
                    enemy.setBoss(true);
                    level.setHasBoss(true);
                }
                
                level.addEnemy(enemy);
            }
            
            // Parse player start position
            NodeList playerNodes = doc.getElementsByTagName("player");
            if (playerNodes.getLength() > 0) {
                Element playerElement = (Element) playerNodes.item(0);
                
                int x = Integer.parseInt(playerElement.getAttribute("x"));
                int y = Integer.parseInt(playerElement.getAttribute("y"));
                
                System.out.println("Level " + levelNumber + " - Player position from XML: " + x + ", " + y);
                level.setPlayerStartPosition(x, y);
            } else {
                System.out.println("Level " + levelNumber + " - No player position found in XML, using default");
            }
            
            // Parse door (if exists)
            NodeList doorNodes = doc.getElementsByTagName("door");
            if (doorNodes.getLength() > 0) {
                Element doorElement = (Element) doorNodes.item(0);
                
                int x = Integer.parseInt(doorElement.getAttribute("x"));
                int y = Integer.parseInt(doorElement.getAttribute("y"));
                int width = Integer.parseInt(doorElement.getAttribute("width"));
                int height = Integer.parseInt(doorElement.getAttribute("height"));
                
                Door door = new Door(x, y, width, height);
                level.setDoor(door);
            }
            
            return level;
            
        } catch (Exception e) {
            System.out.println("Error loading level: " + filePath);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Returns the currently active level.
     * 
     * @return Level The current level, or null if no valid level exists
     */
    public Level getCurrentLevel() {
        if (currentLevelIndex < 0 || currentLevelIndex >= levels.size()) {
            return null;
        }
        return levels.get(currentLevelIndex);
    }
    
    /**
     * Loads a specific level by index and positions the player at the level's start position.
     * 
     * @param levelIndex int The index of the level to load
     * @return boolean True if the level was loaded successfully, false otherwise
     */
    public boolean loadLevel(int levelIndex) {
        if (levelIndex < 0 || levelIndex >= levels.size()) {
            return false;
        }
        
        currentLevelIndex = levelIndex;
        Level level = getCurrentLevel();
        
        // Set player position
        Point startPos = level.getPlayerStartPosition();
        System.out.println("Setting player position to: " + startPos.x + ", " + startPos.y);
        player.setPosition(startPos.x, startPos.y);
        
        return true;
    }
    
    /**
     * Moves to the next level in sequence.
     * 
     * @return boolean True if the next level was loaded successfully, false if there are no more levels
     */
    public boolean moveToNextLevel() {
        return loadLevel(currentLevelIndex + 1);
    }
    
    /**
     * Returns the list of all levels.
     * 
     * @return ArrayList<Level> The complete list of game levels
     */
    public ArrayList<Level> getLevels() {
        return levels;
    }
    
    /**
     * Returns the index of the current level.
     * 
     * @return int The current level index
     */
    public int getCurrentLevelIndex() {
        return currentLevelIndex;
    }
} 