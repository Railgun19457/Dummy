package dev.dummy.nms.paper;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.dummy.DummyPlugin;
import dev.dummy.dummy.DummySettings;
import dev.dummy.dummy.DummySkin;
import dev.dummy.nms.DummyHandle;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitTask;

/**
 * 基于 Paper 的假人句柄实现。
 *
 * <p>可见性策略：完全依赖 vanilla {@code ChunkMap} 的实体追踪，插件不主动 untrack/re-track。
 * 只有在需要调整 PlayerInfo 的 listed 标志或销毁假人时才手动发包。
 * 皮肤切换走 Paper {@link Player#setPlayerProfile(PlayerProfile)} API，由 Paper 负责原子广播。</p>
 */
public final class PaperDummyHandle implements DummyHandle {
    private static final String NO_COLLISION_TEAM = "dummy_no_collision";
    private static final String TEXTURES_PROPERTY = "textures";
    /** Paper API 的视距下限，加载半径小于该值时仍按该值设置。 */
    private static final int MIN_API_VIEW_DISTANCE = 2;
    /** 假人不接收任何有效数据包，把发包距离压到最小以减少区块包构造。 */
    private static final int SEND_VIEW_DISTANCE = 2;

    private final DummyPlugin plugin;
    private final ServerPlayer handle;
    private final BukkitTask tickerTask;
    private boolean removed;
    private boolean listed = true;

    public PaperDummyHandle(DummyPlugin plugin, ServerPlayer handle, BukkitTask tickerTask) {
        this.plugin = plugin;
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
        player.setInvulnerable(settings.invulnerable());
        player.setCollidable(settings.collision());
        applyCollisionRule(player, settings.collision());
        player.setGravity(settings.physics());
        player.setNoPhysics(!settings.physics());
        player.setInvisible(false);
        applyChunkLoading(settings);
        player.displayName(displayName);
        player.playerListName(displayName);
        player.customName(displayName);
        player.setCustomNameVisible(true);
        listed = settings.showInTab();
    }

    /**
     * 用 per-player 视距替代插件区块票据：假人本身是真实玩家，服务端的玩家区块加载器
     * （Paper/moonrise 的 {@code RegionizedPlayerChunkLoader}）会按其 tick 视距自动加载并
     * tick 周围区块，且随假人移动自动跟随，无需插件逐区块挂票据或周期刷新。
     *
     * <p>{@code chunk-loader} 关闭时视距按 0 处理，只保留假人自身所在区块。</p>
     */
    private void applyChunkLoading(DummySettings settings) {
        int radius = Math.min(
                settings.effectiveChunkRadius(),
                DummySettings.maxRadius(plugin.getConfig(), player().getWorld())
        );
        try {
            player().setSimulationDistance(radius);
            player().setViewDistance(Math.max(MIN_API_VIEW_DISTANCE, radius));
            player().setSendViewDistance(SEND_VIEW_DISTANCE);
        } catch (RuntimeException ex) {
            // 视距 API 不可用时保持服务端默认视距，功能不退化到需要逐区块票据
            plugin.getLogger().warning("Failed to apply chunk view distances for dummy '" + player().getName() + "': " + ex.getMessage());
        }
    }

    @Override
    public void applySkin(DummySkin skin) {
        // 通过 Paper 的 PlayerProfile API 原子更新皮肤，由 Paper 负责广播 ClientboundPlayerInfoUpdatePacket。
        // 不再手动 untrack / re-track 实体，避免破坏 vanilla ChunkMap 已建立的追踪状态。
        Player player = player();
        PlayerProfile profile = player.getPlayerProfile();
        if (skin.hasTexture()) {
            profile.setProperty(new ProfileProperty(
                    TEXTURES_PROPERTY,
                    skin.value(),
                    skin.signature().isBlank() ? null : skin.signature()
            ));
        } else {
            profile.removeProperty(TEXTURES_PROPERTY);
        }
        player.setPlayerProfile(profile);
        // 同步更新模型部件（披风等），通过 NMS 更新 client information
        handle.updateOptionsNoEvents(PaperSkinSupport.withSkinModelParts(handle.clientInformation(), skin));
    }

    @Override
    public void refreshForViewer(Player viewer, boolean listed) {
        // 不再主动操作实体追踪。vanilla ChunkMap 会在玩家进入追踪范围时自动处理实体 spawn。
        // 仅同步 PlayerInfo 的 listed 标志，确保 showInTab 设置生效。
        updateListedForViewer(viewer, listed);
    }

    @Override
    public void updateListedForViewer(Player viewer, boolean listed) {
        if (viewer.getUniqueId().equals(handle.getUUID())) {
            return;
        }
        if (!(viewer instanceof CraftPlayer craftPlayer)) {
            return;
        }
        craftPlayer.getHandle().connection.send(
                ClientboundPlayerInfoUpdatePacket.updateListed(handle.getUUID(), listed)
        );
    }

    @Override
    public void remove(Component reason) {
        if (removed) {
            return;
        }
        removed = true;
        tickerTask.cancel();
        Player player = player();
        // PlayerList.remove/discard may remove a vehicle that still contains
        // this synthetic ServerPlayer. Detach first so the vehicle survives
        // dummy removal and can be reused during the next restore.
        org.bukkit.entity.Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            vehicle.setPersistent(true);
            player.leaveVehicle();
        }
        removeCollisionRule(player);
        sendRemovePackets();
        closeConnection();
        if (player.isOnline()) {
            ((CraftServer) Bukkit.getServer()).getHandle().remove(handle, reason);
        }
        if (!handle.isRemoved()) {
            handle.discard();
        }
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

    private void removeCollisionRule(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(NO_COLLISION_TEAM);
        if (team != null) {
            team.removeEntry(player.getName());
        }
    }

    private void sendRemovePackets() {
        var removeInfo = new ClientboundPlayerInfoRemovePacket(List.of(handle.getUUID()));
        var removeEntity = new ClientboundRemoveEntitiesPacket(handle.getId());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online instanceof CraftPlayer craftPlayer && !online.getUniqueId().equals(handle.getUUID())) {
                craftPlayer.getHandle().connection.send(removeEntity);
                craftPlayer.getHandle().connection.send(removeInfo);
            }
        }
    }

    private void closeConnection() {
        if (handle.connection instanceof DummyServerGamePacketListener dummyConnection) {
            dummyConnection.closeDummyConnection();
        }
    }
}
