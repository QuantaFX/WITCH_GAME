import java.awt.*;
import java.util.ArrayList;

/**
 * Represents a game level with platforms, enemies, a door, and other level-specific properties.
 * Acts as a container for all elements that make up a level in the game.
 */
public class Level {
    private ArrayList<Platform> platforms;
    private ArrayList<Enemy> enemies;
    private Door door;
    private Point playerStartPosition;
    private int levelNumber;
    private boolean hasBoss;
    private String backgroundPath;
    
    /**
     * Constructor for the Level class.
     * Initializes a new level with the specified level number and default values.
     * 
     * @param levelNumber int The number/index of this level
     * @return void
     */
    public Level(int levelNumber) {
        this.levelNumber = levelNumber;
        this.platforms = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.playerStartPosition = new Point(100, 300); // Default position
        this.hasBoss = false;
        this.backgroundPath = "../assets/Level_bg/Level1.png"; // Default background
    }
    
    /**
     * Adds a platform to this level.
     * 
     * @param platform Platform The platform to add to the level
     * @return void
     */
    public void addPlatform(Platform platform) {
        platforms.add(platform);
    }
    
    /**
     * Adds an enemy to this level.
     * 
     * @param enemy Enemy The enemy to add to the level
     * @return void
     */
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }
    
    /**
     * Sets the door for this level.
     * The door is used to transition to the next level.
     * 
     * @param door Door The door to set for this level
     * @return void
     */
    public void setDoor(Door door) {
        this.door = door;
    }
    
    /**
     * Sets the starting position for the player in this level.
     * 
     * @param x int X-coordinate of the player's starting position
     * @param y int Y-coordinate of the player's starting position
     * @return void
     */
    public void setPlayerStartPosition(int x, int y) {
        this.playerStartPosition = new Point(x, y);
    }
    
    /**
     * Sets whether this level has a boss enemy.
     * 
     * @param hasBoss boolean True if the level has a boss, false otherwise
     * @return void
     */
    public void setHasBoss(boolean hasBoss) {
        this.hasBoss = hasBoss;
    }
    
    /**
     * Sets the background image path for this level.
     * 
     * @param path String The file path to the background image
     * @return void
     */
    public void setBackgroundPath(String path) {
        this.backgroundPath = path;
    }
    
    /**
     * Returns the list of platforms in this level.
     * 
     * @return ArrayList<Platform> The list of platforms
     */
    public ArrayList<Platform> getPlatforms() {
        return platforms;
    }
    
    /**
     * Returns the list of enemies in this level.
     * 
     * @return ArrayList<Enemy> The list of enemies
     */
    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }
    
    /**
     * Returns the door for this level.
     * 
     * @return Door The level's door
     */
    public Door getDoor() {
        return door;
    }
    
    /**
     * Returns the player's starting position for this level.
     * 
     * @return Point The player's starting position
     */
    public Point getPlayerStartPosition() {
        return playerStartPosition;
    }
    
    /**
     * Returns the level number.
     * 
     * @return int The level number/index
     */
    public int getLevelNumber() {
        return levelNumber;
    }
    
    /**
     * Checks if this level has a boss enemy.
     * 
     * @return boolean True if the level has a boss, false otherwise
     */
    public boolean hasBoss() {
        return hasBoss;
    }
    
    /**
     * Checks if this level has a door.
     * 
     * @return boolean True if the level has a door, false otherwise
     */
    public boolean hasDoor() {
        return door != null;
    }
    
    /**
     * Returns the file path to the background image for this level.
     * 
     * @return String The background image file path
     */
    public String getBackgroundPath() {
        return backgroundPath;
    }
} 