package com.mahghuuuls.ashenskills.integration.jafs;

import com.mahghuuuls.ashenskills.attribute.FoodProperty;
import com.mahghuuuls.ashenskills.integration.FoodProperties;
import com.mahghuuuls.jafs.api.IPropertyApplier;
import com.mahghuuuls.jafs.api.JafsApi;
import com.mahghuuuls.jafs.api.PropertyOperation;
import com.mahghuuuls.jafs.api.PropertySpec;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayerMP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The four attributes as JAFS food properties, so a pack's food definitions can give their points.
 * A definition names a property with the operation {@code flat}; JAFS checks the amount against the
 * property's range, sums the player's active foods, and hands the sum over at every change, at
 * login and at respawn. Only this class names JAFS.
 */
public final class JafsFoodProperties implements FoodProperties {

    private static final Logger LOGGER = LogManager.getLogger("Ashen Skills");

    /** Set when giving points failed once; from then on food gives none, and everything else goes on. */
    private boolean failed;
    private int registered;

    /**
     * Registers the properties. Must run while mods load, before JAFS closes its registration. When
     * JAFS refuses one, the ones before it stay registered and work; the mod's start line then
     * reports no food properties, and the error names the refused one.
     */
    public JafsFoodProperties() {
        for (FoodProperty property : FoodProperty.values()) {
            PropertySpec spec = PropertySpec.builder(property.id())
                    .displayNameKey(property.nameKey())
                    .color(property.color())
                    .operation(PropertyOperation.FLAT, property.leastPoints(), property.mostPoints())
                    .build();
            JafsApi.registerProperty(spec, applier(property));
            registered++;
        }
    }

    @Override
    public int registered() {
        return registered;
    }

    private IPropertyApplier applier(FoodProperty property) {
        return (EntityPlayerMP player, double flatSum, double percentSum) -> {
            try {
                IAttributeInstance instance = player.getEntityAttribute(property.attribute());
                if (instance != null) {
                    property.setPoints(instance, failed ? 0 : flatSum);
                }
            } catch (RuntimeException | LinkageError problem) {
                if (!failed) {
                    failed = true;
                    LOGGER.error("Food points from JAFS failed; food gives no skill attribute points from now on", problem);
                }
            }
        };
    }
}
