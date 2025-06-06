package tasks.tutorial;

import antiban.AntibanManager;
import data.NpcData;
import data.ObjectData;
import framework.Task;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

public class QuestGuideTask implements Task {

    private final AntibanManager antiban;

    private static final Tile QUEST_HOUSE_TILE = new Tile(3086, 3126);

    public QuestGuideTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        int progress = getProgress();
        return progress >= 7 && progress <= 8;
    }

    @Override
    public int execute() {
        antiban.tick();

        // Step 1: Walk to the house if not close
        if (Players.getLocal().distance(QUEST_HOUSE_TILE) > 4) {
            Walking.walk(QUEST_HOUSE_TILE);
            Sleep.sleepUntil(() -> Players.getLocal().distance(QUEST_HOUSE_TILE) <= 2, 5000);
            return antiban.sleepMedium();
        }

        // Step 2: Open door if closed (if the player is outside)
        int doorId = ObjectData.getId("door");
        if (doorId != -1 && Players.getLocal().distance(QUEST_HOUSE_TILE) <= 4) {
            GameObject door = GameObjects.closest(obj -> obj.getID() == doorId && obj.hasAction("Open"));
            if (door != null) {
                Logger.log("[QuestGuide] Opening quest house door");
                if (door.interact("Open")) {
                    Sleep.sleepUntil(() -> !door.exists() || Players.getLocal().distance(QUEST_HOUSE_TILE) <= 2, 3000);
                    return antiban.sleepShort();
                }
            }
        }

        // Step 3: Talk to Quest Guide until instructed to check quest tab
        if (getProgress() == 7) {
            int guideId = NpcData.getId("quest guide");
            if (guideId != -1) {
                NPC guide = NPCs.closest(n -> n != null && n.getID() == guideId);
                if (guide != null && guide.interact("Talk-to")) {
                    Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                    handleDialogue();
                    return antiban.sleepMedium();
                }
            }
        }

        // Step 4: Open quest tab if icon appears (progress==8)
        if (getProgress() == 8 && !Tabs.isOpen(Tab.QUEST)) {
            Logger.log("[QuestGuide] Opening Quest tab");
            Tabs.open(Tab.QUEST);
            return antiban.sleepMedium();
        }

        // Step 5: Continue talking to Quest Guide if in dialogue and quest tab is open (progress==8)
        if (getProgress() == 8 && Tabs.isOpen(Tab.QUEST) && Dialogues.inDialogue()) {
            handleDialogue();
            return antiban.sleepMedium();
        }

        // Step 6: Climb down ladder to Mining Instructor (progress==8)
        if (getProgress() == 8 && !Dialogues.inDialogue()) {
            GameObject ladder = GameObjects.closest(obj -> obj != null && obj.hasAction("Climb-down"));
            if (ladder != null && ladder.interact("Climb-down")) {
                Logger.log("[QuestGuide] Climbing down ladder to Mining Instructor");
                Sleep.sleepUntil(() -> Players.getLocal().getZ() == 0 && Players.getLocal().getY() < 3118, 5000);
                return antiban.sleepMedium();
            }
        }

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Quest Guide";
    }

    private int getProgress() {
        // Tutorial Island progress varp 281 is typical
        return org.dreambot.api.methods.settings.PlayerSettings.getConfig(281);
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