package dev.dummy.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class DummyInventoryHolder implements InventoryHolder {
    private final String dummyName;
    private Inventory inventory;

    public DummyInventoryHolder(String dummyName) {
        this.dummyName = dummyName;
    }

    public String dummyName() {
        return dummyName;
    }

    public void inventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
