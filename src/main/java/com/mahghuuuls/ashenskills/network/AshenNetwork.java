package com.mahghuuuls.ashenskills.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/** The mod's single network channel. Messages are registered here, in one place, with fixed ids. */
public final class AshenNetwork {

    public static final String CHANNEL_NAME = "ashenskills";

    private static SimpleNetworkWrapper channel;
    private static SkillInputMessage.Receiver inputReceiver;

    private AshenNetwork() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
        channel.registerMessage(SkillInputMessage.Handler.class, SkillInputMessage.class, 0, Side.SERVER);
        channel.registerMessage(PhaseUpdateMessage.Handler.class, PhaseUpdateMessage.class, 1, Side.CLIENT);
        channel.registerMessage(RefusalMessage.Handler.class, RefusalMessage.class, 2, Side.CLIENT);
        channel.registerMessage(CooldownStateMessage.Handler.class, CooldownStateMessage.class, 3, Side.CLIENT);
        channel.registerMessage(PoseControlMessage.Handler.class, PoseControlMessage.class, 4, Side.CLIENT);
        channel.registerMessage(EffectCueMessage.Handler.class, EffectCueMessage.class, 5, Side.CLIENT);
        channel.registerMessage(SettingsViewMessage.Handler.class, SettingsViewMessage.class, 6, Side.CLIENT);
        SettingsViewMessage.sendAtLogin();
    }

    public static SimpleNetworkWrapper channel() {
        return channel;
    }

    /** Names who handles skill key input on the server. */
    public static void setInputReceiver(SkillInputMessage.Receiver receiver) {
        inputReceiver = receiver;
    }

    static void deliverInput(EntityPlayerMP player, byte action) {
        if (inputReceiver != null) {
            inputReceiver.onSkillInput(player, action);
        }
    }
}
