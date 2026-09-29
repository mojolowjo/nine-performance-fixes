package io.github.mojolowjo.ninefix.mixin;

import io.github.mojolowjo.ninefix.TrophyScan;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** ArPhEx 5.0.2 WorldRenderTestProcedure: only hand over chunks that may contain a mob trophy. */
@Mixin(targets = "net.arphex.procedures.WorldRenderTestProcedure", remap = false)
public abstract class ArphexTrophyScanMixin {
    @Redirect(
            method = "execute(Lnet/neoforged/bus/api/Event;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;",
                    remap = false),
            require = 0, remap = false)
    private static LevelChunk ninefix$onlyTrophyChunks(ClientLevel level, int chunkX, int chunkZ) {
        return TrophyScan.chunkIfItMayHaveTrophies(level, chunkX, chunkZ);
    }
}
