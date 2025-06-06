package antiban;

public class FatigueTracker {

    private int fatigue = 0;
    private final int maxFatigue = 100;

    public void increase(int amount) {
        fatigue += amount;
        if (fatigue > maxFatigue) {
            fatigue = maxFatigue;
        }
    }

    public void reduce(int amount) {
        fatigue -= amount;
        if (fatigue < 0) {
            fatigue = 0;
        }
    }

    public int getFatigueLevel() {
        return fatigue;
    }

    public boolean isFatigued() {
        return fatigue > 50;
    }

    public void reset() {
        fatigue = 0;
    }
}
