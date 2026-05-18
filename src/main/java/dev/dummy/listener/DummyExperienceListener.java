package dev.dummy.listener;

import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummyManager;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DummyExperienceListener implements Listener {
    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final Map<UUID, Integer> lastMendTickByDummy = new java.util.LinkedHashMap<>();

    public DummyExperienceListener(DummyPlugin plugin, DummyManager dummyManager) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerItemMend(PlayerItemMendEvent event) {
        Player player = event.getPlayer();
        if (dummyManager.isDummy(player)) {
            lastMendTickByDummy.put(player.getUniqueId(), player.getTicksLived());
            if (!event.isCancelled() && event.getRepairAmount() > 0) {
                plugin.getServer().getScheduler().runTask(plugin, dummyManager::save);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        if (!dummyManager.isDummy(player) || event.getAmount() <= 0 || !(event.getSource() instanceof ExperienceOrb)) {
            return;
        }
        if (lastMendTickByDummy.getOrDefault(player.getUniqueId(), -1) == player.getTicksLived()) {
            return;
        }

        int remaining = player.applyMending(event.getAmount());
        if (remaining != event.getAmount()) {
            event.setAmount(remaining);
            plugin.getServer().getScheduler().runTask(plugin, dummyManager::save);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastMendTickByDummy.remove(event.getPlayer().getUniqueId());
    }
}
