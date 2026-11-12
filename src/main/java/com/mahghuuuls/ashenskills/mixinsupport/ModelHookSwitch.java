package com.mahghuuuls.ashenskills.mixinsupport;

import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Lets the body-model hook be left out for one launch, with the Java option
 * {@code -Dashenskills.modelHook=false}. For comparing the game with and without the hook when a
 * rendering problem is looked into; skills then show no body animation. This class must live
 * outside the Mixin package: Mixin refuses to load ordinary classes from a package it owns.
 */
public final class ModelHookSwitch implements IMixinConfigPlugin {

    static final String PROPERTY = "ashenskills.modelHook";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if ("false".equalsIgnoreCase(System.getProperty(PROPERTY))) {
            LogManager.getLogger("Ashen Skills").warn(
                    "-D{}=false: the body-model hook {} is left out; skills show no body animation",
                    PROPERTY, mixinClassName);
            return false;
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
