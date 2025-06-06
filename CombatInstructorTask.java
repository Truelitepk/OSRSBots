package tasks.tutorial;

import antiban.AntibanManager;
import antiban.UUIDProfileCache;
import data.NpcData;
import framework.Task;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.equipment.Equipment;
import org.dreambot.api.methods.container.impl.equipment.EquipmentSlot;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.settings.PlayerSettings;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.wrappers.widgets.WidgetChild;

public class CombatInstructorTask implements Task {

    private final AntibanManager antiban;
    private static final Tile COMBAT_AREA_TILE = new Tile(3110, 9518);
    private static final Tile GATE_TILE = new Tile(3111, 9514);
    private boolean completed = false;

    public CombatInstructorTask(UUIDProfileCache profile) {
        this.antiban = new AntibanManager(profile);
    }

    public CombatInstructorTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        int progress = getProgress();
        return !completed && progress >= 13 && progress <= 17;
    }

    @Override
    public int execute() {
        antiban.tick();
        Logger.log("[CombatInstructor] Executing Combat Instructor Task");

        if (Players.getLocal() == null) return antiban.sleepShort();

        if (Players.getLocal().distance(COMBAT_AREA_TILE) > 5) {
            Walking.walk(COMBAT_AREA_TILE);
            Sleep.sleepUntil(() -> Players.getLocal().distance(COMBAT_AREA_TILE) <= 2, 5000);
            return antiban.sleepMedium();
        }

        if ((getProgress() == 13 || getProgress() == 14) && !Inventory.contains("Bronze dagger") && !Equipment.contains("Bronze dagger")) {
            NPC instructor = NPCs.closest(n -> n != null && n.getID() == NpcData.getId("combat instructor"));
            if (instructor != null && instructor.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                return antiban.sleepMedium();
            }
        }

        if ((getProgress() == 13 || getProgress() == 14) && Inventory.contains("Bronze dagger") && !Equipment.isOpen()) {
            Logger.log("[CombatInstructor] Opening equipment tab for dagger equip.");
            Equipment.open();
            Sleep.sleep(600);
            return antiban.sleepShort();
        }

        if ((getProgress() == 13 || getProgress() == 14) && Equipment.isOpen() && Inventory.contains("Bronze dagger") && !Equipment.contains("Bronze dagger")) {
            Logger.log("[CombatInstructor] Equipping Bronze dagger via equipment API.");
            if (Equipment.equip(EquipmentSlot.WEAPON, "Bronze dagger")) {
                Sleep.sleepUntil(() -> Equipment.contains("Bronze dagger"), 2000);
            }
            return antiban.sleepShort();
        }

        if ((getProgress() == 13 || getProgress() == 14) && Equipment.isOpen() && Equipment.contains("Bronze dagger")) {
            WidgetChild closeButton = Widgets.get(387, 3);
            if (closeButton != null && closeButton.isVisible()) {
                closeButton.interact();
                Sleep.sleep(600);
            } else {
                Tabs.open(Tab.INVENTORY);
            }
            return antiban.sleepShort();
        }

        if (getProgress() == 14 && (!Inventory.contains("Bronze sword") || !Inventory.contains("Wooden shield"))) {
            NPC instructor = NPCs.closest(n -> n != null && n.getID() == NpcData.getId("combat instructor"));
            if (instructor != null && instructor.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                return antiban.sleepMedium();
            }
        }

        if (getProgress() == 14 && Inventory.contains("Bronze sword") && Inventory.contains("Wooden shield") &&
            (!Equipment.contains("Bronze sword") || !Equipment.contains("Wooden shield"))) {
            Equipment.equip(EquipmentSlot.WEAPON, "Bronze sword");
            antiban.sleepShort();
            Equipment.equip(EquipmentSlot.SHIELD, "Wooden shield");
            Sleep.sleepUntil(() -> Equipment.contains("Bronze sword") && Equipment.contains("Wooden shield"), 2000);
            return antiban.sleepMedium();
        }

        if (getProgress() == 15 && Players.getLocal().getY() < 9516) {
            GameObject gate = GameObjects.closest(obj -> obj != null && obj.getTile().equals(GATE_TILE) && obj.hasAction("Open"));
            if (gate != null && gate.interact("Open")) {
                Sleep.sleepUntil(() -> Players.getLocal().getY() >= 9516, 3000);
                return antiban.sleepMedium();
            }
        }

        if (getProgress() == 15 && Players.getLocal().getY() >= 9516) {
            NPC rat = NPCs.closest(n -> n != null && n.getName().equalsIgnoreCase("Giant rat")
                    && !n.isInCombat() && n.getTile().getY() >= 9516);
            if (rat != null && rat.interact("Attack")) {
                Sleep.sleepUntil(() -> rat.isInCombat() || Players.getLocal().isInCombat(), 5000);
                return antiban.sleepMedium();
            }
        }

        if (getProgress() == 15 && Players.getLocal().getY() >= 9516) {
            GameObject gate = GameObjects.closest(obj -> obj != null && obj.getTile().equals(GATE_TILE) && obj.hasAction("Open"));
            if (gate != null && gate.interact("Open")) {
                Sleep.sleepUntil(() -> Players.getLocal().getY() < 9516, 3000);
                return antiban.sleepMedium();
            }
        }

        if (getProgress() == 16 && (!Inventory.contains("Shortbow") || !Inventory.contains("Bronze arrow"))) {
            NPC instructor = NPCs.closest(n -> n != null && n.getID() == NpcData.getId("combat instructor"));
            if (instructor != null && instructor.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 5000);
                handleDialogue();
                return antiban.sleepMedium();
            }
        }

        if (getProgress() == 16 && Inventory.contains("Shortbow") && Inventory.contains("Bronze arrow") &&
                (!Equipment.contains("Shortbow") || !Equipment.contains("Bronze arrow"))) {
            Equipment.equip(EquipmentSlot.WEAPON, "Shortbow");
            antiban.sleepShort();
            Equipment.equip(EquipmentSlot.ARROWS, "Bronze arrow");
            Sleep.sleepUntil(() -> Equipment.contains("Shortbow") && Equipment.contains("Bronze arrow"), 2000);
            return antiban.sleepMedium();
        }

        if (getProgress() == 17) {
            NPC rat = NPCs.closest(n -> n != null && n.getName().equalsIgnoreCase("Giant rat")
                    && !n.isInCombat() && n.getTile().getY() < 9516);
            if (rat != null && rat.interact("Attack")) {
                Sleep.sleepUntil(() -> rat.isInCombat() || Players.getLocal().isInCombat(), 5000);
                return antiban.sleepMedium();
            }
        }

        if (getProgress() == 17) {
            GameObject ladder = GameObjects.closest(obj -> obj != null && obj.hasAction("Climb-up"));
            if (ladder != null && ladder.interact("Climb-up")) {
                Sleep.sleepUntil(() -> Players.getLocal().getZ() == 1, 5000);
                completed = true;
                return antiban.sleepMedium();
            }
        }

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Combat Instructor";
    }

    private int getProgress() {
        return PlayerSettings.getConfig(281);
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
