package com.mahghuuuls.ashenskills.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mahghuuuls.ashenskills.api.SkillDefinition;
import com.mahghuuuls.ashenskills.skill.SkillRegistry;
import com.mahghuuuls.ashenskills.weapon.TypeRules;
import com.mahghuuuls.ashenskills.weapon.WeaponTypes;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.Logger;

/**
 * The pack's own files: {@code weapon_types.json} and {@code skills.json} in the mod's
 * configuration folder. Both are optional. They are read once, after every mod has registered its
 * items and skills, so each entry can be checked against what exists. A wrong entry is named in
 * the log and left out; a file that is not readable JSON is named and left out as a whole.
 */
public final class PackConfiguration {

    public static final String WEAPON_TYPES_FILE = "weapon_types.json";
    public static final String SKILLS_FILE = "skills.json";

    private static SettingsView view;

    private PackConfiguration() {
    }

    /** Reads both files and puts their content in force. Called once, when the game has loaded. */
    public static void load(File folder, Logger logger) {
        DefaultsData defaults = Settings.defaults();

        List<String> typeProblems = new ArrayList<>();
        JsonObject typesFile = read(new File(folder, WEAPON_TYPES_FILE), logger);
        TypeRules rules = typesFile == null
                ? TypeRules.NONE
                : TypeRulesFile.parse(typesFile, defaults.builtInTypeIds(), new Predicate<String>() {
                    @Override
                    public boolean test(String itemName) {
                        ResourceLocation name = new ResourceLocation(itemName);
                        return name.toString().equals(itemName) && ForgeRegistries.ITEMS.containsKey(name);
                    }
                }, typeProblems);
        report(logger, WEAPON_TYPES_FILE, typeProblems);
        WeaponTypes.useLocalRules(rules);

        Set<String> knownTypes = new LinkedHashSet<>(defaults.builtInTypeIds());
        knownTypes.addAll(rules.packTypes());
        List<String> skillProblems = new ArrayList<>();
        JsonObject skillsFile = read(new File(folder, SKILLS_FILE), logger);
        Map<ResourceLocation, SkillDefinition> registered = SkillRegistry.registered();
        Map<ResourceLocation, SkillDefinition> configured = skillsFile == null
                ? registered
                : SkillOverrides.apply(skillsFile, registered, defaults, knownTypes, skillProblems);
        report(logger, SKILLS_FILE, skillProblems);
        SkillRegistry.useConfigured(configured);

        view = SettingsView.of(configured.values(), rules);
        logger.info("Pack configuration: {} {} ({} type(s), {} item rule(s), {} problem(s)); {} {} ({} problem(s))",
                WEAPON_TYPES_FILE, typesFile == null ? "not used" : "read", rules.packTypes().size(),
                rules.items().size(), typeProblems.size(),
                SKILLS_FILE, skillsFile == null ? "not used" : "read", skillProblems.size());
    }

    /** This installation's pack configuration, as a server sends it to the clients that join. */
    public static SettingsView view() {
        return view;
    }

    /** The file's content, or null when the file does not exist or cannot be used. */
    static JsonObject read(File file, Logger logger) {
        if (!file.isFile()) {
            return null;
        }
        try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            JsonElement content = new JsonParser().parse(reader);
            if (content.isJsonObject()) {
                return content.getAsJsonObject();
            }
            logger.error("Pack file {}: the file must hold one JSON object; the whole file is ignored", file.getName());
        } catch (JsonParseException | IOException | IllegalStateException unreadable) {
            logger.error("Pack file {}: not readable as JSON ({}); the whole file is ignored",
                    file.getName(), unreadable.getMessage());
        }
        return null;
    }

    private static void report(Logger logger, String fileName, List<String> problems) {
        for (String problem : problems) {
            logger.error("Pack file {}: {}; this entry is ignored", fileName, problem);
        }
    }
}
