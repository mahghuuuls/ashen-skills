package com.mahghuuuls.ashenskills.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeneralSettingsTest {

    /** A small defaults file with values that differ from the real one, so no real default leaks in. */
    private static final DefaultsData DEFAULTS = DefaultsData.parse(new StringReader(fixture()));

    private static String fixture() {
        StringBuilder general = new StringBuilder();
        for (SettingKey key : SettingKey.values()) {
            if (general.length() > 0) {
                general.append(',');
            }
            general.append('"').append(key.jsonName()).append("\":");
            switch (key.kind()) {
                case BOOLEAN:
                    general.append("{\"default\":true,\"owner\":\"server\"}");
                    break;
                case INTEGER:
                    general.append("{\"default\":7,\"min\":5,\"max\":9,\"owner\":\"server\"}");
                    break;
                case DECIMAL:
                    general.append("{\"default\":0.25,\"min\":0.1,\"max\":0.5,\"owner\":\"client\"}");
                    break;
                default:
                    general.append("{\"default\":\"K\",\"owner\":\"client\"}");
            }
        }
        return "{\"general\":{" + general + "}}";
    }

    private static GeneralSettings resolve(SettingKey key, String raw) {
        Map<SettingKey, String> values = new EnumMap<>(SettingKey.class);
        values.put(key, raw);
        return GeneralSettings.resolve(DEFAULTS, values);
    }

    @Test
    void missingValuesUseTheDefaultsOfTheFile() {
        GeneralSettings settings = GeneralSettings.resolve(DEFAULTS, new EnumMap<SettingKey, String>(SettingKey.class));
        assertEquals(7, settings.getInt(SettingKey.FORGE_ASH_SLOTS));
        assertEquals(0.25, settings.getDouble(SettingKey.CAMERA_EFFECT_INTENSITY));
        assertTrue(settings.getBoolean(SettingKey.DIAGNOSTIC_LOGGING));
        assertTrue(settings.problems().isEmpty());
    }

    @Test
    void theShippedDefaultsFileResolvesToItsOwnDefaults() {
        DefaultsData shipped = DefaultsData.loadBundled();
        GeneralSettings settings = GeneralSettings.resolve(shipped, new EnumMap<SettingKey, String>(SettingKey.class));
        assertTrue(settings.problems().isEmpty());
        for (SettingKey key : SettingKey.values()) {
            if (!key.isInConfigurationFile()) {
                continue;
            }
            com.google.gson.JsonPrimitive expected = shipped.general(key.jsonName()).defaultValue();
            switch (key.kind()) {
                case BOOLEAN:
                    assertEquals(expected.getAsBoolean(), settings.getBoolean(key), key.jsonName());
                    break;
                case INTEGER:
                    assertEquals(expected.getAsInt(), settings.getInt(key), key.jsonName());
                    break;
                default:
                    assertEquals(expected.getAsDouble(), settings.getDouble(key), key.jsonName());
            }
        }
    }

    @Test
    void validValuesAreApplied() {
        assertEquals(9, resolve(SettingKey.FORGE_ASH_SLOTS, "9").getInt(SettingKey.FORGE_ASH_SLOTS));
        assertEquals(5, resolve(SettingKey.FORGE_ASH_SLOTS, " 5 ").getInt(SettingKey.FORGE_ASH_SLOTS));
        assertEquals(0.5,
                resolve(SettingKey.CAMERA_EFFECT_INTENSITY, "0.5").getDouble(SettingKey.CAMERA_EFFECT_INTENSITY));
        assertFalse(resolve(SettingKey.SKILL_HUD, "FALSE").getBoolean(SettingKey.SKILL_HUD));
    }

    @Test
    void aValueOutsideItsRangeIsReplacedByTheDefaultAndReported() {
        GeneralSettings settings = resolve(SettingKey.FORGE_ASH_SLOTS, "10");
        assertEquals(7, settings.getInt(SettingKey.FORGE_ASH_SLOTS));
        assertEquals(1, settings.problems().size());
        String problem = settings.problems().get(0);
        assertTrue(problem.contains("forgeAshSlots"), problem);
        assertTrue(problem.contains("'10'"), problem);

        assertEquals(7, resolve(SettingKey.FORGE_ASH_SLOTS, "4").getInt(SettingKey.FORGE_ASH_SLOTS));
        assertEquals(0.25,
                resolve(SettingKey.CAMERA_EFFECT_INTENSITY, "0.6").getDouble(SettingKey.CAMERA_EFFECT_INTENSITY));
    }

    @Test
    void textThatIsNotAValueIsReplacedByTheDefaultAndReported() {
        GeneralSettings whole = resolve(SettingKey.FORGE_ASH_SLOTS, "many");
        assertEquals(7, whole.getInt(SettingKey.FORGE_ASH_SLOTS));
        assertEquals(1, whole.problems().size());

        GeneralSettings fraction = resolve(SettingKey.FORGE_ASH_SLOTS, "6.5");
        assertEquals(7, fraction.getInt(SettingKey.FORGE_ASH_SLOTS));
        assertEquals(1, fraction.problems().size());

        GeneralSettings flag = resolve(SettingKey.SKILL_HUD, "maybe");
        assertTrue(flag.getBoolean(SettingKey.SKILL_HUD));
        assertEquals(1, flag.problems().size());

        GeneralSettings notANumber = resolve(SettingKey.CAMERA_EFFECT_INTENSITY, "NaN");
        assertEquals(0.25, notANumber.getDouble(SettingKey.CAMERA_EFFECT_INTENSITY));
        assertEquals(1, notANumber.problems().size());
    }

    @Test
    void oneBadValueDoesNotDisturbTheOthers() {
        Map<SettingKey, String> values = new EnumMap<>(SettingKey.class);
        values.put(SettingKey.FORGE_ASH_SLOTS, "100");
        values.put(SettingKey.HIT_TARGET_CAP_PER_WINDOW, "8");
        GeneralSettings settings = GeneralSettings.resolve(DEFAULTS, values);
        assertEquals(7, settings.getInt(SettingKey.FORGE_ASH_SLOTS));
        assertEquals(8, settings.getInt(SettingKey.HIT_TARGET_CAP_PER_WINDOW));
        assertEquals(1, settings.problems().size());
    }
}
