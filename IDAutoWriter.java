package data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class IDAutoWriter {

    private static final File NPC_FILE = new File(System.getProperty("user.home") + "/DreamBot/BotData/NpcData.txt");
    private static final File OBJECT_FILE = new File(System.getProperty("user.home") + "/DreamBot/BotData/ObjectData.txt");
    private static final File ITEM_FILE = new File(System.getProperty("user.home") + "/DreamBot/BotData/ItemData.txt");

    private static final Set<Integer> writtenNpcIds = new HashSet<>();
    private static final Set<Integer> writtenObjectIds = new HashSet<>();
    private static final Set<Integer> writtenItemIds = new HashSet<>();

    public static void writeNpc(int id, String name) {
        if (id <= 0 || writtenNpcIds.contains(id)) return;
        writeToFile(NPC_FILE, id, name);
        writtenNpcIds.add(id);
    }

    public static void writeObject(int id, String name) {
        if (id <= 0 || writtenObjectIds.contains(id)) return;
        writeToFile(OBJECT_FILE, id, name);
        writtenObjectIds.add(id);
    }

    public static void writeItem(int id, String name) {
        if (id <= 0 || writtenItemIds.contains(id)) return;
        writeToFile(ITEM_FILE, id, name);
        writtenItemIds.add(id);
    }

    private static void writeToFile(File file, int id, String name) {
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
            writer.write(id + " = " + name);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            System.err.println("[IDWriter] Error writing to file: " + file.getName());
            e.printStackTrace();
        }
    }
}
