package com.mahghuuuls.ashenskills.client.input;

import com.mahghuuuls.ashenskills.client.ClientSwitches;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState;
import com.mahghuuuls.ashenskills.client.state.ClientSkillState.PhaseView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Keeps the own player's client in step with the locks the server reports for the current skill
 * phase: a locked attack button does nothing, and a locked jump does not lift the player. The
 * server refuses a locked attack anyway; without this the client would still show an arm swing or
 * crack a block. A jump is the client's own action, so this class is what keeps that rule.
 *
 * <p>While the attack is locked the attack key reads as released, so a button held through a skill
 * must be pressed again afterward; any other action bound to the same key is released with it.
 */
public final class SkillInputLock {

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
     * next tick starts, so they are dropped at each of those moments.
     */
    private static void dropAttackPresses() {
        PhaseView phase = ownPhase();
        if (phase == null || !phase.attackLocked) {
            return;
        }
        KeyBinding attack = Minecraft.getMinecraft().gameSettings.keyBindAttack;
        KeyBinding.setKeyBindState(attack.getKeyCode(), false);
        while (attack.isPressed()) {
            // Each call takes one waiting press.
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

    /** The jump key. This also stops swimming upward for the length of the lock. */
    @SubscribeEvent
    public void onMovementInput(InputUpdateEvent event) {
        if (isJumpLocked()) {
            event.getMovementInput().jump = false;
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
