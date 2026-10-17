# Prime Optimize

Client-side Fabric mod project for Minecraft **1.20.1** (Java 17), with settings screen and Mod Menu integration.

## Features

- `WorldRenderer` HEAD hook that records camera coordinates without cancelling the render pipeline, avoiding fragile invocation-target injections that conflict with renderer replacements.
- Configurable distance-LOD threshold state and settings UI. This starter implementation does **not** replace vanilla chunk meshes with simplified geometry; that requires a dedicated renderer/chunk mesh pipeline.
- Settings persisted to `.minecraft/config/prime_optimize.properties`.
- Native configuration screen with toggles and credits for **Prime**, **GMU Studios**, and **djawad1**.
- GitHub Actions builds with Java 17 and uploads the resulting JAR as an artifact.

## Build

Requires Java 17 and an internet connection on first build. The included `gradlew` bootstrap downloads Gradle 8.7, then invokes the build:

```sh
./gradlew build
```

On Windows, use Git Bash/WSL for the included bootstrap, or install Gradle 8.7 and run `gradle build`.

The output JAR is created under `build/libs/`. Install it in the `mods` folder with Fabric API. Mod Menu is optional and enables the in-menu configuration entry.

## Technical notes

The renderer hook deliberately does not cancel rendering or claim to implement full solid-block occlusion. Doing either at the render HEAD would incorrectly hide the whole world or cause visual holes. Vanilla/Sodium should retain ownership of their chunk visibility pipeline. Similarly, texture atlases and GPU vertex buffers are shared renderer resources and must be disposed by their owning renderer; this mod does not call `System.gc()` or manually free shared resources.
