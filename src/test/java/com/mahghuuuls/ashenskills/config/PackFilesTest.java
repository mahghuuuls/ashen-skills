package com.mahghuuuls.ashenskills.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import com.mahghuuuls.ashenskills.skill.BuiltInSkills;
import com.mahghuuuls.ashenskills.weapon.TypeRules;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

/** The pack files: what a good entry does, and that a bad entry is named and changes nothing else. */
class PackFilesTest {

    private static final DefaultsData DEFAULTS = DefaultsData.loadBundled();
    private static final ResourceLocation SLASH_ID = new ResourceLocation("ashenskills", "powerful_slash");
    private static final SkillDefinition SLASH = BuiltInSkills.definitionOf("powerful_slash", DEFAULTS);
    private static final Set<String> EXISTING_ITEMS =
            new HashSet<>(Arrays.asList("minecraft:stick", "minecraft:diamond_sword", "spartanweaponry:katana_iron"));

    private static JsonObject json(String text) {
        return new JsonParser().parse(text.replace('\'', '"')).getAsJsonObject();
    }

    private static TypeRules rules(String text, List<String> problems) {
        return TypeRulesFile.parse(json(text), DEFAULTS.builtInTypeIds(), new Predicate<String>() {
            @Override
            public boolean test(String name) {
                return EXISTING_ITEMS.contains(name);
            }
        }, problems);
    }

    private static SkillDefinition overridden(String entries, List<String> problems) {
        Map<ResourceLocation, SkillDefinition> registered = new LinkedHashMap<>();
        registered.put(SLASH_ID, SLASH);
        Set<String> knownTypes = new HashSet<>(DEFAULTS.builtInTypeIds());
        knownTypes.add("club_heavy");
        return SkillOverrides.apply(
                json("{'ashenskills:powerful_slash': " + entries + "}"), registered, DEFAULTS, knownTypes, problems)
                .get(SLASH_ID);
    }

    private static SkillDefinition overriddenUnsheathe(String entries, List<String> problems) {
        ResourceLocation id = new ResourceLocation("ashenskills", "unsheathe");
        Map<ResourceLocation, SkillDefinition> registered = new LinkedHashMap<>();
        registered.put(id, BuiltInSkills.definitionOf("unsheathe", DEFAULTS));
        return SkillOverrides.apply(
                json("{'ashenskills:unsheathe': " + entries + "}"), registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems)
                .get(id);
    }

    @Test
    void aPackChangesThePreparationValuesOfAStanceSkillWithinTheirRanges() {
        List<String> problems = new ArrayList<>();

        SkillDefinition changed = overriddenUnsheathe(
                "{'preparationSpeed': 0.8, 'preparationStaminaRegen': 0, 'preparationCanceledByDamage': false}", problems);

        assertEquals(Collections.emptyList(), problems);
        assertEquals(0.8, changed.number(SkillNumber.PREPARATION_SPEED));
        assertEquals(0.0, changed.number(SkillNumber.PREPARATION_STAMINA_REGEN));
        assertEquals(false, changed.flag(SkillFlag.PREPARATION_CANCELED_BY_DAMAGE));
        assertEquals(true, changed.flag(SkillFlag.PREPARATION_CANCELED_BY_KNOCKBACK));

        overriddenUnsheathe("{'preparationSpeed': 1.5, 'preparationStaminaRegen': 2}", problems);
        assertEquals(2, problems.size(), problems.toString());
    }

    @Test
    void aPackChangesTheWindUpOfAStanceWithinItsRange() {
        List<String> problems = new ArrayList<>();

        assertEquals(0.0, overriddenUnsheathe("{'minPreparation': 0}", problems).number(SkillNumber.MIN_PREPARATION));
        assertEquals(1.2, overriddenUnsheathe("{'minPreparation': 1.2}", problems).number(SkillNumber.MIN_PREPARATION));
        assertEquals(Collections.emptyList(), problems);

        assertEquals(0.5, overriddenUnsheathe("{'minPreparation': 6}", problems).number(SkillNumber.MIN_PREPARATION));
        assertEquals(1, problems.size(), problems.toString());
    }

    @Test
    void aFullChargeBelowTheMinimumChargeIsReportedAndBothKeepTheSkillsOwnValues() {
        ResourceLocation id = new ResourceLocation("ashenskills", "crushing_blow");
        Map<ResourceLocation, SkillDefinition> registered = new LinkedHashMap<>();
        registered.put(id, BuiltInSkills.definitionOf("crushing_blow", DEFAULTS));
        List<String> problems = new ArrayList<>();

        SkillDefinition changed = SkillOverrides.apply(
                json("{'ashenskills:crushing_blow': {'minCharge': 3, 'fullCharge': 2.5, 'maxChargeBonus': 1.2}}"),
                registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems).get(id);

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("shorter than the minimum charge"), problems.get(0));
        assertEquals(0.5, changed.number(SkillNumber.MIN_CHARGE));
        assertEquals(2.0, changed.number(SkillNumber.FULL_CHARGE));
        assertEquals(1.2, changed.number(SkillNumber.MAX_CHARGE_BONUS));

        // One value alone can break the pair too: the file's value is compared with the skill's other one.
        problems.clear();
        SkillDefinition oneSided = SkillOverrides.apply(
                json("{'ashenskills:crushing_blow': {'fullCharge': 0.3}}"),
                registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems).get(id);
        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("'fullCharge', 0.3") && problems.get(0).contains("'minCharge', 0.5"),
                problems.get(0));
        assertEquals(2.0, oneSided.number(SkillNumber.FULL_CHARGE));

        problems.clear();
        SkillDefinition longer = SkillOverrides.apply(
                json("{'ashenskills:crushing_blow': {'minCharge': 1, 'fullCharge': 4}}"),
                registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems).get(id);
        assertEquals(Collections.emptyList(), problems);
        assertEquals(1.0, longer.number(SkillNumber.MIN_CHARGE));
        assertEquals(4.0, longer.number(SkillNumber.FULL_CHARGE));
    }

    @Test
    void aTravelLongerThanTheExecutionIsAMistakeAndTheSkillKeepsItsOwnTiming() {
        ResourceLocation id = new ResourceLocation("ashenskills", "piercing_lunge");
        Map<ResourceLocation, SkillDefinition> registered = new LinkedHashMap<>();
        registered.put(id, BuiltInSkills.definitionOf("piercing_lunge", DEFAULTS));
        List<String> problems = new ArrayList<>();

        SkillDefinition changed = SkillOverrides.apply(
                json("{'ashenskills:piercing_lunge': {'travelTime': 0.6, 'travelDistance': 6}}"),
                registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems).get(id);

        assertEquals(1, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("lasts longer than the execution"), problems.get(0));
        assertEquals(0.3, changed.number(SkillNumber.TRAVEL_TIME));
        assertEquals(0.4, changed.number(SkillNumber.EXECUTION_LENGTH));
        assertEquals(6.0, changed.number(SkillNumber.TRAVEL_DISTANCE));

        // A shorter execution alone breaks the pair too.
        problems.clear();
        SkillDefinition shorter = SkillOverrides.apply(
                json("{'ashenskills:piercing_lunge': {'executionLength': 0.28, 'hitWindows': [{'at': 0.2}]}}"),
                registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems).get(id);
        assertEquals(1, problems.size(), problems.toString());
        assertEquals(0.4, shorter.number(SkillNumber.EXECUTION_LENGTH));
        assertEquals(Arrays.asList(0.25), shorter.hitWindowSeconds());

        // A travel as long as the execution is allowed.
        problems.clear();
        SkillDefinition whole = SkillOverrides.apply(
                json("{'ashenskills:piercing_lunge': {'travelTime': 0.4}}"),
                registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems).get(id);
        assertEquals(Collections.emptyList(), problems);
        assertEquals(0.4, whole.number(SkillNumber.TRAVEL_TIME));
    }

    @Test
    void aPreparationValueOnAnInstantSkillIsAMistake() {
        List<String> problems = new ArrayList<>();

        SkillDefinition slash = overridden("{'preparationSpeed': 0.8, 'preparationCanceledByDamage': true}", problems);

        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("does not use this value"), problems.get(0));
        assertTrue(problems.get(1).contains("does not use this value"), problems.get(1));
        assertEquals(Collections.emptyList(), slash.missingValues());
    }

    private static Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    @Test
    void aPackTypeCanBeAddedToAnItemAndAReplaceRuleStandsInPlaceOfTheBuiltInTypes() {
        List<String> problems = new ArrayList<>();
        TypeRules rules = rules("{'types': ['club_heavy'], 'items': {"
                + "'minecraft:stick': {'add': ['club_heavy']},"
                + "'minecraft:diamond_sword': {'add': ['katana']},"
                + "'spartanweaponry:katana_iron': {'replace': ['sword']}}}", problems);

        assertEquals(Collections.emptyList(), problems);
        assertEquals(set("club_heavy"), rules.packTypes());
        assertEquals(set("club_heavy"), rules.effectiveTypes("minecraft:stick", Collections.<String>emptySet()));
        assertEquals(set("sword", "katana"), rules.effectiveTypes("minecraft:diamond_sword", set("sword")));
        assertEquals(set("sword"), rules.effectiveTypes("spartanweaponry:katana_iron", set("katana")));
        assertEquals(set("sword"), rules.effectiveTypes("minecraft:iron_sword", set("sword")));
    }

    @Test
    void eachWrongTypeEntryIsNamedAndLeftOutWhileTheOthersApply() {
        List<String> problems = new ArrayList<>();
        TypeRules rules = rules("{'types': ['club_heavy', 'Bad Type'], 'extra': 1, 'items': {"
                + "'minecraft:no_such_item': {'add': ['sword']},"
                + "'minecraft:stick': {'add': ['club_heavy']},"
                + "'minecraft:diamond_sword': {'add': ['never_defined']},"
                + "'spartanweaponry:katana_iron': {'add': ['sword'], 'replace': ['sword']}}}", problems);

        assertEquals(5, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("'types'") && problems.get(0).contains("Bad Type"), problems.get(0));
        assertTrue(problems.get(1).contains("'items.minecraft:no_such_item'") && problems.get(1).contains("no item"),
                problems.get(1));
        assertTrue(problems.get(2).contains("'items.minecraft:diamond_sword'") && problems.get(2).contains("never_defined"),
                problems.get(2));
        assertTrue(problems.get(3).contains("'items.spartanweaponry:katana_iron'"), problems.get(3));
        assertTrue(problems.get(4).contains("'extra'"), problems.get(4));
        assertEquals(set("club_heavy"), rules.packTypes());
        assertEquals(Collections.singleton("minecraft:stick"), rules.items().keySet());
    }

    @Test
    void aSkillFileChangesTheValuesItNamesAndNoOthers() {
        List<String> problems = new ArrayList<>();
        SkillDefinition changed = overridden("{'cooldown': 20, 'damageEnchantments': false,"
                + " 'weaponTypes': ['sword', 'club_heavy'], 'allowedStartStates': ['grounded', 'airborne'],"
                + " 'executionLength': 0.6, 'hitWindows': [{'at': 0.2}, {'at': 0.6}], '_note': 'ignored'}", problems);

        assertEquals(Collections.emptyList(), problems);
        assertEquals(20.0, changed.number(SkillNumber.COOLDOWN));
        assertFalse(changed.flag(SkillFlag.DAMAGE_ENCHANTMENTS));
        assertEquals(set("sword", "club_heavy"), changed.weaponTypes());
        assertEquals(set("grounded", "airborne"), changed.allowedStates());
        assertEquals(Arrays.asList(0.2, 0.6), changed.hitWindowSeconds());
        assertEquals(0.6, changed.number(SkillNumber.EXECUTION_LENGTH));
        assertEquals(SLASH.number(SkillNumber.DAMAGE_MULTIPLIER), changed.number(SkillNumber.DAMAGE_MULTIPLIER));
        assertEquals(SLASH.flag(SkillFlag.LOOTING), changed.flag(SkillFlag.LOOTING));
        assertEquals(SLASH.id(), changed.id());
        assertEquals(SLASH.activation(), changed.activation());
    }

    @Test
    void aWrongSkillValueIsNamedAndKeepsTheSkillsOwnValueWhileTheOthersApply() {
        List<String> problems = new ArrayList<>();
        SkillDefinition changed = overridden("{'cooldown': -5, 'recovery': 0.8, 'damageMultiplier': 9,"
                + " 'looting': 'yes', 'weaponTypes': ['no_such_type'], 'width': 2, 'reach': -1,"
                + " 'noSuchValue': 1, 'recoverySpeed': 1.5, 'hitWindows': [{'at': -1}]}", problems);

        assertEquals(9, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("'ashenskills:powerful_slash.cooldown'") && problems.get(0).contains("-5"),
                problems.get(0));
        assertEquals(SLASH.number(SkillNumber.COOLDOWN), changed.number(SkillNumber.COOLDOWN));
        assertEquals(0.8, changed.number(SkillNumber.RECOVERY));
        assertEquals(SLASH.number(SkillNumber.DAMAGE_MULTIPLIER), changed.number(SkillNumber.DAMAGE_MULTIPLIER));
        assertEquals(SLASH.flag(SkillFlag.LOOTING), changed.flag(SkillFlag.LOOTING));
        assertEquals(SLASH.weaponTypes(), changed.weaponTypes());
        assertEquals(SLASH.number(SkillNumber.REACH), changed.number(SkillNumber.REACH));
        assertEquals(SLASH.number(SkillNumber.RECOVERY_SPEED), changed.number(SkillNumber.RECOVERY_SPEED));
        assertEquals(SLASH.hitWindowSeconds(), changed.hitWindowSeconds());
    }

    @Test
    void aValueAtTheEdgeOfItsRangeIsAcceptedAndOneJustOutsideIsNot() {
        double[] range = DEFAULTS.skillRange("cooldown");
        List<String> problems = new ArrayList<>();

        assertEquals(range[0], overridden("{'cooldown': " + range[0] + "}", problems).number(SkillNumber.COOLDOWN));
        assertEquals(range[1], overridden("{'cooldown': " + range[1] + "}", problems).number(SkillNumber.COOLDOWN));
        assertEquals(Collections.emptyList(), problems);
        overridden("{'cooldown': " + (range[1] + 0.5) + "}", problems);
        overridden("{'cooldown': " + (range[0] - 0.5) + "}", problems);
        assertEquals(2, problems.size());
        assertNull(DEFAULTS.skillRange("reach"));
        overridden("{'executionSpeed': 1.5}", problems);
        assertEquals(3, problems.size());
        assertEquals(1.0, overridden("{'executionSpeed': 1.0}", problems).number(SkillNumber.EXECUTION_SPEED));
        overridden("{'executionLength': 0}", problems);
        assertEquals(4, problems.size());
    }

    @Test
    void aHitWindowAfterTheEndOfTheExecutionRevertsBothValues() {
        List<String> problems = new ArrayList<>();
        SkillDefinition shortened = overridden("{'executionLength': 0.2}", problems);

        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("hit window"), problems.get(0));
        assertEquals(SLASH.number(SkillNumber.EXECUTION_LENGTH), shortened.number(SkillNumber.EXECUTION_LENGTH));
        assertEquals(SLASH.hitWindowSeconds(), shortened.hitWindowSeconds());
    }

    @Test
    void anUnknownSkillIdIsNamedAndTheRegisteredSkillsStay() {
        List<String> problems = new ArrayList<>();
        Map<ResourceLocation, SkillDefinition> registered = new LinkedHashMap<>();
        registered.put(SLASH_ID, SLASH);
        Map<ResourceLocation, SkillDefinition> result = SkillOverrides.apply(
                json("{'ashenskills:no_such_skill': {'cooldown': 5}, 'AshenSkills:Powerful_Slash': {'cooldown': 5}}"),
                registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems);

        assertEquals(2, problems.size());
        assertTrue(problems.get(0).contains("ashenskills:no_such_skill"), problems.get(0));
        assertEquals(SLASH.number(SkillNumber.COOLDOWN), result.get(SLASH_ID).number(SkillNumber.COOLDOWN));
    }

    @Test
    void notesAreSkippedAndEveryProblemLineNamesTheWrongValue() {
        List<String> problems = new ArrayList<>();
        TypeRules rules = rules("{'_note': 1, 'types': 'club', 'items': {'_note': 'x', 'minecraft:stick': 5}}", problems);

        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("'types'") && problems.get(0).contains("\"club\""), problems.get(0));
        assertTrue(problems.get(1).contains("'items.minecraft:stick'") && problems.get(1).contains("value 5"), problems.get(1));
        assertEquals(Collections.emptyMap(), rules.items());

        Map<ResourceLocation, SkillDefinition> registered = new LinkedHashMap<>();
        registered.put(SLASH_ID, SLASH);
        SkillOverrides.apply(json("{'ashenskills:powerful_slash': 7}"), registered, DEFAULTS, DEFAULTS.builtInTypeIds(), problems);
        assertTrue(problems.get(2).contains("value 7"), problems.get(2));
    }

    @Test
    void aFileThatIsNotOneJsonObjectIsIgnoredAsAWholeAndAMissingFileIsNoProblem() throws Exception {
        java.io.File folder = java.nio.file.Files.createTempDirectory("ashen-pack").toFile();
        org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger("test");
        java.io.File good = new java.io.File(folder, "good.json");
        java.io.File broken = new java.io.File(folder, "broken.json");
        java.io.File list = new java.io.File(folder, "list.json");
        java.io.File empty = new java.io.File(folder, "empty.json");
        java.nio.file.Files.write(good.toPath(), "{\"types\": []}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.write(broken.toPath(), "{\"types\": [".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.write(list.toPath(), "[1, 2]".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Files.write(empty.toPath(), new byte[0]);

        assertTrue(PackConfiguration.read(good, logger).has("types"));
        assertNull(PackConfiguration.read(broken, logger));
        assertNull(PackConfiguration.read(list, logger));
        assertNull(PackConfiguration.read(empty, logger));
        assertNull(PackConfiguration.read(new java.io.File(folder, "absent.json"), logger));
    }

    @Test
    void aViewFromAServerThatSendsLessKeepsTheClientsRegisteredValuesForTheRest() {
        ByteBuf wire = Unpooled.buffer();
        // One skill with no numbers, rules or lists at all, and no type rules.
        wire.writeShort(1);
        byte[] id = "ashenskills:powerful_slash".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        wire.writeShort(id.length);
        wire.writeBytes(id);
        wire.writeByte(0);
        wire.writeByte(0);
        wire.writeShort(0);
        wire.writeShort(0);
        wire.writeShort(0);
        wire.writeShort(0);
        wire.writeShort(0);
        Map<ResourceLocation, SkillDefinition> registered = new LinkedHashMap<>();
        registered.put(SLASH_ID, SLASH);

        SkillDefinition used = SettingsView.read(wire).appliedTo(registered).get(SLASH_ID);

        assertEquals(Collections.emptyList(), used.missingValues());
        assertEquals(SLASH.number(SkillNumber.COOLDOWN), used.number(SkillNumber.COOLDOWN));
        assertEquals(SLASH.weaponTypes(), used.weaponTypes());
        assertEquals(SLASH.hitWindowSeconds(), used.hitWindowSeconds());
    }

    @Test
    void theSettingsViewCarriesEverySkillValueAndTypeRuleToAClient() {
        List<String> problems = new ArrayList<>();
        SkillDefinition serverSlash = overridden("{'cooldown': 20, 'weaponTypes': ['club_heavy'], 'looting': false}", problems);
        TypeRules serverRules = rules("{'types': ['club_heavy'], 'items': {'minecraft:stick': {'add': ['club_heavy']},"
                + "'spartanweaponry:katana_iron': {'replace': ['sword']}}}", problems);
        ByteBuf wire = Unpooled.buffer();
        SettingsView.of(Collections.singletonList(serverSlash), serverRules).write(wire);

        SettingsView received = SettingsView.read(wire);
        Map<ResourceLocation, SkillDefinition> clientSkills = new LinkedHashMap<>();
        // The client's own file said something else; that plays no part.
        clientSkills.put(SLASH_ID, overridden("{'cooldown': 8}", problems));
        ResourceLocation clientOnly = new ResourceLocation("other", "client_only");
        clientSkills.put(clientOnly, SLASH);
        Map<ResourceLocation, SkillDefinition> used = received.appliedTo(clientSkills);

        assertEquals(0, wire.readableBytes());
        assertEquals(20.0, used.get(SLASH_ID).number(SkillNumber.COOLDOWN));
        assertEquals(set("club_heavy"), used.get(SLASH_ID).weaponTypes());
        assertFalse(used.get(SLASH_ID).flag(SkillFlag.LOOTING));
        assertEquals(serverSlash.numbers(), used.get(SLASH_ID).numbers());
        assertEquals(serverSlash.flags(), used.get(SLASH_ID).flags());
        assertEquals(serverSlash.hitWindowSeconds(), used.get(SLASH_ID).hitWindowSeconds());
        assertEquals(serverSlash.allowedStates(), used.get(SLASH_ID).allowedStates());
        assertEquals(SLASH.number(SkillNumber.COOLDOWN), used.get(clientOnly).number(SkillNumber.COOLDOWN));
        assertEquals(set("club_heavy"), received.typeRules().packTypes());
        assertEquals(set("sword"), received.typeRules().effectiveTypes("spartanweaponry:katana_iron", set("katana")));
        assertEquals(set("club_heavy"), received.typeRules().effectiveTypes("minecraft:stick", Collections.<String>emptySet()));
    }
}
