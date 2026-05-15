package dev.dummy.nms.paper.compat;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

final class Paper12111NmsCompatibility implements PaperNmsCompatibility {
    @Override
    public String name() {
        return "Paper 1.21.11";
    }

    @Override
    public Integer motionPacketEntityId(ClientboundSetEntityMotionPacket packet) {
        return packet.getId();
    }

    @Override
    public Vec3 motionPacketMovement(ClientboundSetEntityMotionPacket packet) {
        return packet.getMovement();
    }
}
