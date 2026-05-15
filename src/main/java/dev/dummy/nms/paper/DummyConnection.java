package dev.dummy.nms.paper;

import io.netty.channel.ChannelFutureListener;
import java.net.InetAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

public final class DummyConnection extends Connection {
    private boolean connected = true;

    public DummyConnection(InetAddress address) {
        super(PacketFlow.SERVERBOUND);
        this.channel = new DummyChannel(null, address);
        this.address = this.channel.remoteAddress();
        Connection.configureSerialization(this.channel.pipeline(), PacketFlow.SERVERBOUND, false, null);
    }

    @Override
    public boolean isConnected() {
        return connected && this.channel.isOpen();
    }

    @Override
    public void disconnect(Component disconnectReason) {
        connected = false;
        super.disconnect(disconnectReason);
    }

    @Override
    public void disconnect(DisconnectionDetails disconnectionDetails) {
        connected = false;
        super.disconnect(disconnectionDetails);
    }

    public void closeDummyConnection() {
        connected = false;
        this.channel.close();
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener) {
    }

    @Override
    public void send(Packet<?> packet, ChannelFutureListener listener, boolean flush) {
    }
}
