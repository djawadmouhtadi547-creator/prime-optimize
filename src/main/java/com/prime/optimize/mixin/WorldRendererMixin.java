package com.prime.optimize.mixin;

import com.prime.optimize.PrimeOptimizeConfig;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stable HEAD hook for 1.20.1's WorldRenderer.render method.
 * This intentionally never cancels rendering: doing so would blank the world,
 * and custom occlusion decisions must be integrated into chunk traversal itself.
 */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Unique private double primeOptimize$cameraX;
    @Unique private double primeOptimize$cameraY;
    @Unique private double primeOptimize$cameraZ;
    @Unique private boolean primeOptimize$cameraReady;

    // Kept as explicit LOD trigger state for other mixins/helpers to use.
    @Unique private boolean primeOptimize$lodEnabled = true;
    @Unique private int primeOptimize$lodDistanceChunks = 6;

    @Inject(
        method = "render(Lnet/minecraft/client/util/math/MatrixStack;FJZLnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/GameRenderer;Lnet/minecraft/client/render/LightmapTextureManager;Lorg/joml/Matrix4f;)V",
        at = @At("HEAD"),
        require = 0
    )
    private void primeOptimize$onRenderHead(
        MatrixStack matrices,
        float tickDelta,
        long limitTime,
        boolean renderBlockOutline,
        Camera camera,
        GameRenderer gameRenderer,
        LightmapTextureManager lightmapTextureManager,
        Matrix4f projectionMatrix,
        CallbackInfo ci
    ) {
        if (camera == null) {
            primeOptimize$cameraReady = false;
            return;
        }

        var pos = camera.getPos();
        primeOptimize$cameraX = pos.x;
        primeOptimize$cameraY = pos.y;
        primeOptimize$cameraZ = pos.z;
        primeOptimize$cameraReady = true;
        primeOptimize$lodEnabled = PrimeOptimizeConfig.enableDistanceLod;
        primeOptimize$lodDistanceChunks = PrimeOptimizeConfig.lodDistanceChunks;
    }

    @Unique
    private boolean primeOptimize$isBeyondLodDistance(double x, double y, double z) {
        if (!primeOptimize$cameraReady || !primeOptimize$lodEnabled) return false;
        double dx = x - primeOptimize$cameraX;
        double dy = y - primeOptimize$cameraY;
        double dz = z - primeOptimize$cameraZ;
        double distance = primeOptimize$lodDistanceChunks * 16.0;
        return dx * dx + dy * dy + dz * dz > distance * distance;
    }
}
