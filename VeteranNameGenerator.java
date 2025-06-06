package namegen;

import java.util.Random;

public class VeteranNameGenerator {

    private static final String[] PREFIXES = {
            "Afk", "PK", "W0rld", "L33t", "Tank", "Mage", "Str", "Range",
            "Iron", "Zerk", "Chad", "Josh", "Pure", "Boss", "Noob", "Dead"
    };

    private static final String[] SUFFIXES = {
            "B0t", "PvM", "Main", "Xx", "99", "G0d", "z", "Hopz", "Alt", "2Stronk", "YT", "LOL", "Lag"
    };

    private static final char[] LEET_CHARS = { '0', '1', '3', '4', '5', '7', '9' };

    private static final Random random = new Random();

    public static String generate() {
        String prefix = maybeLeetspeak(PREFIXES[random.nextInt(PREFIXES.length)]);
        String suffix = maybeLeetspeak(SUFFIXES[random.nextInt(SUFFIXES.length)]);

        // 50% chance to append a number
        if (random.nextBoolean()) {
            suffix += random.nextInt(999);
        }

        String name = prefix + suffix;

        // Ensure max 12 characters
        return name.length() > 12 ? name.substring(0, 12) : name;
    }

    private static String maybeLeetspeak(String input) {
        StringBuilder result = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (random.nextInt(4) == 0) { // 25% chance to convert
                result.append(randomLeetChar());
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    private static char randomLeetChar() {
        return LEET_CHARS[random.nextInt(LEET_CHARS.length)];
    }
}
