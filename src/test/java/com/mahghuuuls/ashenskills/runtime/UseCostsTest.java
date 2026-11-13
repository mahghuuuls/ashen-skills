package com.mahghuuuls.ashenskills.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.config.DefaultsData;
import com.mahghuuuls.ashenskills.config.GeneralSettings;
import com.mahghuuuls.ashenskills.config.SettingKey;
import com.mahghuuuls.ashenskills.integration.PaymentResult;
import com.mahghuuuls.ashenskills.skill.BuiltInSkills;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class UseCostsTest {

    private static final double EXACT = 1.0e-9;

    @Test
    void theShippedSlashCooldownIsHalvedByAHundredHasteAtTheShippedScale() {
        DefaultsData defaults = DefaultsData.loadBundled();
        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", defaults);
        int scale = GeneralSettings.resolve(defaults, Collections.<SettingKey, String>emptyMap())
                .getInt(SettingKey.HYPERBOLIC_SCALE_POINTS);
        double cooldown = slash.number(SkillNumber.COOLDOWN);

        assertEquals(8000, UseCosts.cooldownMillis(cooldown, scale, 0));
        assertEquals(4000, UseCosts.cooldownMillis(cooldown, scale, 100));
        assertEquals(8000, UseCosts.cooldownMillis(cooldown, scale, -30));
    }

    @Test
    void efficiencyLowersACostTheSameWay() {
        assertEquals(15.0, UseCosts.resourceCost(15, 100, 0), EXACT);
        assertEquals(7.5, UseCosts.resourceCost(15, 100, 100), EXACT);
        assertEquals(12.0, UseCosts.resourceCost(15, 100, 25), EXACT);
        assertEquals(0.0, UseCosts.resourceCost(0, 100, 100), EXACT);
    }

    @Test
    void everyFailedPaymentHasAReasonThePlayerSeesAndAPaidOneHasNone() {
        assertNull(UseCosts.refusalFor(PaymentResult.PAID));
        assertEquals(RefusalReason.NO_STAMINA_SYSTEM, UseCosts.refusalFor(PaymentResult.STAMINA_MOD_MISSING));
        assertEquals(RefusalReason.NO_MANA_SYSTEM, UseCosts.refusalFor(PaymentResult.MANA_MOD_MISSING));
        assertEquals(RefusalReason.NOT_ENOUGH_STAMINA, UseCosts.refusalFor(PaymentResult.NOT_ENOUGH_STAMINA));
        assertEquals(RefusalReason.NOT_ENOUGH_MANA, UseCosts.refusalFor(PaymentResult.NOT_ENOUGH_MANA));
        for (PaymentResult result : PaymentResult.values()) {
            if (result != PaymentResult.PAID) {
                assertEquals(true, UseCosts.refusalFor(result).isShownToPlayer());
            }
        }
    }

    @Test
    void theShippedSlashCostsStaminaAndNoMana() {
        SkillDefinition slash = BuiltInSkills.definitionOf("powerful_slash", DefaultsData.loadBundled());

        assertEquals(15.0, slash.number(SkillNumber.STAMINA_COST), EXACT);
        assertEquals(0.0, slash.number(SkillNumber.MANA_COST), EXACT);
    }
}
