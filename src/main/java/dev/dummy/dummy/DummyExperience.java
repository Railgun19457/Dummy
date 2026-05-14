package dev.dummy.dummy;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public record DummyExperience(int level, float progress, int total) {
    public static final DummyExperience ZERO = new DummyExperience(0, 0.0F, 0);

    public static DummyExperience fromPlayer(Player player) {
        return new DummyExperience(player.getLevel(), player.getExp(), player.getTotalExperience());
    }

    public static DummyExperience fromConfig(ConfigurationSection section) {
        if (section == null) {
            return ZERO;
        }
        return new DummyExperience(
                section.getInt("level", 0),
                (float) section.getDouble("progress", 0.0D),
                section.getInt("total", 0)
        );
    }

    public void apply(Player player) {
        player.setLevel(Math.max(0, level));
        player.setExp(Math.max(0.0F, Math.min(1.0F, progress)));
        player.setTotalExperience(Math.max(0, total));
    }
}
