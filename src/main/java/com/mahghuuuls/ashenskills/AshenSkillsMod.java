package com.mahghuuuls.ashenskills;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = Tags.MOD_ID,
        name = Tags.MOD_NAME,
        version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:mixinbooter@[12.1,)")
public class AshenSkillsMod {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
}
