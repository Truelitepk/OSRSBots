package tasks.dev;

import antiban.AntibanManager;
import data.IDAutoWriter;
import framework.Task;
import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.interactive.GameObjects; // <-- FIXED import
import org.dreambot.api.methods.interactive.NPCs;
import org.dreambot.api.utilities.Logger;
import org.dreambot.api.wrappers.items.Item;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.interactive.NPC;

import java.util.HashSet;
import java.util.Set;

public class DevIDScannerTask implements Task {

    private final AntibanManager antiban;
    private final Set<Integer> seenNpcIds = new HashSet<>();
    private final Set<Integer> seenObjectIds = new HashSet<>();
    private final Set<Integer> seenItemIds = new HashSet<>();

    public DevIDScannerTask(AntibanManager antiban) {
        this.antiban = antiban;
    }

    @Override
    public boolean accept() {
        return true; // Always on for dev
    }

    @Override
    public int execute() {
        antiban.tick();

        for (NPC npc : NPCs.all()) {
            if (npc != null && !seenNpcIds.contains(npc.getID())) {
                Logger.log("[DevScanner] NPC ID: " + npc.getID() + " - Name: " + npc.getName());
                seenNpcIds.add(npc.getID());
                IDAutoWriter.writeNpc(npc.getID(), npc.getName());
            }
        }

        for (GameObject obj : GameObjects.all()) {
            if (obj != null && !seenObjectIds.contains(obj.getID())) {
                Logger.log("[DevScanner] Object ID: " + obj.getID() + " - Name: " + obj.getName());
                seenObjectIds.add(obj.getID());
                IDAutoWriter.writeObject(obj.getID(), obj.getName());
            }
        }

        for (Item item : Inventory.all()) {
            if (item != null && !seenItemIds.contains(item.getID())) {
                Logger.log("[DevScanner] Item ID: " + item.getID() + " - Name: " + item.getName());
                seenItemIds.add(item.getID());
                IDAutoWriter.writeItem(item.getID(), item.getName());
            }
        }

        return 300; // sleep time in ms
    }

    @Override
    public String getName() {
        return "Dev ID Scanner";
    }
}