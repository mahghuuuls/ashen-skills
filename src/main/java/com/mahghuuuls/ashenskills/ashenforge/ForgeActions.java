package com.mahghuuuls.ashenskills.ashenforge;

import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.ForgeActionMessage;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * The server side of what a player does at a Forge beyond moving items: the attach and detach
 * requests, and the two ways out of a Forge screen that the game does not report as a closed
 * screen (death and leaving the game).
 */
public final class ForgeActions {

    private ForgeActions() {
    }

    /** Called once while the game loads. */
    public static void install() {
        AshenNetwork.setForgeReceiver(ForgeActions::handle);
        MinecraftForge.EVENT_BUS.register(ForgeActions.class);
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

    /**
     * A player who dies with a weapon in the input gets it back into the inventory first, before
     * the game decides what happens to that inventory. So the weapon is kept or dropped exactly
     * like every other item, and mods that collect the drops of a death see it too.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) event.getEntityLiving();
            if (player.openContainer instanceof ForgeContainer) {
                ((ForgeContainer) player.openContainer).returnWeapon(player, true);
            }
        }
    }

    /** A player who leaves the game takes the weapon along, in the inventory that is saved next. */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP && event.player.openContainer instanceof ForgeContainer) {
            ForgeContainer forge = (ForgeContainer) event.player.openContainer;
            forge.returnWeapon(event.player, true);
            // Also gives back an item the player still held on the mouse.
            ((EntityPlayerMP) event.player).closeContainer();
        }
    }
}
