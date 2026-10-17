package com.prime.optimize;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Small, dependency-free config stored in the normal Minecraft config directory. */
public final class PrimeOptimizeConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("prime_optimize.properties");

    public static boolean enableFrustumHelpers = true;
    public static boolean enableDistanceLod = true;
    public static boolean enableBlockEntityDistanceLimit = true;
    public static int lodDistanceChunks = 6;
    public static int maxBlockEntityDistance = 128;

    private PrimeOptimizeConfig() {}

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(FILE)) {
            p.load(in);
            enableFrustumHelpers = Boolean.parseBoolean(p.getProperty("frustum_helpers", "true"));
            enableDistanceLod = Boolean.parseBoolean(p.getProperty("distance_lod", "true"));
            enableBlockEntityDistanceLimit = Boolean.parseBoolean(p.getProperty("block_entity_distance_limit", "true"));
            lodDistanceChunks = clamp(parseInt(p.getProperty("lod_distance_chunks"), 6), 2, 32);
            maxBlockEntityDistance = clamp(parseInt(p.getProperty("max_block_entity_distance"), 128), 32, 512);
        } catch (IOException ignored) {
            // Keep safe defaults if the config cannot be read.
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("frustum_helpers", Boolean.toString(enableFrustumHelpers));
        p.setProperty("distance_lod", Boolean.toString(enableDistanceLod));
        p.setProperty("block_entity_distance_limit", Boolean.toString(enableBlockEntityDistanceLimit));
        p.setProperty("lod_distance_chunks", Integer.toString(lodDistanceChunks));
        p.setProperty("max_block_entity_distance", Integer.toString(maxBlockEntityDistance));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Prime Optimize settings");
            }
        } catch (IOException ignored) {
            // A read-only config directory should not crash client startup.
        }
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (RuntimeException ignored) { return fallback; }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
