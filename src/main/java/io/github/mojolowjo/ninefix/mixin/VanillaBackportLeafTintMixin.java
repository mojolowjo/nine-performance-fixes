package io.github.mojolowjo.ninefix.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ColorResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * VanillaBackport 1.1.7.x LeafColors.getAverageDryFoliageColor: for every single block it builds a new
 * BlockTintCache (hash map + colour arrays), asks it for one colour and throws it away. Distant Horizons
 * calls this for every leaf block it draws far away, so it was ~7% of all memory garbage. The cache is
 * never reused, so computing the colour directly gives exactly the same result.
 */
@Mixin(targets = "com.blackgear.vanillabackport.client.api.color.LeafColors", remap = false)
public abstract class VanillaBackportLeafTintMixin {

    @Shadow(remap = false)
    @Final
    public static ColorResolver DRY_FOLIAGE_COLOR_RESOLVER;

    @Inject(method = "lambda$getAverageDryFoliageColor$3(Lnet/minecraft/client/multiplayer/ClientLevel;Lnet/minecraft/core/BlockPos;)Ljava/lang/Integer;",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void ninefix$tintWithoutThrowawayCache(ClientLevel level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(level.calculateBlockTint(pos, DRY_FOLIAGE_COLOR_RESOLVER));
    }
}
