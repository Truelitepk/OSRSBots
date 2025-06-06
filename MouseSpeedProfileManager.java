package antiban;

import org.dreambot.api.methods.input.mouse.MouseSettings;

import java.util.Random;

/**
 * Manages mouse speed to simulate human-like variability and adapt to bot "fatigue".
 * Each bot (by profile name) gets a unique baseline speed and a unique variance pattern.
 * Fatigue and randomness modulate the speed, with safe bounds.
 */
public class MouseSpeedProfileManager {

    private final String profileName;
    private final Random random = new Random();
    private final int baseSpeed;
    private int fatigueLevel;
    private int lastAppliedSpeed;
    private int sessionVariance;

    /**
     * Initializes a unique mouse speed profile for the given profile name.
     * @param profileName Unique identifier for this bot/profile.
     */
    public MouseSpeedProfileManager(String profileName) {
        this.profileName = profileName;

        // Create a unique, stable baseline per profile (hash-based seed)
        int hashSeed = profileName != null ? profileName.hashCode() : random.nextInt();
        Random profileRandom = new Random(hashSeed);

        // Range: 80–140
        this.baseSpeed = 80 + profileRandom.nextInt(61);

        // Add a session-unique minor variance (makes multiple runs less predictable)
        this.sessionVariance = random.nextInt(11) - 5; // [-5, +5]
        this.fatigueLevel = 0;

        applyMouseSpeed(true);
    }

    /**
     * Update the fatigue level. Higher fatigue = slower mouse.
     * @param fatigueLevel Value from 0 (rested) to 100+ (very tired)
     */
    public void updateFatigue(int fatigueLevel) {
        this.fatigueLevel = Math.max(0, fatigueLevel);
        applyMouseSpeed(false);
    }

    /**
     * Recalculates and applies the mouse speed, factoring in fatigue and randomness.
     * @param force If true, always applies; if false, only applies if speed changes.
     */
    private void applyMouseSpeed(boolean force) {
        // Fatigue can slow mouse up to 40 units below base
        int fatiguePenalty = (int)(0.3 * Math.min(100, fatigueLevel)); // Max -30

        // Simulate slight natural hand 'tremor' on each update
        int tickVariance = random.nextInt(9) - 4; // [-4, +4]

        int calculatedSpeed = baseSpeed + sessionVariance + tickVariance - fatiguePenalty;

        // Clamp to safe range
        calculatedSpeed = Math.max(40, Math.min(160, calculatedSpeed));

        if (force || calculatedSpeed != lastAppliedSpeed) {
            MouseSettings.setSpeed(calculatedSpeed);
            lastAppliedSpeed = calculatedSpeed;
        }
    }

    /**
     * Resets fatigue and mouse speed to profile baseline.
     */
    public void reset() {
        fatigueLevel = 0;
        applyMouseSpeed(true);
    }

    /**
     * @return The last speed set for the mouse.
     */
    public int getCurrentSpeed() {
        return lastAppliedSpeed;
    }

    /**
     * For debugging or logging: returns the profile name, base speed, and current settings.
     */
    @Override
    public String toString() {
        return "MouseSpeedProfileManager{" +
                "profileName='" + profileName + '\'' +
                ", baseSpeed=" + baseSpeed +
                ", sessionVariance=" + sessionVariance +
                ", fatigueLevel=" + fatigueLevel +
                ", lastAppliedSpeed=" + lastAppliedSpeed +
                '}';
    }
}