package dev.dummy.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class DummyMenuHolder implements InventoryHolder {
    private final String dummyName;
    private Inventory inventory;

    public DummyMenuHolder(String dummyName) {
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
