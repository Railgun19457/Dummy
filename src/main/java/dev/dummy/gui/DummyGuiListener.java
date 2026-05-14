package dev.dummy.gui;

import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummyInstance;
import dev.dummy.dummy.DummyManager;
import dev.dummy.dummy.DummySettings;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class DummyGuiListener implements Listener {
    private final DummyPlugin plugin;
    private final DummyManager dummyManager;

    public DummyGuiListener(DummyPlugin plugin, DummyManager dummyManager) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player target) || !dummyManager.isDummy(target)) {
            return;
        }
        event.setCancelled(true);
        DummyInstance dummy = dummyManager.get(target.getUniqueId());
        if (dummy != null) {
            openMenu(event.getPlayer(), dummy);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof DummyMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        DummyInstance dummy = dummyManager.get(holder.dummyName());
        if (dummy == null) {
            player.closeInventory();
            return;
        }

        switch (event.getRawSlot()) {
            case 10 -> Bukkit.getScheduler().runTask(plugin, () -> player.openInventory(dummy.player().getInventory()));
            case 12 -> toggle(player, dummy, "invulnerable", dummy.settings().invulnerable());
            case 13 -> toggle(player, dummy, "collision", dummy.settings().collision());
            case 14 -> toggle(player, dummy, "chunk-loader", dummy.settings().chunkLoader());
            case 15 -> toggle(player, dummy, "show-in-tab", dummy.settings().showInTab());
            case 16 -> toggle(player, dummy, "ghost", dummy.settings().ghost());
            default -> {
            }
        }
    }

    private void toggle(Player viewer, DummyInstance dummy, String key, boolean currentValue) {
        dummyManager.updateSettings(dummy.name(), key, Boolean.toString(!currentValue));
        openMenu(viewer, dummyManager.require(dummy.name()));
    }

    private void openMenu(Player viewer, DummyInstance dummy) {
        DummyMenuHolder holder = new DummyMenuHolder(dummy.name());
        Inventory inventory = Bukkit.createInventory(holder, 27, "Dummy: " + dummy.name());
        holder.inventory(inventory);
        DummySettings settings = dummy.settings();
        inventory.setItem(10, item(Material.CHEST, "Inventory", "Open inventory and equipment"));
        inventory.setItem(12, toggleItem("Invulnerable", settings.invulnerable()));
        inventory.setItem(13, toggleItem("Collision", settings.collision()));
        inventory.setItem(14, toggleItem("Chunk Loader", settings.chunkLoader()));
        inventory.setItem(15, toggleItem("Show In Tab", settings.showInTab()));
        inventory.setItem(16, toggleItem("Ghost", settings.ghost()));
        viewer.openInventory(inventory);
    }

    private ItemStack toggleItem(String name, boolean enabled) {
        return item(enabled ? Material.LIME_DYE : Material.GRAY_DYE, name + ": " + enabled, "Click to toggle");
    }

    private ItemStack item(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(java.util.List.of(Component.text(lore)));
        item.setItemMeta(meta);
        return item;
    }
}
