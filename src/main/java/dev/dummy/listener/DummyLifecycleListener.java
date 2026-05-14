package dev.dummy.listener;

import dev.dummy.dummy.DummyManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class DummyLifecycleListener implements Listener {
    private final DummyManager dummyManager;

    public DummyLifecycleListener(DummyManager dummyManager) {
        this.dummyManager = dummyManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (dummyManager.isDummy(event.getPlayer())) {
            dummyManager.cleanup(event.getPlayer());
        }
    }
}
