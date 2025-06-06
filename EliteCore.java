package elitecore;

import antiban.AntibanManager;
import antiban.MouseSpeedProfileManager;
import antiban.UUIDProfileCache;
import tasks.tutorial.TutorialTaskHandler;

import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Logger;

@ScriptManifest(
        name = "EliteBot",
        author = "Kylejosh2",
        version = 1.0,
        description = "Modular Anti-Ban + Tutorial Island Bot",
        category = Category.MISC
)
public class EliteCore extends AbstractScript {

    private AntibanManager antibanManager;
    private TutorialTaskHandler tutorialHandler;
    private MouseSpeedProfileManager mouseSpeedManager;

    @Override
    public void onStart() {
        Logger.log("EliteCore started.");

        UUIDProfileCache profile = UUIDProfileCache.loadOrCreate("EliteProfile");
        antibanManager = new AntibanManager(profile);
        mouseSpeedManager = new MouseSpeedProfileManager("EliteProfile");
        tutorialHandler = new TutorialTaskHandler(antibanManager); // <--- FIX HERE
    }

    @Override
    public int onLoop() {
        if (antibanManager != null && mouseSpeedManager != null) {
            mouseSpeedManager.updateFatigue(antibanManager.getFatigueTracker().getFatigueLevel());
        }

        if (tutorialHandler != null) {
            return tutorialHandler.execute();
        }

        return 300;
    }

    @Override
    public void onExit() {
        Logger.log("EliteCore shutting down...");
        if (mouseSpeedManager != null) {
            mouseSpeedManager.reset();
        }
    }
}