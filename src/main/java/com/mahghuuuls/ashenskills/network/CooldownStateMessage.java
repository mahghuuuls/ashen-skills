package com.mahghuuuls.ashenskills.network;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to one client: how long that player's exclusive cooldown still runs, and its length. */
public final class CooldownStateMessage implements IMessage {

    private long remainingMillis;
    private long lengthMillis;

    public CooldownStateMessage() {
    }

    public CooldownStateMessage(long remainingMillis, long lengthMillis) {
        this.remainingMillis = remainingMillis;
        this.lengthMillis = lengthMillis;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        remainingMillis = buf.readLong();
        lengthMillis = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(remainingMillis);
        buf.writeLong(lengthMillis);
    }

    public static final class Handler implements IMessageHandler<CooldownStateMessage, IMessage> {

        @Override
        public IMessage onMessage(CooldownStateMessage message, MessageContext ctx) {
            AshenSkillsMod.proxy.onCooldownState(message.remainingMillis, message.lengthMillis);
            return null;
        }
    }
}
