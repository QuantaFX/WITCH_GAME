import java.awt.*;
import java.util.ArrayList;

public class Level {
    private ArrayList<Platform> platforms;
    private ArrayList<Enemy> enemies;
    private Door door;
    private Point playerStartPosition;
    private int levelNumber;
    private boolean hasBoss;
    
    public Level(int levelNumber) {
        this.levelNumber = levelNumber;
        this.platforms = new ArrayList<>();
        this.enemies = new ArrayList<>();
        this.playerStartPosition = new Point(100, 300); // Default position
        this.hasBoss = false;
    }
    
    public void addPlatform(Platform platform) {
        platforms.add(platform);
    }
    
    public void addEnemy(Enemy enemy) {
        enemies.add(enemy);
    }
    
    public void setDoor(Door door) {
        this.door = door;
    }
    
    public void setPlayerStartPosition(int x, int y) {
        this.playerStartPosition = new Point(x, y);
    }
    
    public void setHasBoss(boolean hasBoss) {
        this.hasBoss = hasBoss;
    }
    
    public ArrayList<Platform> getPlatforms() {
        return platforms;
    }
    
    public ArrayList<Enemy> getEnemies() {
        return enemies;
    }
    
    public Door getDoor() {
        return door;
    }
    
    public Point getPlayerStartPosition() {
        return playerStartPosition;
    }
    
    public int getLevelNumber() {
        return levelNumber;
    }
    
    public boolean hasBoss() {
        return hasBoss;
    }
    
    public boolean hasDoor() {
        return door != null;
    }
} 