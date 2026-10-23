package dev.dummy.dummy;

import dev.dummy.i18n.LocalizedException;
import org.bukkit.configuration.ConfigurationSection;

public record DummySettings(
        boolean invulnerable,
        boolean collision,
        boolean physics,
        boolean spawning,
        boolean chunkLoader,
        int chunkRadius,
        boolean showInTab,
        String nameFormat
) {
    public static final int MIN_CHUNK_RADIUS = 0;
    public static final int MAX_CHUNK_RADIUS = 32;

    public static DummySettings defaults() {
        return new DummySettings(false, true, true, false, true, MIN_CHUNK_RADIUS, false, "%name%");
    }

    public static DummySettings fromConfig(ConfigurationSection section) {
        DummySettings fallback = defaults();
        return new DummySettings(
                section.getBoolean("invulnerable", fallback.invulnerable()),
                section.getBoolean("collision", fallback.collision()),
                section.getBoolean("physics", fallback.physics()),
                section.getBoolean("spawning", fallback.spawning()),
                section.getBoolean("chunk-loader", fallback.chunkLoader()),
                clampRadius(section.getInt("chunk-radius", fallback.chunkRadius())),
                section.getBoolean("show-in-tab", fallback.showInTab()),
                section.getString("name-format", fallback.nameFormat())
        );
    }

    public static DummySettings defaults(ConfigurationSection root) {
        ConfigurationSection section = root.getConfigurationSection("defaults");
        return section == null ? defaults() : fromConfig(section);
    }

    public DummySettings with(String key, String rawValue) {
        return switch (key) {
            case "invulnerable" -> new DummySettings(parseBoolean(key, rawValue), collision, physics, spawning, chunkLoader, chunkRadius, showInTab, nameFormat);
            case "collision" -> new DummySettings(invulnerable, parseBoolean(key, rawValue), physics, spawning, chunkLoader, chunkRadius, showInTab, nameFormat);
            case "physics" -> new DummySettings(invulnerable, collision, parseBoolean(key, rawValue), spawning, chunkLoader, chunkRadius, showInTab, nameFormat);
            case "spawning" -> new DummySettings(invulnerable, collision, physics, parseBoolean(key, rawValue), chunkLoader, chunkRadius, showInTab, nameFormat);
            case "chunk-loader" -> new DummySettings(invulnerable, collision, physics, spawning, parseBoolean(key, rawValue), chunkRadius, showInTab, nameFormat);
            case "chunk-radius" -> new DummySettings(invulnerable, collision, physics, spawning, chunkLoader, parseRadius(rawValue), showInTab, nameFormat);
            case "show-in-tab" -> new DummySettings(invulnerable, collision, physics, spawning, chunkLoader, chunkRadius, parseBoolean(key, rawValue), nameFormat);
            case "name-format" -> new DummySettings(invulnerable, collision, physics, spawning, chunkLoader, chunkRadius, showInTab, rawValue);
            default -> throw new LocalizedException("error.unknown-config-key", key);
        };
    }

    public DummySettings withChunkRadius(int radius) {
        return new DummySettings(invulnerable, collision, physics, spawning, chunkLoader, clampRadius(radius), showInTab, nameFormat);
    }

    /**
     * 区块加载生效时的实际半径；{@code chunk-loader} 关闭时返回 0（仅假人所在区块）。
     */
    public int effectiveChunkRadius() {
        return chunkLoader ? chunkRadius : 0;
    }

    public String displayName(String name) {
        return nameFormat.replace("%name%", name);
    }

    private static int parseRadius(String rawValue) {
        int radius;
        try {
            radius = Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ignored) {
            throw new LocalizedException("error.invalid-number", rawValue);
        }
        if (radius < MIN_CHUNK_RADIUS || radius > MAX_CHUNK_RADIUS) {
            throw new LocalizedException("error.chunk-radius-range", MIN_CHUNK_RADIUS, MAX_CHUNK_RADIUS);
        }
        return radius;
    }

    private static int clampRadius(int radius) {
        return Math.max(MIN_CHUNK_RADIUS, Math.min(MAX_CHUNK_RADIUS, radius));
    }

    private static boolean parseBoolean(String key, String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
            return false;
        }
        throw new LocalizedException("error.boolean-required", key);
    }
}
