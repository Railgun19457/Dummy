package dev.dummy.command;

import dev.dummy.DummyPlugin;
import dev.dummy.action.DummyActionService;
import dev.dummy.dummy.DummyInstance;
import dev.dummy.dummy.DummyManager;
import dev.dummy.dummy.DummySkin;
import dev.dummy.skin.SkinService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class DummyCommand implements BasicCommand {
    private static final List<String> SUBCOMMANDS = List.of(
            "spawn", "remove", "list", "reload", "config", "skin", "exp", "inv", "tpto", "tphere", "tps", "actions"
    );
    private static final List<String> CONFIG_KEYS = List.of("invulnerable", "collision", "ghost", "chunk-loader", "show-in-tab", "name-format");
    private static final List<String> ACTIONS = List.of(
            "attack", "chat", "command", "drop", "hold", "jump", "look", "mine", "mount", "move", "sleep", "sneak", "sprint", "swap", "use", "wakeup", "stop"
    );

    private final DummyPlugin plugin;
    private final DummyManager dummyManager;
    private final SkinService skinService;
    private final DummyActionService actionService;

    public DummyCommand(DummyPlugin plugin, DummyManager dummyManager, SkinService skinService, DummyActionService actionService) {
        this.plugin = plugin;
        this.dummyManager = dummyManager;
        this.skinService = skinService;
        this.actionService = actionService;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        String subcommand = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (subcommand) {
                case "spawn" -> spawn(sender, args);
                case "remove" -> remove(sender, args);
                case "list" -> list(sender);
                case "reload" -> reload(sender);
                case "config" -> config(sender, args);
                case "skin" -> skin(sender, args);
                case "exp" -> exp(sender, args);
                case "inv" -> inv(sender, args);
                case "tpto" -> tpto(sender, args);
                case "tphere" -> tphere(sender, args);
                case "tps" -> tps(sender, args);
                case "actions" -> actions(sender, args);
                default -> sendUsage(sender);
            }
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Command failed: " + ex.getMessage());
            sender.sendMessage(Component.text("Command failed: " + ex.getMessage(), NamedTextColor.RED));
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        if (args.length == 0) {
            return SUBCOMMANDS;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("remove")) {
            return removeSuggestions("");
        }
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            return removeSuggestions(args[1]);
        }
        if (args.length == 2 && needsDummyName(args[0])) {
            return filter(dummyManager.names(), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("config")) {
            return filter(CONFIG_KEYS, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("config") && !args[2].equalsIgnoreCase("name-format")) {
            return filter(List.of("true", "false"), args[3]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("skin")) {
            return filter(List.of("player", "texture", "clear"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("exp")) {
            return filter(List.of("all"), args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("actions")) {
            return filter(ACTIONS, args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("actions") && !args[2].equalsIgnoreCase("stop")) {
            return filter(List.of("once", "repeat"), args[3]);
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "dummy.command";
    }

    private void spawn(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.spawn");
        if (!(sender instanceof Player player)) {
            throw new IllegalArgumentException("Only players can spawn a dummy at the current location for now");
        }
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /dummy spawn <name>", NamedTextColor.YELLOW));
            return;
        }
        DummyInstance dummy = dummyManager.spawn(sender, args[1], player.getLocation());
        sender.sendMessage(Component.text("Spawned dummy " + dummy.name(), NamedTextColor.GREEN));
    }

    private void remove(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.remove");
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /dummy remove <name|all>", NamedTextColor.YELLOW));
            return;
        }
        if (args[1].equalsIgnoreCase("all")) {
            int removed = dummyManager.removeAll("removed by " + sender.getName());
            sender.sendMessage(Component.text("Removed " + removed + " dummy player(s)", NamedTextColor.GREEN));
            return;
        }
        boolean removed = dummyManager.remove(args[1], "removed by " + sender.getName());
        sender.sendMessage(Component.text(
                removed ? "Removed dummy " + args[1] : "Dummy not found: " + args[1],
                removed ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
    }

    private void list(CommandSender sender) {
        requirePermission(sender, "dummy.command.list");
        List<String> names = dummyManager.names();
        if (names.isEmpty()) {
            sender.sendMessage(Component.text("No dummy players are currently spawned", NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text("Dummy players: " + String.join(", ", names), NamedTextColor.GREEN));
    }

    private void reload(CommandSender sender) {
        requirePermission(sender, "dummy.command.reload");
        plugin.reloadDummyConfig();
        dummyManager.save();
        sender.sendMessage(Component.text("Dummy configuration reloaded", NamedTextColor.GREEN));
    }

    private void config(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.config");
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /dummy config <name> <key> <value>", NamedTextColor.YELLOW));
            return;
        }
        String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        dummyManager.updateSettings(args[1], args[2].toLowerCase(Locale.ROOT), value);
        sender.sendMessage(Component.text("Updated " + args[1] + " config " + args[2] + " = " + value, NamedTextColor.GREEN));
    }

    private void skin(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.skin");
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /dummy skin <name> <player|texture|clear> ...", NamedTextColor.YELLOW));
            return;
        }
        dummyManager.require(args[1]);
        switch (args[2].toLowerCase(Locale.ROOT)) {
            case "clear" -> {
                dummyManager.setSkin(args[1], DummySkin.NONE);
                sender.sendMessage(Component.text("Cleared skin for " + args[1], NamedTextColor.GREEN));
            }
            case "texture" -> {
                if (args.length < 4) {
                    throw new IllegalArgumentException("Usage: /dummy skin <name> texture <value> [signature]");
                }
                dummyManager.setSkin(args[1], DummySkin.texture(args[3], args.length >= 5 ? args[4] : ""));
                sender.sendMessage(Component.text("Updated skin texture for " + args[1], NamedTextColor.GREEN));
            }
            case "player" -> {
                if (args.length < 4) {
                    throw new IllegalArgumentException("Usage: /dummy skin <name> player <playerName>");
                }
                sender.sendMessage(Component.text("Fetching skin for " + args[3] + "...", NamedTextColor.GRAY));
                skinService.fetchPlayerSkin(args[3]).whenComplete((skin, throwable) -> plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (throwable != null) {
                        sender.sendMessage(Component.text("Failed to fetch skin: " + throwable.getMessage(), NamedTextColor.RED));
                        return;
                    }
                    dummyManager.setSkin(args[1], skin);
                    sender.sendMessage(Component.text("Updated skin for " + args[1] + " from " + args[3], NamedTextColor.GREEN));
                }));
            }
            default -> throw new IllegalArgumentException("Unknown skin mode: " + args[2]);
        }
    }

    private void exp(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.exp");
        Player player = requirePlayer(sender);
        if (args.length != 3) {
            sender.sendMessage(Component.text("Usage: /dummy exp <name> <amount|all>", NamedTextColor.YELLOW));
            return;
        }
        boolean all = args[2].equalsIgnoreCase("all");
        int amount = all ? 0 : Integer.parseInt(args[2]);
        int transferred = dummyManager.transferExperience(args[1], player, all, amount);
        sender.sendMessage(Component.text("Transferred " + transferred + " experience point(s)", NamedTextColor.GREEN));
    }

    private void inv(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.inv");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /dummy inv <name>", NamedTextColor.YELLOW));
            return;
        }
        player.openInventory(dummyManager.require(args[1]).player().getInventory());
    }

    private void tpto(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.tpto");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /dummy tpto <name>", NamedTextColor.YELLOW));
            return;
        }
        dummyManager.teleportPlayerToDummy(player, args[1]);
        sender.sendMessage(Component.text("Teleported to " + args[1], NamedTextColor.GREEN));
    }

    private void tphere(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.tphere");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /dummy tphere <name>", NamedTextColor.YELLOW));
            return;
        }
        dummyManager.teleportDummy(args[1], player.getLocation());
        sender.sendMessage(Component.text("Teleported " + args[1] + " to you", NamedTextColor.GREEN));
    }

    private void tps(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.tps");
        Player player = requirePlayer(sender);
        if (args.length != 2) {
            sender.sendMessage(Component.text("Usage: /dummy tps <name>", NamedTextColor.YELLOW));
            return;
        }
        dummyManager.swap(player, args[1]);
        sender.sendMessage(Component.text("Swapped positions with " + args[1], NamedTextColor.GREEN));
    }

    private void actions(CommandSender sender, String[] args) {
        requirePermission(sender, "dummy.command.actions");
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /dummy actions <name> <action> [once|repeat] ...", NamedTextColor.YELLOW));
            return;
        }
        DummyInstance dummy = dummyManager.require(args[1]);
        String action = args[2].toLowerCase(Locale.ROOT);
        if (action.equals("stop")) {
            int stopped = actionService.stop(dummy, args.length >= 4 ? args[3] : null);
            sender.sendMessage(Component.text("Stopped " + stopped + " action(s)", NamedTextColor.GREEN));
            return;
        }

        boolean repeat = false;
        int interval = 20;
        int duration = -1;
        String[] actionArgs = DummyActionService.tail(args, 3);
        if (args.length >= 4 && args[3].equalsIgnoreCase("once")) {
            actionArgs = DummyActionService.tail(args, 4);
        } else if (args.length >= 5 && args[3].equalsIgnoreCase("repeat")) {
            repeat = true;
            interval = Integer.parseInt(args[4]);
            int start = 5;
            if (args.length >= 6 && isInteger(args[5])) {
                duration = Integer.parseInt(args[5]);
                start = 6;
            }
            actionArgs = DummyActionService.tail(args, start);
        }
        actionService.run(dummy, action, actionArgs, repeat, interval, duration);
        sender.sendMessage(Component.text("Started action " + action + " for " + dummy.name(), NamedTextColor.GREEN));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /dummy <spawn|remove|list|reload|config|skin|exp|inv|tpto|tphere|tps|actions>", NamedTextColor.YELLOW));
    }

    private void requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            throw new IllegalArgumentException("You do not have permission: " + permission);
        }
    }

    private List<String> filter(List<String> values, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized))
                .toList();
    }

    private List<String> removeSuggestions(String prefix) {
        List<String> names = new ArrayList<>(dummyManager.names());
        names.add("all");
        return filter(names, prefix);
    }

    private boolean needsDummyName(String subcommand) {
        return List.of("remove", "config", "skin", "exp", "inv", "tpto", "tphere", "tps", "actions").contains(subcommand.toLowerCase(Locale.ROOT));
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        throw new IllegalArgumentException("Only players can use this command");
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
