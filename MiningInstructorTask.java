package tasks.tutorial;

import antiban.AntibanManager;
import data.NpcData;
import data.ObjectData;
import framework.Task;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

import java.awt.*;
import java.awt.event.KeyEvent;

public class MiningInstructorTask implements Task {

    private final AntibanManager antiban;

    private static final Tile INSTRUCTOR_TILE = new Tile(3081, 9502);
    private static final Tile GATE_TILE = new Tile(3081, 9502);

    public MiningInstructorTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        int progress = getProgress();
        return progress >= 9 && progress <= 12;
    }

    @Override
    public int execute() {
        antiban.tick();

        // Step 1: Walk to instructor area if not there yet
        if (Players.getLocal().distance(INSTRUCTOR_TILE) > 5) {
            Walking.walk(INSTRUCTOR_TILE);
            Sleep.sleepUntil(() -> Players.getLocal().distance(INSTRUCTOR_TILE) <= 3, 5000);
            return antiban.sleepMedium();
        }

        // Step 2: Talk to instructor for pickaxe (progress 9)
        if (getProgress() == 9 && !Inventory.contains("Bronze pickaxe")) {
            NPC instructor = NPCs.closest(n -> n.getID() == NpcData.getId("mining instructor"));
            if (instructor != null && instructor.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                return antiban.sleepMedium();
            }
        }

        // Step 3: Mine Tin
        if (!Inventory.contains("Tin ore")) {
            GameObject tin = GameObjects.closest(obj -> obj.getID() == ObjectData.getId("tin rock") && obj.hasAction("Mine"));
            if (tin != null && tin.interact("Mine")) {
                Logger.log("[MiningInstructor] Mining Tin ore...");
                Sleep.sleepUntil(() -> Inventory.contains("Tin ore"), 8000);
                return antiban.sleepMedium();
            }
        }

        // Step 4: Mine Copper
        if (!Inventory.contains("Copper ore")) {
            GameObject copper = GameObjects.closest(obj -> obj.getID() == ObjectData.getId("copper rock") && obj.hasAction("Mine"));
            if (copper != null && copper.interact("Mine")) {
                Logger.log("[MiningInstructor] Mining Copper ore...");
                Sleep.sleepUntil(() -> Inventory.contains("Copper ore"), 8000);
                return antiban.sleepMedium();
            }
        }

        // Step 5: Smelt bronze bar at furnace
        if (Inventory.contains("Tin ore") && Inventory.contains("Copper ore") && !Inventory.contains("Bronze bar")) {
            GameObject furnace = GameObjects.closest(obj -> obj.hasAction("Use") && obj.getName().toLowerCase().contains("furnace"));
            if (furnace != null && furnace.interact("Use")) {
                Logger.log("[MiningInstructor] Smelting bronze bar...");
                Sleep.sleepUntil(() -> Inventory.contains("Bronze bar"), 8000);
                return antiban.sleepMedium();
            }
        }

        // Step 6: Talk to instructor after smelting (progress 11)
        if (getProgress() == 11 && Inventory.contains("Bronze bar") && !Dialogues.inDialogue()) {
            NPC instructor = NPCs.closest(n -> n.getID() == NpcData.getId("mining instructor"));
            if (instructor != null && instructor.interact("Talk-to")) {
                Logger.log("[MiningInstructor] Talking to instructor after smelting...");
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                return antiban.sleepMedium();
            }
        }

        // Step 7: Smith bronze dagger (use Bronze bar on Anvil and select dagger option)
        if (Inventory.contains("Bronze bar")) {
            GameObject anvil = GameObjects.closest(obj -> obj.hasAction("Smith") && obj.getName().toLowerCase().contains("anvil"));
            if (anvil != null) {
                Logger.log("[MiningInstructor] Smithing bronze dagger...");
                Inventory.interact("Bronze bar", "Use");
                Sleep.sleep(400);
                if (anvil.interact("Use")) {
                    Sleep.sleep(1200); // Wait for smithing interface
                    pressKey1();
                    Sleep.sleepUntil(() -> Inventory.contains("Bronze dagger"), 8000);
                    return antiban.sleepMedium();
                }
            }
        }

        // Step 8: Final talk after smithing (progress 12)
        if (getProgress() == 12 && !Dialogues.inDialogue()) {
            NPC instructor = NPCs.closest(n -> n.getID() == NpcData.getId("mining instructor"));
            if (instructor != null && instructor.interact("Talk-to")) {
                Logger.log("[MiningInstructor] Final talk after smithing...");
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                return antiban.sleepMedium();
            }
        }

        // Step 9: Open gate to Combat Area (progress 12)
        if (getProgress() == 12) {
            GameObject gate = GameObjects.closest(obj -> obj.getTile().equals(GATE_TILE) && obj.hasAction("Open"));
            if (gate != null) {
                Logger.log("[MiningInstructor] Opening exit gate to combat area...");
                if (gate.interact("Open")) {
                    Sleep.sleepUntil(() -> Players.getLocal().getX() > GATE_TILE.getX(), 5000);
                    return antiban.sleepShort();
                }
            }
        }

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Mining Instructor";
    }

    private int getProgress() {
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

    /**
     * Presses the '1' key using Java's Robot class, as Keyboard is deprecated.
     */
    private void pressKey1() {
        try {
            Robot robot = new Robot();
            robot.keyPress(KeyEvent.VK_1);
            robot.keyRelease(KeyEvent.VK_1);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }
}