package io.github.mojolowjo.ninefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Punchy 2.8b-2.8d ModelPartsParticleSystem. Two leftover debug methods write an INFO line to latest.log
 * about once a second per arm whenever Punchy's particles are on, and Punchy has no setting for them:
 *  - maybeLogGlowDefinition: "[Punchy Glow Definition] arm=... item=... root=[...]" while an item is held
 *  - maybeLogGlowRender: "[Punchy Glow Render] ..." while a glow particle is drawn
 * They only read state, format it and log it (the one thing they write is their own once-a-second
 * timestamp, which nothing else reads), so skipping them changes nothing else.
 */
@Mixin(targets = "punchy.client.modelparts.ModelPartsParticleSystem", remap = false)
public abstract class PunchyDebugLogMixin {
    @Inject(method = "maybeLogGlowDefinition(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/entity/HumanoidArm;"
            + "Lorg/joml/Matrix4f;Ljava/util/List;)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void ninefix$skipGlowDefinitionLog(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "maybeLogGlowRender(Lnet/minecraft/client/Minecraft;Lnet/minecraft/world/entity/HumanoidArm;"
            + "Lpunchy/client/modelparts/ModelPartsParticleSystem$RenderParticle;Lorg/joml/Matrix4f;Lorg/joml/Vector3f;"
            + "Lorg/joml/Matrix4f;Lorg/joml/Quaternionf;ZLjava/lang/Object;Ljava/lang/String;FFF)V",
            at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void ninefix$skipGlowRenderLog(CallbackInfo ci) {
        ci.cancel();
    }
}
