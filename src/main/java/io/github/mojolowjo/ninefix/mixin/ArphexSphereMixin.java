package io.github.mojolowjo.ninefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * ArPhEx 5.0.2: RenderTest6Procedure.execute() starts with
 *     if (begin(Mode.QUADS, DefaultVertexFormat.POSITION_COLOR, true)) { ...build 4050-quad sphere...; end(); }
 * The "true" (update) flag throws away the finished sphere and rebuilds + re-uploads it
 * on every call - twice per frame (AFTER_SKY and AFTER_PARTICLES). The sphere never
 * changes and it is the only shape this class builds, so we pass "false": it is built
 * once, then every later draw reuses it. Nothing looks different.
 *
 * Soft target (string) + require = 0: if ArPhEx changes or is removed, this simply
 * does nothing instead of crashing the game.
 */
@Mixin(targets = "net.arphex.procedures.RenderTest6Procedure", remap = false)
public abstract class ArphexSphereMixin {
    @ModifyArg(
            method = "execute(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/arphex/procedures/RenderTest6Procedure;begin(Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;Lcom/mojang/blaze3d/vertex/VertexFormat;Z)Z",
                    remap = false),
            index = 2,
            require = 0,
            remap = false)
    private static boolean ninefix$buildSphereOnce(boolean update) {
        return false;
    }
}
