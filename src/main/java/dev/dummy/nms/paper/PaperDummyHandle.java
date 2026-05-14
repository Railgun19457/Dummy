package dev.dummy.nms.paper;

import com.destroystokyo.paper.profile.ProfileProperty;
import dev.dummy.dummy.DummySettings;
import dev.dummy.dummy.DummySkin;
import dev.dummy.nms.DummyHandle;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.ChatVisiblity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

public final class PaperDummyHandle implements DummyHandle {
    private static final String NO_COLLISION_TEAM = "dummy_no_collision";
    private static final int ALL_SKIN_PARTS = 0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 | 0x40;

    private ServerPlayer handle;
    private final DummyTicker ticker;
    private final BukkitTask tickerTask;

    public PaperDummyHandle(ServerPlayer handle, DummyTicker ticker, BukkitTask tickerTask) {
        this.handle = handle;
        this.ticker = ticker;
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
        applyCollisionRule(player, settings.collision() && !settings.ghost());
        player.setGravity(!settings.ghost());
        player.setNoPhysics(settings.ghost());
        player.setInvisible(false);
        player.displayName(displayName);
        player.playerListName(displayName);
        player.customName(displayName);
        player.setCustomNameVisible(true);
        applyClientOptions(settings.showInTab());
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
    public void respawn() {
        if (!player().isDead()) {
            return;
        }
        Player player = player();
        player.spigot().respawn();
        if (player instanceof CraftPlayer craftPlayer) {
            handle = craftPlayer.getHandle();
        }

        var connection = handle.connection;
        if (connection instanceof DummyServerGamePacketListener dummyConnection) {
            dummyConnection.completeRespawn();
        } else {
            connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            connection.resetPosition();
        }
        ticker.handle(handle);
        resetPostRespawnPhysics();
    }

    @Override
    public void hideEntity() {
        var packet = new ClientboundRemoveEntitiesPacket(handle.getId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online instanceof CraftPlayer craftPlayer && !online.getUniqueId().equals(handle.getUUID())) {
                craftPlayer.getHandle().connection.send(packet);
            }
        }
    }

    @Override
    public void remove(Component reason) {
        tickerTask.cancel();
        Player player = player();
        removeCollisionRule(player);
        if (player.isOnline()) {
            player.kick(reason);
            return;
        }
        handle.discard();
    }

    private void applyCollisionRule(Player player, boolean collision) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (collision) {
            if (team != null) {
                team.removeEntry(player.getName());
            }
            return;
        }
        if (team == null) {
            team = scoreboard.registerNewTeam(NO_COLLISION_TEAM);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.addEntry(player.getName());
    }

    private void resetPostRespawnPhysics() {
        handle.unsetRemoved();
        handle.valid = true;
        handle.noPhysics = false;
        handle.setNoGravity(false);
        handle.setPose(Pose.STANDING);

        Player player = player();
        player.setNoPhysics(false);
        player.setGravity(true);
        player.setFallDistance(0.0F);
        if (player.isFlying()) {
            player.setFlying(false);
        }
        player.setSleepingIgnored(true);
    }

    private void applyClientOptions(boolean listed) {
        handle.updateOptionsNoEvents(new ClientInformation(
                "en_us",
                Bukkit.getViewDistance(),
                ChatVisiblity.SYSTEM,
                false,
                ALL_SKIN_PARTS,
                HumanoidArm.RIGHT,
                false,
                listed,
                ParticleStatus.MINIMAL
        ));
    }

    private void removeCollisionRule(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team != null) {
            team.removeEntry(player.getName());
        }
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
