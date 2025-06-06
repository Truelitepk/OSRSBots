package tasks.tutorial;

import antiban.AntibanManager;
import antiban.UUIDProfileCache;
import framework.Task;
import org.dreambot.api.input.Keyboard;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.widgets.WidgetChild;
import java.awt.event.KeyEvent;

public class SurvivalExpertTask implements Task {

    private final AntibanManager antiban;

    // Item constants
    private static final String FISHING_NET = "Small fishing net";
    private static final String RAW_SHRIMPS = "Raw shrimps";
    private static final String SHRIMPS = "Shrimps";
    private static final String BRONZE_AXE = "Bronze axe";
    private static final String TINDERBOX = "Tinderbox";
    private static final String LOGS = "Logs";

    // Survival Expert/door/exit widget constants
    private static final String SURVIVAL_EXPERT_NAME = "Survival Expert";
    private static final String MOVING_ON_TEXT = "Moving on";
    private static final String CONTINUE_GATE_TEXT = "all you need to do is click on the gate";
    private static final String FINAL_DIALOGUE_TEXT = "To continue the tutorial go through the gate and follow the path"; // Adapt as needed

    // Survival area bounds (adjusted to not include the tile outside the gate)
    private static final Area SURVIVAL_AREA = new Area(
        new Tile(3097, 3090, 0), // bottom left
        new Tile(3108, 3105, 0)  // top right, gate at 3108,3099
    );

    // Progress tracking
    private boolean gotFishingNet, caughtShrimp, gotAxeTinderbox, choppedTree, litFire, cookedShrimp;

    // Dialogue stuck detection
    private int dialogueStuckCounter = 0;
    private static final int DIALOGUE_STUCK_THRESHOLD = 5;

    // Track if we've handled the "open skills tab" step after catching shrimp
    private boolean hasOpenedSkillsTabAfterShrimp = false;
    private boolean hasReturnedToInventoryAfterShrimp = false;

    // Track if we already opened the gate and walked out
    private boolean hasUsedGateAfterShrimp = false;

    public SurvivalExpertTask(UUIDProfileCache profile) {
        this.antiban = new AntibanManager(profile);
    }

    public SurvivalExpertTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        updateProgressFlags();
        // Debug: Print current tile and area check
        Logger.log("[SurvivalExpertTask] Player tile: " + Players.getLocal().getTile() +
                   ", in area: " + SURVIVAL_AREA.contains(Players.getLocal()));
        // Accept only if not finished and player is still in Survival area and Cooking Instructor not present
        boolean inSurvivalArea = SURVIVAL_AREA.contains(Players.getLocal());
        boolean nearCookingInstructor = (NPCs.closest("Master Chef") != null);
        return !hasCompletedSurvivalSection() && inSurvivalArea && !nearCookingInstructor;
    }

    @Override
    public int execute() {
        antiban.tick();
        updateProgressFlags();

        // --- Handle skill tab flashing step after catching shrimp ---
        if (shouldOpenSkillsTabAfterShrimp()) {
            Logger.log("[SurvivalExpert] Opening Skills tab after catching shrimp (per tutorial prompt).");
            Tabs.open(Tab.SKILLS);
            Sleep.sleep(600, 900);
            hasOpenedSkillsTabAfterShrimp = true;
            return antiban.sleepMedium();
        }
        if (shouldReturnToInventoryTab()) {
            Logger.log("[SurvivalExpert] Returning to Inventory tab after opening Skills tab.");
            Tabs.open(Tab.INVENTORY);
            Sleep.sleep(400, 700);
            hasReturnedToInventoryAfterShrimp = true;
            return antiban.sleepMedium();
        }

        // --- Handle leaving the area after cooking shrimp ---
        if (shouldUseGateAfterCookedShrimp()) {
            Logger.log("[SurvivalExpert] Cooked shrimp in inventory, using the gate to leave area!");
            if (Tabs.isOpen(Tab.OPTIONS)) {
                Tabs.open(Tab.INVENTORY);
                Sleep.sleep(300, 500);
            }
            GameObject gate = GameObjects.closest(obj -> obj.hasAction("Open") && obj.getName().equalsIgnoreCase("Gate"));
            if (gate != null && gate.hasAction("Open")) {
                Logger.log("[SurvivalExpert] Found gate, attempting to open...");
                if (!gate.isOnScreen()) {
                    gate.interact("Open");
                    Sleep.sleepUntil(() -> !gate.exists(), 3000);
                } else {
                    gate.interact("Open");
                    Sleep.sleepUntil(() -> !gate.exists(), 3000);
                }
                hasUsedGateAfterShrimp = true;
                Logger.log("[SurvivalExpert] Clicked gate, now progressing to next instructor.");
                return antiban.sleepMedium();
            } else {
                Logger.log("[SurvivalExpert] Cannot find gate to leave area!");
            }
            return antiban.sleepShort();
        }

        // --- Handle "open skills tab" tutorial step (dialogue version) ---
        if (isSkillsTabRequiredDialogue()) {
            if (!Tabs.isOpen(Tab.SKILLS)) {
                Logger.log("[SurvivalExpert] Opening Skills tab as instructed by dialogue.");
                Tabs.open(Tab.SKILLS);
                Sleep.sleep(400, 700);
                return antiban.sleepMedium();
            } else if (Dialogues.canContinue()) {
                Logger.log("[SurvivalExpert] Skills tab is open, continuing dialogue.");
                Dialogues.continueDialogue();
                Sleep.sleep(300, 500);
                return antiban.sleepShort();
            }
        }

        if (shouldExitToGate()) {
            Logger.log("[SurvivalExpertTask] Requirements met, proceeding to open the gate and move on.");
            if (Tabs.isOpen(Tab.OPTIONS)) {
                Tabs.open(Tab.INVENTORY);
                Sleep.sleep(300, 500);
            }
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                Sleep.sleep(400, 700);
            }
            clickGateAndWalkToNextInstructor();
            return antiban.sleepShort();
        }

        if (handleFlashingTabs()) {
            dialogueStuckCounter = 0;
            return antiban.sleepShort();
        }

        boolean actionableDialogue = Dialogues.canContinue() ||
                (Dialogues.getOptions() != null && Dialogues.getOptions().length > 0);

        if (actionableDialogue) {
            Logger.log("[SurvivalExpertTask] Actionable dialogue detected, handling...");
            handleDialogue();
            dialogueStuckCounter = 0;
            return antiban.sleepShort();
        }

        if (Dialogues.inDialogue() && !actionableDialogue) {
            dialogueStuckCounter++;
            if (dialogueStuckCounter > DIALOGUE_STUCK_THRESHOLD) {
                Logger.log("[SurvivalExpertTask] Stuck in dialogue, trying to escape with ESC key...");
                Keyboard.type(KeyEvent.VK_ESCAPE);
                Sleep.sleep(300, 500);
                dialogueStuckCounter = 0;
            } else {
                Logger.log("[SurvivalExpertTask] In dialogue but no action available. Ignoring and proceeding with main logic...");
            }
        } else {
            dialogueStuckCounter = 0;
        }

        // Step 1: Get the fishing net
        if (!gotFishingNet) {
            if (!Inventory.contains(FISHING_NET)) {
                GameObject groundNet = GameObjects.closest(obj -> obj.getName().equals(FISHING_NET) && obj.hasAction("Take"));
                if (groundNet != null) {
                    Logger.log("[SurvivalExpert] Found fishing net on ground, picking up.");
                    groundNet.interact("Take");
                    Sleep.sleepUntil(() -> Inventory.contains(FISHING_NET), 3000);
                    updateProgressFlags();
                    return antiban.sleepMedium();
                }
                NPC expert = NPCs.closest(SURVIVAL_EXPERT_NAME);
                if (expert != null && expert.interact("Talk-to")) {
                    Logger.log("[SurvivalExpert] Talking to Survival Expert for Small fishing net.");
                    Sleep.sleepUntil(Dialogues::inDialogue, 2500);
                    updateProgressFlags();
                    return antiban.sleepMedium();
                }
                Logger.log("[SurvivalExpert] Waiting for net or Survival Expert to appear.");
                return antiban.sleepShort();
            } else {
                gotFishingNet = true;
            }
        }

        // Step 2: Catch shrimp using NPCs API for fishing spot
        if (gotFishingNet && !caughtShrimp) {
            if (!Inventory.contains(RAW_SHRIMPS)) {
                NPC fishingSpot = NPCs.closest(npc ->
                    npc != null &&
                    npc.getName() != null &&
                    npc.getName().toLowerCase().contains("fishing spot") &&
                    npc.hasAction("Net")
                );
                if (fishingSpot != null) {
                    Logger.log("[SurvivalExpert] Fishing spot found (NPCs API): " + fishingSpot.getName() + " Actions: " + String.join(", ", fishingSpot.getActions()));
                    if (Players.getLocal() != null && !Players.getLocal().isAnimating() && !Players.getLocal().isMoving()) {
                        boolean interacted = fishingSpot.interact("Net");
                        Logger.log("[SurvivalExpert] Tried to interact with fishing spot, result: " + interacted);
                        if (interacted) {
                            Sleep.sleepUntil(() -> Inventory.contains(RAW_SHRIMPS), 8000);
                            updateProgressFlags();
                            return antiban.sleepMedium();
                        } else {
                            Logger.log("[SurvivalExpert] Could not interact with fishing spot! Maybe not clickable right now.");
                        }
                    } else {
                        Logger.log("[SurvivalExpert] Player is busy, waiting...");
                    }
                } else {
                    Logger.log("[SurvivalExpert] No fishing spot found! (NPCs API)");
                }
                return antiban.sleepShort();
            } else {
                caughtShrimp = true;
            }
        }

        // Step 3: Get the axe and tinderbox
        if (caughtShrimp && !gotAxeTinderbox) {
            boolean hasAxe = Inventory.contains(BRONZE_AXE);
            boolean hasTinder = Inventory.contains(TINDERBOX);
            if (!hasAxe) {
                GameObject groundAxe = GameObjects.closest(obj -> obj.getName().equals(BRONZE_AXE) && obj.hasAction("Take"));
                if (groundAxe != null) {
                    groundAxe.interact("Take");
                    Sleep.sleepUntil(() -> Inventory.contains(BRONZE_AXE), 3000);
                }
                hasAxe = Inventory.contains(BRONZE_AXE);
            }
            if (!hasTinder) {
                GameObject groundTinder = GameObjects.closest(obj -> obj.getName().equals(TINDERBOX) && obj.hasAction("Take"));
                if (groundTinder != null) {
                    groundTinder.interact("Take");
                    Sleep.sleepUntil(() -> Inventory.contains(TINDERBOX), 3000);
                }
                hasTinder = Inventory.contains(TINDERBOX);
            }
            if (hasAxe && hasTinder) {
                gotAxeTinderbox = true;
                updateProgressFlags();
                return antiban.sleepMedium();
            }
            NPC expert = NPCs.closest(SURVIVAL_EXPERT_NAME);
            if (expert != null && expert.interact("Talk-to")) {
                Logger.log("[SurvivalExpert] Talking to Survival Expert for axe/tinderbox.");
                Sleep.sleepUntil(Dialogues::inDialogue, 2500);
                updateProgressFlags();
                return antiban.sleepMedium();
            }
            Logger.log("[SurvivalExpert] Waiting for axe/tinderbox or Survival Expert to appear.");
            return antiban.sleepShort();
        }

        // Step 4: Chop tree for logs
        if (gotAxeTinderbox && !choppedTree) {
            if (!Inventory.contains(LOGS)) {
                GameObject tree = GameObjects.closest(obj -> obj.hasAction("Chop down") && obj.getName().equals("Tree"));
                if (tree != null && tree.interact("Chop down")) {
                    Logger.log("[SurvivalExpert] Chopping tree for logs.");
                    Sleep.sleepUntil(() -> Inventory.contains(LOGS), 6000);
                    updateProgressFlags();
                    return antiban.sleepMedium();
                } else {
                    Logger.log("[SurvivalExpert] Waiting for tree to chop...");
                }
                return antiban.sleepShort();
            } else {
                choppedTree = true;
            }
        }

        // Step 5: Light fire with logs and tinderbox
        if (choppedTree && !litFire) {
            GameObject fire = findNearbyFire();
            if (fire == null) {
                Item tinderbox = Inventory.get(TINDERBOX);
                Item logs = Inventory.get(LOGS);
                if (tinderbox != null && logs != null) {
                    tinderbox.useOn(logs);
                    Logger.log("[SurvivalExpert] Attempting to light fire.");
                    Sleep.sleepUntil(() -> findNearbyFire() != null, 5000);
                    updateProgressFlags();
                    return antiban.sleepMedium();
                }
            } else {
                litFire = true;
            }
            return antiban.sleepShort();
        }

        // Step 6: Cook the shrimp on the fire
        if (litFire && Inventory.contains(RAW_SHRIMPS) && !cookedShrimp) {
            GameObject fire = findNearbyFire();
            Item shrimps = Inventory.get(RAW_SHRIMPS);
            if (fire != null && shrimps != null) {
                shrimps.useOn(fire);
                Logger.log("[SurvivalExpert] Cooking shrimp on the fire.");
                Sleep.sleepUntil(() -> Inventory.contains(SHRIMPS), 7000);
                cookedShrimp = Inventory.contains(SHRIMPS);
                updateProgressFlags();
                return antiban.sleepMedium();
            }
            return antiban.sleepShort();
        }

        if (hasCompletedSurvivalSection()) {
            Logger.log("[SurvivalExpert] Survival Expert section complete! Shrimp cooked and ready to move on.");
            return antiban.sleepLong();
        }

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Survival Expert Task";
    }

    private boolean shouldOpenSkillsTabAfterShrimp() {
        // Only do this once per account/session!
        return !hasOpenedSkillsTabAfterShrimp
            && !Tabs.isOpen(Tab.SKILLS)
            && Inventory.count(RAW_SHRIMPS) == 1
            && !Inventory.contains(SHRIMPS);
    }

    private boolean shouldReturnToInventoryTab() {
        // Only do this once per account/session!
        return hasOpenedSkillsTabAfterShrimp
            && !hasReturnedToInventoryAfterShrimp
            && Tabs.isOpen(Tab.SKILLS)
            && Inventory.count(RAW_SHRIMPS) == 1
            && !Inventory.contains(SHRIMPS);
    }

    private boolean shouldUseGateAfterCookedShrimp() {
        // If we just cooked shrimp and haven't already left, go use the gate
        return Inventory.contains(SHRIMPS)
            && !hasUsedGateAfterShrimp;
    }

    private boolean isSkillsTabRequiredDialogue() {
        String dialogue = Dialogues.getNPCDialogue();
        return dialogue != null && dialogue.contains("take a look at that menu before we continue");
    }

    private boolean handleFlashingTabs() {
        WidgetChild continueWidget = Widgets.get(162, 33);
        if (continueWidget != null && continueWidget.isVisible() && continueWidget.getText() != null
                && continueWidget.getText().toLowerCase().contains("click here to continue")) {
            continueWidget.interact();
            Sleep.sleep(200, 400);
            return true;
        }
        return false;
    }

    private GameObject findNearbyFire() {
        return GameObjects.closest(obj -> obj.getName().equals("Fire"));
    }

    private boolean shouldExitToGate() {
        if (!(caughtShrimp && choppedTree && litFire && cookedShrimp)) return false;
        return isMovingOnInstructionOpen() || isMovingOnDialogueOpen() || isFinalSurvivalDialogueOpen();
    }

    private boolean isMovingOnInstructionOpen() {
        WidgetChild child = null;
        if (Widgets.get(263) != null) {
            child = Widgets.get(263, 1);
        }
        if (child != null && child.isVisible()) {
            String text = child.getText();
            return text != null && text.contains(MOVING_ON_TEXT)
                && text.contains(CONTINUE_GATE_TEXT);
        }
        return false;
    }

    private boolean isMovingOnDialogueOpen() {
        return Dialogues.inDialogue()
            && Dialogues.getNPCDialogue() != null
            && Dialogues.getNPCDialogue().contains(MOVING_ON_TEXT)
            && Dialogues.getNPCDialogue().contains(CONTINUE_GATE_TEXT);
    }

    private boolean isFinalSurvivalDialogueOpen() {
        return Dialogues.inDialogue()
            && Dialogues.getNPCDialogue() != null
            && Dialogues.getNPCDialogue().contains(FINAL_DIALOGUE_TEXT);
    }

    private void clickGateAndWalkToNextInstructor() {
        GameObject gate = GameObjects.closest(obj -> obj.hasAction("Open") && obj.getName().equalsIgnoreCase("Gate"));
        if (gate != null && gate.hasAction("Open")) {
            if (!gate.isOnScreen()) {
                gate.interact("Open");
                Sleep.sleepUntil(() -> !gate.exists(), 3000);
            } else {
                gate.interact("Open");
                Sleep.sleepUntil(() -> !gate.exists(), 3000);
            }
        }
        Logger.log("[SurvivalExpert] Clicked gate, now progressing to next instructor.");
    }

    private void handleDialogue() {
        int tries = 0;
        int maxTries = 10;
        while (
            Dialogues.inDialogue() &&
            (Dialogues.canContinue() ||
                (Dialogues.getOptions() != null && Dialogues.getOptions().length > 0)) &&
            tries < maxTries
        ) {
            if (shouldExitToGate()) {
                Logger.log("[SurvivalExpertTask] Exit dialogue or widget detected inside handleDialogue. Breaking to click gate!");
                break;
            }
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                Sleep.sleep(200, 400);
            } else if (Dialogues.getOptions() != null && Dialogues.getOptions().length > 0) {
                Dialogues.chooseOption(Dialogues.getOptions()[0]);
                Sleep.sleep(200, 400);
            } else {
                Sleep.sleep(150, 300);
            }
            tries++;
        }
        if (tries >= maxTries && shouldExitToGate()) {
            Logger.log("[SurvivalExpertTask] Failsafe in dialogue handler: requirements met, forcing gate click!");
            if (Tabs.isOpen(Tab.OPTIONS)) {
                Tabs.open(Tab.INVENTORY);
                Sleep.sleep(200, 400);
            }
            clickGateAndWalkToNextInstructor();
        } else if (tries >= maxTries) {
            Logger.log("[SurvivalExpertTask] Failsafe: Steps not complete. Will NOT click gate.");
        }
    }

    private void updateProgressFlags() {
        gotFishingNet = Inventory.contains(FISHING_NET);
        caughtShrimp = Inventory.contains(RAW_SHRIMPS) || Inventory.contains(SHRIMPS);
        gotAxeTinderbox = Inventory.contains(BRONZE_AXE) && Inventory.contains(TINDERBOX);
        choppedTree = Inventory.contains(LOGS) || (litFire && !Inventory.contains(LOGS));
        litFire = findNearbyFire() != null;
        cookedShrimp = Inventory.contains(SHRIMPS);
    }

    private boolean hasCompletedSurvivalSection() {
        // Survival section is complete when shrimp is cooked and we've left the Survival area
        boolean inSurvivalArea = SURVIVAL_AREA.contains(Players.getLocal());
        boolean nearCookingInstructor = (NPCs.closest("Master Chef") != null);
        boolean completed = cookedShrimp && (!inSurvivalArea || nearCookingInstructor);
        Logger.log("[SurvivalExpertTask] Completion check: cookedShrimp=" + cookedShrimp +
                   ", inSurvivalArea=" + inSurvivalArea +
                   ", nearCookingInstructor=" + nearCookingInstructor +
                   ", completed=" + completed);
        return completed;
    }
}