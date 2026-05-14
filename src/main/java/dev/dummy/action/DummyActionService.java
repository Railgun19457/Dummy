package dev.dummy.action;

import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummyInstance;
import dev.dummy.dummy.DummyManager;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public final class DummyActionService {
    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final Map<UUID, Map<String, BukkitTask>> tasks = new LinkedHashMap<>();

    public DummyActionService(DummyPlugin plugin, DummyManager dummyManager) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
    }

    public void run(DummyInstance dummy, String action, String[] args, boolean repeat, int intervalTicks, int durationTicks) {
        String normalized = normalize(action);
        if (normalized.equals("stop")) {
            stop(dummy, args.length == 0 ? null : args[0]);
            return;
        }

        if (!repeat) {
            perform(dummy, normalized, args);
            dummyManager.save();
            return;
        }

        stop(dummy, normalized);
        int interval = Math.max(1, intervalTicks);
        int[] elapsed = {0};
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!dummyManager.isDummy(dummy.player())) {
                stop(dummy, normalized);
                return;
            }
            perform(dummy, normalized, args);
            elapsed[0] += interval;
            if (durationTicks > 0 && elapsed[0] >= durationTicks) {
                stop(dummy, normalized);
            }
        }, 0L, interval);
        tasks.computeIfAbsent(dummy.uuid(), ignored -> new LinkedHashMap<>()).put(normalized, task);
    }

    public int stop(DummyInstance dummy, String action) {
        Map<String, BukkitTask> dummyTasks = tasks.get(dummy.uuid());
        if (dummyTasks == null || dummyTasks.isEmpty()) {
            return 0;
        }
        if (action == null || action.isBlank()) {
            int size = dummyTasks.size();
            dummyTasks.values().forEach(BukkitTask::cancel);
            dummyTasks.clear();
            return size;
        }

        BukkitTask task = dummyTasks.remove(normalize(action));
        if (task == null) {
            return 0;
        }
        task.cancel();
        return 1;
    }

    public static String[] tail(String[] args, int from) {
        if (from >= args.length) {
            return new String[0];
        }
        return Arrays.copyOfRange(args, from, args.length);
    }

    private void perform(DummyInstance dummy, String action, String[] args) {
        Player player = dummy.player();
        switch (action) {
            case "attack" -> attack(player);
            case "chat" -> player.chat(join(args));
            case "command" -> player.performCommand(stripSlash(join(args)));
            case "drop" -> player.dropItem(true);
            case "hold" -> hold(player, args);
            case "jump" -> jump(player);
            case "look" -> look(player, args);
            case "mine" -> mine(player);
            case "mount" -> mount(player);
            case "move" -> move(player, args);
            case "sleep" -> sleep(player);
            case "sneak" -> player.setSneaking(parseToggle(args, true));
            case "sprint" -> player.setSprinting(parseToggle(args, true));
            case "swap" -> swapHands(player);
            case "use" -> use(player);
            case "wakeup" -> player.wakeup(false);
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private void attack(Player player) {
        Entity target = player.getTargetEntity(4, true);
        if (target == null) {
            target = nearestEntity(player, 4.0D);
        }
        if (target == null || target.equals(player)) {
            throw new IllegalArgumentException("No target entity in range");
        }
        player.attack(target);
        player.swingMainHand();
    }

    private void hold(Player player, String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("hold requires a hotbar slot 0-8");
        }
        int slot = Integer.parseInt(args[0]);
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("hold slot must be 0-8");
        }
        player.getInventory().setHeldItemSlot(slot);
    }

    private void jump(Player player) {
        Vector velocity = player.getVelocity();
        velocity.setY(Math.max(velocity.getY(), 0.42D));
        player.setVelocity(velocity);
        player.setJumping(true);
    }

    private void look(Player player, String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("look requires yaw and pitch");
        }
        player.setRotation(Float.parseFloat(args[0]), Float.parseFloat(args[1]));
    }

    private void mine(Player player) {
        Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
        if (block == null || block.getType() == Material.AIR) {
            throw new IllegalArgumentException("No target block in range");
        }
        player.swingMainHand();
        player.breakBlock(block);
    }

    private void mount(Player player) {
        Entity target = nearestEntity(player, 3.0D);
        if (target == null) {
            throw new IllegalArgumentException("No mountable entity in range");
        }
        target.addPassenger(player);
    }

    private void move(Player player, String[] args) {
        double speed = args.length == 0 ? 0.25D : Double.parseDouble(args[0]);
        Vector direction = player.getLocation().getDirection().setY(0.0D);
        if (direction.lengthSquared() == 0.0D) {
            return;
        }
        player.setVelocity(direction.normalize().multiply(speed).setY(player.getVelocity().getY()));
    }

    private void sleep(Player player) {
        Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
        Location location = block == null ? player.getLocation() : block.getLocation();
        if (!player.sleep(location, true)) {
            throw new IllegalArgumentException("Dummy cannot sleep at target location");
        }
    }

    private void swapHands(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack offhand = player.getInventory().getItemInOffHand();
        player.getInventory().setItemInMainHand(offhand);
        player.getInventory().setItemInOffHand(main);
    }

    private void use(Player player) {
        player.swingMainHand();
        player.startUsingItem(EquipmentSlot.HAND);
    }

    private Entity nearestEntity(Player player, double range) {
        return player.getNearbyEntities(range, range, range)
                .stream()
                .filter(entity -> entity instanceof LivingEntity)
                .filter(entity -> !entity.equals(player))
                .min((left, right) -> Double.compare(
                        left.getLocation().distanceSquared(player.getLocation()),
                        right.getLocation().distanceSquared(player.getLocation())
                ))
                .orElse(null);
    }

    private boolean parseToggle(String[] args, boolean fallback) {
        if (args.length == 0) {
            return fallback;
        }
        return Boolean.parseBoolean(args[0]);
    }

    private String join(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Action requires additional arguments");
        }
        return String.join(" ", args);
    }

    private String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    private String normalize(String action) {
        return action.toLowerCase(Locale.ROOT);
    }
}
