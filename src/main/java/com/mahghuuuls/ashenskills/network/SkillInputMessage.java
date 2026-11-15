package com.mahghuuuls.ashenskills.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Client to server: the player pressed or released the skill key, or pressed attack while preparing
 * a skill. A request, never a decision.
 */
public final class SkillInputMessage implements IMessage {

    public static final byte KEY_DOWN = 0;
    public static final byte KEY_UP = 1;
    /** The attack button, pressed during a preparation; the normal attack does not happen then. */
    public static final byte ATTACK_PRESS = 2;
    /**
     * The skill key stopped counting as held because a screen opened, not because the player let
     * go of it. A preparation ends with it at no cost; nothing executes.
     */
    public static final byte KEY_LOST = 3;

    private byte action;

    public SkillInputMessage() {
    }

    public SkillInputMessage(byte action) {
        this.action = action;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        action = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte(action);
    }

    /** Receives the input on the server thread. */
    public interface Receiver {

        void onSkillInput(EntityPlayerMP player, byte action);
    }

    public static final class Handler implements IMessageHandler<SkillInputMessage, IMessage> {

        @Override
        public IMessage onMessage(final SkillInputMessage message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    AshenNetwork.deliverInput(player, message.action);
                }
            });
            return null;
        }
    }
}
