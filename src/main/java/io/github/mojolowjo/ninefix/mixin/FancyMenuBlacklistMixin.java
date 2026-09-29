package io.github.mojolowjo.ninefix.mixin;

import io.github.mojolowjo.ninefix.FancyMenuCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FancyMenu 3.9.x ScreenCustomization.isScreenBlacklisted(String): one-entry memo. */
@Mixin(targets = "de.keksuccino.fancymenu.customization.ScreenCustomization", remap = false)
public abstract class FancyMenuBlacklistMixin {
    private static final String TARGET = "isScreenBlacklisted(Ljava/lang/String;)Z";

    @Inject(method = TARGET, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void ninefix$useCachedBlacklist(String screenClassPath, CallbackInfoReturnable<Boolean> cir) {
        Boolean cached = FancyMenuCache.cachedBlacklist(screenClassPath);
        if (cached != null) cir.setReturnValue(cached);
    }

    @Inject(method = TARGET, at = @At("RETURN"), require = 0, remap = false)
    private static void ninefix$rememberBlacklist(String screenClassPath, CallbackInfoReturnable<Boolean> cir) {
        FancyMenuCache.storeBlacklist(screenClassPath, cir.getReturnValue());
    }

    /** A new blacklist rule can change answers: drop the memos. */
    @Inject(method = "addScreenBlacklistRule(Lde/keksuccino/fancymenu/customization/ScreenCustomization$ScreenBlacklistRule;)V",
            at = @At("TAIL"), require = 0, remap = false)
    private static void ninefix$rulesChanged(CallbackInfo ci) {
        FancyMenuCache.invalidate();
    }
}
