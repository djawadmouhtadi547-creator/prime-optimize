package com.prime.optimize.mixin;

import com.prime.optimize.PrimeOptimize;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Yarn 1.20.1 names the chunk render dispatcher ChunkBuilder. Distant rebuilds
 * are demoted to background priority, preserving vanilla mesh correctness while
 * letting nearby chunks become ready first. This is a safe scheduling LOD
 * fallback; it intentionally does not pretend to generate a simplified mesh.
 */
@Mixin(ChunkBuilder.class)
public abstract class ChunkRenderDispatcherMixin {
    @Redirect(
            method = "scheduleRebuild",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk;scheduleRebuild(Z)V"
            ),
            require = 0
    )
    private void primeOptimize$prioritizeNearChunkRebuilds(ChunkBuilder.BuiltChunk builtChunk, boolean important) {
        boolean distant = false;
        if (PrimeOptimize.lodEnabled) {
            BlockPos origin = builtChunk.getOrigin();
            distant = PrimeOptimize.shouldUseLodPriority(origin.getX() >> 4, origin.getZ() >> 4);
        }
        builtChunk.scheduleRebuild(important && !distant);
    }
}
