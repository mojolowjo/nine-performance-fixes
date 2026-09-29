package io.github.mojolowjo.ninefix.mixin;

import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * KubeJS 2101.7.x HighlightRenderer.ShaderChain. Every frame KubeJS clears its highlight buffers
 * and copies the depth buffer, but they're only used when something is highlighted
 * (renderAnything == true), and draw() already skips otherwise.
 *  - clearInput: nothing was drawn since the last clear (renderAnything false), so the buffer is
 *    still empty: skip clearing it again.
 *  - clearDepth(copy = true) for the world chain: the copy is only read by draw(), which won't
 *    run this frame: skip it. (The GUI chain's clearDepth(copy = false) is left untouched.)
 */
@Mixin(targets = "dev.latvian.mods.kubejs.client.highlight.HighlightRenderer$ShaderChain", remap = false)
public abstract class KubeJSHighlightMixin {
    @Shadow(remap = false)
    public abstract MutableBoolean renderAnything();

    @Inject(method = "clearInput(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void ninefix$skipCleanClear(Minecraft mc, CallbackInfo ci) {
        if (renderAnything().isFalse()) ci.cancel();
    }

    @Inject(method = "clearDepth(Lnet/minecraft/client/Minecraft;Z)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void ninefix$skipUnusedDepthCopy(Minecraft mc, boolean copy, CallbackInfo ci) {
        if (copy && renderAnything().isFalse()) ci.cancel();
    }
}
