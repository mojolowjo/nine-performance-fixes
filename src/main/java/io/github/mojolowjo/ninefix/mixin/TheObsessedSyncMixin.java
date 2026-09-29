package io.github.mojolowjo.ninefix.mixin;

import io.github.mojolowjo.ninefix.ObsessedSync;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The Obsessed 1.5.x PlayerVariables: see {@link ObsessedSync}. Server side. */
@Mixin(targets = "net.theobsessed.network.TheObsessedModVariables$PlayerVariables", remap = false)
public abstract class TheObsessedSyncMixin implements ObsessedSync.Vars {

    @Shadow(remap = false)
    public abstract void syncPlayerVariables(Entity entity);

    @Inject(method = "syncPlayerVariables(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"),
            cancellable = true, require = 0, remap = false)
    private void ninefix$coalesceSync(Entity entity, CallbackInfo ci) {
        if (entity instanceof ServerPlayer player && ObsessedSync.intercept((ObsessedSync.Vars) (Object) this, player)) {
            ci.cancel();
        }
    }

    @Override
    public void ninefix$sendNow(ServerPlayer player) {
        syncPlayerVariables(player);
    }
}
