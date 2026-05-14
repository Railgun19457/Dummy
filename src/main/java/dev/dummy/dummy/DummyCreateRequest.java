package dev.dummy.dummy;

import java.util.UUID;
import org.bukkit.Location;

public record DummyCreateRequest(UUID uuid, String name, Location location, DummySettings settings, DummySkin skin) {
}
