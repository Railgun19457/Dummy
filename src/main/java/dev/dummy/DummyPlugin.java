package dev.dummy;

import dev.dummy.action.DummyActionService;
import dev.dummy.command.DummyCommand;
import dev.dummy.dummy.DummyManager;
import dev.dummy.dummy.DummyStorage;
import dev.dummy.gui.DummyGuiListener;
import dev.dummy.listener.DummyLifecycleListener;
import dev.dummy.nms.FakePlayerAdapter;
import dev.dummy.nms.paper.PaperFakePlayerAdapter;
import dev.dummy.skin.SkinService;
import java.util.List;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DummyPlugin extends JavaPlugin {
    private DummyManager dummyManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        String minecraftVersion = Bukkit.getMinecraftVersion();
        if (!isSupported(minecraftVersion)) {
            getComponentLogger().warn(Component.text(
                    "Unsupported Paper version " + minecraftVersion + "; first target supports 1.21.11 and 26.1.2.",
                    NamedTextColor.YELLOW
            ));
        }

        try {
            FakePlayerAdapter adapter = new PaperFakePlayerAdapter(this);
            DummyStorage storage = new DummyStorage(this);
            this.dummyManager = new DummyManager(this, adapter, storage);
        } catch (RuntimeException ex) {
            getLogger().log(Level.SEVERE, "Failed to initialize Dummy NMS adapter", ex);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        SkinService skinService = new SkinService(this);
        DummyActionService actionService = new DummyActionService(this, dummyManager);
        DummyCommand dummyCommand = new DummyCommand(this, dummyManager, skinService, actionService);
        registerCommand("dummy", "Manage dummy players.", List.of("dummies"), dummyCommand);
        Bukkit.getPluginManager().registerEvents(new DummyLifecycleListener(dummyManager), this);
        Bukkit.getPluginManager().registerEvents(new DummyGuiListener(this, dummyManager), this);

        if (getConfig().getBoolean("storage.restore-on-startup", true)) {
            Bukkit.getScheduler().runTask(this, () -> dummyManager.restoreSavedDummies());
        }
    }

    @Override
    public void onDisable() {
        if (dummyManager != null) {
            dummyManager.shutdown("plugin disabled");
        }
    }

    public void reloadDummyConfig() {
        reloadConfig();
    }

    private boolean isSupported(String minecraftVersion) {
        List<String> supported = getConfig().getStringList("supported-versions");
        return supported.isEmpty() || supported.contains(minecraftVersion);
    }
}
