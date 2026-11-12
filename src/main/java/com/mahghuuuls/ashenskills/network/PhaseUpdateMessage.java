package com.mahghuuuls.ashenskills.network;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to clients: the skill phase a player is in now. Sent at every phase change and when a
 * client starts to see the player; receivers replace what they knew with it.
 */
public final class PhaseUpdateMessage implements IMessage {

    /** Phase code for "in no phase". Other codes are the phase's ordinal plus one. */
    public static final byte NO_PHASE = 0;

    private int entityId;
    private String skillId = "";
    private byte phaseCode;
    private int elapsedTicks;
    private int lengthTicks;

    public PhaseUpdateMessage() {
    }

    public PhaseUpdateMessage(int entityId, String skillId, byte phaseCode, int elapsedTicks, int lengthTicks) {
        this.entityId = entityId;
        this.skillId = skillId;
        this.phaseCode = phaseCode;
        this.elapsedTicks = elapsedTicks;
        this.lengthTicks = lengthTicks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        entityId = buf.readInt();
        skillId = ByteBufUtils.readUTF8String(buf);
        phaseCode = buf.readByte();
        elapsedTicks = buf.readInt();
        lengthTicks = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(entityId);
        ByteBufUtils.writeUTF8String(buf, skillId);
        buf.writeByte(phaseCode);
        buf.writeInt(elapsedTicks);
        buf.writeInt(lengthTicks);
    }

    public static final class Handler implements IMessageHandler<PhaseUpdateMessage, IMessage> {

        @Override
        public IMessage onMessage(PhaseUpdateMessage message, MessageContext ctx) {
            AshenSkillsMod.proxy.onPhaseUpdate(
                    message.entityId, message.skillId, message.phaseCode, message.elapsedTicks, message.lengthTicks);
            return null;
        }
    }
}
