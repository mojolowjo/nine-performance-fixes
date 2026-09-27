# Compile-time stubs

These are **not** part of the mod and are never packaged into the jar.

They are minimal declarations of the few external APIs the mod refers to
(NeoForge's `@Mod`, Mixin's `@Mixin` / `@ModifyArg` / `@At`, SLF4J's `Logger`,
and a handful of Gson methods). They exist only so `build.sh` can compile the mod
with nothing but a JDK: no Gradle and no downloads.

At runtime the real classes are provided by NeoForge and Minecraft. Every stub
matches the real API's package, name, method signatures and (for annotations)
retention policy. If you change the code to use more of an API, add the matching
declaration here or build with a full NeoForge toolchain instead.

The Mixin annotations (`org/spongepowered/asm/mixin/**`) are the real files from Mixin 0.8.7
(FabricMC fork tag `0.15.2+mixin.0.8.7`, the version NeoForge 21.1 ships), copied with their MIT
license headers intact and only documentation-only imports removed. Using the real definitions
means the compiler enforces the exact annotation shapes the game's Mixin expects.
