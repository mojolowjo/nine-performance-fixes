package io.github.mojolowjo.ninefix.mixin;

import io.github.mojolowjo.ninefix.FancyMenuCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A newly registered universal screen identifier can change getIdentifierOfScreen results. */
@Mixin(targets = "de.keksuccino.fancymenu.customization.screen.identifier.UniversalScreenIdentifierRegistry", remap = false)
public abstract class FancyMenuRegistryMixin {
    @Inject(method = "register(Ljava/lang/String;Ljava/lang/String;)V", at = @At("TAIL"), require = 0, remap = false)
    private static void ninefix$registryChanged(CallbackInfo ci) {
        FancyMenuCache.invalidate();
    }
}
