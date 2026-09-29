package io.github.mojolowjo.ninefix.mixin;

import io.github.mojolowjo.ninefix.Settings;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies a fix's mixins only when that fix is switched on (config/ninefix-startup.toml, read at
 * startup by {@link Settings}). A fix that's switched off isn't patched into the game at all.
 */
public final class NineFixMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOG = LoggerFactory.getLogger("NINE Fix");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simple = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        Settings.Fix fix = Settings.fixForMixin(simple);
        if (fix == null) return true; // not tied to a switch
        boolean on = Settings.on(fix.key());
        if (!on) LOG.info("Fix '{}' is switched off - not applying {}", fix.key(), simple);
        return on;
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
