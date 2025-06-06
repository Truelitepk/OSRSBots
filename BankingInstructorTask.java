package tasks.tutorial;

import antiban.AntibanManager;
import antiban.UUIDProfileCache;
import framework.Task;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.wrappers.widgets.WidgetChild;

public class BankingInstructorTask implements Task {

    private final AntibanManager antiban;
    private final Area BANK_ROOM = new Area(3120, 3124, 3125, 3119, 1);
    private final Tile POLL_BOOTH_TILE = new Tile(3122, 3124, 1);
    private final Tile BANKER_TILE = new Tile(3123, 3122, 1);
    private boolean completed = false;
    private boolean polled = false;
    private boolean banked = false;

    public BankingInstructorTask(UUIDProfileCache profile) {
        this.antiban = new AntibanManager(profile);
    }

    public BankingInstructorTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        return !completed && BANK_ROOM.contains(Players.getLocal());
    }

    @Override
    public int execute() {
        antiban.tick();
        Logger.log("[BankingInstructor] Executing Banking Instructor Task");

        if (Dialogues.inDialogue()) {
            if (Dialogues.canContinue()) {
                Dialogues.continueDialogue();
                return antiban.sleepMedium();
            }
            if (Dialogues.getOptions() != null && Dialogues.getOptions().length > 0) {
                Dialogues.chooseOption(Dialogues.getOptions()[0]);
                return antiban.sleepMedium();
            }
        }

        if (!banked) {
            if (!Bank.isOpen()) {
                NPC banker = NPCs.closest("Banker");
                if (banker != null && banker.distance() > 2) {
                    Walking.walk(BANKER_TILE);
                    return antiban.sleepMedium();
                }
                if (banker != null && banker.interact("Bank")) {
                    org.dreambot.api.utilities.Sleep.sleepUntil(Bank::isOpen, 5000L);
                    return antiban.sleepMedium();
                }
            }

            if (Bank.isOpen()) {
                Logger.log("[BankingInstructor] Depositing all items.");
                Bank.depositAllItems();
                org.dreambot.api.utilities.Sleep.sleep(800);
                Bank.close();
                banked = true;
                return antiban.sleepMedium();
            }
        }

        if (!polled) {
            if (Players.getLocal().distance(POLL_BOOTH_TILE) > 2) {
                Walking.walk(POLL_BOOTH_TILE);
                return antiban.sleepMedium();
            }
            GameObject pollBooth = GameObjects.closest(obj -> obj != null && obj.getName().equals("Poll booth"));
            if (pollBooth != null && pollBooth.interact("Use")) {
                org.dreambot.api.utilities.Sleep.sleepUntil(
                    () -> !Widgets.getAllContainingText("Poll").isEmpty(),
                    5000L
                );
                antiban.sleepMedium();
                WidgetChild closeButton = Widgets.get(310, 11);
                if (closeButton == null || !closeButton.isVisible()) {
                    closeButton = Widgets.get(595, 15);
                }
                if (closeButton != null && closeButton.isVisible()) {
                    closeButton.interact();
                    org.dreambot.api.utilities.Sleep.sleep(600);
                } else {
                    Widgets.closeAll();
                }
                polled = true;
                return antiban.sleepShort();
            }
        }

        if (banked && polled) {
            completed = true;
        }

        return antiban.sleepMedium();
    }

    @Override
    public String getName() {
        return "Banking Instructor Task";
    }
}
