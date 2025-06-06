package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ObjectData {

    private static final File OBJECT_FILE = new File(System.getProperty("user.home") + "/DreamBot/BotData/ObjectData.txt");
    private static final Map<String, Integer> objectMap = new HashMap<>();

    static {
        loadObjectData();
    }

    private static void loadObjectData() {
        if (!OBJECT_FILE.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(OBJECT_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" = ", 2);
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[0].trim());
                    String name = parts[1].trim().toLowerCase();
                    objectMap.put(name, id);
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[ObjectData] Failed to load ObjectData.txt");
            e.printStackTrace();
        }
    }

    public static int getId(String name) {
        return objectMap.getOrDefault(name.toLowerCase(), -1);
    }

    public static boolean contains(String name) {
        return objectMap.containsKey(name.toLowerCase());
    }
}
