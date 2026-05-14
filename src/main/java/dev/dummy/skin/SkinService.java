package dev.dummy.skin;

import com.destroystokyo.paper.profile.ProfileProperty;
import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummySkin;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SkinService {
    private final DummyPlugin plugin;
    private final File file;
    private final Map<String, DummySkin> cache = new ConcurrentHashMap<>();

    public SkinService(DummyPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "skins.yml");
        load();
    }

    public CompletableFuture<DummySkin> fetchPlayerSkin(String playerName) {
        String key = normalize(playerName);
        DummySkin cached = cache.get(key);
        if (cached != null && cached.hasTexture()) {
            return CompletableFuture.completedFuture(cached);
        }

        return Bukkit.createProfile(playerName).update().thenApply(profile -> {
            ProfileProperty property = profile.getProperties()
                    .stream()
                    .filter(candidate -> candidate.getName().equals("textures"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No skin texture found for player: " + playerName));
            DummySkin skin = DummySkin.player(playerName, property.getValue(), property.getSignature());
            cache.put(key, skin);
            save();
            return skin;
        });
    }

    private void load() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("skins");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            cache.put(key, DummySkin.fromConfig(root.getConfigurationSection(key)));
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("skins");
        for (Map.Entry<String, DummySkin> entry : cache.entrySet()) {
            DummySkin skin = entry.getValue();
            ConfigurationSection section = root.createSection(entry.getKey());
            section.set("type", skin.type());
            section.set("value", skin.value());
            section.set("signature", skin.signature());
        }
        try {
            config.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Failed to save skins.yml", ex);
        }
    }

    private String normalize(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }
}
