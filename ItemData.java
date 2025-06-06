package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ItemData {

    private static final File ITEM_FILE = new File(System.getProperty("user.home") + "/DreamBot/BotData/ItemData.txt");
    private static final Map<String, Integer> itemMap = new HashMap<>();

    static {
        loadItemData();
    }

    private static void loadItemData() {
        if (!ITEM_FILE.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(ITEM_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" = ", 2);
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim().toLowerCase();
                    itemMap.put(name, id);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[ItemData] Failed to load ItemData.txt");
            e.printStackTrace();
        }
    }

    public static int getId(String name) {
        return itemMap.getOrDefault(name.toLowerCase(), -1);
    }

    public static boolean contains(String name) {
        return itemMap.containsKey(name.toLowerCase());
    }
}
