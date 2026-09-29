// Compile-time stub (see stubs/README.md). The real class is provided at runtime.
package org.spongepowered.asm.mixin.extensibility;
// Signatures from Mixin 0.8.7 (FabricMC fork tag 0.15.2+mixin.0.8.7).
public interface IMixinConfigPlugin {
    void onLoad(String mixinPackage);
    String getRefMapperConfig();
    boolean shouldApplyMixin(String targetClassName, String mixinClassName);
    void acceptTargets(java.util.Set<String> myTargets, java.util.Set<String> otherTargets);
    java.util.List<String> getMixins();
    void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo);
    void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo);
}
