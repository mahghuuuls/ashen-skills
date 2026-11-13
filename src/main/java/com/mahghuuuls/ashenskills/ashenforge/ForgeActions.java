package com.mahghuuuls.ashenskills.ashenforge;

import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.ForgeActionMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;

/** The server side of what a player does at a Forge beyond moving items: the attach and detach requests. */
public final class ForgeActions {

    private ForgeActions() {
    }

    /** Called once while the game loads. */
    public static void install() {
        AshenNetwork.setForgeReceiver(ForgeActions::handle);
    }

    /** A request counts only for the Forge screen the player has open at this moment. */
    private static void handle(EntityPlayerMP player, int windowId, byte action, String skillId) {
        if (!(player.openContainer instanceof ForgeContainer) || player.openContainer.windowId != windowId) {
            return;
        }
        ForgeContainer forge = (ForgeContainer) player.openContainer;
        if (action == ForgeActionMessage.DETACH) {
            forge.detach(player);
        } else if (action == ForgeActionMessage.ATTACH) {
            ForgeRules.Refusal refusal = forge.attach(player, skillId);
            if (refusal != null) {
                player.sendStatusMessage(new TextComponentTranslation(refusal.messageKey()), true);
            }
        }
    }
}
