# Prime Optimize

Client-side Fabric optimization project for **Minecraft 1.20.1** (Java 17).

## Features

- Retains Minecraft's real frustum test and adds a conservative view-cone broad-phase for chunk bounds when the hook matches the 1.20.1 renderer call site.
- Adds bounded block-entity culling by distance and view direction, with at most six block raycasts per terrain setup to avoid turning occlusion checks into a CPU/GC hotspot.
- Gives distant chunk rebuilds lower priority so nearby chunks finish first. Vanilla's chunk tessellator does not expose a stable public simplified-mesh hook, so this is deliberately a **scheduling LOD fallback**, not a claim that far chunks are rendered as lower-polygon meshes.
- Keeps a fixed-capacity primitive LOD decision cache and invalidates its epoch on chunk unload. Vanilla owns GPU buffer and shared texture-atlas lifetimes; Java heap memory is reclaimed by the JVM, so the mod does not force `System.gc()` or dispose shared textures unsafely.
- Native settings screen with toggle controls and credits for **Prime**, **GMU Studios**, and **djawad1**. Optional Mod Menu integration opens the screen from the Mods menu.

## Build

Requires a JDK 17 installation. On Linux/macOS:

```sh
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The included lightweight wrapper bootstrap downloads Gradle 8.7 on first run and caches it under the Gradle user directory. The workflow runs the same command on GitHub Actions and uploads the JAR from `build/libs/`.

## Install

Copy `build/libs/prime-optimize-1.0.0.jar` into the Minecraft `mods` directory alongside Fabric API. Install Mod Menu to expose the configuration shortcut in the Mods list; Mod Menu is optional.

Configuration is stored in `.minecraft/config/prime_optimize.properties`. `lod_distance_chunks` defaults to 6; `max_block_entity_distance` defaults to 128 blocks.

## Compatibility note

Mixin hooks are optional where a mapped call-site may differ, allowing vanilla behavior to remain as the fallback. Test on a backup instance before combining with other renderer/culling mods. Do not combine multiple mods that redirect the same renderer call site without checking conflicts.
