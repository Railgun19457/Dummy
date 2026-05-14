package dev.dummy.nms.paper;

import dev.dummy.DummyPlugin;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
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
    public void send(Packet<?> packet) {
        if (packet instanceof ClientboundSetEntityMotionPacket motionPacket) {
            handleMotionPacket(motionPacket);
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
}
