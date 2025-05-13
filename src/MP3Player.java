import java.io.File;
import java.io.IOException;

public class MP3Player {
    private Process process;
    private String filePath;
    private boolean isPlaying = false;
    
    public MP3Player(String filePath) {
        this.filePath = filePath;
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("MP3 file not found: " + filePath);
        }
    }
    
    public void play(boolean loop) {
        if (isPlaying) {
            return;
        }
        
        try {
            // Use the system's default audio player to play the MP3
            String osName = System.getProperty("os.name").toLowerCase();
            
            ProcessBuilder builder;
            if (osName.contains("mac")) {
                // macOS
                builder = new ProcessBuilder("afplay", filePath);
            } else if (osName.contains("windows")) {
                // Windows
                builder = new ProcessBuilder("cmd.exe", "/c", "start", filePath);
            } else {
                // Linux and others
                builder = new ProcessBuilder("xdg-open", filePath);
            }
            
            process = builder.start();
            isPlaying = true;
            
            // If looping is requested, start a thread to monitor and restart playback
            if (loop) {
                new Thread(() -> {
                    try {
                        while (isPlaying) {
                            int exitCode = process.waitFor();
                            if (isPlaying) {
                                // Restart playback if still playing
                                process = builder.start();
                            }
                        }
                    } catch (IOException | InterruptedException e) {
                        System.err.println("Error in playback loop: " + e.getMessage());
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("Error playing MP3: " + e.getMessage());
        }
    }
    
    public void stop() {
        if (isPlaying && process != null) {
            process.destroy();
            isPlaying = false;
        }
    }
    
    // Volume control isn't directly supported with this approach
    public void setVolume(float volume) {
        System.out.println("Volume control not supported for system player");
    }
}
 