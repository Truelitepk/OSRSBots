package antiban;

import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.wrappers.interactive.Player;
import org.dreambot.api.utilities.Logger;

import java.awt.*;
import java.util.Random;

public class AntibanManager {

    private final FatigueTracker fatigue;
    private final UUIDProfileCache profile;
    private final Random random = new Random();

    private int ticksSinceLastInterrupt = 0;

    public AntibanManager(UUIDProfileCache profile) {
        this.profile = profile;
        this.fatigue = new FatigueTracker();
    }

    public void tick() {
        fatigue.increase(1);
        ticksSinceLastInterrupt++;

        simulateCameraDrift();
        maybeHoverRandomTab();
        maybeAFK();
        maybeMisclick();
        maybeInterruptBehavior();
    }

    public int getReactionDelay() {
        int fatigueLevel = fatigue.getFatigueLevel();
        int baseDelay = 400 + random.nextInt(300);
        return baseDelay + (int)(fatigueLevel * 1.2);
    }

    public int sleepShort() {
        int time = 250 + random.nextInt(150);
        sleep(time);
        return time;
    }

    public int sleepMedium() {
        int time = 500 + random.nextInt(300);
        sleep(time);
        return time;
    }

    public int sleepLong() {
        int time = 1000 + random.nextInt(600);
        sleep(time);
        return time;
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private void simulateCameraDrift() {
        if (random.nextInt(profile.getCameraActivity()) == 0) {
            Logger.log("[AntiBan] Camera drift.");
            int yaw = Camera.getYaw() + random.nextInt(90) - 45;
            int pitch = 300 + random.nextInt(100);
            Camera.rotateToYaw(yaw);
            Camera.rotateToPitch(pitch);
        }
    }

    private void maybeHoverRandomTab() {
        if (random.nextInt(400) == 0) {
            Tab[] tabs = Tab.values();
            Tab hoverTab = tabs[random.nextInt(tabs.length)];
            Logger.log("[AntiBan] Hovering tab: " + hoverTab.name());
            Tabs.open(hoverTab);
            sleepShort();
        }
    }

    private void maybeAFK() {
        if (random.nextInt(profile.getAfkTendency()) == 0 && fatigue.getFatigueLevel() > 25) {
            int afkTime = 2000 + random.nextInt(4000);
            Logger.log("[AntiBan] Simulating AFK: " + afkTime + "ms");
            sleep(afkTime);
        }
    }

    private void maybeMisclick() {
        if (random.nextInt(100) < profile.getMisclickChance()) {
            Logger.log("[AntiBan] Simulating misclick.");
            Player p = Players.getLocal();

            // Replacement for deprecated Mouse.moveMouseOutsideScreen()
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int offScreenX = (random.nextBoolean()) ? -20 : (int) screenSize.getWidth() + 20;
            int offScreenY = (random.nextBoolean()) ? -20 : (int) screenSize.getHeight() + 20;
            Mouse.move(new Point(offScreenX, offScreenY));

            sleepShort();
            int x = p.getX() + random.nextInt(3);
            int y = p.getY() + random.nextInt(3);
            Mouse.move(new Rectangle(x, y, 5, 5));
        }
    }

    private void maybeInterruptBehavior() {
        if (ticksSinceLastInterrupt >= 40 && random.nextInt(300) == 0) {
            Logger.log("[AntiBan] Behavior interrupt triggered.");
            if (random.nextBoolean()) {
                Tabs.open(Tab.INVENTORY);
            } else {
                Tabs.open(Tab.SKILLS);
            }
            sleepMedium();
            ticksSinceLastInterrupt = 0;
        }
    }

    public FatigueTracker getFatigueTracker() {
        return fatigue;
    }

    public UUIDProfileCache getProfile() {
        return profile;
    }
}