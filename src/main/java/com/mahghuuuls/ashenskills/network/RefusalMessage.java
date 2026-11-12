package com.mahghuuuls.ashenskills.network;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Server to one client: the skill did not start, and the language key of the reason. */
public final class RefusalMessage implements IMessage {

    private String messageKey = "";

    public RefusalMessage() {
    }

    public RefusalMessage(String messageKey) {
        this.messageKey = messageKey;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        messageKey = ByteBufUtils.readUTF8String(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, messageKey);
    }

    public static final class Handler implements IMessageHandler<RefusalMessage, IMessage> {

        @Override
        public IMessage onMessage(RefusalMessage message, MessageContext ctx) {
            AshenSkillsMod.proxy.onRefusal(message.messageKey);
            return null;
        }
    }
}
