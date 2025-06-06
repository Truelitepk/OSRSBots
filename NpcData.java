package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class NpcData {

    private static final File NPC_FILE = new File(System.getProperty("user.home") + "/DreamBot/BotData/NpcData.txt");
    private static final Map<String, Integer> npcMap = new HashMap<>();

    static {
        loadNpcData();
    }

    private static void loadNpcData() {
        if (!NPC_FILE.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(NPC_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" = ", 2);
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim().toLowerCase();
                    npcMap.put(name, id);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[NpcData] Failed to load NpcData.txt");
            e.printStackTrace();
        }
    }

    public static int getId(String name) {
        return npcMap.getOrDefault(name.toLowerCase(), -1);
    }

    public static boolean contains(String name) {
        return npcMap.containsKey(name.toLowerCase());
    }
}
