package com.mahghuuuls.ashenskills.attribute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.ai.attributes.AttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import org.junit.jupiter.api.Test;

class FoodPropertyTest {

    private static IAttributeInstance fresh(FoodProperty property) {
        return new AttributeMap().registerAttribute(property.attribute());
    }

    @Test
    void thereIsOnePropertyForEachOfTheFourAttributes() {
        Set<String> ids = new HashSet<>();
        Set<Object> attributes = new HashSet<>();
        for (FoodProperty property : FoodProperty.values()) {
            assertTrue(property.id().startsWith("ashenskills:skill_"), property.id());
            ids.add(property.id());
            attributes.add(property.attribute());
        }
        assertEquals(4, ids.size());
        assertEquals(4, attributes.size());
        assertTrue(ids.contains("ashenskills:skill_damage") && ids.contains("ashenskills:skill_haste")
                && ids.contains("ashenskills:skill_stamina_efficiency") && ids.contains("ashenskills:skill_mana_efficiency"),
                ids.toString());
    }

    @Test
    void thePointsAFoodMayGiveAreTheLimitsOfTheAttribute() {
        assertEquals(-100.0, FoodProperty.SKILL_DAMAGE.leastPoints());
        assertEquals(-1024.0, FoodProperty.SKILL_HASTE.leastPoints());
        for (FoodProperty property : FoodProperty.values()) {
            assertEquals(1024.0, property.mostPoints());
            assertTrue(property.leastPoints() < 0);
        }
    }

    @Test
    void everyPropertyNameHasATextInTheLanguageFile() throws IOException {
        List<String> lines = Files.readAllLines(
                Paths.get("src", "main", "resources", "assets", "ashenskills", "lang", "en_us.lang"), StandardCharsets.UTF_8);
        for (FoodProperty property : FoodProperty.values()) {
            String key = property.nameKey() + "=";
            boolean found = false;
            for (String line : lines) {
                found |= line.startsWith(key) && line.length() > key.length();
            }
            assertTrue(found, "no text for " + key);
        }
    }

    @Test
    void theSumBecomesTheAttributesPointsAndZeroTakesThemAway() {
        IAttributeInstance damage = fresh(FoodProperty.SKILL_DAMAGE);
        assertEquals(0.0, damage.getAttributeValue());

        FoodProperty.SKILL_DAMAGE.setPoints(damage, 15);
        assertEquals(15.0, damage.getAttributeValue());

        FoodProperty.SKILL_DAMAGE.setPoints(damage, 15);
        assertEquals(15.0, damage.getAttributeValue());
        assertEquals(1, damage.getModifiers().size());

        FoodProperty.SKILL_DAMAGE.setPoints(damage, 40);
        assertEquals(40.0, damage.getAttributeValue());
        assertEquals(1, damage.getModifiers().size());

        FoodProperty.SKILL_DAMAGE.setPoints(damage, 0);
        assertEquals(0.0, damage.getAttributeValue());
        assertTrue(damage.getModifiers().isEmpty());
    }

    @Test
    void foodPointsAddToPointsFromOtherSourcesAndAreNotSavedWithThePlayer() {
        IAttributeInstance haste = fresh(FoodProperty.SKILL_HASTE);
        haste.applyModifier(new AttributeModifier("ring", 10, 0));

        FoodProperty.SKILL_HASTE.setPoints(haste, 5);

        assertEquals(15.0, haste.getAttributeValue());
        int foodModifiers = 0;
        for (AttributeModifier modifier : haste.getModifiers()) {
            if (modifier.getAmount() == 5) {
                foodModifiers++;
                assertFalse(modifier.isSaved(), "food points must not be saved with the player");
                assertEquals(0, modifier.getOperation());
            } else {
                assertTrue(modifier.isSaved(), "the other source's modifier is untouched");
            }
        }
        assertEquals(1, foodModifiers);

        FoodProperty.SKILL_HASTE.setPoints(haste, 0);
        assertEquals(10.0, haste.getAttributeValue());
        assertEquals(1, haste.getModifiers().size());
    }

    @Test
    void eachPropertyKeepsItsOwnPointsOnItsOwnAttribute() {
        AttributeMap player = new AttributeMap();
        for (FoodProperty property : FoodProperty.values()) {
            player.registerAttribute(property.attribute());
        }
        double points = 1;
        for (FoodProperty property : FoodProperty.values()) {
            property.setPoints(player.getAttributeInstance(property.attribute()), points++);
        }
        points = 1;
        for (FoodProperty property : FoodProperty.values()) {
            assertEquals(points++, player.getAttributeInstance(property.attribute()).getAttributeValue(), property.id());
        }
    }

    @Test
    void pointsBelowTheAttributesLimitAreClampedByTheAttribute() {
        IAttributeInstance damage = fresh(FoodProperty.SKILL_DAMAGE);

        FoodProperty.SKILL_DAMAGE.setPoints(damage, -500);

        assertEquals(-100.0, damage.getAttributeValue());
    }
}
