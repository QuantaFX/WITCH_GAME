import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioPlayer {
    private Clip clip;
    private boolean isPlaying = false;
    private String filePath;
    private FloatControl volumeControl;
    private float currentVolume = 0.1f; // 50% volume
    
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
            
            // Set up volume control if available
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                setVolume(currentVolume);
            }
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
    
    /**
     * Sets the volume for this audio player
     * @param volume Volume level from 0.0 (mute) to 1.0 (max volume)
     */
    public void setVolume(float volume) {
        if (volumeControl == null) return;
        
        // Clamp volume between 0.0 and 1.0
        currentVolume = Math.max(0.0f, Math.min(1.0f, volume));
        
        // Convert to decibels
        // Formula: dB = 20 * log10(volume)
        // This gives a range from -∞ (mute) to 0 dB (max volume)
        float dB = 20f * (float) Math.log10(currentVolume);
        
        // Clamp the lower bound to the minimum value allowed by the control
        if (currentVolume > 0) {
            dB = Math.max(dB, volumeControl.getMinimum());
        } else {
            dB = volumeControl.getMinimum();
        }
        
        // Set the gain
        volumeControl.setValue(dB);
    }
    
    /**
     * Gets the current volume level
     * @return Volume from 0.0 to 1.0
     */
    public float getVolume() {
        return currentVolume;
    }
}
