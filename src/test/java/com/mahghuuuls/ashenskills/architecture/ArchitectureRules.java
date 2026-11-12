package com.mahghuuuls.ashenskills.architecture;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The dependency rules of the mod, as checks on one source file's package and imports. Kept apart
 * from the file scan so the rules themselves can be tested with made-up sources.
 */
final class ArchitectureRules {

    static final String ROOT = "com.mahghuuuls.ashenskills";

    /** Packages of other mods, and the only package of this mod allowed to import each. */
    private static final Map<String, String> FOREIGN_MODS = new LinkedHashMap<>();

    static {
        FOREIGN_MODS.put("com.mahghuuuls.critworks", "integration.critworks");
        FOREIGN_MODS.put("com.mahghuuuls.jass", "integration.jass");
        FOREIGN_MODS.put("com.mahghuuuls.jawms", "integration.jawms");
        FOREIGN_MODS.put("electroblob.wizardry", "integration.jawms");
        FOREIGN_MODS.put("com.mahghuuuls.jafs", "integration.jafs");
        FOREIGN_MODS.put("com.oblivioussp.spartanweaponry", "integration.spartan");
    }

    private static final String[] CLIENT_ONLY_PREFIXES = {
        "net.minecraft.client.", "net.minecraftforge.client.", "net.minecraftforge.fml.client.", ROOT + ".client."
    };

    private static final String[] GAME_PREFIXES = {"net.minecraft.", "net.minecraftforge."};

    /** Packages that hold rules other parts build on, and so must not know those parts. */
    private static final String[] LOW_LEVEL_PACKAGES = {"weapon", "attribute", "combat.math", "client.animation.clip"};

    /** Packages of plain arithmetic and data, testable without the game. */
    private static final String[] PURE_PACKAGES = {"combat.math", "client.animation.clip"};

    private static final String[] HIGH_LEVEL_PACKAGES = {"runtime", "network", "ashenforge"};

    private ArchitectureRules() {
    }

    /**
     * @param packageName package of the source file
     * @param imports imported names, as written after "import" or "import static"
     * @return one sentence per broken rule; empty when the file follows the rules
     */
    static List<String> violations(String packageName, List<String> imports) {
        List<String> found = new ArrayList<>();
        String local = localName(packageName);
        boolean clientSide = inPackage(local, "client") || inPackage(local, "mixin");
        for (String imported : imports) {
            if (!clientSide && startsWithAny(imported, CLIENT_ONLY_PREFIXES)) {
                found.add(packageName + " imports client-only " + imported);
            }
            for (Map.Entry<String, String> foreign : FOREIGN_MODS.entrySet()) {
                if (isUnder(imported, foreign.getKey()) && !inPackage(local, foreign.getValue())) {
                    found.add(packageName + " imports " + imported + "; only " + foreign.getValue() + " may");
                }
            }
            if (inAny(local, PURE_PACKAGES) && startsWithAny(imported, GAME_PREFIXES)) {
                found.add(packageName + " is pure arithmetic and data but imports " + imported);
            }
            String importedLocal = localName(imported);
            if (importedLocal == null) {
                continue;
            }
            if (inAny(local, LOW_LEVEL_PACKAGES) && inAny(importedLocal, HIGH_LEVEL_PACKAGES)) {
                found.add(packageName + " must not depend on " + imported);
            }
            if (inPackage(local, "api") && !inPackage(importedLocal, "api")) {
                found.add("api must stand alone but imports " + imported);
            }
            if (inPackage(local, "skill.builtin") && !inPackage(importedLocal, "api")
                    && !inPackage(importedLocal, "skill.builtin")) {
                found.add("built-in skills may use only the api package but " + packageName + " imports " + imported);
            }
        }
        return found;
    }

    /** Name below the mod's root package, "" for the root itself, null for anything outside the mod. */
    private static String localName(String name) {
        if (name.equals(ROOT)) {
            return "";
        }
        return name.startsWith(ROOT + ".") ? name.substring(ROOT.length() + 1) : null;
    }

    private static boolean inPackage(String local, String packageName) {
        return local != null && (local.equals(packageName) || local.startsWith(packageName + "."));
    }

    private static boolean inAny(String local, String[] packages) {
        for (String candidate : packages) {
            if (inPackage(local, candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnder(String name, String packageName) {
        return name.equals(packageName) || name.startsWith(packageName + ".");
    }

    private static boolean startsWithAny(String name, String[] prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
