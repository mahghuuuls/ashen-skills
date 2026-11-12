package com.mahghuuuls.ashenskills.weapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.weapon.AttachedSkill.Status;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

class AttachedSkillTest {

    private static final SkillDefinition SLASH = SkillDefinition
            .builder(new ResourceLocation("test", "slash"), ActivationKind.INSTANT)
            .weaponTypes(types("sword", "greatsword"))
            .build();

    private static Set<String> types(String... names) {
        return new HashSet<>(Arrays.asList(names));
    }

    @Test
    void noSkillIdMeansNoSkill() {
        AttachedSkill attached = AttachedSkill.resolve(null, null, types("sword"));
        assertEquals(Status.NONE, attached.status());
        assertNull(attached.skillId());
        assertFalse(attached.isUsable());
    }

    @Test
    void aSkillThatSharesOneTypeWithTheWeaponIsCompatible() {
        AttachedSkill attached = AttachedSkill.resolve("test:slash", SLASH, types("katana", "sword"));
        assertEquals(Status.COMPATIBLE, attached.status());
        assertSame(SLASH, attached.definition());
        assertTrue(attached.isUsable());
    }

    @Test
    void aSkillThatSharesNoTypeStaysAttachedButIsIncompatible() {
        AttachedSkill otherTypes = AttachedSkill.resolve("test:slash", SLASH, types("katana"));
        assertEquals(Status.INCOMPATIBLE, otherTypes.status());
        assertEquals("test:slash", otherTypes.skillId());
        assertFalse(otherTypes.isUsable());

        AttachedSkill noTypes = AttachedSkill.resolve("test:slash", SLASH, Collections.<String>emptySet());
        assertEquals(Status.INCOMPATIBLE, noTypes.status());
    }

    @Test
    void anIdNoModProvidesStaysAttachedAndIsUnavailable() {
        AttachedSkill attached = AttachedSkill.resolve("other:missing", null, types("sword"));
        assertEquals(Status.UNAVAILABLE, attached.status());
        assertEquals("other:missing", attached.skillId());
        assertNull(attached.definition());
        assertFalse(attached.isUsable());
    }

    @Test
    void typesDoNotImplyOtherTypes() {
        assertFalse(WeaponTypes.shareAType(types("sword"), types("greatsword")));
        assertFalse(WeaponTypes.shareAType(types("great", "greatswords"), types("greatsword")));
        assertFalse(WeaponTypes.shareAType(Collections.<String>emptySet(), types("greatsword")));
        assertFalse(WeaponTypes.shareAType(types("sword"), Collections.<String>emptySet()));
        assertTrue(WeaponTypes.shareAType(types("katana", "greatsword"), types("greatsword", "sword")));
    }
}
