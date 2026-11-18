package com.mahghuuuls.ashenskills.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

class AshenSkillsApiTest {

    /** One test, in this order: the class under test keeps its host for the whole run. */
    @Test
    void callsAreRefusedBeforeTheModStartedAndHandedOnAfterIt() {
        IllegalStateException early =
                assertThrows(IllegalStateException.class, () -> AshenSkillsApi.registerWeaponType("whip"));
        assertTrue(early.getMessage().contains("required-after:ashenskills"), early.getMessage());

        List<String> taken = new ArrayList<>();
        AshenSkillsApi.attach(new AshenSkillsApi.Host() {
            @Override
            public void registerSkill(SkillDefinition definition, SkillBehavior behavior) {
                taken.add("skill");
            }

            @Override
            public void registerWeaponType(String typeId) {
                taken.add("type " + typeId);
            }

            @Override
            public void addItemType(ResourceLocation itemName, String typeId) {
                taken.add("item " + itemName + " " + typeId);
            }

            @Override
            public ItemStack ashOf(ResourceLocation skillId) {
                taken.add("ash " + skillId);
                return null;
            }
        });

        AshenSkillsApi.registerWeaponType("whip");
        AshenSkillsApi.addItemType(new ResourceLocation("mymod", "whip"), "whip");
        AshenSkillsApi.registerSkill(null, null);
        AshenSkillsApi.ash(new ResourceLocation("mymod", "lash"));

        List<String> expected = new ArrayList<>();
        expected.add("type whip");
        expected.add("item mymod:whip whip");
        expected.add("skill");
        expected.add("ash mymod:lash");
        assertEquals(expected, taken);

        IllegalStateException second =
                assertThrows(IllegalStateException.class, () -> AshenSkillsApi.attach(new AshenSkillsApi.Host() {
                    @Override
                    public void registerSkill(SkillDefinition definition, SkillBehavior behavior) {
                    }

                    @Override
                    public void registerWeaponType(String typeId) {
                    }

                    @Override
                    public void addItemType(ResourceLocation itemName, String typeId) {
                    }

                    @Override
                    public ItemStack ashOf(ResourceLocation skillId) {
                        return null;
                    }
                }));
        assertTrue(second.getMessage().contains("already started"), second.getMessage());
        assertThrows(IllegalArgumentException.class, () -> AshenSkillsApi.attach(null));
    }
}
