package dev.dummy.nms.paper;

import com.destroystokyo.paper.profile.ProfileProperty;
import dev.dummy.dummy.DummySettings;
import dev.dummy.dummy.DummySkin;
import dev.dummy.nms.DummyHandle;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

public final class PaperDummyHandle implements DummyHandle {
    private final ServerPlayer handle;
    private final BukkitTask tickerTask;

    public PaperDummyHandle(ServerPlayer handle, BukkitTask tickerTask) {
        this.handle = handle;
        this.tickerTask = tickerTask;
    }

    @Override
    public Player player() {
        return handle.getBukkitEntity();
    }

    @Override
    public void teleport(Location location) {
        player().teleport(location);
    }

    @Override
    public void applySettings(String name, DummySettings settings) {
        Player player = player();
        Component displayName = Component.text(settings.displayName(name));
        player.setInvulnerable(settings.invulnerable() || settings.ghost());
        player.setCollidable(settings.collision() && !settings.ghost());
        player.setGravity(!settings.ghost());
        player.setNoPhysics(settings.ghost());
        player.setInvisible(false);
        player.displayName(displayName);
        player.playerListName(displayName);
        player.customName(displayName);
        player.setCustomNameVisible(true);
        setListed(settings.showInTab());
    }

    @Override
    public void applySkin(DummySkin skin) {
        Player player = player();
        com.destroystokyo.paper.profile.PlayerProfile profile = player.getPlayerProfile();
        profile.removeProperty("textures");
        if (skin.hasTexture()) {
            ProfileProperty property = skin.signature().isBlank()
                    ? new ProfileProperty("textures", skin.value())
                    : new ProfileProperty("textures", skin.value(), skin.signature());
            profile.setProperty(property);
        }
        player.setPlayerProfile(profile);
        refreshPlayerInfo();
    }

    @Override
    public void remove(Component reason) {
        tickerTask.cancel();
        Player player = player();
        if (player.isOnline()) {
            player.kick(reason);
            return;
        }
        handle.discard();
    }

    private void setListed(boolean listed) {
        var packet = ClientboundPlayerInfoUpdatePacket.updateListed(handle.getUUID(), listed);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online instanceof CraftPlayer craftPlayer && !online.getUniqueId().equals(handle.getUUID())) {
                craftPlayer.getHandle().connection.send(packet);
            }
        }
    }

    private void refreshPlayerInfo() {
        var remove = new ClientboundPlayerInfoRemovePacket(List.of(handle.getUUID()));
        var add = ClientboundPlayerInfoUpdatePacket.createSinglePlayerInitializing(handle, true);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online instanceof CraftPlayer craftPlayer && !online.getUniqueId().equals(handle.getUUID())) {
                craftPlayer.getHandle().connection.send(remove);
                craftPlayer.getHandle().connection.send(add);
            }
        }
    }
}
