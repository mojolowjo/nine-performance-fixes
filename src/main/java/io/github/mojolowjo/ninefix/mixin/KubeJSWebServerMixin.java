package io.github.mojolowjo.ninefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * KubeJS 2101.7.x starts a local web server for script developers (the same as "enabled": true in
 * kubejs/config/web_server.json). Its accept loop never waits, so it keeps ~3/4 of a CPU core busy and
 * makes garbage nonstop. Skipping the start is the same as setting "enabled": false.
 */
@Mixin(targets = "dev.latvian.mods.kubejs.web.LocalWebServer", remap = false)
public abstract class KubeJSWebServerMixin {
    @Inject(method = "start(Lnet/minecraft/util/thread/BlockableEventLoop;Z)V", at = @At("HEAD"),
            cancellable = true, require = 0, remap = false)
    private static void ninefix$dontStart(CallbackInfo ci) {
        org.slf4j.LoggerFactory.getLogger("NINE Fix").info("Not starting the KubeJS web server (switch 'kubejsWebServerOff')");
        ci.cancel();
    }
}
