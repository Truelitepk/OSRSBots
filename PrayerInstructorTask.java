package tasks.tutorial;

import antiban.AntibanManager;
import antiban.UUIDProfileCache;
import framework.Task;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

public class PrayerInstructorTask implements Task {

    private final AntibanManager antiban;
    private final Area PRAYER_ROOM = new Area(3129, 3132, 3133, 3129, 1);
    private boolean completed = false;
    private boolean talked = false;
    private boolean tabOpened = false;

    public PrayerInstructorTask(UUIDProfileCache profile) {
        this.antiban = new AntibanManager(profile);
    }

    public PrayerInstructorTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        return !completed && PRAYER_ROOM.contains(Players.getLocal());
    }

    @Override
    public int execute() {
        antiban.tick();
        Logger.log("[PrayerInstructor] Executing Prayer Instructor Task");

        if (Players.getLocal() == null) return antiban.sleepShort();

        // Step 1: Talk to Prayer Instructor
        if (!talked) {
            NPC instructor = NPCs.closest("Prayer Instructor");
            if (instructor != null && instructor.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                talked = true;
                return antiban.sleepMedium();
            }
        }

        // Step 2: Open Prayer tab
        if (talked && !tabOpened) {
            if (!Tabs.isOpen(Tab.PRAYER)) {
                Tabs.open(Tab.PRAYER);
                Sleep.sleep(800);
            }
            tabOpened = true;
            return antiban.sleepMedium();
        }

        // Step 3: Talk again after opening tab
        if (tabOpened && !completed) {
            NPC instructor = NPCs.closest("Prayer Instructor");
            if (instructor != null && instructor.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                completed = true;
                return antiban.sleepMedium();
            }
        }

        // Step 4: Climb down ladder to Magic Instructor
        if (completed) {
            GameObject ladder = GameObjects.closest(obj -> obj != null && obj.hasAction("Climb-down"));
            if (ladder != null && ladder.interact("Climb-down")) {
                Sleep.sleepUntil(() -> Players.getLocal().getZ() == 0, 5000);
                return antiban.sleepMedium();
            }
        }

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Prayer Instructor Task";
    }

    private void handleDialogue() {
        while (Dialogues.inDialogue()) {
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                Sleep.sleep(600);
            }
        }
    }
}
