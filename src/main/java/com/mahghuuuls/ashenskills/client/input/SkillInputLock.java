package com.mahghuuuls.ashenskills.client.input;

import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.client.ClientSwitches;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState.PhaseView;
import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.network.PhaseUpdateMessage;
import com.mahghuuuls.ashenskills.network.SkillInputMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInput;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Keeps the own player's client in step with the locks the server reports for the current skill
 * phase: a locked attack button does nothing, a locked jump does not lift the player, and under
 * a sneak lock a sneaking player stands up until the lock ends. The
 * server refuses a locked attack anyway; without this the client would still show an arm swing or
 * crack a block. Jumping and sneaking are the client's own actions, so this class is what keeps
 * those rules.
 *
 * <p>While the attack is locked the attack key reads as released, so a button held through a skill
 * must be pressed again afterward; any other action bound to the same key is released with it.
 *
 * <p>During a preparation the attack button is the skill's own input: a press does no normal
 * attack and is reported to the server, which decides whether the skill executes.
 */
public final class SkillInputLock {

    /** The share of walking speed the game leaves a sneaking player. */
    private static final float SNEAK_SLOWING = 0.3F;
    private static final byte PREPARATION = PhaseUpdateMessage.codeOf(SkillPhase.PREPARATION);

    private SkillInputLock() {
    }

    public static void register() {
        if (!ClientSwitches.isLeftOut(ClientSwitches.INPUT_LOCK)) {
            MinecraftForge.EVENT_BUS.register(new SkillInputLock());
        }
    }

    /** The own player's phase as last reported, or null. */
    private static PhaseView ownPhase() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.player == null || !ClientSkillState.hasAnyPhase()) {
            return null;
        }
        return ClientSkillState.phaseOf(minecraft.player.getEntityId());
    }

    private static boolean isJumpLocked() {
        PhaseView phase = ownPhase();
        return phase != null && phase.jumpLocked;
    }

    /**
     * Throws away attack presses. The game acts on them after its input events and before the
     * next tick starts, so they are dropped at each of those moments. A press thrown away during a
     * preparation is reported to the server, once.
     */
    private static void dropAttackPresses() {
        PhaseView phase = ownPhase();
        if (phase == null || !phase.attackLocked) {
            return;
        }
        KeyBinding attack = Minecraft.getMinecraft().gameSettings.keyBindAttack;
        KeyBinding.setKeyBindState(attack.getKeyCode(), false);
        boolean pressed = false;
        while (attack.isPressed()) {
            // Each call takes one waiting press.
            pressed = true;
        }
        if (pressed && phase.phaseCode == PREPARATION) {
            AshenNetwork.channel().sendToServer(new SkillInputMessage(SkillInputMessage.ATTACK_PRESS));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        dropAttackPresses();
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        dropAttackPresses();
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        dropAttackPresses();
    }

    /**
     * The jump key and the sneak key. A locked jump also stops swimming upward for the length of
     * the lock. The game has already slowed this tick's walking for sneaking when this runs, so
     * that slowing is taken back with the sneak.
     */
    @SubscribeEvent
    public void onMovementInput(InputUpdateEvent event) {
        PhaseView phase = ownPhase();
        if (phase == null) {
            return;
        }
        MovementInput input = event.getMovementInput();
        if (phase.jumpLocked) {
            input.jump = false;
        }
        if (phase.sneakLocked && input.sneak) {
            input.sneak = false;
            input.moveStrafe /= SNEAK_SLOWING;
            input.moveForward /= SNEAK_SLOWING;
        }
    }

    /**
     * A jump the game starts by itself (auto-jump at a block edge) does not pass the key, so its
     * lift is taken away here, at the moment the jump happens.
     */
    @SubscribeEvent
    public void onJump(LivingEvent.LivingJumpEvent event) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (event.getEntityLiving() == minecraft.player && isJumpLocked()) {
            minecraft.player.motionY = 0.0;
        }
    }
}
