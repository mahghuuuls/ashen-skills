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
        assertEquals(set("grounded"), slash.allowedStates());
        assertEquals(false, slash.flag(SkillFlag.MOUNTED_ALLOWED));
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
