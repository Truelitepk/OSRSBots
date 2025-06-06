package tasks.tutorial;

import antiban.AntibanManager;
import data.ObjectData;
import framework.Task;
import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.input.Camera;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.settings.PlayerSettings;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.widgets.WidgetChild;

public class IntroGuideTask implements Task {

    private final AntibanManager antiban;
    private boolean completed = false;
    private static final String GUIDE_NAME = "Gielinor Guide";
    private static final Tile GUIDE_TILE = new Tile(3094, 3107, 0);
    private static final Tile DOOR_TILE = new Tile(3098, 3107, 0);
    private static final Tile SURVIVAL_EXPERT_TILE = new Tile(3103, 3099, 0);

    public IntroGuideTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        int progress = getProgress();
        if (Players.getLocal() == null) return false;
        boolean inStartingRoom = Players.getLocal().getTile().getX() <= 3102 && Players.getLocal().getTile().getY() >= 3095;
        NPC guide = findGielinorGuide();
        return !completed
            && isOnTutorialIsland()
            && getPlane() == 0
            && (progress < 4 || (guide != null && inStartingRoom));
    }

    @Override
    public int execute() {
        antiban.tick();

        // 1. If "Moving on" widget or final dialogue is present, IMMEDIATELY click door.
        if (shouldExitToDoor()) {
            Logger.log("[IntroGuide] Ready to leave detected! Forcing door interaction.");
            if (Tabs.isOpen(Tab.OPTIONS)) {
                Tabs.open(Tab.INVENTORY);
                Sleep.sleep(300, 500);
            }
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                Sleep.sleep(400, 700);
            }
            clickDoorAndWalkToSurvivalExpert();
            return antiban.sleepShort();
        }

        // 2. If in actual NPC dialogue, handle it (with aggressive break for exit condition)
        if (Dialogues.inDialogue()) {
            Logger.log("[IntroGuide] In real dialogue, handling...");
            handleDialogue();
            return antiban.sleepShort();
        }

        // 3. If the "Getting started" box is up and not in dialogue, click the Guide
        if (needsToStartTutorialDialogue()) {
            NPC guide = findGielinorGuide();
            if (guide == null) {
                walkToTile(GUIDE_TILE);
                return antiban.sleepShort();
            }
            if (!Players.getLocal().isMoving() && Players.getLocal().distance(guide) > 3) {
                walkToTile(guide.getTile());
                Sleep.sleepUntil(() -> Players.getLocal().distance(guide) <= 2, Calculations.random(1800, 2600));
            }
            if (!guide.isOnScreen() || !guide.canReach()) {
                Camera.mouseRotateToEntity(guide);
                Sleep.sleep(400, 650);
            }
            if (guide.hasAction("Talk-to") && guide.interact("Talk-to")) {
                Sleep.sleepUntil(() -> Dialogues.inDialogue(), Calculations.random(1800, 2500));
            }
            return antiban.sleepShort();
        }

        // 4. Handle the flashing spanner/wrench step
        if (Dialogues.inDialogue() && Dialogues.getNPCDialogue() != null &&
                Dialogues.getNPCDialogue().contains("flashing icon of a spanner")) {
            if (!Tabs.isOpen(Tab.OPTIONS)) {
                Tabs.open(Tab.OPTIONS);
                Sleep.sleepUntil(() -> Tabs.isOpen(Tab.OPTIONS), 1200);
            }
            Sleep.sleep(400, 700);
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
            }
            return antiban.sleepShort();
        }

        // 5. If dialogue is complete, open the door (fallback)
        if (hasCompletedDialogues()) {
            clickDoorAndWalkToSurvivalExpert();
            completed = true;
            return antiban.sleepShort();
        }

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Intro Guide";
    }

    // --- Key Aggressive Exit Detection ---
    private boolean shouldExitToDoor() {
        return isMovingOnInstructionOpen() || isMovingOnDialogueOpen() || isGuideIntroCompleteDialogue();
    }

    private boolean isMovingOnInstructionOpen() {
        WidgetChild child = null;
        if (Widgets.getWidget(263) != null) {
            child = Widgets.getWidget(263).getChild(1);
        }
        if (child != null && child.isVisible()) {
            String text = child.getText();
            return text != null && text.contains("Moving on")
                    && text.contains("all you need to do is click on the door");
        }
        return false;
    }

    private boolean isMovingOnDialogueOpen() {
        return Dialogues.inDialogue()
            && Dialogues.getNPCDialogue() != null
            && Dialogues.getNPCDialogue().contains("Moving on")
            && Dialogues.getNPCDialogue().contains("all you need to do is click on the door");
    }

    private boolean isGuideIntroCompleteDialogue() {
        return Dialogues.inDialogue()
            && Dialogues.getNPCDialogue() != null
            && Dialogues.getNPCDialogue().contains("To continue the tutorial go through that door over there and speak to your first instructor!");
    }

    private void clickDoorAndWalkToSurvivalExpert() {
        int doorId = ObjectData.getId("door");
        GameObject door = GameObjects.closest(obj -> obj.getID() == doorId && obj.getTile().equals(DOOR_TILE));
        if (door != null && door.hasAction("Open")) {
            if (!door.isOnScreen()) {
                Camera.mouseRotateToEntity(door);
                Sleep.sleep(350, 600);
            }
            if (door.interact("Open")) {
                Sleep.sleepUntil(() -> Players.getLocal().getTile().getX() > 3096, Calculations.random(2000, 3400));
            }
        } else {
            Walking.walk(DOOR_TILE);
            Sleep.sleepUntil(() -> Players.getLocal().getTile().distance(DOOR_TILE) <= 1, Calculations.random(800, 1500));
        }
        Walking.walk(SURVIVAL_EXPERT_TILE);
    }

    private void handleDialogue() {
        int tries = 0;
        int maxTries = 10;
        int startProgress = getProgress();
        while (Dialogues.inDialogue() && tries < maxTries) {
            // AGGRESSIVE EXIT: If we see ready-to-leave dialogue, break and click the door
            if (shouldExitToDoor()) {
                Logger.log("[IntroGuide] Exit dialogue or widget detected inside handleDialogue. Breaking to click door!");
                break;
            }
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                Sleep.sleep(Calculations.random(400, 700));
            } else if (Dialogues.getOptions() != null && Dialogues.getOptions().length > 0) {
                Dialogues.chooseOption(Dialogues.getOptions()[0]);
                Sleep.sleep(Calculations.random(400, 700));
            } else {
                Sleep.sleep(Calculations.random(300, 600));
            }
            if (getProgress() != startProgress) break;
            tries++;
        }
        // Failsafe: If stuck, still try to click the door!
        if (tries >= maxTries || shouldExitToDoor()) {
            Logger.log("[IntroGuide] Failsafe in dialogue handler: forcing door click!");
            if (Tabs.isOpen(Tab.OPTIONS)) {
                Tabs.open(Tab.INVENTORY);
                Sleep.sleep(300, 500);
            }
            clickDoorAndWalkToSurvivalExpert();
        }
    }

    // ---- Supporting logic ----

    private boolean isOnTutorialIsland() {
        int progress = PlayerSettings.getConfig(281);
        return progress >= 0 && progress < 1000;
    }

    private int getPlane() {
        return Players.getLocal().getZ();
    }

    private int getProgress() {
        return PlayerSettings.getConfig(281);
    }

    private NPC findGielinorGuide() {
        return NPCs.closest(n ->
            n != null &&
            n.getName() != null &&
            n.getName().equalsIgnoreCase(GUIDE_NAME) &&
            n.canReach()
        );
    }

    private boolean needsToStartTutorialDialogue() {
        return isTutorialChatOpen() && !Dialogues.inDialogue();
    }

    private boolean isTutorialChatOpen() {
        WidgetChild child = null;
        if (Widgets.getWidget(263) != null) {
            child = Widgets.getWidget(263).getChild(1);
        }
        if (child != null && child.isVisible()) {
            String text = child.getText();
            return text != null && (
                    text.contains("Getting started") ||
                    text.contains("Before you begin, have a read") ||
                    text.contains("When you're ready to get started, click on the Gielinor Guide")
            );
        }
        return false;
    }

    private boolean hasCompletedDialogues() {
        return getProgress() >= 4;
    }

    private void walkToTile(Tile tile) {
        if (tile == null) return;
        if (!Players.getLocal().isMoving() && Players.getLocal().getTile().distance(tile) > 1) {
            Walking.walk(tile);
            Sleep.sleepUntil(() -> Players.getLocal().getTile().distance(tile) <= 1 || Players.getLocal().isMoving(), Calculations.random(1200, 2400));
        }
    }

    private boolean isPlayerStuck() {
        return Players.getLocal().distance(GUIDE_TILE) > 5 && !Players.getLocal().isMoving() && !Dialogues.inDialogue() && !isTutorialChatOpen();
    }

    private void recoverPlayer() {
        Camera.rotateToYaw(Calculations.random(0, 2048));
        Camera.rotateToPitch(Calculations.random(383, 423));
        Sleep.sleep(Calculations.random(200, 400));
        walkToTile(GUIDE_TILE);
    }
}