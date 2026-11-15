package com.mahghuuuls.ashenskills.sound;

import com.mahghuuuls.ashenskills.Tags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The sounds of skills. The sound of skill {@code ns:name} for a moment is the sound event
 * {@code ns:skill.name.<moment>}, described in that namespace's {@code sounds.json}; so an addon or
 * a resource pack gives a skill its sounds with plain files. This mod's own skills use sounds built
 * from the game's existing sound files.
 */
@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
public final class SkillSounds {

    /** The moment a strike is made. */
    public static final String SWING = "swing";
    /** The moment a strike damages a target. */
    public static final String IMPACT = "impact";
    /** The moment a charge reaches its full time. */
    public static final String CHARGED = "charged";

    private static final String[] BUILT_IN_SKILLS = {"powerful_slash", "unsheathe", "crushing_blow"};
    private static final String[] BUILT_IN_CHARGED_SKILLS = {"crushing_blow"};

    private SkillSounds() {
    }

    /** The name of a skill's sound event for a moment. */
    public static ResourceLocation nameOf(String skillId, String moment) {
        ResourceLocation skill = new ResourceLocation(skillId);
        return new ResourceLocation(skill.getNamespace(), "skill." + skill.getPath() + "." + moment);
    }

    /**
     * The sound event to play. A sound that no mod registered still plays when a {@code sounds.json}
     * describes it; when nothing describes it, the game plays nothing and says so in its log.
     */
    public static SoundEvent of(String skillId, String moment) {
        ResourceLocation name = nameOf(skillId, moment);
        SoundEvent registered = SoundEvent.REGISTRY.getObject(name);
        return registered != null ? registered : new SoundEvent(name);
    }

    @SubscribeEvent
    public static void onRegisterSounds(RegistryEvent.Register<SoundEvent> event) {
        for (String skill : BUILT_IN_SKILLS) {
            for (String moment : new String[] {SWING, IMPACT}) {
                register(event, skill, moment);
            }
        }
        for (String skill : BUILT_IN_CHARGED_SKILLS) {
            register(event, skill, CHARGED);
        }
    }

    private static void register(RegistryEvent.Register<SoundEvent> event, String skill, String moment) {
        ResourceLocation name = nameOf(Tags.MOD_ID + ":" + skill, moment);
        event.getRegistry().register(new SoundEvent(name).setRegistryName(name));
    }
}
