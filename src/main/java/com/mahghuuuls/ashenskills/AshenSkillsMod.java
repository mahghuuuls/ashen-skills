package com.mahghuuuls.ashenskills;

import com.mahghuuuls.ashenskills.command.AshenSkillsCommand;
import com.mahghuuuls.ashenskills.config.Settings;
import com.mahghuuuls.ashenskills.network.AshenNetwork;
import com.mahghuuuls.ashenskills.proxy.CommonProxy;
import com.mahghuuuls.ashenskills.runtime.SkillRuntimeHooks;
import com.mahghuuuls.ashenskills.skill.BuiltInSkills;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
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

    @SidedProxy(
            clientSide = "com.mahghuuuls.ashenskills.client.ClientProxy",
            serverSide = "com.mahghuuuls.ashenskills.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Settings.load(event.getSuggestedConfigurationFile(), LOGGER);
        AshenNetwork.init();
        WeaponTypes.load(Settings.defaults());
        SkillRuntimeHooks.install();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        BuiltInSkills.registerAll(Settings.defaults());
        proxy.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        SkillRegistry.close();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new AshenSkillsCommand());
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        SkillRuntimeHooks.onServerStopped();
    }
}
