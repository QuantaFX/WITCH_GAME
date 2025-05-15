import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * Audio player class for handling sound effects and background music.
 * Provides functionality to play, stop, and control volume of audio files.
 */
public class AudioPlayer {
    private Clip clip;
    private boolean isPlaying = false;
    private String filePath;
    private FloatControl volumeControl;
    private float currentVolume = 0.5f; // 50% volume
    
    /**
     * Constructor for AudioPlayer.
     * Initializes the audio player with the specified audio file.
     * 
     * @param filePath String path to the audio file to load
     * @return void
     */
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
            
            // Add line listener to update isPlaying flag when clip stops
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    isPlaying = false;
                } else if (event.getType() == LineEvent.Type.START) {
                    isPlaying = true;
                }
            });
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("Error initializing audio player: " + e.getMessage());
        }
    }
    
    /**
     * Plays the audio clip.
     * If the clip is already playing, it will be stopped and restarted.
     * 
     * @param loop Boolean indicating whether the audio should loop continuously
     * @return void
     */
    public void play(boolean loop) {
        if (clip != null) {
            // Stop the clip first if it's currently playing
            if (clip.isRunning()) {
                clip.stop();
            }
            
            // Reset to the beginning
            clip.setFramePosition(0);
            
            // Set up looping if requested
            if (loop) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.loop(0); // No looping
            }
            
            // Start playback
            clip.start();
            isPlaying = true;
        }
    }
    
    /**
     * Stops the audio playback if it's currently playing.
     * 
     * @return void
     */
    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            isPlaying = false;
        }
    }
    
    /**
     * Checks if the audio is currently playing.
     * 
     * @return boolean True if the audio is playing, false otherwise
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * Sets the volume for this audio player.
     * Converts the linear volume value to logarithmic decibels for better volume control.
     * 
     * @param volume Float volume level from 0.0 (mute) to 1.0 (max volume)
     * @return void
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
     * Gets the current volume level.
     * 
     * @return float Volume from 0.0 to 1.0
     */
    public float getVolume() {
        return currentVolume;
    }
}
