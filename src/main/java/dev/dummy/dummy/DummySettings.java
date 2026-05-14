package dev.dummy.dummy;

import org.bukkit.configuration.ConfigurationSection;

public record DummySettings(
        boolean invulnerable,
        boolean collision,
        boolean ghost,
        boolean chunkLoader,
        boolean showInTab,
        String nameFormat
) {
    public static DummySettings fromConfig(ConfigurationSection section) {
        return new DummySettings(
                section.getBoolean("invulnerable", false),
                section.getBoolean("collision", true),
                section.getBoolean("ghost", false),
                section.getBoolean("chunk-loader", false),
                section.getBoolean("show-in-tab", true),
                section.getString("name-format", "%name%")
        );
    }

    public static DummySettings defaults(ConfigurationSection root) {
        ConfigurationSection section = root.getConfigurationSection("defaults");
        if (section == null) {
            return new DummySettings(false, true, false, false, true, "%name%");
        }
        return fromConfig(section);
    }

    public DummySettings with(String key, String rawValue) {
        return switch (key) {
            case "invulnerable" -> new DummySettings(parseBoolean(key, rawValue), collision, ghost, chunkLoader, showInTab, nameFormat);
            case "collision" -> new DummySettings(invulnerable, parseBoolean(key, rawValue), ghost, chunkLoader, showInTab, nameFormat);
            case "ghost" -> new DummySettings(invulnerable, collision, parseBoolean(key, rawValue), chunkLoader, showInTab, nameFormat);
            case "chunk-loader" -> new DummySettings(invulnerable, collision, ghost, parseBoolean(key, rawValue), showInTab, nameFormat);
            case "show-in-tab" -> new DummySettings(invulnerable, collision, ghost, chunkLoader, parseBoolean(key, rawValue), nameFormat);
            case "name-format" -> new DummySettings(invulnerable, collision, ghost, chunkLoader, showInTab, rawValue);
            default -> throw new IllegalArgumentException("Unknown config key: " + key);
        };
    }

    public String displayName(String name) {
        return nameFormat.replace("%name%", name);
    }

    private static boolean parseBoolean(String key, String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
            return false;
        }
        throw new IllegalArgumentException(key + " must be true or false");
    }
}
