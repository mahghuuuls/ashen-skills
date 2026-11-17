package com.mahghuuuls.ashenskills.runtime;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.ChargeCurve;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillMotion;
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
 * <p>An instant skill executes when it is asked for. A stance skill and a charged skill first enter
 * a preparation that costs nothing. The attack button executes a stance; letting go of the key
 * executes a charge that lasted its minimum time. Everything that belongs to executing (payment,
 * cooldown, direction, the worth of the charge) happens at that moment, in {@link #execute}.
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
     * The skill key came up. A stance ends with it, at no cost. A charge that lasted its minimum
     * time executes, when it can be paid for; a shorter charge, or one that cannot be paid for,
     * ends at no cost.
     *
     * @return true when a preparation ended or executed
     */
    public boolean onKeyReleased(EntityPlayerMP player) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session == null || session.phase() != SkillPhase.PREPARATION) {
            return false;
        }
        if (session.skill().activation() == ActivationKind.CHARGED) {
            releaseCharge(player, session);
        } else {
            end(session, player, "keyReleased");
        }
        return true;
    }

    /**
     * The skill key stopped counting as held without the player letting go of it (a screen opened).
     * A preparation ends at no cost: a charge does not execute by this.
     *
     * @return true when a preparation ended
     */
    public boolean onKeyLost(EntityPlayerMP player) {
        SkillSession session = sessions.get(player.getUniqueID());
        if (session == null || session.phase() != SkillPhase.PREPARATION) {
            return false;
        }
        end(session, player, "keyLost");
        return true;
    }

    /** The key came up during a charge. The time charged is counted here, in server ticks, and nowhere else. */
    private void releaseCharge(EntityPlayerMP player, SkillSession session) {
        ChargeCurve curve = ChargeCurve.of(session.skill());
        double chargedSeconds = secondsOf(session.ticksInPhase());
        String endCause = preparationEndCause(player, session);
        if (endCause == null && !curve.reachesMinimum(chargedSeconds)) {
            endCause = "releasedEarly";
        }
        Diagnostics.log("CHARGE", "player={} skill={} chargedTicks={} chargedSeconds={} bonus={} result={}",
                player.getName(), session.skill().id(), session.ticksInPhase(), chargedSeconds,
                curve.bonus(chargedSeconds), endCause == null ? "release" : endCause);
        if (endCause != null) {
            end(session, player, endCause);
            return;
        }
        RefusalReason unpaid = payAndExecute(player, session.skill(), null, session, "chargeReleased");
        if (unpaid != null) {
            // The key is up, so the charge cannot go on waiting for the payment as a stance does.
            reportRefusal(player, session.skill().id().toString(), unpaid);
            end(session, player, "unpaid");
        }
    }

    /**
     * The attack button was pressed. In a stance this executes the skill, when it can be paid for
     * now; when it cannot, the player is told why and the stance goes on. A stance that something
     * ended in this same tick (a jump, another item) ends here, at no cost, and nothing executes.
     * A press during the wind-up of the stance is kept: the skill executes at the moment the
     * wind-up is over, if the stance is still held then.
     *
     * @return null when the skill executed, the reason when the payment failed,
     *     {@link RefusalReason#NOT_READY_YET} when the press was kept for the end of the wind-up,
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
        double windUp = session.skill().numberOr(SkillNumber.MIN_PREPARATION, 0);
        if (!WindUp.isOver(session.ticksInPhase(), windUp)) {
            session.keepAttack();
            Diagnostics.log("WIND_UP", "player={} skill={} ticksInStance={} ticksNeeded={} result=kept",
                    player.getName(), session.skill().id(), session.ticksInPhase(), WindUp.ticks(windUp));
            return RefusalReason.NOT_READY_YET;
        }
        return executeFromStance(player, session, "attackPressed");
    }

    /** Pays for and executes the skill of a held stance; when it cannot be paid, the player is told and the stance goes on. */
    private RefusalReason executeFromStance(EntityPlayerMP player, SkillSession session, String cause) {
        session.dropKeptAttack();
        RefusalReason unpaid = payAndExecute(player, session.skill(), null, session, cause);
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
        int preparedTicks = session.ticksInPhase();
        if (session.timeline().beginExecution()) {
            restoreRegen(session, player);
            if (skill.activation() == ActivationKind.CHARGED) {
                session.fixChargeBonus(ChargeCurve.of(skill).bonus(secondsOf(preparedTicks)));
            }
        }
        session.fixFacing(player.rotationYaw);
        Diagnostics.log("ACTIVATION", "player={} skill={} result=accepted cooldownMs={} free={}",
                player.getName(), skill.id(), cooldownMillis, free);
        if (!free) {
            messenger.cooldownChanged(player, cooldownMillis, cooldownMillis);
        }
        enteredPhase(player, session, cause);
        beginMotion(player, session);
    }

    /** Asks the skill how it moves its user, now that the direction is fixed. */
    private void beginMotion(EntityPlayerMP player, SkillSession session) {
        SkillBehavior behavior = SkillRegistry.behaviorOf(session.skill().id());
        SkillMotion motion = behavior == null
                ? null : behavior.motionAtExecution(new RuntimeSkillContext(player, session, messenger));
        if (motion == null) {
            return;
        }
        MotionPlan plan = new MotionPlan(motion, session.facingYaw());
        session.beginMotion(plan, player.posX, player.posZ);
        Diagnostics.log("MOTION", "player={} skill={} state=begun distance={} pushTicks={} yaw={} x={} z={}",
                player.getName(), session.skill().id(), motion.distance(), plan.pushTicks(), session.facingYaw(),
                player.posX, player.posZ);
    }

    /**
     * One tick of a skill's motion. The player's own client moves the player, as for every player
     * movement in the game; the server sends it the speed, and the client moves with its normal
     * collision. So the motion stops at solid blocks and the player is never placed anywhere.
     *
     * @return whether the player was pushed in this tick
     */
    private boolean tickMotion(EntityPlayerMP player, SkillSession session) {
        MotionPlan plan = session.motion();
        if (plan == null) {
            return false;
        }
        MotionPlan.Step step = plan.stepAt(session.timeline().executionTicksElapsed());
        if (step == MotionPlan.Step.PUSH) {
            sendSpeed(player, plan.pushX(), plan.pushZ());
            return true;
        }
        if (step == MotionPlan.Step.STOP) {
            stopMotion(player, session, "travelled");
        }
        return false;
    }

    /** Ends a motion that still runs: the player stops. Called on every way out of the execution too. */
    private void stopMotion(EntityPlayerMP player, SkillSession session, String cause) {
        if (session.motion() == null) {
            return;
        }
        double travelled = player == null ? Double.NaN : session.travelled(player.posX, player.posZ);
        session.endMotion();
        if (player != null) {
            sendSpeed(player, 0.0, 0.0);
        }
        Diagnostics.log("MOTION", "player={} skill={} state=stopped cause={} travelled={}",
                player == null ? session.playerId() : player.getName(), session.skill().id(), cause, travelled);
    }

    /**
     * Tells the player's client its speed. The game's message carries all three directions, so
     * the client's vertical speed becomes the one the server has for the player, each time.
     */
    private static void sendSpeed(EntityPlayerMP player, double x, double z) {
        player.motionX = x;
        player.motionZ = z;
        player.velocityChanged = true;
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
                // While the skill's own motion decides where the player is, being off the ground is
                // the skill's doing. That ends when the motion is over and the player stands again.
                boolean stateAllowed = isStateAllowed(player, session.skill());
                if (session.placeExcused()) {
                    if (stateAllowed && session.motionSettled()) {
                        session.endPlaceExcuse();
                        Diagnostics.log("MOTION", "player={} skill={} state=standsAgain",
                                player.getName(), session.skill().id());
                    }
                    stateAllowed = isAbleToAct(player, session.skill());
                }
                String cause = hasSwitchedWeapon(player, session)
                        ? "weaponSwitched"
                        : stateAllowed ? null : "stateNotAllowed";
                if (cause != null) {
                    interrupt(player, cause);
                }
            }
            boolean pushed = false;
            if (session.phase() == SkillPhase.EXECUTION) {
                pushed = tickMotion(player, session);
            } else if (session.stopOwed()) {
                stopMotion(player, session, "travelled");
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
                if (pushed) {
                    // A travel as long as the execution: its last push must still go out.
                    session.oweStop();
                }
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
        if (session.isAttackKept()
                && WindUp.isOver(session.ticksInPhase(), session.skill().numberOr(SkillNumber.MIN_PREPARATION, 0))) {
            Diagnostics.log("WIND_UP", "player={} skill={} ticksInStance={} result=over",
                    player.getName(), session.skill().id(), session.ticksInPhase());
            if (executeFromStance(player, session, "windUpOver") == null) {
                return;
            }
        }
        if (session.skill().activation() == ActivationKind.CHARGED) {
            if (ChargeCurve.of(session.skill())
                    .becomesFullAtTick(session.ticksInPhase(), SessionTimeline.TICKS_PER_SECOND)) {
                messenger.chargeFullShown(player, session);
            }
        }
        // A preparation has no length, so clients tell a current report of one from an old one by its age.
        if (session.ticksInPhase() % PhaseUpdateMessage.OPEN_ENDED_REPORT_TICKS == 0) {
            messenger.phaseChanged(player, session);
        }
    }

    private static double secondsOf(int ticks) {
        return ticks / (double) SessionTimeline.TICKS_PER_SECOND;
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
        if (session.phase() != SkillPhase.EXECUTION && !session.stopOwed()) {
            stopMotion(player, session, "executionEnded");
        }
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
        stopMotion(player, session, cause);
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
        return isAbleToAct(player, skill) && skill.allowedStates().contains(stateOf(player));
    }

    /** The part of the state rule that does not ask where the player is: alive, not a spectator, riding only when allowed. */
    private static boolean isAbleToAct(EntityPlayerMP player, SkillDefinition skill) {
        if (!player.isEntityAlive() || player.isSpectator()) {
            return false;
        }
        return !player.isRiding() || skill.flag(SkillFlag.MOUNTED_ALLOWED);
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
