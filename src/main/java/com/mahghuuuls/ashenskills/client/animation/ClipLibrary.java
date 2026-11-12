package com.mahghuuuls.ashenskills.client.animation;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.api.SkillPhase;
import com.mahghuuuls.ashenskills.client.animation.clip.Clip;
import com.mahghuuuls.ashenskills.client.animation.clip.ClipFormatException;
import com.mahghuuuls.ashenskills.client.animation.clip.ClipParser;
import com.mahghuuuls.ashenskills.client.animation.clip.View;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

/**
 * The clips of every skill, read from resources when first needed and kept until resources are
 * reloaded. The clip of skill {@code ns:name} for a view and a phase is the resource
 * {@code assets/ns/ashen_clips/name/<view>_<phase>.json}, so an addon or a resource pack ships
 * clips as plain files.
 */
public final class ClipLibrary implements IResourceManagerReloadListener {

    private static final String FOLDER = "ashen_clips";

    /** Found clips, and null for clips looked for and not usable, so a problem is logged once. */
    private static final Map<String, Clip> CLIPS = new HashMap<>();

    private ClipLibrary() {
    }

    /** Makes the library forget its clips whenever the game reloads resources. */
    public static void register() {
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        if (manager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) manager).registerReloadListener(new ClipLibrary());
        }
    }

    /** @return the clip, or null when the skill has no usable clip for that view and phase */
    public static Clip find(String skillId, SkillPhase phase, View view) {
        String key = skillId + '|' + view.fileName() + '|' + phase.name();
        if (!CLIPS.containsKey(key)) {
            CLIPS.put(key, load(skillId, phase, view));
        }
        return CLIPS.get(key);
    }

    /** Where the clip of a skill is looked for. */
    static ResourceLocation locationOf(String skillId, SkillPhase phase, View view) {
        ResourceLocation skill = new ResourceLocation(skillId);
        return new ResourceLocation(skill.getNamespace(), FOLDER + '/' + skill.getPath() + '/'
                + view.fileName() + '_' + phase.name().toLowerCase(Locale.ROOT) + ".json");
    }

    private static Clip load(String skillId, SkillPhase phase, View view) {
        ResourceLocation location = locationOf(skillId, phase, view);
        try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
                InputStream stream = resource.getInputStream()) {
            return ClipParser.parse(IOUtils.toString(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            AshenSkillsMod.LOGGER.error("No animation clip {}: the skill shows no animation there", location);
        } catch (ClipFormatException e) {
            AshenSkillsMod.LOGGER.error("Animation clip {} is not usable: {}", location, e.getMessage());
        }
        return null;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        CLIPS.clear();
    }
}
