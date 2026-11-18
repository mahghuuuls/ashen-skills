package com.mahghuuuls.ashenskills.combat;

import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.combat.TargetSelector.Area;
import com.mahghuuuls.ashenskills.combat.TargetSelector.Selection;
import com.mahghuuuls.ashenskills.combat.math.DamageFormula;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.diagnostics.Diagnostics;
import com.mahghuuuls.ashenskills.integration.CritGateway;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;

/**
 * One strike of a skill: finds the targets and damages each once. The damage is the user's weapon
 * attack scaled by the skill, credited to the user as a normal player attack, so armor, kill
 * credit and other mods' weapon rules apply as for a normal hit. The vanilla attack itself is not
 * used: it would add attack-cooldown scaling, sweeping, and every enchantment.
 */
public final class SkillDamage {

    /** Push per level of the Knockback enchantment, as the vanilla attack gives it. */
    private static final float ENCHANTMENT_PUSH_PER_LEVEL = 0.5F;
    /** Burn time per level of Fire Aspect, as the vanilla attack gives it. */
    private static final int FIRE_SECONDS_PER_LEVEL = 4;

    private static CritGateway critGateway = CritGateway.NONE;
    /** The creature being damaged right now by a skill that does not let Looting count, or null. */
    private static EntityLivingBase struckWithoutLooting;

    private SkillDamage() {
    }

    /** Names the critical-hit system to use. Called once while the game loads, if one is installed. */
    public static void setCritGateway(CritGateway gateway) {
        critGateway = gateway;
    }

    /**
     * The Looting level a death may use. The game asks while the damage is dealt, so only the
     * creature a no-Looting skill is hitting at this moment loses the weapon's Looting.
     *
     * @param dying the creature whose drops are being decided
     * @param level the level the game would use
     */
    public static int lootingLevelFor(EntityLivingBase dying, int level) {
        return dying == struckWithoutLooting ? 0 : level;
    }

    /** Told about each target a strike damaged, in the order they were damaged. */
    public interface StrikeListener {

        void onDamaged(Entity target);
    }

    /** The most targets one hit window may damage, by this installation's settings. */
    public static int targetCapPerWindow() {
        return Settings.general().getInt(SettingKey.HIT_TARGET_CAP_PER_WINDOW);
    }

    /**
     * One strike of a hit window, which may hold several.
     *
     * @param facingYaw the direction fixed when the skill executed
     * @param chargeBonus the extra share of damage this use earned by charging; 0 for none
     * @param skillDamagePoints the user's Ashen Skill Damage, in percent points
     * @param cap how many targets the window may still damage; 0 or less strikes nothing
     * @param struckBefore ids of the creatures the window has damaged already; they are passed over,
     *     and this strike adds the ones it damages
     * @param listener told about each damaged target
     * @return how many targets took damage
     */
    public static int strike(
            EntityPlayer user, ItemStack weapon, SkillDefinition skill, HitShape shape, float facingYaw,
            double chargeBonus, double skillDamagePoints, int cap, java.util.Set<Integer> struckBefore,
            StrikeListener listener) {
        if (cap <= 0) {
            return 0;
        }
        Area area = new Area(shape, facingYaw,
                skill.number(SkillNumber.REACH_BELOW), skill.number(SkillNumber.REACH_ABOVE));
        Selection selection = TargetSelector.select(
                user, area, cap, skill.flag(SkillFlag.SOLID_BLOCKS_OBSTRUCT), struckBefore);
        StringBuilder report = Diagnostics.enabled() ? new StringBuilder() : null;
        int[] damagedCount = {0};
        // One strike is one attack for the critical-hit system: all its targets share one roll.
        CritGateway.around(critGateway, user, weapon, () -> {
            for (Entity target : selection.targets) {
                float dealt = damage(user, weapon, skill, target, chargeBonus, skillDamagePoints);
                if (dealt >= 0) {
                    damagedCount[0]++;
                    struckBefore.add(TargetSelector.creatureId(target));
                    listener.onDamaged(target);
                }
                if (report != null) {
                    report.append(' ').append(target.getName()).append('#').append(target.getEntityId())
                            .append('=').append(dealt < 0 ? "none" : String.format(java.util.Locale.ROOT, "%.2f", dealt));
                }
            }
        });
        int damaged = damagedCount[0];
        if (report != null) {
            Diagnostics.log("HIT_WINDOW",
                    "player={} skill={} candidates={} inShape={} selected={} damaged={} serverTick={} dealt:{}",
                    user.getName(), skill.id(), selection.candidates, selection.inShape, selection.targets.size(),
                    damaged, user.getServer() == null ? -1 : user.getServer().getTickCounter(), report);
        }
        return damaged;
    }

    /** @return the health the target lost, or -1 when the game refused the damage */
    private static float damage(
            EntityPlayer user, ItemStack weapon, SkillDefinition skill, Entity target, double chargeBonus,
            double skillDamagePoints) {
        EntityLivingBase creature = livingOf(target);
        double enchantmentBonus = creature != null && skill.flag(SkillFlag.DAMAGE_ENCHANTMENTS)
                ? EnchantmentHelper.getModifierForCreature(weapon, creature.getCreatureAttribute())
                : 0.0;
        float amount = DamageFormula.hitDamage(
                user.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue(),
                enchantmentBonus,
                skill.number(SkillNumber.DAMAGE_MULTIPLIER),
                chargeBonus,
                skillDamagePoints);
        float healthBefore = creature == null ? 0 : creature.getHealth();
        struckWithoutLooting = skill.flag(SkillFlag.LOOTING) ? null : creature;
        boolean accepted;
        try {
            accepted = target.attackEntityFrom(DamageSource.causePlayerDamage(user), amount);
        } finally {
            struckWithoutLooting = null;
        }
        if (!accepted) {
            return -1;
        }
        applyOnHitEffects(user, skill, target, creature);
        return creature == null ? amount : healthBefore - creature.getHealth();
    }

    private static void applyOnHitEffects(
            EntityPlayer user, SkillDefinition skill, Entity target, EntityLivingBase creature) {
        if (skill.flag(SkillFlag.FIRE_ASPECT)) {
            int fireLevel = EnchantmentHelper.getFireAspectModifier(user);
            if (fireLevel > 0) {
                target.setFire(fireLevel * FIRE_SECONDS_PER_LEVEL);
            }
        }
        if (creature != null) {
            float push = (float) skill.number(SkillNumber.KNOCKBACK_STRENGTH);
            if (skill.flag(SkillFlag.KNOCKBACK_ENCHANTMENT)) {
                push += EnchantmentHelper.getKnockbackModifier(user) * ENCHANTMENT_PUSH_PER_LEVEL;
            }
            if (push > 0) {
                pushAway(user, creature, push);
            }
        }
    }

    /**
     * Pushes a creature straight away from the user. The game has already given its own small push
     * for the damage; this is the skill's push on top of it.
     */
    private static void pushAway(EntityPlayer user, EntityLivingBase creature, float strength) {
        double towardUserX = user.posX - creature.posX;
        double towardUserZ = user.posZ - creature.posZ;
        if (towardUserX * towardUserX + towardUserZ * towardUserZ < 1.0e-4) {
            // Standing inside the user: push the way the user looks.
            double yaw = Math.toRadians(user.rotationYaw);
            towardUserX = Math.sin(yaw);
            towardUserZ = -Math.cos(yaw);
        }
        creature.knockBack(user, strength, towardUserX, towardUserZ);
    }

    /** The living creature behind a target; for a part of a many-part creature, its owner. */
    private static EntityLivingBase livingOf(Entity target) {
        if (target instanceof EntityLivingBase) {
            return (EntityLivingBase) target;
        }
        if (target instanceof MultiPartEntityPart && ((MultiPartEntityPart) target).parent instanceof EntityLivingBase) {
            return (EntityLivingBase) ((MultiPartEntityPart) target).parent;
        }
        return null;
    }
}
