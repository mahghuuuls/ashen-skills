package com.mahghuuuls.ashenskills.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArchitectureRulesTest {

    private static final String ROOT = ArchitectureRules.ROOT;
    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.*]+)\\s*;");

    @Test
    void productionSourcesFollowTheDependencyRules() throws IOException {
        Path sources = Paths.get("src", "main", "java");
        assertTrue(Files.isDirectory(sources), "run from the project folder: missing " + sources.toAbsolutePath());
        assertEquals(Collections.emptyList(), scan(sources));
    }

    @Test
    void theScanReportsAForbiddenImportInARealSourceFile(@TempDir Path sources) throws IOException {
        Path weapon = Files.createDirectories(sources.resolve("com/mahghuuuls/ashenskills/weapon"));
        Files.write(weapon.resolve("Broken.java"), Arrays.asList(
                "package " + ROOT + ".weapon;",
                "",
                "import java.util.List;",
                "import " + ROOT + ".runtime.SkillRuntime;",
                "import static " + ROOT + ".network.AshenNetwork.channel;",
                "",
                "final class Broken {}"), StandardCharsets.UTF_8);
        Path config = Files.createDirectories(sources.resolve("com/mahghuuuls/ashenskills/config"));
        Files.write(config.resolve("Fine.java"), Arrays.asList(
                "package " + ROOT + ".config;",
                "import java.util.List;",
                "final class Fine {}"), StandardCharsets.UTF_8);

        List<String> problems = scan(sources);

        assertEquals(2, problems.size(), problems.toString());
        assertTrue(problems.get(0).contains("Broken.java") && problems.get(0).contains("runtime.SkillRuntime"),
                problems.toString());
        assertTrue(problems.get(1).contains("network.AshenNetwork"), problems.toString());
    }

    /** Reads every Java file under the folder and returns one line per broken dependency rule. */
    private static List<String> scan(Path sources) throws IOException {
        List<Path> files;
        try (Stream<Path> walk = Files.walk(sources)) {
            files = walk.filter(path -> path.toString().endsWith(".java")).sorted().collect(Collectors.toList());
        }
        assertFalse(files.isEmpty(), "no source was scanned");
        List<String> problems = new ArrayList<>();
        for (Path file : files) {
            String packageName = "";
            List<String> imports = new ArrayList<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Matcher packageLine = PACKAGE.matcher(line);
                if (packageLine.find()) {
                    packageName = packageLine.group(1);
                }
                Matcher importLine = IMPORT.matcher(line);
                if (importLine.find()) {
                    imports.add(importLine.group(1));
                }
            }
            assertFalse(packageName.isEmpty(), "no package line was read from " + file);
            for (String violation : ArchitectureRules.violations(packageName, imports)) {
                problems.add(sources.relativize(file) + ": " + violation);
            }
        }
        return problems;
    }

    @Test
    void sharedCodeMayNotImportClientClasses() {
        assertBroken(ROOT + ".runtime", "net.minecraft.client.Minecraft");
        assertBroken(ROOT + ".network", ROOT + ".client.hud.SkillHud");
        assertBroken(ROOT, "net.minecraftforge.fml.client.registry.ClientRegistry");
        assertAllowed(ROOT + ".client.render", "net.minecraft.client.Minecraft");
        assertAllowed(ROOT + ".mixin", "net.minecraft.client.model.ModelBiped");
        assertAllowed(ROOT + ".runtime", "net.minecraftforge.fml.relauncher.SideOnly");
    }

    @Test
    void anotherModsClassesStayInsideItsIntegrationPackage() {
        assertBroken(ROOT + ".runtime", "com.mahghuuuls.jass.api.JassApi");
        assertBroken(ROOT + ".integration.jass", "com.mahghuuuls.jawms.api.ManaApi");
        assertBroken(ROOT + ".combat", "com.mahghuuuls.critworks.api.CritworksApi");
        assertBroken(ROOT + ".weapon", "com.oblivioussp.spartanweaponry.item.ItemKatana");
        assertAllowed(ROOT + ".integration.jass", "com.mahghuuuls.jass.api.JassApi");
        assertAllowed(ROOT + ".integration.jawms", "electroblob.wizardry.spell.Spell");
        assertAllowed(ROOT + ".integration.jafs", "com.mahghuuuls.jafs.api.JafsApi");
    }

    @Test
    void lowLevelPackagesDoNotKnowTheRuntimeNetworkOrForge() {
        assertBroken(ROOT + ".weapon", ROOT + ".runtime.SkillRuntime");
        assertBroken(ROOT + ".attribute", ROOT + ".network.AshenNetwork");
        assertBroken(ROOT + ".weapon.types", ROOT + ".ashenforge.ForgeTile");
        assertBroken(ROOT + ".combat.math", ROOT + ".runtime.SkillRuntime");
        assertBroken(ROOT + ".combat.math", "net.minecraft.entity.Entity");
        assertAllowed(ROOT + ".combat.math", "java.util.List");
        assertBroken(ROOT + ".client.animation.clip", "net.minecraft.client.Minecraft");
        assertBroken(ROOT + ".client.animation.clip", "net.minecraft.util.ResourceLocation");
        assertBroken(ROOT + ".client.animation.clip", ROOT + ".network.AshenNetwork");
        assertBroken(ROOT + ".client.animation.clip", ROOT + ".runtime.SkillRuntime");
        assertAllowed(ROOT + ".client.animation.clip", "com.google.gson.JsonObject");
        assertAllowed(ROOT + ".client.animation", "net.minecraft.client.Minecraft");
        assertAllowed(ROOT + ".combat", "net.minecraft.entity.Entity");
        assertAllowed(ROOT + ".runtime", ROOT + ".weapon.SkillTag");
        assertAllowed(ROOT + ".weapon", ROOT + ".config.Settings");
    }

    @Test
    void theApiStandsAloneAndBuiltInSkillsUseOnlyTheApi() {
        assertBroken(ROOT + ".api", ROOT + ".runtime.SkillRuntime");
        assertAllowed(ROOT + ".api", ROOT + ".api.SkillBehavior");
        assertAllowed(ROOT + ".api", "net.minecraft.entity.player.EntityPlayer");
        assertBroken(ROOT + ".skill.builtin", ROOT + ".combat.SkillDamage");
        assertAllowed(ROOT + ".skill.builtin", ROOT + ".api.SkillContext");
    }

    private static void assertBroken(String packageName, String imported) {
        assertFalse(ArchitectureRules.violations(packageName, Arrays.asList(imported)).isEmpty(),
                packageName + " importing " + imported + " must be reported");
    }

    private static void assertAllowed(String packageName, String imported) {
        assertEquals(Collections.emptyList(), ArchitectureRules.violations(packageName, Arrays.asList(imported)));
    }
}
