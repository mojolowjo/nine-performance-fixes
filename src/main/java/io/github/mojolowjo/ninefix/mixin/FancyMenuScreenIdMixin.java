package io.github.mojolowjo.ninefix.mixin;

import io.github.mojolowjo.ninefix.FancyMenuCache;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** FancyMenu 3.9.x ScreenIdentifierHandler.getIdentifierOfScreen(Screen): one-entry memo. */
@Mixin(targets = "de.keksuccino.fancymenu.customization.screen.identifier.ScreenIdentifierHandler", remap = false)
public abstract class FancyMenuScreenIdMixin {
    private static final String TARGET = "getIdentifierOfScreen(Lnet/minecraft/client/gui/screens/Screen;)Ljava/lang/String;";

    @Inject(method = TARGET, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void ninefix$useCachedId(Screen screen, CallbackInfoReturnable<String> cir) {
        String cached = FancyMenuCache.cachedScreenId(screen);
        if (cached != null) cir.setReturnValue(cached);
    }

    @Inject(method = TARGET, at = @At("RETURN"), require = 0, remap = false)
    private static void ninefix$rememberId(Screen screen, CallbackInfoReturnable<String> cir) {
        FancyMenuCache.storeScreenId(screen, cir.getReturnValue());
    }
}
