package com.mahghuuuls.ashenskills.network;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.config.PackConfiguration;
import com.mahghuuuls.ashenskills.config.SettingsView;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to one client, when that client joins: the server's pack configuration. The client then
 * shows and assumes the server's skill values and weapon types, not those of its own files.
 */
public final class SettingsViewMessage implements IMessage {

    private SettingsView view;

    public SettingsViewMessage() {
    }

    public SettingsViewMessage(SettingsView view) {
        this.view = view;
    }

    /** Starts sending the view to every player who joins. Called once while the game loads. */
    static void sendAtLogin() {
        MinecraftForge.EVENT_BUS.register(new LoginSender());
    }

    /** Public and named because the game's event system cannot call into an anonymous class. */
    public static final class LoginSender {

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP) {
                AshenNetwork.channel().sendTo(
                        new SettingsViewMessage(PackConfiguration.view()), (EntityPlayerMP) event.player);
            }
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        view = SettingsView.read(buf);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        view.write(buf);
    }

    public static final class Handler implements IMessageHandler<SettingsViewMessage, IMessage> {

        @Override
        public IMessage onMessage(SettingsViewMessage message, MessageContext ctx) {
            AshenSkillsMod.proxy.onSettingsView(message.view);
            return null;
        }
    }
}
