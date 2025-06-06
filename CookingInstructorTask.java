package tasks.tutorial;

import antiban.AntibanManager;
import data.NpcData;
import framework.Task;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.settings.PlayerSettings;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

public class CookingInstructorTask implements Task {

    private final AntibanManager antiban;
    private static final Tile COOK_TILE = new Tile(3088, 3091);
    private static final Tile GATE_TILE = new Tile(3092, 3092);
    private static final Tile ENTRANCE_DOOR_TILE = new Tile(3089, 3093);
    private static final Tile EXIT_DOOR_TILE = new Tile(3092, 3091);

    public CookingInstructorTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        int progress = getProgress();
        return (progress >= 6 && progress <= 8)
                && Players.getLocal() != null
                && Players.getLocal().getTile().distance(GATE_TILE) <= 15;
    }

    @Override
    public int execute() {
        antiban.tick();

        if (Players.getLocal().getTile().distance(GATE_TILE) <= 2 && !isPastGate()) {
            GameObject gate = GameObjects.closest(obj ->
                obj != null && obj.getName().toLowerCase().contains("gate") && obj.getTile().equals(GATE_TILE) && obj.hasAction("Open"));
            if (gate != null) {
                Logger.log("[CookingInstructor] Opening the gate to cooking area.");
                if (gate.interact("Open")) {
                    Sleep.sleepUntil(this::isPastGate, 3000);
                    return antiban.sleepMedium();
                }
            }
        }

        if (!isInsideCookingArea()) {
            if (Players.getLocal().distance(ENTRANCE_DOOR_TILE) > 2) {
                Walking.walk(ENTRANCE_DOOR_TILE);
                Sleep.sleepUntil(() -> Players.getLocal().distance(ENTRANCE_DOOR_TILE) <= 2, 4000);
                return antiban.sleepMedium();
            }
            GameObject entranceDoor = GameObjects.closest(obj ->
                obj != null && obj.getTile().equals(ENTRANCE_DOOR_TILE) && obj.hasAction("Open"));
            if (entranceDoor != null) {
                Logger.log("[CookingInstructor] Opening the entrance door to cooking hut.");
                entranceDoor.interact("Open");
                Sleep.sleepUntil(this::isInsideCookingArea, 3000);
                return antiban.sleepMedium();
            }
            return antiban.sleepShort();
        }

        int cookId = NpcData.getId("master chef");
        if (cookId != -1 && !Inventory.contains("Bread dough")) {
            NPC chef = NPCs.closest(n -> n != null && n.getID() == cookId);
            if (chef != null && chef.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                return antiban.sleepMedium();
            }
        }

        if (!Inventory.contains("Pot of flour")) {
            GameObject flour = GameObjects.closest(obj -> obj != null && obj.getName().equalsIgnoreCase("Pot of flour"));
            if (flour != null && flour.interact("Take")) {
                Sleep.sleepUntil(() -> Inventory.contains("Pot of flour"), 3000);
                return antiban.sleepMedium();
            }
        }
        if (!Inventory.contains("Bucket of water")) {
            GameObject water = GameObjects.closest(obj -> obj != null && obj.getName().equalsIgnoreCase("Bucket of water"));
            if (water != null && water.interact("Take")) {
                Sleep.sleepUntil(() -> Inventory.contains("Bucket of water"), 3000);
                return antiban.sleepMedium();
            }
        }

        if (Inventory.contains("Pot of flour") && Inventory.contains("Bucket of water") && !Inventory.contains("Bread dough")) {
            Inventory.interact("Pot of flour", "Use");
            Sleep.sleep(300);
            Inventory.interact("Bucket of water", "Use");
            Sleep.sleepUntil(() -> Inventory.contains("Bread dough"), 2000);
            return antiban.sleepMedium();
        }

        if (Inventory.contains("Bread dough")) {
            GameObject range = GameObjects.closest(obj -> obj != null && obj.getName().equalsIgnoreCase("Range"));
            if (range != null) {
                Inventory.interact("Bread dough", "Use");
                Sleep.sleep(300);
                if (range.interact("Use")) {
                    Sleep.sleepUntil(() -> !Inventory.contains("Bread dough"), 4000);
                    return antiban.sleepMedium();
                }
            }
        }

        if (!Inventory.contains("Bread dough") && !Inventory.contains("Pot of flour") && !Inventory.contains("Bucket of water")) {
            int chefId = NpcData.getId("master chef");
            NPC chef = NPCs.closest(n -> n != null && n.getID() == chefId);
            if (chef != null && chef.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                return antiban.sleepMedium();
            }
        }

        GameObject exitDoor = GameObjects.closest(obj ->
            obj != null && obj.getTile().equals(EXIT_DOOR_TILE) && obj.hasAction("Open"));
        if (exitDoor != null) {
            Logger.log("[CookingInstructor] Opening the exit door after cooking.");
            if (exitDoor.interact("Open")) {
                Sleep.sleepUntil(() -> Players.getLocal().getTile().getX() > EXIT_DOOR_TILE.getX(), 3000);
                return antiban.sleepMedium();
            }
        }

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Cooking Instructor";
    }

    private int getProgress() {
        return PlayerSettings.getConfig(281); // Tutorial Island progress varp
    }

    private void handleDialogue() {
        while (Dialogues.inDialogue()) {
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                Sleep.sleep(600);
            }
        }
    }

    private boolean isPastGate() {
        return Players.getLocal().getTile().getX() <= ENTRANCE_DOOR_TILE.getX();
    }

    private boolean isInsideCookingArea() {
        Tile tile = Players.getLocal().getTile();
        return tile.getX() <= COOK_TILE.getX() && tile.getY() <= COOK_TILE.getY() + 3 && tile.getY() >= COOK_TILE.getY() - 3;
    }
}
