package io.github.mojolowjo.ninefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SkillExpNotifier ConfigWatcher.tick(): check the config files' timestamps once per second, not 20x. */
@Mixin(targets = "com.github.spacemex.config.ConfigWatcher", remap = false)
public abstract class SkillExpWatcherMixin {
    private static long ninefix$lastCheck;

    @Inject(method = "tick()V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void ninefix$throttle(CallbackInfo ci) {
        long now = System.currentTimeMillis();
        if (now - ninefix$lastCheck < 1000L) {
            ci.cancel();
        } else {
            ninefix$lastCheck = now;
        }
    }
}
