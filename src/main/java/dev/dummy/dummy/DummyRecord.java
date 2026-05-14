package dev.dummy.dummy;

import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public record DummyRecord(
        UUID uuid,
        UUID creatorUuid,
        String creatorName,
        String name,
        Location location,
        DummySettings settings,
        DummySkin skin,
        ItemStack[] storageContents,
        ItemStack[] armorContents,
        ItemStack offhandItem,
        DummyExperience experience
) {
}
