package dev.dummy.nms.paper;

import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoop;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

public final class DummyChannel extends AbstractChannel {
    private static final EventLoop EVENT_LOOP = new DefaultEventLoop();

    private final ChannelConfig config = new DefaultChannelConfig(this);
    private final ChannelPipeline pipeline = new DummyChannelPipeline(this);
    private final InetAddress address;
    private boolean open = true;

    public DummyChannel(Channel parent, InetAddress address) {
        super(parent);
        this.address = address;
    }

    @Override
    public ChannelConfig config() {
        config.setAutoRead(true);
        return config;
    }

    @Override
    protected void doBeginRead() {
    }

    @Override
    protected void doBind(SocketAddress localAddress) {
    }

    @Override
    protected void doClose() {
        open = false;
    }

    @Override
    protected void doDisconnect() {
        open = false;
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) {
        while (in.current() != null) {
            in.remove();
        }
    }

    @Override
    public boolean isActive() {
        return open;
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return true;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    protected SocketAddress localAddress0() {
        return new InetSocketAddress(address, 25565);
    }

    @Override
    public ChannelMetadata metadata() {
        return new ChannelMetadata(true);
    }

    @Override
    protected AbstractUnsafe newUnsafe() {
        return new AbstractUnsafe() {
            @Override
            public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
                safeSetSuccess(promise);
            }
        };
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return new InetSocketAddress(address, 25565);
    }

    @Override
    public EventLoop eventLoop() {
        return EVENT_LOOP;
    }
}
