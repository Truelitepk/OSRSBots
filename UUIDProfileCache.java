package antiban;

import java.io.*;
import java.util.UUID;

/**
 * Handles per-bot UUID profile caching and persistence.
 * This version robustly ensures that the EliteBotData folder is created automatically
 * at the project root if it does not exist.
 */
public class UUIDProfileCache implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID uuid;
    private final String botName;
    private final int baseMouseSpeed;
    private final int misclickChance;
    private final int afkTendency;
    private final int cameraActivity;

    public UUIDProfileCache(String botName) {
        this.uuid = UUID.randomUUID();
        this.botName = botName;

        // Behavior profile generated per bot identity
        this.baseMouseSpeed = 80 + (int)(Math.random() * 60);       // 80–140
        this.misclickChance = 1 + (int)(Math.random() * 5);         // 1–5%
        this.afkTendency = 1000 + (int)(Math.random() * 3000);      // Delay between AFKs
        this.cameraActivity = 100 + (int)(Math.random() * 400);     // Chance threshold
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getBotName() {
        return botName;
    }

    public int getBaseMouseSpeed() {
        return baseMouseSpeed;
    }

    public int getMisclickChance() {
        return misclickChance;
    }

    public int getAfkTendency() {
        return afkTendency;
    }

    public int getCameraActivity() {
        return cameraActivity;
    }

    public static void saveProfile(UUIDProfileCache profile) {
        File file = new File(getPath(profile.botName));
        // Ensure the parent directory exists
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                System.out.println("[UUIDProfileCache] Created directory: " + parentDir.getAbsolutePath());
            } else {
                System.err.println("[UUIDProfileCache] Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(profile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static UUIDProfileCache loadOrCreate(String botName) {
        File profileFile = new File(getPath(botName));
        // Ensure the parent directory exists before loading or creating
        File parentDir = profileFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (parentDir.mkdirs()) {
                System.out.println("[UUIDProfileCache] Created directory: " + parentDir.getAbsolutePath());
            } else {
                System.err.println("[UUIDProfileCache] Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }
        if (profileFile.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(profileFile))) {
                return (UUIDProfileCache) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        UUIDProfileCache profile = new UUIDProfileCache(botName);
        saveProfile(profile);
        return profile;
    }

    private static String getPath(String botName) {
        return "EliteBotData/" + botName + "_uuid_profile.ser";
    }
}