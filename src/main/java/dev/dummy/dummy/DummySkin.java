package dev.dummy.dummy;

import dev.dummy.i18n.LocalizedException;
import org.bukkit.configuration.ConfigurationSection;

public record DummySkin(String type, String value, String signature) {
    public static final DummySkin NONE = new DummySkin("none", "", "");

    public static DummySkin texture(String value, String signature) {
        if (value == null || value.isBlank()) {
            throw new LocalizedException("error.skin-value-blank");
        }
        return new DummySkin("texture", value, signature == null ? "" : signature);
    }

    public static DummySkin player(String playerName, String value, String signature) {
        DummySkin texture = texture(value, signature);
        return new DummySkin("player:" + playerName, texture.value(), texture.signature());
    }

    public static DummySkin fromConfig(ConfigurationSection section) {
        if (section == null) {
            return NONE;
        }
        return new DummySkin(
                section.getString("type", "none"),
                section.getString("value", ""),
                section.getString("signature", "")
        );
    }

    public boolean hasTexture() {
        return !value.isBlank();
    }
}
