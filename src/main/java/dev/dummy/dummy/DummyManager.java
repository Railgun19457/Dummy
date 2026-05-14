package dev.dummy.dummy;

import dev.dummy.DummyPlugin;
import dev.dummy.nms.DummyHandle;
import dev.dummy.nms.FakePlayerAdapter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DummyManager {
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");

    private final DummyPlugin plugin;
    private final FakePlayerAdapter adapter;
    private final DummyStorage storage;
    private final Map<String, DummyInstance> dummiesByName = new LinkedHashMap<>();
    private final Map<UUID, DummyInstance> dummiesByUuid = new LinkedHashMap<>();
    private final Map<UUID, ChunkTicket> chunkTickets = new LinkedHashMap<>();
    private boolean shuttingDown;

    public DummyManager(DummyPlugin plugin, FakePlayerAdapter adapter, DummyStorage storage) {
        this.plugin = plugin;
        this.adapter = adapter;
        this.storage = storage;
    }

    public DummyInstance spawn(CommandSender sender, String name, Location location) {
        return spawn(sender, uuidForName(name), name, location, DummySettings.defaults(plugin.getConfig()), DummySkin.NONE, true);
    }

    public void restoreSavedDummies() {
        for (DummyRecord record : storage.load()) {
            if (contains(record.name())) {
                continue;
            }
            try {
                DummyInstance dummy = spawn(Bukkit.getConsoleSender(), record.uuid(), record.name(), record.location(), record.settings(), record.skin(), false);
                dummy.applyRecord(record);
            } catch (RuntimeException ex) {
                plugin.getLogger().warning("Failed to restore dummy '" + record.name() + "': " + ex.getMessage());
            }
        }
        save();
    }

    public boolean remove(String name, String reason) {
        DummyInstance dummy = dummiesByName.remove(normalize(name));
        if (dummy == null) {
            return false;
        }
        releaseChunkTicket(dummy);
        dummiesByUuid.remove(dummy.uuid());
        dummy.handle().remove(Component.text("[Dummy] " + reason));
        save();
        return true;
    }

    public int removeAll(String reason) {
        List<DummyInstance> snapshot = new ArrayList<>(dummiesByName.values());
        dummiesByName.clear();
        dummiesByUuid.clear();
        for (DummyInstance dummy : snapshot) {
            releaseChunkTicket(dummy);
            dummy.handle().remove(Component.text("[Dummy] " + reason));
        }
        save();
        return snapshot.size();
    }

    public void shutdown(String reason) {
        shuttingDown = true;
        save();
        List<DummyInstance> snapshot = new ArrayList<>(dummiesByName.values());
        for (DummyInstance dummy : snapshot) {
            releaseChunkTicket(dummy);
            dummy.handle().remove(Component.text("[Dummy] " + reason));
        }
        dummiesByName.clear();
        dummiesByUuid.clear();
        shuttingDown = false;
    }

    public void cleanup(Player player) {
        DummyInstance dummy = dummiesByUuid.remove(player.getUniqueId());
        if (dummy == null) {
            return;
        }
        releaseChunkTicket(dummy);
        dummiesByName.remove(normalize(dummy.name()));
        if (!shuttingDown) {
            save();
        }
    }

    public Collection<DummyInstance> all() {
        return dummiesByName.values()
                .stream()
                .sorted(Comparator.comparing(DummyInstance::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<String> names() {
        return all().stream().map(DummyInstance::name).toList();
    }

    public DummyInstance get(String name) {
        return dummiesByName.get(normalize(name));
    }

    public DummyInstance get(UUID uuid) {
        return dummiesByUuid.get(uuid);
    }

    public boolean contains(String name) {
        return dummiesByName.containsKey(normalize(name));
    }

    public boolean isDummy(Player player) {
        return dummiesByUuid.containsKey(player.getUniqueId());
    }

    public void save() {
        storage.save(dummiesByName.values());
    }

    public DummySettings updateSettings(String name, String key, String value) {
        DummyInstance dummy = require(name);
        DummySettings settings = dummy.settings().with(key, value);
        dummy.settings(settings);
        dummy.handle().applySettings(dummy.name(), settings);
        updateChunkTicket(dummy);
        save();
        return settings;
    }

    public void setSkin(String name, DummySkin skin) {
        DummyInstance dummy = require(name);
        dummy.skin(skin);
        dummy.handle().applySkin(skin);
        save();
    }

    public void teleportDummy(String name, Location location) {
        DummyInstance dummy = require(name);
        dummy.handle().teleport(location);
        updateChunkTicket(dummy);
        save();
    }

    public void teleportPlayerToDummy(Player player, String name) {
        DummyInstance dummy = require(name);
        player.teleport(dummy.location());
    }

    public void swap(Player player, String name) {
        DummyInstance dummy = require(name);
        Location playerLocation = player.getLocation();
        Location dummyLocation = dummy.location();
        player.teleport(dummyLocation);
        dummy.handle().teleport(playerLocation);
        updateChunkTicket(dummy);
        save();
    }

    public int transferExperience(String name, Player receiver, boolean all, int amount) {
        DummyInstance dummy = require(name);
        Player source = dummy.player();
        int available = Math.max(0, source.getTotalExperience());
        int transferred = all ? available : Math.min(Math.max(0, amount), available);
        if (transferred <= 0) {
            return 0;
        }
        source.setLevel(0);
        source.setExp(0.0F);
        source.setTotalExperience(0);
        source.giveExp(available - transferred, false);
        receiver.giveExp(transferred, true);
        save();
        return transferred;
    }

    public DummyInstance require(String name) {
        DummyInstance dummy = get(name);
        if (dummy == null) {
            throw new IllegalArgumentException("Dummy not found: " + name);
        }
        return dummy;
    }

    private DummyInstance spawn(
            CommandSender sender,
            UUID uuid,
            String name,
            Location location,
            DummySettings settings,
            DummySkin skin,
            boolean save
    ) {
        validateName(name);
        if (contains(name)) {
            throw new IllegalArgumentException("Dummy '" + name + "' already exists");
        }
        if (Bukkit.getPlayerExact(name) != null) {
            throw new IllegalArgumentException("A real player named '" + name + "' is already online");
        }
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("Spawn location has no world");
        }
        DummyCreateRequest request = new DummyCreateRequest(uuid, name, location.clone(), settings, skin);
        DummyHandle handle = adapter.spawn(request);
        DummyInstance dummy = new DummyInstance(uuid, name, settings, skin, handle);
        dummiesByName.put(normalize(name), dummy);
        dummiesByUuid.put(uuid, dummy);
        updateChunkTicket(dummy);
        if (sender != null) {
            plugin.getLogger().info(sender.getName() + " spawned dummy " + name + " at " + formatLocation(location));
        }
        if (save) {
            save();
        }
        return dummy;
    }

    private void updateChunkTicket(DummyInstance dummy) {
        releaseChunkTicket(dummy);
        if (!dummy.settings().chunkLoader() || dummy.settings().ghost()) {
            return;
        }
        Location location = dummy.location();
        World world = location.getWorld();
        Chunk chunk = location.getChunk();
        world.addPluginChunkTicket(chunk.getX(), chunk.getZ(), plugin);
        chunkTickets.put(dummy.uuid(), new ChunkTicket(world, chunk.getX(), chunk.getZ()));
    }

    private void releaseChunkTicket(DummyInstance dummy) {
        ChunkTicket ticket = chunkTickets.remove(dummy.uuid());
        if (ticket != null) {
            ticket.world().removePluginChunkTicket(ticket.x(), ticket.z(), plugin);
        }
    }

    private void validateName(String name) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Dummy name must be 3-16 characters and contain only letters, numbers, or underscore");
        }
    }

    private String formatLocation(Location location) {
        return "%s %.1f %.1f %.1f".formatted(
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ()
        );
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public static UUID uuidForName(String name) {
        return UUID.nameUUIDFromBytes(("dummy:" + name.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }

    private record ChunkTicket(World world, int x, int z) {
    }
}
