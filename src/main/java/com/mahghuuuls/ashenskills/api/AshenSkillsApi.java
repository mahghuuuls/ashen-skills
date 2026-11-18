package com.mahghuuuls.ashenskills.api;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * What another mod calls to add to Ashen Skills. Everything that adds is called from the calling
 * mod's pre-initialization: the game chooses the pictures of items right after it, so a skill added
 * later has no Ash icon. When the game has finished loading mods the sets are closed, and a call
 * fails with an {@link IllegalStateException} that says so. The calling mod declares
 * {@code dependencies = "required-after:ashenskills"}, so that Ashen Skills has started first.
 *
 * <p>A skill is a {@link SkillDefinition} (its data, which packs may change) and a
 * {@link SkillBehavior} (what it does, as code). The rest of a skill is files in the adding mod's
 * own assets, found by the skill's id; {@code docs/addon-api.md} in the repository lists them.
 */
public final class AshenSkillsApi {

    /**
     * The part of Ashen Skills that takes the registrations. Not for other mods: Ashen Skills
     * attaches the one host itself.
     */
    public interface Host {

        void registerSkill(SkillDefinition definition, SkillBehavior behavior);

        void registerWeaponType(String typeId);

        void addItemType(ResourceLocation itemName, String typeId);

        ItemStack ashOf(ResourceLocation skillId);
    }

    private static Host host;

    private AshenSkillsApi() {
    }

    /**
     * Called once, by Ashen Skills itself, when it starts.
     *
     * @throws IllegalStateException when a host is already attached
     */
    public static synchronized void attach(Host newHost) {
        if (newHost == null) {
            throw new IllegalArgumentException("A host is needed");
        }
        if (host != null) {
            throw new IllegalStateException("Ashen Skills is already started; other mods must not call attach");
        }
        host = newHost;
    }

    /**
     * Adds a skill. Its id is permanent: weapons and Ashes store it. The skill then obeys every
     * rule of the mod (costs, the shared cooldown, checks, PvP, target caps) and shows in Forges,
     * tooltips, the HUD, commands and pack files like the mod's own skills.
     *
     * @throws IllegalStateException when loading is over, the id is taken, or the definition lacks
     *     a required value ({@link SkillDefinition#missingValues()})
     * @throws IllegalArgumentException when the behavior is null
     */
    public static void registerSkill(SkillDefinition definition, SkillBehavior behavior) {
        host().registerSkill(definition, behavior);
    }

    /**
     * Adds a weapon type, so that skills and pack files can name it. A type is an open id of
     * lower-case letters, digits and {@code _ . : -}; a type never implies another type. Adding a
     * type that exists already changes nothing.
     *
     * @throws IllegalStateException when loading is over
     * @throws IllegalArgumentException when the id has other characters or is empty
     */
    public static void registerWeaponType(String typeId) {
        host().registerWeaponType(typeId);
    }

    /**
     * Gives an item a weapon type without a pack file, in addition to the types it has. The type
     * must exist: one of the mod's own, or one added with {@link #registerWeaponType}. A pack's
     * rules still come last and can add to this or replace it.
     *
     * @param itemName the item's registry name, for example {@code mymod:whip}
     * @throws IllegalStateException when loading is over
     * @throws IllegalArgumentException when the type does not exist
     */
    public static void addItemType(ResourceLocation itemName, String typeId) {
        host().addItemType(itemName, typeId);
    }

    /**
     * One Ash of the skill with that id: the item a Forge needs to attach the skill. May be called
     * at any time. The id is not checked: an Ash keeps the id of a skill no installed mod provides.
     *
     * @throws IllegalArgumentException when the id is null
     */
    public static ItemStack ash(ResourceLocation skillId) {
        if (skillId == null) {
            throw new IllegalArgumentException("A skill id is needed");
        }
        return host().ashOf(skillId);
    }

    private static synchronized Host host() {
        if (host == null) {
            throw new IllegalStateException("Ashen Skills has not started yet. Call its API from your mod's "
                    + "pre-initialization, and declare dependencies = \"required-after:ashenskills\"");
        }
        return host;
    }
}
