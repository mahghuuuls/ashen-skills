package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.runtime.SessionTimeline.Step;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import com.mahghuuuls.ashenskills.weapon.AttachedSkill;
import com.mahghuuuls.ashenskills.weapon.SkillTag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;

/**
 * The server's skill engine: the only code that starts, advances, interrupts or ends a skill use.
 * Everything else asks it. Every way a skill use ends goes through {@link #end}, so cleanup cannot
 * be forgotten on one path.
 */
public final class SkillRuntime {

    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("5d0d3b0e-6a53-4f0b-9c55-a5b2f2a7c001");
    private static final String SPEED_MODIFIER_NAME = "ashenskills.skillPhase";
    private static final int MULTIPLY_TOTAL = 2;

    private final Map<UUID, SkillSession> sessions = new HashMap<>();
    private final CooldownStore cooldowns;
    private final SkillMessenger messenger;
    private int nextUseId;

    public SkillRuntime(CooldownStore cooldowns, SkillMessenger messenger) {
        this.cooldowns = cooldowns;
        this.messenger = messenger;
    }

    public CooldownStore cooldowns() {
        return cooldowns;
    }

    /** The player's running skill use, or null when the player is in no phase. */
    public SkillSession sessionOf(EntityPlayer player) {
        return sessions.get(player.getUniqueID());
    }

    /**
     * The player asks to use the skill on the main-hand weapon.
     *
     * @return null when the skill started, otherwise why it did not
     */
    public RefusalReason requestStart(EntityPlayerMP player) {
        ItemStack weapon = player.getHeldItemMainhand();
        AttachedSkill attached = AttachedSkill.of(weapon);
        boolean stateAllowed = attached.isUsable() && isStateAllowed(player, attached.definition());
        RefusalReason refusal = ActivationRules.firstRefusal(
                attached.status(), sessions.containsKey(player.getUniqueID()), cooldowns.remainingMillis(player), stateAllowed);
        if (refusal != null) {
            Diagnostics.log("ACTIVATION", "player={} skill={} result=refused reason={}",
                    player.getName(), attached.skillId(), refusal);
            if (refusal.isShownToPlayer()) {
                messenger.refused(player, refusal);
            }
            return refusal;
        }
        commit(player, attached.definition(), weapon);
        return null;
    }

    /** Everything that becomes true the moment a skill executes, as one step. */
    private void commit(EntityPlayerMP player, SkillDefinition skill, ItemStack weapon) {
        long cooldownMillis = Math.round(skill.number(SkillNumber.COOLDOWN) * 1000);
        cooldowns.start(player, cooldownMillis);
        SkillSession session = new SkillSession(
                player.getUniqueID(), skill, player.inventory.currentItem, weapon.getItem(), player.rotationYaw,
                Settings.general().getInt(SettingKey.HIT_WINDOW_CAP_PER_ACTIVATION), nextUseId++);
        sessions.put(player.getUniqueID(), session);
        applySpeed(player, session.speedFraction());
        ActionLock.stopForbiddenUse(session, player);
        Diagnostics.log("ACTIVATION", "player={} skill={} result=accepted cooldownMs={}",
                player.getName(), skill.id(), cooldownMillis);
        Diagnostics.log("PHASE", "player={} skill={} phase={} lengthTicks={} cause=activation serverTick={}",
                player.getName(), skill.id(), session.phase(), session.phaseLengthTicks(), serverTick(player));
        messenger.cooldownChanged(player, cooldownMillis, cooldownMillis);
        messenger.phaseChanged(player, session);
    }

    /** Advances every running skill use by one server tick. */
    public void tick(MinecraftServer server) {
        if (sessions.isEmpty()) {
            return;
        }
        for (SkillSession session : new ArrayList<>(sessions.values())) {
            EntityPlayerMP player = server.getPlayerList().getPlayerByUUID(session.playerId());
            if (player == null || player.isDead) {
                end(session, player, "playerGone");
                continue;
            }
            if (session.phase() == SkillPhase.EXECUTION) {
                String cause = hasSwitchedWeapon(player, session)
                        ? "weaponSwitched"
                        : isStateAllowed(player, session.skill()) ? null : "stateNotAllowed";
                if (cause != null) {
                    interrupt(player, cause);
                }
            }
            boolean wasExecuting = session.phase() == SkillPhase.EXECUTION;
            Step step = session.timeline().tick();
            if (wasExecuting) {
                // A window due exactly at the end of execution still opens: the execution completed.
                openDueHitWindows(player, session);
            }
            if (sessions.get(session.playerId()) != session) {
                // The hit itself ended this skill use (for example the user died of thorns).
                continue;
            }
            if (step == Step.EXECUTION_ENDED) {
                enteredPhase(player, session, "executionCompleted");
            } else if (step == Step.FINISHED) {
                end(session, player, "completed");
            }
        }
    }

    /** Opens every window whose time has come. Stops when a hit ends or interrupts the skill use. */
    private void openDueHitWindows(EntityPlayerMP player, SkillSession session) {
        SkillBehavior behavior = SkillRegistry.behaviorOf(session.skill().id());
        if (behavior == null) {
            return;
        }
        int window;
        while (sessions.get(session.playerId()) == session
                && session.phase() != SkillPhase.INTERRUPTED_RECOVERY
                && (window = session.hitSchedule().nextDue(session.timeline().executionTicksElapsed())) >= 0) {
            behavior.onHitWindow(new RuntimeSkillContext(player, session, messenger), window);
        }
    }

    /**
     * Cuts a running execution short. Does nothing in recovery or without a skill use.
     *
     * @return true when an execution was interrupted
     */
    public boolean interrupt(EntityPlayerMP player, String cause) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session == null || !session.timeline().interrupt()) {
            return false;
        }
        Diagnostics.log("INTERRUPT", "player={} skill={} cause={}", player.getName(), session.skill().id(), cause);
        enteredPhase(player, session, cause);
        return true;
    }

    /** Taking damage or knockback interrupts the execution when the skill says so. */
    public void onHurt(EntityPlayerMP player, boolean knockback) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session == null || session.phase() != SkillPhase.EXECUTION) {
            return;
        }
        SkillFlag rule = knockback ? SkillFlag.EXECUTION_INTERRUPTED_BY_KNOCKBACK : SkillFlag.EXECUTION_INTERRUPTED_BY_DAMAGE;
        if (session.skill().flag(rule)) {
            interrupt(player, knockback ? "knockback" : "damage");
        }
    }

    /**
     * Ends the player's skill use at once, with no recovery: death, leaving, changing dimension,
     * or an operator's command.
     *
     * @return true when a skill use was running
     */
    public boolean endNow(EntityPlayerMP player, String cause) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session == null) {
            return false;
        }
        end(session, player, cause);
        return true;
    }

    /**
     * Forgets every skill use without the usual cleanup. Only for server shutdown: no player
     * remains to tell, and the speed change is not part of a player's saved data.
     */
    public void reset() {
        sessions.clear();
    }

    /** Tells a player who starts to see another player which phase that player is in. */
    public void showPhaseTo(EntityPlayerMP watcher, EntityPlayerMP player) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session != null) {
            messenger.phaseShownTo(watcher, player, session);
        }
    }

    /** Ends the player's cooldown and tells the player. */
    public void clearCooldown(EntityPlayerMP player) {
        cooldowns.clear(player);
        sendCooldownTo(player);
    }

    /** Whether a normal attack by this player has no effect now. */
    public boolean blocksAttack(EntityPlayer player) {
        return ActionLock.blocksAttack(sessions.get(player.getUniqueID()));
    }

    /** Whether this player may not use that item now. */
    public boolean blocksUse(EntityPlayer player, ItemStack item) {
        return ActionLock.blocksUse(sessions.get(player.getUniqueID()), player, item);
    }

    /** Tells a player the state of the own cooldown, for example after joining. */
    public void sendCooldownTo(EntityPlayerMP player) {
        messenger.cooldownChanged(player, cooldowns.remainingMillis(player), cooldowns.lengthMillis(player));
    }

    private void enteredPhase(EntityPlayerMP player, SkillSession session, String cause) {
        applySpeed(player, session.speedFraction());
        ActionLock.stopForbiddenUse(session, player);
        Diagnostics.log("PHASE", "player={} skill={} phase={} lengthTicks={} cause={} serverTick={}",
                player.getName(), session.skill().id(), session.phase(), session.phaseLengthTicks(), cause,
                serverTick(player));
        messenger.phaseChanged(player, session);
    }

    /** The single way a skill use ends. The player is null when no longer on the server. */
    private void end(SkillSession session, EntityPlayerMP player, String cause) {
        sessions.remove(session.playerId());
        if (player != null) {
            removeSpeed(player);
            messenger.phaseChanged(player, null);
        }
        Diagnostics.log("PHASE", "player={} skill={} phase=NONE cause={} serverTick={}",
                player == null ? session.playerId() : player.getName(), session.skill().id(), cause,
                player == null ? "unknown" : serverTick(player));
    }

    /** The server's tick count, so log lines can be compared with the configured phase lengths. */
    private static int serverTick(EntityPlayerMP player) {
        return player.getServer().getTickCounter();
    }

    private static boolean hasSwitchedWeapon(EntityPlayerMP player, SkillSession session) {
        ItemStack held = player.getHeldItemMainhand();
        return player.inventory.currentItem != session.hotbarSlot()
                || held.isEmpty()
                || held.getItem() != session.weaponItem()
                || !session.skill().id().toString().equals(SkillTag.getSkillId(held));
    }

    private static boolean isStateAllowed(EntityPlayerMP player, SkillDefinition skill) {
        if (!player.isEntityAlive() || player.isSpectator()) {
            return false;
        }
        if (player.isRiding() && !skill.flag(SkillFlag.MOUNTED_ALLOWED)) {
            return false;
        }
        return skill.allowedStates().contains(stateOf(player));
    }

    private static String stateOf(EntityPlayerMP player) {
        if (player.isInWater()) {
            return SkillDefinition.STATE_SWIMMING;
        }
        return player.onGround ? SkillDefinition.STATE_GROUNDED : SkillDefinition.STATE_AIRBORNE;
    }

    private static void applySpeed(EntityPlayerMP player, double fractionOfNormal) {
        IAttributeInstance speed = player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
        speed.removeModifier(SPEED_MODIFIER_ID);
        if (fractionOfNormal != 1.0) {
            speed.applyModifier(
                    new AttributeModifier(SPEED_MODIFIER_ID, SPEED_MODIFIER_NAME, fractionOfNormal - 1.0, MULTIPLY_TOTAL)
                            .setSaved(false));
        }
    }

    private static void removeSpeed(EntityPlayerMP player) {
        player.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).removeModifier(SPEED_MODIFIER_ID);
    }
}
