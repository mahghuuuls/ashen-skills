package com.mahghuuuls.ashenskills.network;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to clients: a moment of a skill that is seen and heard, such as the swing of a strike or
 * its impact on a target. Sent to the user and to everyone who sees the user. The client decides
 * how the moment looks and sounds; the server only says that it happened and where.
 */
public final class EffectCueMessage implements IMessage {

    /** A strike is made. The place is in front of the user. */
    public static final byte SWING = 1;
    /** A strike damaged a target. The place is the middle of the target. */
    public static final byte IMPACT = 2;
    /** A charge reached its full time. The place is the user. */
    public static final byte FULL_CHARGE = 3;

    private byte kind;
    private int userEntityId;
    private int useId;
    private String skillId = "";
    private double x;
    private double y;
    private double z;
    private int particles;

    public EffectCueMessage() {
    }

    /**
     * @param useId tells one use of a skill from the next, so the client can keep a particle budget per use
     * @param particles how many impact particles the skill asks for at this moment
     */
    public EffectCueMessage(
            byte kind, int userEntityId, int useId, String skillId, double x, double y, double z, int particles) {
        this.kind = kind;
        this.userEntityId = userEntityId;
        this.useId = useId;
        this.skillId = skillId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.particles = particles;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        kind = buf.readByte();
        userEntityId = buf.readInt();
        useId = buf.readInt();
        skillId = ByteBufUtils.readUTF8String(buf);
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        particles = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(kind);
        buf.writeInt(userEntityId);
        buf.writeInt(useId);
        ByteBufUtils.writeUTF8String(buf, skillId);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeInt(particles);
    }

    public static final class Handler implements IMessageHandler<EffectCueMessage, IMessage> {

        @Override
        public IMessage onMessage(EffectCueMessage message, MessageContext ctx) {
            AshenSkillsMod.proxy.onEffectCue(
                    message.kind, message.userEntityId, message.useId, message.skillId,
                    message.x, message.y, message.z, message.particles);
            return null;
        }
    }
}
