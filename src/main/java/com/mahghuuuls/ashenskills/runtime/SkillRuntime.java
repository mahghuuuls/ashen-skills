package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.attribute.SkillAttributes;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.integration.PaymentPlan;
import com.mahghuuuls.ashenskills.integration.PaymentResult;
import com.mahghuuuls.ashenskills.integration.PreparationRegen;
import com.mahghuuuls.ashenskills.integration.ResourceGateway;
import com.mahghuuuls.ashenskills.network.PhaseUpdateMessage;
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
 *
 * <p>An instant skill executes when it is asked for. A stance skill first enters a preparation
 * that costs nothing; the attack button then executes it, and everything that belongs to executing
 * (payment, cooldown, direction) happens at that moment, in {@link #execute}.
 */
public final class SkillRuntime {

    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("5d0d3b0e-6a53-4f0b-9c55-a5b2f2a7c001");
    private static final String SPEED_MODIFIER_NAME = "ashenskills.skillPhase";
    private static final int MULTIPLY_TOTAL = 2;

    private final Map<UUID, SkillSession> sessions = new HashMap<>();
    private final CooldownStore cooldowns;
    private final SkillMessenger messenger;
    private final ResourceGateway resources;
    private final PreparationRegen preparationRegen;
    private int nextUseId;

    public SkillRuntime(
            CooldownStore cooldowns, SkillMessenger messenger, ResourceGateway resources,
            PreparationRegen preparationRegen) {
        this.cooldowns = cooldowns;
        this.messenger = messenger;
        this.resources = resources;
        this.preparationRegen = preparationRegen;
    }

    public CooldownStore cooldowns() {
        return cooldowns;
    }

    /** The player's running skill use, or null when the player is in no phase. */
    public SkillSession sessionOf(EntityPlayer player) {
        return sessions.get(player.getUniqueID());
    }

    /**
     * The player asks to use the skill on the main-hand weapon: the skill key went down. An instant
     * skill executes; a skill with a preparation enters it.
     *
     * @return null when the skill started, otherwise why it did not
     */
    public RefusalReason requestStart(EntityPlayerMP player) {
        ItemStack weapon = player.getHeldItemMainhand();
        AttachedSkill attached = AttachedSkill.of(weapon);
        boolean instant = attached.isUsable() && attached.definition().activation() == ActivationKind.INSTANT;
        // A preparation ends when an item is in use, so it does not start during one either.
        boolean mayStartNow = attached.isUsable() && isStateAllowed(player, attached.definition())
                && (instant || !player.isHandActive());
        boolean free = usesSkillsForFree(player);
        // A cooldown does not hold back a player who uses skills for free; it keeps counting.
        long blockingCooldownMillis = free ? 0 : cooldowns.remainingMillis(player);
        RefusalReason refusal = ActivationRules.firstRefusal(
                attached.status(), sessions.containsKey(player.getUniqueID()), blockingCooldownMillis, mayStartNow);
        if (refusal == null) {
            if (instant) {
                refusal = payAndExecute(player, attached.definition(), weapon, null, "activation");
            } else {
                prepare(player, open(player, attached.definition(), weapon));
            }
        }
        if (refusal != null) {
            reportRefusal(player, attached.skillId(), refusal);
        }
        return refusal;
    }

    /**
     * The one way a skill comes to execute: the stamina and mana are taken first, because their
     * payment is the only part that can fail, and nothing else has happened when it fails; then
     * {@link #execute} does the rest.
     *
     * @param preparing the skill use that held the preparation, or null for an instant skill
     * @return null when the skill executes, otherwise why the payment failed
     */
    private RefusalReason payAndExecute(
            EntityPlayerMP player, SkillDefinition skill, ItemStack weapon, SkillSession preparing, String cause) {
        boolean free = usesSkillsForFree(player);
        RefusalReason unpaid = free ? null : payResources(player, skill);
        if (unpaid != null) {
            return unpaid;
        }
        execute(player, preparing != null ? preparing : open(player, skill, weapon), free, cause);
        return null;
    }

    private void reportRefusal(EntityPlayerMP player, String skillId, RefusalReason refusal) {
        Diagnostics.log("ACTIVATION", "player={} skill={} result=refused reason={}", player.getName(), skillId, refusal);
        if (refusal.isShownToPlayer()) {
            messenger.refused(player, refusal);
        }
    }

    /**
     * The skill key came up. A preparation ends with it, at no cost.
     *
     * @return true when a preparation ended
     */
    public boolean onKeyReleased(EntityPlayerMP player) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session == null || session.phase() != SkillPhase.PREPARATION) {
            return false;
        }
        end(session, player, "keyReleased");
        return true;
    }

    /**
     * The attack button was pressed. In a stance this executes the skill, when it can be paid for
     * now; when it cannot, the player is told why and the stance goes on. A stance that something
     * ended in this same tick (a jump, another item) ends here, at no cost, and nothing executes.
     *
     * @return null when the skill executed, the reason when the payment failed,
     *     {@link RefusalReason#NOT_ALLOWED_NOW} when the stance ended instead, and
     *     {@link RefusalReason#NO_SKILL} when the player holds no stance
     */
    public RefusalReason onAttackPressed(EntityPlayerMP player) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session == null || session.phase() != SkillPhase.PREPARATION
                || session.skill().activation() != ActivationKind.STANCE) {
            return RefusalReason.NO_SKILL;
        }
        String endCause = preparationEndCause(player, session);
        if (endCause != null) {
            end(session, player, endCause);
            reportRefusal(player, session.skill().id().toString(), RefusalReason.NOT_ALLOWED_NOW);
            return RefusalReason.NOT_ALLOWED_NOW;
        }
        RefusalReason unpaid = payAndExecute(player, session.skill(), null, session, "attackPressed");
        if (unpaid != null) {
            reportRefusal(player, session.skill().id().toString(), unpaid);
        }
        return unpaid;
    }

    /** Why this preparation may not go on now, or null. Asked every tick and before the skill executes. */
    private static String preparationEndCause(EntityPlayerMP player, SkillSession session) {
        return PreparationRules.endCause(
                hasSwitchedWeapon(player, session), isStateAllowed(player, session.skill()), player.isHandActive());
    }

    /** Whether skills cost this player nothing: no stamina or mana, and no cooldown started or obeyed. */
    private static boolean usesSkillsForFree(EntityPlayerMP player) {
        return player.capabilities.isCreativeMode && Settings.general().getBoolean(SettingKey.CREATIVE_BYPASS);
    }

    /** Takes the skill's stamina and mana. @return null when paid, otherwise why the skill does not start */
    private RefusalReason payResources(EntityPlayerMP player, SkillDefinition skill) {
        double scalePoints = Settings.general().getInt(SettingKey.HYPERBOLIC_SCALE_POINTS);
        PaymentPlan plan = resources.quote(
                UseCosts.resourceCost(skill.number(SkillNumber.STAMINA_COST), scalePoints,
                        SkillAttributes.points(player, SkillAttributes.SKILL_STAMINA_EFFICIENCY)),
                UseCosts.manaCost(skill.number(SkillNumber.MANA_COST), scalePoints,
                        SkillAttributes.points(player, SkillAttributes.SKILL_MANA_EFFICIENCY)));
        PaymentResult result = resources.pay(player, plan);
        // What was asked of the stamina and mana mods. They apply their own rules to it.
        Diagnostics.log("PAYMENT", "player={} skill={} staminaAsked={} manaAsked={} result={}",
                player.getName(), skill.id(), plan.stamina(), plan.mana(), result);
        return UseCosts.refusalFor(result);
    }

    /** Begins the record of a skill use on the weapon in the player's hand. */
    private SkillSession open(EntityPlayerMP player, SkillDefinition skill, ItemStack weapon) {
        SkillSession session = new SkillSession(
                player.getUniqueID(), skill, player.inventory.currentItem, weapon.getItem(), player.rotationYaw,
                Settings.general().getInt(SettingKey.HIT_WINDOW_CAP_PER_ACTIVATION), nextUseId++);
        sessions.put(player.getUniqueID(), session);
        return session;
    }

    /** What a preparation changes while it lasts: the player's speed and stamina regeneration. */
    private void prepare(EntityPlayerMP player, SkillSession session) {
        double regenMultiplier = session.skill().number(SkillNumber.PREPARATION_STAMINA_REGEN);
        preparationRegen.begin(player, regenMultiplier);
        Diagnostics.log("PREPARATION_REGEN", "player={} state=slowed multiplier={}", player.getName(), regenMultiplier);
        enteredPhase(player, session, "keyDown");
    }

    /**
     * Gives the player's stamina regeneration back to the stamina mod. Called on every way out of a
     * preparation: into the execution, and through {@link #end}.
     *
     * @param player null when the player is no longer on the server
     */
    private void restoreRegen(SkillSession session, EntityPlayerMP player) {
        if (player != null) {
            preparationRegen.end(player);
        } else {
            preparationRegen.forget(session.playerId());
        }
        Diagnostics.log("PREPARATION_REGEN", "player={} state=restored",
                player == null ? session.playerId() : player.getName());
    }

    /**
     * Everything that becomes true the moment a skill executes, as one step: the cooldown starts,
     * the direction is fixed, and the execution phase begins. Only {@link #payAndExecute} calls
     * this, after the payment.
     *
     * @param session new for an instant skill, in preparation for the others
     */
    private void execute(EntityPlayerMP player, SkillSession session, boolean free, String cause) {
        SkillDefinition skill = session.skill();
        long cooldownMillis = 0;
        if (!free) {
            cooldownMillis = UseCosts.cooldownMillis(
                    skill.number(SkillNumber.COOLDOWN),
                    Settings.general().getInt(SettingKey.HYPERBOLIC_SCALE_POINTS),
                    SkillAttributes.points(player, SkillAttributes.SKILL_HASTE));
            cooldowns.start(player, cooldownMillis);
        }
        if (session.timeline().beginExecution()) {
            restoreRegen(session, player);
        }
        session.fixFacing(player.rotationYaw);
        Diagnostics.log("ACTIVATION", "player={} skill={} result=accepted cooldownMs={} free={}",
                player.getName(), skill.id(), cooldownMillis, free);
        if (!free) {
            messenger.cooldownChanged(player, cooldownMillis, cooldownMillis);
        }
        enteredPhase(player, session, cause);
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
            if (session.phase() == SkillPhase.PREPARATION) {
                tickPreparation(player, session);
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

    /** One tick of a preparation: it ends when something forbids it, and is reported again now and then. */
    private void tickPreparation(EntityPlayerMP player, SkillSession session) {
        String cause = preparationEndCause(player, session);
        if (cause != null) {
            end(session, player, cause);
            return;
        }
        session.timeline().tick();
        // A preparation has no length, so clients tell a current report of one from an old one by its age.
        if (session.ticksInPhase() % PhaseUpdateMessage.OPEN_ENDED_REPORT_TICKS == 0) {
            messenger.phaseChanged(player, session);
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

    /** Taking damage or knockback ends a preparation or interrupts an execution, when the skill says so. */
    public void onHurt(EntityPlayerMP player, boolean knockback) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session != null && session.phase() == SkillPhase.PREPARATION) {
            if (PreparationRules.endedByHurt(session.skill(), knockback)) {
                end(session, player, knockback ? "knockback" : "damage");
            }
            return;
        }
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
        for (SkillSession session : sessions.values()) {
            if (session.phase() == SkillPhase.PREPARATION) {
                restoreRegen(session, null);
            }
        }
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
        Diagnostics.log("PHASE", "player={} skill={} phase={} lengthTicks={} cause={} serverTick={} sneaking={}",
                player.getName(), session.skill().id(), session.phase(), session.phaseLengthTicks(), cause,
                serverTick(player), player.isSneaking());
        messenger.phaseChanged(player, session);
    }

    /** The single way a skill use ends. The player is null when no longer on the server. */
    private void end(SkillSession session, EntityPlayerMP player, String cause) {
        sessions.remove(session.playerId());
        if (session.phase() == SkillPhase.PREPARATION) {
            restoreRegen(session, player);
        }
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
