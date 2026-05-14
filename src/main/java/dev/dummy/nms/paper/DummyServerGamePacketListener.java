package dev.dummy.nms.paper;

import dev.dummy.DummyPlugin;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;

public final class DummyServerGamePacketListener extends ServerGamePacketListenerImpl {
    private final DummyPlugin plugin;

    public DummyServerGamePacketListener(
            MinecraftServer server,
            Connection connection,
            ServerPlayer player,
            CommonListenerCookie cookie,
            DummyPlugin plugin
    ) {
        super(server, connection, player, cookie);
        this.plugin = plugin;
    }

    @Override
    public void tick() {
        // Fake players do not send movement packets. The vanilla connection tick
        // snaps players back to the last client-confirmed position every tick,
        // which prevents gravity and knockback after respawn.
    }

    @Override
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            handleMotionPacket(motionPacket);
        } else if (packet instanceof ClientboundPlayerPositionPacket positionPacket) {
            handlePositionPacket(positionPacket);
        }
    }

    private void handleMotionPacket(ClientboundSetEntityMotionPacket packet) {
        if (packet.getId() != this.player.getId() || !this.player.hurtMarked) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            this.player.hurtMarked = true;
            this.player.lerpMotion(packet.getMovement());
        });
    }

    private void handlePositionPacket(ClientboundPlayerPositionPacket packet) {
        runOnMain(() -> {
            handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(packet.id()));
            resetPosition();
        });
    }

    public void completeRespawn() {
        runOnMain(() -> {
            handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            resetPosition();
            resetFlyingTicks();
        });
    }

    private void runOnMain(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
