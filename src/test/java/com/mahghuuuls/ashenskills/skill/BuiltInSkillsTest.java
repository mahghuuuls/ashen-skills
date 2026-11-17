package com.mahghuuuls.ashenskills.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import java.io.StringReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BuiltInSkillsTest {

    private static DefaultsData file(String skills, String itemTypes) {
        return DefaultsData.parse(new StringReader(
                "{\"skills\":{" + skills + "},\"builtInWeaponTypes\":{\"vanillaItemTypes\":{" + itemTypes + "}}}"));
    }

    private static Set<String> set(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    @Test
    void aDefinitionTakesItsIdActivationAndTypesFromTheFile() {
        DefaultsData defaults = file("\"spin\":{\"activation\":\"stance\",\"weaponTypes\":[\"axe\",\"mace\"]}", "");

        SkillDefinition definition = BuiltInSkills.definitionOf("spin", defaults);

        assertEquals("ashenskills:spin", definition.id().toString());
        assertEquals(ActivationKind.STANCE, definition.activation());
        assertEquals(set("axe", "mace"), definition.weaponTypes());
    }

    @Test
    void anUnknownActivationOrAMissingSkillIsRejected() {
        DefaultsData defaults = file("\"spin\":{\"activation\":\"sometimes\",\"weaponTypes\":[]}", "");
        assertThrows(IllegalArgumentException.class, () -> BuiltInSkills.definitionOf("spin", defaults));
        assertThrows(IllegalArgumentException.class, () -> BuiltInSkills.definitionOf("absent", defaults));
    }

    @Test
    void builtInItemTypesAreReadPerItem() {
        DefaultsData defaults = file("", "\"mod:blade\":[\"sword\",\"katana\"],\"mod:club\":[\"mace\"]");

        Map<String, Set<String>> types = defaults.builtInItemTypes();

        assertEquals(set("mod:blade", "mod:club"), types.keySet());
        assertEquals(set("sword", "katana"), types.get("mod:blade"));
        assertEquals(set("mace"), types.get("mod:club"));
    }

    @Test
    void numbersAndRulesComeFromTheSkillAndElseFromTheCommonRules() {
        DefaultsData defaults = DefaultsData.parse(new StringReader("{\"skills\":{\"spin\":{"
                + "\"activation\":\"instant\",\"weaponTypes\":[\"axe\"],"
                + "\"cooldown\":3.5,\"mountedAllowed\":true}},"
                + "\"commonSkillRules\":{\"allowedStartStates\":[\"grounded\",\"swimming\"],"
                + "\"mountedAllowed\":false,\"itemUseDuringRecovery\":true,\"recovery\":0.25}}"));

        SkillDefinition spin = BuiltInSkills.definitionOf("spin", defaults);

        assertEquals(3.5, spin.number(SkillNumber.COOLDOWN));
        assertEquals(0.25, spin.number(SkillNumber.RECOVERY));
        assertEquals(true, spin.flag(SkillFlag.MOUNTED_ALLOWED));
        assertEquals(true, spin.flag(SkillFlag.ITEM_USE_DURING_RECOVERY));
        assertEquals(set("grounded", "swimming"), spin.allowedStates());
        assertEquals(true, spin.missingValues().contains("executionLength"));
        assertEquals(false, spin.missingValues().contains("cooldown"));
        assertThrows(IllegalStateException.class, () -> spin.number(SkillNumber.EXECUTION_LENGTH));
        assertThrows(IllegalStateException.class, () -> spin.flag(SkillFlag.BLOCKING_DURING_EXECUTION));
    }

    @Test
    void theShippedPowerfulSlashIsCompleteAndStartsOnTheGroundOnly() {
        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", DefaultsData.loadBundled());

        assertEquals(java.util.Collections.emptyList(), slash.missingValues());
        assertEquals(java.util.Arrays.asList(0.30), slash.hitWindowSeconds());
        assertEquals(1.8, slash.number(SkillNumber.DAMAGE_MULTIPLIER));
        assertEquals(3.5, slash.number(SkillNumber.REACH));
        assertEquals(110.0, slash.number(SkillNumber.ARC));
        assertEquals(0.6, slash.number(SkillNumber.KNOCKBACK_STRENGTH));
        assertEquals(12.0, slash.number(SkillNumber.IMPACT_PARTICLES));
        assertEquals(1.0, slash.number(SkillNumber.REACH_BELOW));
        assertEquals(3.0, slash.number(SkillNumber.REACH_ABOVE));
        assertEquals(true, slash.flag(SkillFlag.DAMAGE_ENCHANTMENTS));
        assertEquals(true, slash.flag(SkillFlag.FIRE_ASPECT));
        assertEquals(false, slash.flag(SkillFlag.KNOCKBACK_ENCHANTMENT));
        assertEquals(true, slash.flag(SkillFlag.LOOTING));
        assertEquals(true, slash.flag(SkillFlag.SOLID_BLOCKS_OBSTRUCT));
        assertThrows(IllegalStateException.class, () -> slash.number(SkillNumber.WIDTH));
        assertEquals(set("grounded"), slash.allowedStates());
        assertEquals(false, slash.flag(SkillFlag.MOUNTED_ALLOWED));
        assertEquals(false, slash.flag(SkillFlag.JUMP_DURING_EXECUTION));
        assertEquals(false, slash.flag(SkillFlag.SNEAKING_DURING_SKILL));
    }

    @Test
    void theShippedUnsheatheIsACompleteStanceSkillForTheKatana() {
        SkillDefinition unsheathe = BuiltInSkills.definitionOf("unsheathe", DefaultsData.loadBundled());

        assertEquals("ashenskills:unsheathe", unsheathe.id().toString());
        assertEquals(ActivationKind.STANCE, unsheathe.activation());
        assertEquals(0.5, unsheathe.number(SkillNumber.MIN_PREPARATION));
        assertEquals(0.0, BuiltInSkills.definitionOf("powerful_slash", DefaultsData.loadBundled())
                .numberOr(SkillNumber.MIN_PREPARATION, 0.0));
        assertEquals(set("katana"), unsheathe.weaponTypes());
        assertEquals(java.util.Collections.emptyList(), unsheathe.missingValues());
        assertEquals(java.util.Arrays.asList(0.3), unsheathe.hitWindowSeconds());
        assertEquals(2.0, unsheathe.number(SkillNumber.DAMAGE_MULTIPLIER));
        // The cut is made on a short dash: a wide band along the dash, not an arc on the spot.
        assertEquals(2.5, unsheathe.number(SkillNumber.REACH));
        assertEquals(3.0, unsheathe.number(SkillNumber.WIDTH));
        assertEquals(3.0, unsheathe.number(SkillNumber.TRAVEL_DISTANCE));
        assertEquals(0.2, unsheathe.number(SkillNumber.TRAVEL_TIME));
        // The dash is not steered: no walking speed while it runs.
        assertEquals(0.0, unsheathe.number(SkillNumber.EXECUTION_SPEED));
        // The skill that carries its user and cuts a band, not the arc on the spot.
        assertEquals(com.mahghuuuls.ashenskills.skill.builtin.DashStrike.class, BuiltInSkills.behaviorOf("unsheathe").getClass());
        assertEquals(com.mahghuuuls.ashenskills.skill.builtin.DashStrike.class, BuiltInSkills.behaviorOf("piercing_lunge").getClass());
        assertEquals(com.mahghuuuls.ashenskills.skill.builtin.ArcStrike.class, BuiltInSkills.behaviorOf("powerful_slash").getClass());
        assertEquals(com.mahghuuuls.ashenskills.skill.builtin.ArcStrike.class, BuiltInSkills.behaviorOf("crushing_blow").getClass());
        assertEquals(12.0, unsheathe.number(SkillNumber.STAMINA_COST));
        assertEquals(10.0, unsheathe.number(SkillNumber.COOLDOWN));
        assertEquals(0.5, unsheathe.number(SkillNumber.PREPARATION_SPEED));
        assertEquals(0.2, unsheathe.number(SkillNumber.PREPARATION_STAMINA_REGEN));
        assertEquals(true, unsheathe.flag(SkillFlag.PREPARATION_CANCELED_BY_DAMAGE));
        assertEquals(true, unsheathe.flag(SkillFlag.PREPARATION_CANCELED_BY_KNOCKBACK));
    }

    @Test
    void aChargedSkillWithoutItsChargeValuesIsRefusedAtRegistration() {
        SkillDefinition.Builder charged = SkillDefinition.builder(
                new net.minecraft.util.ResourceLocation("test", "charged_blow"), ActivationKind.CHARGED);
        for (SkillNumber number : SkillNumber.values()) {
            if (number.isRequiredForEverySkill()) {
                charged.number(number, 1);
            }
        }
        for (SkillFlag flag : SkillFlag.values()) {
            charged.flag(flag, false);
        }
        charged.number(SkillNumber.PREPARATION_SPEED, 0.2).number(SkillNumber.PREPARATION_STAMINA_REGEN, 0);
        SkillDefinition incomplete = charged.build();

        assertEquals(java.util.Arrays.asList("minCharge", "fullCharge", "maxChargeBonus"), incomplete.missingValues());
        IllegalStateException refusal = assertThrows(IllegalStateException.class,
                () -> SkillRegistry.register(incomplete, (context, window) -> { }));
        org.junit.jupiter.api.Assertions.assertTrue(refusal.getMessage().contains("minCharge"), refusal.getMessage());
        org.junit.jupiter.api.Assertions.assertNull(SkillRegistry.get(incomplete.id()));
    }

    @Test
    void theShippedPiercingLungeIsACompleteInstantSkillWithALineAndATravel() {
        SkillDefinition lunge = BuiltInSkills.definitionOf("piercing_lunge", DefaultsData.loadBundled());

        assertEquals("ashenskills:piercing_lunge", lunge.id().toString());
        assertEquals(ActivationKind.INSTANT, lunge.activation());
        assertEquals(set("spear", "pike", "rapier"), lunge.weaponTypes());
        assertEquals(java.util.Collections.emptyList(), lunge.missingValues());
        assertEquals(1.6, lunge.number(SkillNumber.DAMAGE_MULTIPLIER));
        assertEquals(4.0, lunge.number(SkillNumber.REACH));
        assertEquals(1.0, lunge.number(SkillNumber.WIDTH));
        assertEquals(4.0, lunge.number(SkillNumber.TRAVEL_DISTANCE));
        assertEquals(0.3, lunge.number(SkillNumber.TRAVEL_TIME));
        assertEquals(0.0, lunge.number(SkillNumber.EXECUTION_SPEED));
        assertThrows(IllegalStateException.class, () -> lunge.number(SkillNumber.ARC));
    }

    @Test
    void theShippedCrushingBlowIsACompleteChargedSkillForWarhammerAndMace() {
        SkillDefinition blow = BuiltInSkills.definitionOf("crushing_blow", DefaultsData.loadBundled());

        assertEquals("ashenskills:crushing_blow", blow.id().toString());
        assertEquals(ActivationKind.CHARGED, blow.activation());
        assertEquals(set("warhammer", "mace"), blow.weaponTypes());
        assertEquals(java.util.Collections.emptyList(), blow.missingValues());
        assertEquals(java.util.Arrays.asList(0.25), blow.hitWindowSeconds());
        assertEquals(1.5, blow.number(SkillNumber.DAMAGE_MULTIPLIER));
        assertEquals(0.5, blow.number(SkillNumber.MIN_CHARGE));
        assertEquals(2.0, blow.number(SkillNumber.FULL_CHARGE));
        assertEquals(0.8, blow.number(SkillNumber.MAX_CHARGE_BONUS));
        assertEquals(0.2, blow.number(SkillNumber.PREPARATION_SPEED));
        assertEquals(0.0, blow.number(SkillNumber.PREPARATION_STAMINA_REGEN));
        assertEquals(false, blow.flag(SkillFlag.PREPARATION_CANCELED_BY_DAMAGE));
        assertEquals(false, blow.flag(SkillFlag.PREPARATION_CANCELED_BY_KNOCKBACK));
        assertEquals(60.0, blow.number(SkillNumber.ARC));
    }

    @Test
    void aSkillWithAPreparationNeedsThePreparationValuesAndAnInstantSkillDoesNot() {
        SkillDefinition instant = BuiltInSkills.definitionOf("powerful_slash", DefaultsData.loadBundled());
        assertEquals(java.util.Collections.emptyList(), instant.missingValues());
        assertThrows(IllegalStateException.class, () -> instant.number(SkillNumber.PREPARATION_SPEED));
        assertThrows(IllegalStateException.class, () -> instant.flag(SkillFlag.PREPARATION_CANCELED_BY_DAMAGE));

        SkillDefinition.Builder stance = SkillDefinition.builder(
                new net.minecraft.util.ResourceLocation("test", "stance"), ActivationKind.STANCE);
        for (SkillNumber number : SkillNumber.values()) {
            if (number.isRequiredForEverySkill()) {
                stance.number(number, 1);
            }
        }
        for (SkillFlag flag : SkillFlag.values()) {
            if (flag.isRequiredForEverySkill()) {
                stance.flag(flag, false);
            }
        }

        assertEquals(java.util.Arrays.asList("preparationSpeed", "preparationStaminaRegen",
                "preparationCanceledByDamage", "preparationCanceledByKnockback"), stance.build().missingValues());
    }

    /**
     * States the shipped identity values on purpose, as a second view of the defaults file: a
     * changed activation, type list or sword list must be a deliberate change to both.
     */
    @Test
    void theShippedFileDefinesPowerfulSlashAndTheFiveVanillaSwords() {
        DefaultsData shipped = DefaultsData.loadBundled();

        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", shipped);

        assertEquals("ashenskills:powerful_slash", slash.id().toString());
        assertEquals(ActivationKind.INSTANT, slash.activation());
        assertEquals(set("sword", "longsword", "greatsword"), slash.weaponTypes());

        Map<String, Set<String>> itemTypes = shipped.builtInItemTypes();
        assertEquals(set("minecraft:wooden_sword", "minecraft:stone_sword", "minecraft:iron_sword",
                "minecraft:golden_sword", "minecraft:diamond_sword"), itemTypes.keySet());
        for (Map.Entry<String, Set<String>> item : itemTypes.entrySet()) {
            assertEquals(set("sword"), item.getValue(), item.getKey());
        }
    }
}
