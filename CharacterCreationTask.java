package tasks.tutorial;

import antiban.AntibanManager;
import framework.Task;
import namegen.VeteranNameGenerator;
import org.dreambot.api.input.Mouse;
import org.dreambot.api.methods.dialogues.Dialogues;
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.methods.tabs.Tab;
import org.dreambot.api.methods.tabs.Tabs;
import org.dreambot.api.methods.widget.Widgets;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.widgets.WidgetChild;
import org.dreambot.api.wrappers.interactive.NPC;
import org.dreambot.api.methods.input.Keyboard;

import java.awt.*;
import java.util.Random;

public class CharacterCreationTask implements Task {

    private final AntibanManager antiban;
    private boolean customized = false;
    private boolean nameSet = false;
    private String chosenName = null;

    public CharacterCreationTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        // Only run if the customization UI or name selection is open
        return isCustomizationInterfaceOpen() || isNameSelectionOpen();
    }

    @Override
    @SuppressWarnings("deprecation")
    public int execute() {
        antiban.tick();
        Logger.log("[CharacterCreation] Executing Character Creation Task");

        // Step 1: Handle name selection if open
        if (isNameSelectionOpen() && !nameSet) {
            Logger.log("[CharacterCreation] Name selection interface is open.");
            chosenName = VeteranNameGenerator.generate();
            Logger.log("[CharacterCreation] Generated name: " + chosenName);

            WidgetChild nameInput = Widgets.get(NAME_WIDGET_PARENT, NAME_WIDGET_INPUT);
            if (nameInput != null && nameInput.isVisible()) {
                nameInput.interact();
                Sleep.sleep(400, 600);
                Keyboard.type(chosenName); // Deprecated but necessary for DreamBot
                Sleep.sleep(600, 1000);
            }

            WidgetChild submitButton = Widgets.get(NAME_WIDGET_PARENT, NAME_WIDGET_SUBMIT);
            if (submitButton != null && submitButton.isVisible()) {
                submitButton.interact();
                Logger.log("[CharacterCreation] Submitted name.");
                Sleep.sleepUntil(() -> !isNameSelectionOpen(), 5000);
                nameSet = true;
                return antiban.sleepMedium();
            } else {
                Logger.log("[CharacterCreation] Submit button not found!");
                return antiban.sleepShort();
            }
        }

        // Step 2: Handle appearance customization
        if (!customized && isCustomizationInterfaceOpen()) {
            Logger.log("[CharacterCreation] Customization interface open.");
            randomizeAppearance();
            confirmAppearance();
            customized = true;
            return antiban.sleepMedium();
        }

        // Step 3: Continue dialogue if in dialogue
        if (Dialogues.inDialogue()) {
            Dialogues.continueDialogue();
            return antiban.sleepMedium();
        }

        // Step 4: Interact with Gielinor Guide if possible
        NPC guide = NPCs.closest("Gielinor Guide");
        if (guide != null && guide.interact("Talk-to")) {
            Sleep.sleepUntil(Dialogues::inDialogue, 5000);
            return antiban.sleepMedium();
        }

        // Step 5: Open inventory tab (if not already)
        if (!Tabs.isOpen(Tab.INVENTORY)) {
            Tabs.open(Tab.INVENTORY);
        }

        // Step 6: Move mouse off screen for antiban
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int offScreenX = (new Random().nextBoolean()) ? -20 : (int) screenSize.getWidth() + 20;
        int offScreenY = (new Random().nextBoolean()) ? -20 : (int) screenSize.getHeight() + 20;
        Mouse.move(new Point(offScreenX, offScreenY));

        return antiban.sleepShort();
    }

    @Override
    public String getName() {
        return "Character Creation Task";
    }

    // --- Name Selection Widgets (Update if DreamBot or RS interface changes) ---
    private static final int NAME_WIDGET_PARENT = 558; // OSRS name entry widget parent
    private static final int NAME_WIDGET_INPUT  = 12;  // Input field for username
    private static final int NAME_WIDGET_SUBMIT = 14;  // Confirm button

    private boolean isNameSelectionOpen() {
        WidgetChild parent = Widgets.get(NAME_WIDGET_PARENT, -1);
        return parent != null && parent.isVisible();
    }

    private boolean isCustomizationInterfaceOpen() {
        WidgetChild confirm = Widgets.get(269, 97); // Confirm button for customization
        return confirm != null && confirm.isVisible();
    }

    private void randomizeAppearance() {
        Random rand = new Random(); // Always a new, unpredictable appearance

        for (int i = 38; i <= 48; i += 2) { // Cycle appearance options
            int spins = rand.nextInt(5) + 1;
            WidgetChild option = Widgets.get(269, i);
            if (option != null && option.isVisible()) {
                for (int j = 0; j < spins; j++) {
                    option.interact("Change look");
                    antiban.sleepShort();
                }
            }
        }
        Logger.log("[CharacterCreation] Randomized appearance (fully unique each time).");
    }

    private void confirmAppearance() {
        WidgetChild confirm = Widgets.get(269, 97); // Confirm button
        if (confirm != null && confirm.isVisible()) {
            confirm.interact("Confirm");
            Logger.log("[CharacterCreation] Confirmed appearance.");
        }
    }
}