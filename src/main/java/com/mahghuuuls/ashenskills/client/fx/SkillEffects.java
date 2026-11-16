package com.mahghuuuls.ashenskills.client.fx;

import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.network.EffectCueMessage;
import com.mahghuuuls.ashenskills.sound.SkillSounds;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

/**
 * What a skill's moments look and sound like on this client. A swing plays the skill's swing sound
 * and, for a skill without a cut trail, one sweep mark; an impact plays the skill's impact sound and a burst of sparks at the target,
 * within the player's particle limit for one skill use; a full charge plays the skill's charged
 * sound and a few sparks at the user. Only particles, light and sounds: no block and no entity is changed.
 */
public final class SkillEffects {

    /** How far from the middle of a target a spark may start, in blocks. A look, not a game rule. */
    private static final double SPARK_SPREAD = 0.35;
    /** How fast a spark may fly off, in blocks per tick. A look, not a game rule. */
    private static final double SPARK_SPEED = 0.25;

    /** How many sparks a full charge shows at the user. A look, not a game rule. */
    private static final int FULL_CHARGE_SPARKS = 6;
    private static final ParticleBudget BUDGET = new ParticleBudget();

    private SkillEffects() {
    }

    /**
     * @param kind a kind code of {@link EffectCueMessage}
     * @param particles how many impact particles the skill asks for
     */
    public static void play(
            byte kind, int userEntityId, int useId, String skillId, double x, double y, double z, int particles) {
        WorldClient world = Minecraft.getMinecraft().world;
        if (world == null) {
            return;
        }
        if (kind == EffectCueMessage.FULL_CHARGE) {
            playFullCharge(world, userEntityId, useId, skillId, x, y, z);
            return;
        }
        boolean impact = kind == EffectCueMessage.IMPACT;
        if (!impact && kind != EffectCueMessage.SWING) {
            return;
        }
        SoundEvent sound = SkillSounds.of(skillId, impact ? SkillSounds.IMPACT : SkillSounds.SWING);
        world.playSound(x, y, z, sound, SoundCategory.PLAYERS, 1.0F, 1.0F, false);
        int spawned;
        if (impact) {
            int limit = Settings.general().getInt(SettingKey.MAX_IMPACT_PARTICLES_PER_SKILL);
            spawned = BUDGET.take(userEntityId, useId, particles, limit);
            Random random = world.rand;
            for (int i = 0; i < spawned; i++) {
                world.spawnParticle(
                        EnumParticleTypes.CRIT,
                        x + (random.nextDouble() - 0.5) * 2 * SPARK_SPREAD,
                        y + (random.nextDouble() - 0.5) * 2 * SPARK_SPREAD,
                        z + (random.nextDouble() - 0.5) * 2 * SPARK_SPREAD,
                        (random.nextDouble() - 0.5) * 2 * SPARK_SPEED,
                        random.nextDouble() * SPARK_SPEED,
                        (random.nextDouble() - 0.5) * 2 * SPARK_SPEED);
            }
        } else if (CutTrails.has(skillId)) {
            // The skill's own trail is the mark of this cut.
            spawned = 0;
        } else {
            world.spawnParticle(EnumParticleTypes.SWEEP_ATTACK, x, y, z, 0.0, 0.0, 0.0);
            spawned = 1;
        }
        Diagnostics.log("CLIENT_CUE", "kind={} user={} use={} skill={} sound={} particles={} x={} y={} z={}",
                impact ? "IMPACT" : "SWING", userEntityId, useId, skillId, sound.getSoundName(), spawned,
                round(x), round(y), round(z));
    }

    private static void playFullCharge(
            WorldClient world, int userEntityId, int useId, String skillId, double x, double y, double z) {
        SoundEvent sound = SkillSounds.of(skillId, SkillSounds.CHARGED);
        world.playSound(x, y, z, sound, SoundCategory.PLAYERS, 0.7F, 1.0F, false);
        Random random = world.rand;
        for (int i = 0; i < FULL_CHARGE_SPARKS; i++) {
            world.spawnParticle(
                    EnumParticleTypes.CRIT_MAGIC,
                    x + (random.nextDouble() - 0.5) * 2 * SPARK_SPREAD, y + random.nextDouble() * 0.8,
                    z + (random.nextDouble() - 0.5) * 2 * SPARK_SPREAD, 0.0, SPARK_SPEED / 2, 0.0);
        }
        Diagnostics.log("CLIENT_CUE", "kind=FULL_CHARGE user={} use={} skill={} sound={} particles={} x={} y={} z={}",
                userEntityId, useId, skillId, sound.getSoundName(), FULL_CHARGE_SPARKS, round(x), round(y), round(z));
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /** Forgets the particle counts and the trails in the air; for leaving a world. */
    public static void reset() {
        BUDGET.clear();
        CutTrails.reset();
    }
}
