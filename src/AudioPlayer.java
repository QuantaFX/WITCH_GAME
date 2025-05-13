import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioPlayer {
    private Clip clip;
    private boolean isPlaying = false;
    private String filePath;
    
    public AudioPlayer(String filePath) {
        this.filePath = filePath;
        try {
            // Check if file exists
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                System.out.println("Audio file not found: " + filePath);
                return;
            }
            
            // Set up audio stream
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Error initializing audio player: " + e.getMessage());
        }
    }
    
    public void play(boolean loop) {
        if (clip != null) {
            clip.setFramePosition(0);
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            clip.start();
            isPlaying = true;
        }
    }
    
    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            isPlaying = false;
        }
    }
    
    public boolean isPlaying() {
        return isPlaying;
    }
}
