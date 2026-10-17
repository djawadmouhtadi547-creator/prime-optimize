package com.prime.optimize.mixin;

import com.prime.optimize.PrimeOptimize;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "setupTerrain", at = @At("HEAD"), require = 0)
    private void primeOptimize$captureCamera(Camera camera, Frustum frustum, boolean hasForcedFrustum,
                                              boolean spectator, CallbackInfo ci) {
        PrimeOptimize.captureCamera(camera);
    }

    /** Vanilla frustum culling still runs first; this adds a conservative behind-camera rejection. */
    @Redirect(
            method = "setupTerrain",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/Frustum;isVisible(Lnet/minecraft/util/math/Box;)Z"
            ),
            require = 0
    )
    private boolean primeOptimize$frustumAndViewCone(Frustum frustum, Box box) {
        return frustum.isVisible(box) && PrimeOptimize.isBoxInViewCone(box);
    }
}
