package io.github.mojolowjo.ninefix.mixin;

import io.github.mojolowjo.ninefix.ObsessedSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** End of every server tick: send The Obsessed's coalesced variable syncs (see {@link ObsessedSync}). */
@Mixin(targets = "net.minecraft.server.MinecraftServer", remap = false)
public abstract class ServerTickFlushMixin {
    @Inject(method = "tickServer", at = @At("RETURN"), require = 0, remap = false)
    private void ninefix$flushObsessedSyncs(CallbackInfo ci) {
        ObsessedSync.flush();
    }
}
