package com.prime.optimize;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.client.world.ClientWorld;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

/**
 * Client entry point and shared, allocation-conscious optimization state.
 * The renderer continues to own and dispose its native/GPU buffers; this mod
 * never tries to free Java heap or OpenGL resources behind Minecraft's back.
 */
public final class PrimeOptimize implements ClientModInitializer {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("prime_optimize.properties");

    public static volatile boolean aggressiveOcclusion = true;
    public static volatile boolean lodEnabled = true;
    public static volatile boolean memoryOptimizations = true;
    public static volatile int lodDistanceChunks = 6;
    public static volatile int maxBlockEntityDistance = 128;

    private static volatile boolean cameraReady;
    private static volatile double cameraX;
    private static volatile double cameraY;
    private static volatile double cameraZ;
    private static volatile double forwardX;
    private static volatile double forwardY;
    private static volatile double forwardZ;
    private static int occlusionRayBudget;

    // Fixed-size primitive arrays avoid per-chunk boxed keys and unbounded cache growth.
    private static final int LOD_CACHE_SIZE = 4096;
    private static final int LOD_CACHE_MASK = LOD_CACHE_SIZE - 1;
    private static final long[] LOD_KEYS = new long[LOD_CACHE_SIZE];
    private static final int[] LOD_EPOCHS = new int[LOD_CACHE_SIZE];
    private static final byte[] LOD_VALUES = new byte[LOD_CACHE_SIZE];
    private static int lodCacheEpoch = 1;

    @Override
    public void onInitializeClient() {
        loadConfig();
    }

    public static void captureCamera(Camera camera) {
        if (camera == null) {
            cameraReady = false;
            return;
        }
        Vec3d position = camera.getPos();
        cameraX = position.x;
        cameraY = position.y;
        cameraZ = position.z;

        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());
        double cosPitch = Math.cos(pitch);
        forwardX = -Math.sin(yaw) * cosPitch;
        forwardY = -Math.sin(pitch);
        forwardZ = Math.cos(yaw) * cosPitch;
        cameraReady = true;
        occlusionRayBudget = 6;
        advanceLodCacheEpoch();
    }

    /**
     * Conservative broad-phase view-cone test applied after Minecraft's real
     * frustum test. It only rejects bounds far behind the camera; nearby bounds
     * are deliberately retained to avoid clipping peripheral geometry.
     */
    public static boolean isBoxInViewCone(Box box) {
        if (!aggressiveOcclusion || !cameraReady) {
            return true;
        }

        double centerX = (box.minX + box.maxX) * 0.5;
        double centerY = (box.minY + box.maxY) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        double dx = centerX - cameraX;
        double dy = centerY - cameraY;
        double dz = centerZ - cameraZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        double halfX = (box.maxX - box.minX) * 0.5;
        double halfY = (box.maxY - box.minY) * 0.5;
        double halfZ = (box.maxZ - box.minZ) * 0.5;
        double radius = Math.sqrt(halfX * halfX + halfY * halfY + halfZ * halfZ);
        if (distanceSquared <= Math.max(64.0, radius * radius * 4.0)) {
            return true;
        }

        double distance = Math.sqrt(distanceSquared);
        double facing = (dx * forwardX + dy * forwardY + dz * forwardZ) / distance;
        double rejectThreshold = -0.70 - (radius / distance);
        return facing >= Math.max(-1.0, rejectThreshold);
    }

    /** Returns false when a complex block entity is safely outside our budget. */
    public static boolean shouldRenderBlockEntity(BlockEntity blockEntity) {
        if (!aggressiveOcclusion || !cameraReady || blockEntity == null) {
            return true;
        }

        BlockPos pos = blockEntity.getPos();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double dx = x - cameraX;
        double dy = y - cameraY;
        double dz = z - cameraZ;
        double distanceSquared = dx * dx + dy * dy + dz * dz;

        int maxDistance = Math.max(16, maxBlockEntityDistance);
        if (distanceSquared > (double) maxDistance * maxDistance) {
            return false;
        }
        if (distanceSquared < 32.0 * 32.0) {
            return true;
        }

        double distance = Math.sqrt(distanceSquared);
        double facing = (dx * forwardX + dy * forwardY + dz * forwardZ) / distance;
        if (facing < -0.82) {
            return false;
        }

        // Bound raycasts to six distant block entities per terrain setup/frame.
        // This provides true block-line-of-sight checks without a per-entity raycast storm.
        if (occlusionRayBudget <= 0) {
            return true;
        }
        occlusionRayBudget--;
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        Entity cameraEntity = client.getCameraEntity();
        if (world == null || cameraEntity == null) {
            return true;
        }

        Vec3d start = new Vec3d(cameraX, cameraY, cameraZ);
        Vec3d end = new Vec3d(x, y, z);
        BlockHitResult hit = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                cameraEntity
        ));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    /** ChunkBuilder uses this to de-prioritize distant work, rather than dropping geometry. */
    public static boolean shouldUseLodPriority(int chunkX, int chunkZ) {
        if (!lodEnabled || !cameraReady) {
            return false;
        }

        long key = ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
        int slot = mix(key) & LOD_CACHE_MASK;
        for (int probe = 0; probe < LOD_CACHE_SIZE; probe++, slot = (slot + 1) & LOD_CACHE_MASK) {
            if (LOD_EPOCHS[slot] != lodCacheEpoch) {
                boolean far = isFarChunk(chunkX, chunkZ);
                LOD_KEYS[slot] = key;
                LOD_VALUES[slot] = (byte) (far ? 1 : 0);
                LOD_EPOCHS[slot] = lodCacheEpoch;
                return far;
            }
            if (LOD_KEYS[slot] == key) {
                return LOD_VALUES[slot] != 0;
            }
        }
        return isFarChunk(chunkX, chunkZ);
    }

    private static boolean isFarChunk(int chunkX, int chunkZ) {
        double centerX = ((chunkX << 4) + 8.0) - cameraX;
        double centerZ = ((chunkZ << 4) + 8.0) - cameraZ;
        double distance = Math.max(2, lodDistanceChunks) * 16.0;
        return centerX * centerX + centerZ * centerZ > distance * distance;
    }

    private static int mix(long key) {
        key ^= key >>> 33;
        key *= 0xff51afd7ed558ccdL;
        key ^= key >>> 33;
        key *= 0xc4ceb9fe1a85ec53L;
        key ^= key >>> 33;
        return (int) key;
    }

    /** Called on chunk unload: invalidate fixed-capacity decision cache without forcing a full GC. */
    public static void onChunkUnloaded() {
        advanceLodCacheEpoch();
    }

    private static void advanceLodCacheEpoch() {
        lodCacheEpoch++;
        if (lodCacheEpoch == 0) {
            Arrays.fill(LOD_EPOCHS, 0);
            lodCacheEpoch = 1;
        }
    }

    public static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty("aggressive_occlusion", Boolean.toString(aggressiveOcclusion));
        properties.setProperty("lod_enabled", Boolean.toString(lodEnabled));
        properties.setProperty("memory_optimizations", Boolean.toString(memoryOptimizations));
        properties.setProperty("lod_distance_chunks", Integer.toString(Math.max(2, Math.min(32, lodDistanceChunks))));
        properties.setProperty("max_block_entity_distance", Integer.toString(Math.max(16, Math.min(512, maxBlockEntityDistance))));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                properties.store(writer, "Prime Optimize client configuration");
            }
        } catch (IOException exception) {
            System.err.println("[Prime Optimize] Unable to save config: " + exception.getMessage());
        }
    }

    private static void loadConfig() {
        if (!Files.isRegularFile(CONFIG_PATH)) {
            saveConfig();
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            properties.load(reader);
            aggressiveOcclusion = Boolean.parseBoolean(properties.getProperty("aggressive_occlusion", "true"));
            lodEnabled = Boolean.parseBoolean(properties.getProperty("lod_enabled", "true"));
            memoryOptimizations = Boolean.parseBoolean(properties.getProperty("memory_optimizations", "true"));
            lodDistanceChunks = parseClamped(properties.getProperty("lod_distance_chunks"), 6, 2, 32);
            maxBlockEntityDistance = parseClamped(properties.getProperty("max_block_entity_distance"), 128, 16, 512);
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println("[Prime Optimize] Unable to load config; defaults will be used: " + exception.getMessage());
        }
    }

    private static int parseClamped(String value, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(value)));
        } catch (NumberFormatException | NullPointerException exception) {
            return fallback;
        }
    }
}
