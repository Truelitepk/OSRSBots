package tasks.tutorial;

import antiban.AntibanManager;
import framework.Task;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.magic.Magic;
import org.dreambot.api.methods.magic.Normal;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.map.Tile;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.NPC;

import java.util.Random;

public class MagicInstructorTask implements Task {

    private static final Area MAGIC_ROOM = new Area(3137, 3127, 3142, 3135, 1);
    private static final Tile INSTRUCTOR_TILE = new Tile(3141, 3125, 1);
    private static final String MAGIC_INSTRUCTOR = "Magic Instructor";
    private static final String CHICKEN = "Chicken";
    private static final String MIND_RUNE = "Mind rune";
    private static final String AIR_RUNE = "Air rune";

    private final AntibanManager antiban;
    private boolean magicTabOpened = false;
    private boolean hasRunes = false;
    private boolean hasCastSpell = false;
    private boolean readyToLeave = false;
    private int castCount = 0;
    private final int targetCasts = new Random().nextInt(2) + 1; // 1 or 2

    public MagicInstructorTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        return MAGIC_ROOM.contains(Players.getLocal());
    }

    @Override
    public int execute() {
        antiban.tick();

        // Step 1: Walk to magic instructor if not there
        if (!MAGIC_ROOM.contains(Players.getLocal())) {
            Walking.walk(INSTRUCTOR_TILE);
            return antiban.sleepMedium();
        }

        // Step 2: Talk to Magic Instructor to unlock Magic tab
        if (!magicTabOpened) {
            if (Dialogues.inDialogue()) {
                Dialogues.continueDialogue();
                // Try opening the Magic tab if unlocked
                if (!Tabs.isOpen(Tab.MAGIC)) {
                    Tabs.open(Tab.MAGIC);
                    magicTabOpened = true;
                    Sleep.sleep(600, 900);
                }
                return antiban.sleepMedium();
            }
            NPC instructor = NPCs.closest(MAGIC_INSTRUCTOR);
            if (instructor != null && instructor.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 3000);
                return antiban.sleepMedium();
            }
            return antiban.sleepShort();
        }

        // Step 3: After opening tab, get runes from instructor
        if (!hasRunes) {
            if (Inventory.contains(MIND_RUNE) && Inventory.contains(AIR_RUNE)) {
                hasRunes = true;
            } else {
                if (Dialogues.inDialogue()) {
                    Dialogues.continueDialogue();
                    return antiban.sleepMedium();
                }
                NPC instructor = NPCs.closest(MAGIC_INSTRUCTOR);
                if (instructor != null && instructor.interact("Talk-to")) {
                    Sleep.sleepUntil(Dialogues::inDialogue, 3000);
                    return antiban.sleepMedium();
                }
                return antiban.sleepShort();
            }
        }

        // Step 4: Cast Air Strike on a chicken 1 or 2 times (random)
        if (!hasCastSpell) {
            if (castCount < targetCasts && Magic.canCast(Normal.WIND_STRIKE)) {
                NPC chicken = NPCs.closest(CHICKEN);
                if (chicken != null && Magic.castSpell(Normal.WIND_STRIKE) && chicken.interact("Cast")) {
                    Logger.log("[MagicInstructor] Casting Air Strike on a chicken.");
                    castCount++;
                    Sleep.sleep(2000 + antiban.getReactionDelay());
                    if (castCount >= targetCasts) {
                        hasCastSpell = true;
                    }
                    return antiban.sleepMedium();
                }
                // Walk closer to chickens if none are nearby
                if (chicken == null) {
                    Tile chickenTile = new Tile(3141, 3128, 1);
                    Walking.walk(chickenTile);
                    return antiban.sleepMedium();
                }
            } else {
                hasCastSpell = true;
            }
        }

        // Step 5: Final dialogue with instructor (ironman & mainland)
        if (!readyToLeave) {
            if (Dialogues.inDialogue()) {
                if (Dialogues.canContinue()) {
                    Dialogues.continueDialogue();
                } else if (Dialogues.getOptions() != null) {
                    for (String option : Dialogues.getOptions()) {
                        if (option.toLowerCase().contains("no")) {
                            Dialogues.chooseOption(option);
                        } else if (option.toLowerCase().contains("yes")) {
                            Dialogues.chooseOption(option);
                            readyToLeave = true;
                        }
                    }
                }
                return antiban.sleepMedium();
            }
            NPC instructor = NPCs.closest(MAGIC_INSTRUCTOR);
            if (instructor != null && instructor.interact("Talk-to")) {
                Sleep.sleepUntil(Dialogues::inDialogue, 3000);
                return antiban.sleepMedium();
            }
            return antiban.sleepShort();
        }

        // All done, ready to leave tutorial island!
        Logger.log("[MagicInstructorTask] Finished Magic Instructor section!");
        return antiban.sleepLong();
    }

    @Override
    public String getName() {
        return "Magic Instructor Task";
    }
}