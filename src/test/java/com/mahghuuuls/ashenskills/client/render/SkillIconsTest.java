package com.mahghuuuls.ashenskills.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

class SkillIconsTest {

    private static final String[] BUILT_IN = {"powerful_slash", "unsheathe", "crushing_blow", "piercing_lunge"};

    @Test
    void aSkillIconIsLookedForUnderTheNamespaceAndNameOfItsSkill() {
        assertEquals(new ResourceLocation("ashenskills", "textures/skills/powerful_slash.png"),
                SkillIcons.fileOf("ashenskills:powerful_slash"));
        assertEquals(new ResourceLocation("addon", "textures/skills/whirl.png"), SkillIcons.fileOf("addon:whirl"));
    }

    @Test
    void everyBuiltInSkillShipsASkillIconThatIsNotThePictureOfItsAsh() throws IOException {
        for (String skill : BUILT_IN) {
            byte[] skillIcon = bytes("/assets/ashenskills/textures/skills/" + skill + ".png");
            byte[] ashIcon = bytes("/assets/ashenskills/textures/items/ash_" + skill + ".png");
            assertFalse(Arrays.equals(skillIcon, ashIcon), skill);
        }
    }

    @Test
    void anIdWithoutANamespaceIsLookedForAsTheGameReadsIt() {
        assertEquals(new ResourceLocation("minecraft", "textures/skills/whirl.png"), SkillIcons.fileOf("whirl"));
    }

    @Test
    void theModShipsThePictureThatASkillWithoutAnIconShows() throws IOException {
        bytes("/assets/" + SkillIcons.UNKNOWN.getNamespace() + "/" + SkillIcons.UNKNOWN.getPath());
    }

    private static byte[] bytes(String path) throws IOException {
        try (InputStream in = SkillIconsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path);
            return IOUtils.toByteArray(in);
        }
    }
}
