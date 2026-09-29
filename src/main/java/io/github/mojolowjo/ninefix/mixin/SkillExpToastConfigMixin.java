package io.github.mojolowjo.ninefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SkillExpNotifier 8.1.x CustomToastComponent.config() re-reads and re-parses config.yml from disk
 * on every call (several times per frame per visible toast). Keep the parsed config for up to 2 s.
 */
@Mixin(targets = "com.github.spacemex.client.CustomToastComponent", remap = false)
public abstract class SkillExpToastConfigMixin {
    private static final String TARGET = "config()Lcom/github/spacemex/yml/YamlConfigUtil;";
    private static final long MAX_AGE_MS = 2000L;
    private static volatile Object ninefix$config;
    private static volatile long ninefix$loadedAt;

    @Inject(method = TARGET, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void ninefix$useCachedConfig(CallbackInfoReturnable<Object> cir) {
        Object cached = ninefix$config;
        if (cached != null && System.currentTimeMillis() - ninefix$loadedAt < MAX_AGE_MS) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = TARGET, at = @At("RETURN"), require = 0, remap = false)
    private void ninefix$rememberConfig(CallbackInfoReturnable<Object> cir) {
        ninefix$config = cir.getReturnValue();
        ninefix$loadedAt = System.currentTimeMillis();
    }
}
