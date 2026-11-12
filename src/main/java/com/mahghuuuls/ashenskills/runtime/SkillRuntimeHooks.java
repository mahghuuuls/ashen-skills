package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.combat.SkillDamage;
import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.SkillInputMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Connects the game's events to the skill runtime. Each handler only asks or tells the runtime:
 * what a skill use allows and what ends it is decided there, not here.
 */
public final class SkillRuntimeHooks implements SkillInputMessage.Receiver {

    private static SkillRuntime runtime;

    private SkillRuntimeHooks() {
    }

    /** Creates the runtime and starts listening. Called once while the game loads. */
    public static void install() {
        runtime = new SkillRuntime(new CooldownStore(System::currentTimeMillis), new NetworkSkillMessenger());
        SkillRuntimeHooks hooks = new SkillRuntimeHooks();
        MinecraftForge.EVENT_BUS.register(hooks);
        AshenNetwork.setInputReceiver(hooks);
    }

    public static SkillRuntime runtime() {
        return runtime;
    }

    /** Forgets all skill uses when the server stops, so nothing leaks into the next world. */
    public static void onServerStopped() {
        runtime.reset();
    }

    @Override
    public void onSkillInput(EntityPlayerMP player, byte action) {
        if (action == SkillInputMessage.KEY_DOWN) {
            runtime.requestStart(player);
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            runtime.tick(server);
        }
    }

    /** Normal attacks have no effect while a skill use runs, including its recovery. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttackEntity(AttackEntityEvent event) {
        if (isAttackBlocked(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isAttackBlocked(event.getEntityPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelUseIfNotAllowed(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelUseIfNotAllowed(event);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelUseIfNotAllowed(event);
    }

    private static void cancelUseIfNotAllowed(PlayerInteractEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player.world.isRemote) {
            return;
        }
        if (runtime.blocksUse(player, event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    private static boolean isAttackBlocked(EntityPlayer player) {
        return !player.world.isRemote && runtime.blocksAttack(player);
    }

    /** A kill made by a skill that does not use Looting drops as if the weapon had none. */
    @SubscribeEvent
    public void onLootingLevel(LootingLevelEvent event) {
        event.setLootingLevel(SkillDamage.lootingLevelFor(event.getEntityLiving(), event.getLootingLevel()));
    }

    @SubscribeEvent
    public void onDamage(LivingDamageEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            runtime.onHurt((EntityPlayerMP) event.getEntityLiving(), false);
        }
    }

    @SubscribeEvent
    public void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            runtime.onHurt((EntityPlayerMP) event.getEntityLiving(), true);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayerMP) {
            runtime.endNow((EntityPlayerMP) event.getEntityLiving(), "death");
        }
    }

    @SubscribeEvent
    public void onLogout(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            runtime.endNow((EntityPlayerMP) event.player, "logout");
        }
    }

    @SubscribeEvent
    public void onDimensionChange(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            runtime.endNow((EntityPlayerMP) event.player, "dimensionChange");
        }
    }

    @SubscribeEvent
    public void onLogin(net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP) {
            runtime.sendCooldownTo((EntityPlayerMP) event.player);
        }
    }

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (target instanceof EntityPlayerMP && event.getEntityPlayer() instanceof EntityPlayerMP) {
            runtime.showPhaseTo((EntityPlayerMP) event.getEntityPlayer(), (EntityPlayerMP) target);
        }
    }
}
