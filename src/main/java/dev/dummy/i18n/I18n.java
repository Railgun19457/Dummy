package dev.dummy.i18n;

import dev.dummy.DummyPlugin;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.file.YamlConfiguration;

public final class I18n {
    public static final String DEFAULT_LANGUAGE = "en_US";

    private final DummyPlugin plugin;
    private YamlConfiguration fallbackMessages;
    private YamlConfiguration activeMessages;
    private String activeLanguage = DEFAULT_LANGUAGE;

    public I18n(DummyPlugin plugin) {
        this.plugin = plugin;
    }

    public void saveDefaults() {
        plugin.saveResource("lang/en_US.yml", false);
        plugin.saveResource("lang/zh_CN.yml", false);
    }

    public void reload() {
        this.fallbackMessages = loadResource(DEFAULT_LANGUAGE);
        this.activeLanguage = normalizeLanguage(plugin.getConfig().getString("language", DEFAULT_LANGUAGE));
        this.activeMessages = loadFileOrResource(activeLanguage);
    }

    public String language() {
        return activeLanguage;
    }

    public String tr(String key, Object... args) {
        String value = activeMessages.getString(key);
        if (value == null) {
            value = fallbackMessages.getString(key, key);
        }
        return format(value, args);
    }

    public Component component(String key, NamedTextColor color, Object... args) {
        return Component.text(tr(key, args), color);
    }

    private YamlConfiguration loadFileOrResource(String language) {
        File file = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (file.isFile()) {
            return YamlConfiguration.loadConfiguration(file);
        }
        return loadResource(language);
    }

    private YamlConfiguration loadResource(String language) {
        String path = "lang/" + language + ".yml";
        InputStream stream = plugin.getResource(path);
        if (stream == null) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }

    private String normalizeLanguage(String rawLanguage) {
        String normalized = rawLanguage == null ? DEFAULT_LANGUAGE : rawLanguage.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "zh", "zh_cn", "zh-cn", "chinese" -> "zh_CN";
            case "en", "en_us", "en-us", "english" -> "en_US";
            default -> DEFAULT_LANGUAGE;
        };
    }

    private String format(String value, Object... args) {
        String result = value;
        for (int i = 0; i < args.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return result;
    }
}
