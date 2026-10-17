package com.prime.optimize.mixin;

import com.prime.optimize.PrimeOptimize;
import net.minecraft.client.world.ClientChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Invalidates this mod's fixed-size chunk-detail cache when client chunks unload.
 * Vanilla remains responsible for disposing renderer-owned vertex buffers and
 * GPU resources. Forcing System.gc() or deleting shared atlas textures here would
 * cause pauses or break unrelated chunks, so neither is attempted.
 */
@Mixin(ClientChunkManager.class)
public abstract class MemoryOptimizationMixin {
    @Inject(method = "unload", at = @At("TAIL"), require = 0)
    private void primeOptimize$releaseChunkTracking(CallbackInfo ci) {
        if (PrimeOptimize.memoryOptimizations) {
            PrimeOptimize.onChunkUnloaded();
        }
    }
}
