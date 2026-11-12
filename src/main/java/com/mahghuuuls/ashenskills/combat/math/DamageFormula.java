package com.mahghuuuls.ashenskills.combat.math;

/** How much damage one skill hit deals. Every bonus enters exactly once. */
public final class DamageFormula {

    private DamageFormula() {
    }

    /**
     * @param attackDamage the user's attack damage with the weapon in hand: base, weapon, potions
     *     and other attribute modifiers
     * @param enchantmentBonus the weapon's damage-enchantment bonus against this target, 0 when the
     *     skill does not use damage enchantments
     * @param skillMultiplier the skill's damage multiplier
     * @param chargeBonus extra share from charging, 0 for none, 0.8 for +80 percent
     * @param skillDamagePoints the user's Ashen Skill Damage, in percent points
     */
    public static float hitDamage(
            double attackDamage, double enchantmentBonus, double skillMultiplier, double chargeBonus,
            double skillDamagePoints) {
        double weaponPart = attackDamage + enchantmentBonus;
        double damage = weaponPart * skillMultiplier * (1.0 + chargeBonus) * (1.0 + skillDamagePoints / 100.0);
        return (float) Math.max(0.0, damage);
    }
}
