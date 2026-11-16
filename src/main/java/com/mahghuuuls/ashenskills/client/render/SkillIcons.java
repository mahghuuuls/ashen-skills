package com.mahghuuuls.ashenskills.client.render;

import com.mahghuuuls.ashenskills.AshenSkillsMod;
import com.mahghuuuls.ashenskills.Tags;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

/**
 * The picture of a skill itself, as the skill HUD and the Forge's skill list show it. It is not
 * the picture of the skill's Ash, which is an item model ({@link ItemModels}). The skill
 * {@code <namespace>:<name>} uses the file {@code assets/<namespace>/textures/skills/<name>.png},
 * so an addon or a resource pack ships a skill icon as a plain file. A skill without that file
 * shows the unknown-skill picture.
 */
public final class SkillIcons implements IResourceManagerReloadListener {

    /** The side of a drawn skill icon, in GUI pixels, whatever the size of its file. */
    public static final int SIZE = 16;

    private static final String FOLDER = "textures/skills/";
    static final ResourceLocation UNKNOWN = new ResourceLocation(Tags.MOD_ID, FOLDER + "unknown.png");

    /** Skill id text to the picture drawn for it; emptied when the game loads its resources again. */
    private static final Map<String, ResourceLocation> CHOSEN = new HashMap<>();

    private SkillIcons() {
    }

    public static void register() {
        IResourceManager manager = Minecraft.getMinecraft().getResourceManager();
        if (manager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) manager).registerReloadListener(new SkillIcons());
        }
    }

    /** Where the skill icon of this skill id is looked for. */
    static ResourceLocation fileOf(String skillId) {
        ResourceLocation id = new ResourceLocation(skillId);
        return new ResourceLocation(id.getNamespace(), FOLDER + id.getPath() + ".png");
    }

    /**
     * Draws the icon of this skill with its top left corner at the given place, blended over what
     * is there. The caller has set the color. Afterwards blending is on and the bound texture is
     * this icon; a caller whose surroundings draw without blending turns it off again.
     */
    public static void draw(String skillId, int x, int y) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Minecraft.getMinecraft().getTextureManager().bindTexture(CHOSEN.computeIfAbsent(skillId, SkillIcons::choose));
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
    }

    private static ResourceLocation choose(String skillId) {
        ResourceLocation file = fileOf(skillId);
        try {
            IOUtils.closeQuietly(Minecraft.getMinecraft().getResourceManager().getResource(file));
            return file;
        } catch (IOException missing) {
            AshenSkillsMod.LOGGER.warn("Skill {} has no skill icon at {}; the unknown-skill picture is shown", skillId, file);
            return UNKNOWN;
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        CHOSEN.clear();
    }
}
