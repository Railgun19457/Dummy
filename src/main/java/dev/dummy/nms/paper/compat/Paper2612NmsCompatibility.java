package dev.dummy.nms.paper.compat;

import java.lang.reflect.Method;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

final class Paper2612NmsCompatibility implements PaperNmsCompatibility {
    private final Method motionPacketIdMethod = requireMethod("id");
    private final Method motionPacketMovementMethod = requireMethod("movement");

    @Override
    public String name() {
        return "Paper 26.1.2";
    }

    @Override
    public Integer motionPacketEntityId(ClientboundSetEntityMotionPacket packet) {
        Object value = invoke(motionPacketIdMethod, packet);
        return value instanceof Integer id ? id : null;
    }

    @Override
    public Vec3 motionPacketMovement(ClientboundSetEntityMotionPacket packet) {
        Object value = invoke(motionPacketMovementMethod, packet);
        return value instanceof Vec3 movement ? movement : null;
    }

    private Method requireMethod(String name) {
        try {
            return ClientboundSetEntityMotionPacket.class.getMethod(name);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Unsupported Paper 26 motion packet accessor: " + name, ex);
        }
    }

    private Object invoke(Method method, Object target) {
        try {
            return method.invoke(target);
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }
}
