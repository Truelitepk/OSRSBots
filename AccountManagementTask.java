package tasks.tutorial;

import antiban.AntibanManager;
import antiban.UUIDProfileCache;
import framework.Task;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

public class AccountManagementTask implements Task {

    private final AntibanManager antiban;
    private final Area ACCOUNT_ROOM = new Area(3125, 3129, 3130, 3123, 1);
    private final Tile DOOR_TILE = new Tile(3125, 3123, 1);
    private final Tile PRAYER_ROOM_TILE = new Tile(3130, 3131, 1);
    private boolean completed = false;
    private boolean talkedOnce = false;
    private boolean tabClicked = false;
    private boolean talkedAfterTab = false;

    public AccountManagementTask(UUIDProfileCache profile) {
        this.antiban = new AntibanManager(profile);
    }

    public AccountManagementTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        return !completed && ACCOUNT_ROOM.contains(Players.getLocal());
    }

    @Override
    public int execute() {
        antiban.tick();
        Logger.log("[AccountManager] Executing Account Management Task");

        if (!ACCOUNT_ROOM.contains(Players.getLocal())) {
            GameObject door = GameObjects.closest(obj ->
                obj != null && obj.getName().equals("Door") && obj.hasAction("Open"));
            if (door != null && door.interact("Open")) {
                Sleep.sleepUntil(() -> ACCOUNT_ROOM.contains(Players.getLocal()), 5000);
                return antiban.sleepMedium();
            }
            Walking.walk(DOOR_TILE);
            return antiban.sleepMedium();
        }

        if (!talkedOnce) {
            NPC guide = NPCs.closest("Account Guide");
            if (guide != null && guide.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                talkedOnce = true;
                return antiban.sleepMedium();
            }
        }

        if (talkedOnce && !tabClicked) {
            if (!Tabs.isOpen(Tab.ACCOUNT_MANAGEMENT)) {
                Tabs.open(Tab.ACCOUNT_MANAGEMENT);
                Sleep.sleep(800);
                tabClicked = true;
                return antiban.sleepMedium();
            }
        }

        if (tabClicked && !talkedAfterTab) {
            NPC guide = NPCs.closest("Account Guide");
            if (guide != null && guide.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                talkedAfterTab = true;
                return antiban.sleepMedium();
            }
        }

        if (talkedAfterTab) {
            GameObject door = GameObjects.closest(obj ->
                obj != null && obj.getName().equals("Door") && obj.hasAction("Open"));
            if (door != null && door.interact("Open")) {
                Sleep.sleepUntil(() -> PRAYER_ROOM_TILE.distance(Players.getLocal()) < 5, 5000);
                completed = true;
                return antiban.sleepShort();
            }
            Walking.walk(PRAYER_ROOM_TILE);
            return antiban.sleepShort();
        }

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Account Management Task";
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
