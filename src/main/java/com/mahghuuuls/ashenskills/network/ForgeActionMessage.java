package com.mahghuuuls.ashenskills.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client to server: the player pressed Apply or Remove in an open Ashen Forge screen. A request,
 * never a decision: the server checks everything again.
 */
public final class ForgeActionMessage implements IMessage {

    public static final byte ATTACH = 0;
    public static final byte DETACH = 1;

    private static final int LONGEST_SKILL_ID = 256;

    private int windowId;
    private byte action;
    private String skillId = "";

    public ForgeActionMessage() {
    }

    private ForgeActionMessage(int windowId, byte action, String skillId) {
        this.windowId = windowId;
        this.action = action;
        this.skillId = skillId;
    }

    public static ForgeActionMessage attach(int windowId, String skillId) {
        return new ForgeActionMessage(windowId, ATTACH, skillId);
    }

    public static ForgeActionMessage detach(int windowId) {
        return new ForgeActionMessage(windowId, DETACH, "");
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        windowId = buf.readInt();
        action = buf.readByte();
        skillId = ByteBufUtils.readUTF8String(buf);
        if (skillId.length() > LONGEST_SKILL_ID) {
            skillId = "";
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(windowId);
        buf.writeByte(action);
        ByteBufUtils.writeUTF8String(buf, skillId);
    }

    /** Receives the request on the server thread. */
    public interface Receiver {
        void onForgeAction(EntityPlayerMP player, int windowId, byte action, String skillId);
    }

    public static final class Handler implements IMessageHandler<ForgeActionMessage, IMessage> {
        @Override
        public IMessage onMessage(final ForgeActionMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(
                    () -> AshenNetwork.deliverForgeAction(player, message.windowId, message.action, message.skillId));
            return null;
        }
    }
}
