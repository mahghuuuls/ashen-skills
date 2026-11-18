package com.mahghuuuls.ashentestaddon;

import com.mahghuuuls.ashenskills.api.ActivationKind;
import com.mahghuuuls.ashenskills.api.AshenSkillsApi;
import com.mahghuuuls.ashenskills.api.HitShape;
import com.mahghuuuls.ashenskills.api.SkillBehavior;
import com.mahghuuuls.ashenskills.api.SkillContext;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.api.SkillFlag;
import com.mahghuuuls.ashenskills.api.SkillNumber;
import java.util.Collections;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A second mod, built only for the tests of Ashen Skills. It adds one weapon type and one skill
 * through the public API and nothing else, the way any other mod would. It is never shipped.
 */
@Mod(modid = AshenTestAddon.MOD_ID, name = "Ashen Skills Test Addon", version = "1.0.0",
        dependencies = "required-after:ashenskills")
public final class AshenTestAddon {

    public static final String MOD_ID = "ashentestaddon";
    public static final String WHIP = "whip";
    public static final ResourceLocation LASH = new ResourceLocation(MOD_ID, "lash");

    private static final Logger LOGGER = LogManager.getLogger("Ashen Test Addon");

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        AshenSkillsApi.registerWeaponType(WHIP);
        // The game's lead stands in for a whip, so the test needs no item of its own.
        AshenSkillsApi.addItemType(new ResourceLocation("minecraft", "lead"), WHIP);
        AshenSkillsApi.registerSkill(lash(LASH), new Lash());
        LOGGER.info("[ADDON] registered type={} skill={}", WHIP, LASH);
    }

    /** After loading, the sets are closed: the attempt is refused, and the log keeps the reason. */
    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        try {
            AshenSkillsApi.registerSkill(lash(new ResourceLocation(MOD_ID, "too_late")), new Lash());
            LOGGER.error("[ADDON_LATE] skill result=accepted");
        } catch (IllegalStateException refused) {
            LOGGER.info("[ADDON_LATE] skill result=refused message=\"{}\"", refused.getMessage());
        }
        try {
            AshenSkillsApi.registerWeaponType("too_late");
            LOGGER.error("[ADDON_LATE] type result=accepted");
        } catch (IllegalStateException refused) {
            LOGGER.info("[ADDON_LATE] type result=refused message=\"{}\"", refused.getMessage());
        }
    }

    private static SkillDefinition lash(ResourceLocation id) {
        return SkillDefinition.builder(id, ActivationKind.INSTANT)
                .weaponTypes(Collections.singleton(WHIP))
                .allowedStates(Collections.singleton(SkillDefinition.STATE_GROUNDED))
                .hitWindows(Collections.singletonList(0.2))
                .number(SkillNumber.COOLDOWN, 6)
                .number(SkillNumber.STAMINA_COST, 10)
                .number(SkillNumber.MANA_COST, 0)
                .number(SkillNumber.EXECUTION_LENGTH, 0.4)
                .number(SkillNumber.RECOVERY, 0.5)
                .number(SkillNumber.INTERRUPTED_RECOVERY, 0.3)
                .number(SkillNumber.EXECUTION_SPEED, 0.3)
                .number(SkillNumber.RECOVERY_SPEED, 0.5)
                .number(SkillNumber.DAMAGE_MULTIPLIER, 3.0)
                .number(SkillNumber.REACH, 5.0)
                .number(SkillNumber.REACH_BELOW, 1.0)
                .number(SkillNumber.REACH_ABOVE, 3.0)
                .number(SkillNumber.IMPACT_PARTICLES, 8)
                .number(SkillNumber.KNOCKBACK_STRENGTH, 0.2)
                .number(SkillNumber.ARC, 40)
                .flag(SkillFlag.MOUNTED_ALLOWED, false)
                .flag(SkillFlag.EXECUTION_INTERRUPTED_BY_DAMAGE, false)
                .flag(SkillFlag.EXECUTION_INTERRUPTED_BY_KNOCKBACK, false)
                .flag(SkillFlag.BLOCKING_DURING_EXECUTION, false)
                .flag(SkillFlag.ITEM_USE_DURING_EXECUTION, false)
                .flag(SkillFlag.BLOCKING_DURING_RECOVERY, false)
                .flag(SkillFlag.ITEM_USE_DURING_RECOVERY, false)
                .flag(SkillFlag.JUMP_DURING_EXECUTION, false)
                .flag(SkillFlag.SNEAKING_DURING_SKILL, false)
                .flag(SkillFlag.SOLID_BLOCKS_OBSTRUCT, true)
                .flag(SkillFlag.DAMAGE_ENCHANTMENTS, true)
                .flag(SkillFlag.FIRE_ASPECT, true)
                .flag(SkillFlag.KNOCKBACK_ENCHANTMENT, false)
                .flag(SkillFlag.LOOTING, true)
                .build();
    }

    /** One long, narrow strike ahead. */
    private static final class Lash implements SkillBehavior {

        @Override
        public void onHitWindow(SkillContext context, int windowIndex) {
            int struck = context.hit(HitShape.arc(
                    context.skill().number(SkillNumber.REACH), context.skill().number(SkillNumber.ARC)));
            LOGGER.info("[ADDON] lash window={} struck={}", windowIndex, struck);
        }
    }
}
