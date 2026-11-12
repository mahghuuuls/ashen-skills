package com.mahghuuuls.ashenskills.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import com.mahghuuuls.ashenskills.config.DefaultsData.GeneralEntry;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DefaultsDataTest {

    private final DefaultsData defaults = DefaultsData.loadBundled();

    @Test
    void everyGeneralSettingInTheFileHasAKeyAndEveryKeyIsInTheFile() {
        Set<String> keyNames = new LinkedHashSet<>();
        for (SettingKey key : SettingKey.values()) {
            keyNames.add(key.jsonName());
        }
        assertEquals(defaults.generalNames(), keyNames);
    }

    @Test
    void eachDefaultHasTheKindItsKeyDeclaresAndLiesInsideItsRange() {
        for (SettingKey key : SettingKey.values()) {
            GeneralEntry entry = defaults.general(key.jsonName());
            JsonPrimitive value = entry.defaultValue();
            switch (key.kind()) {
                case BOOLEAN:
                    assertTrue(value.isBoolean(), key + " default must be true or false");
                    break;
                case INTEGER:
                    assertTrue(value.isNumber() && value.getAsDouble() == Math.rint(value.getAsDouble()),
                            key + " default must be a whole number");
                    assertRange(key, entry);
                    break;
                case DECIMAL:
                    assertTrue(value.isNumber(), key + " default must be a number");
                    assertRange(key, entry);
                    break;
                default:
                    assertTrue(value.isString(), key + " default must be text");
            }
            assertTrue(entry.owner().equals("server") || entry.owner().equals("client"), key + " owner");
        }
    }

    private static void assertRange(SettingKey key, GeneralEntry entry) {
        assertNotNull(entry.min(), key + " needs a minimum");
        assertNotNull(entry.max(), key + " needs a maximum");
        assertTrue(entry.allows(entry.defaultValue().getAsDouble()), key + " default must lie inside its range");
    }
}
