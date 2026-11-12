package com.mahghuuuls.ashenskills.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/** The mod's single network channel. Messages are registered here, in one place, with fixed ids. */
public final class AshenNetwork {

    public static final String CHANNEL_NAME = "ashenskills";

    private static SimpleNetworkWrapper channel;

    private AshenNetwork() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
    }

    public static SimpleNetworkWrapper channel() {
        return channel;
    }
}
