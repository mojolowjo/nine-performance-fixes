# Compile-time stubs

These are **not** part of the mod and are never packaged into the jar.

They are minimal declarations of the external APIs the mod refers to, so `build.sh` can compile the
mod with nothing but a JDK: no Gradle and no downloads. At runtime the real classes are provided by
NeoForge, Minecraft and the mods being fixed. Every stub matches the real API's package, name, method
signatures and (for annotations) retention policy. If you change the code to use more of an API, add
the matching declaration here, or build with a full NeoForge toolchain instead.

Where they come from:

| Stubs | Source |
|---|---|
| `org/spongepowered/asm/mixin/**` annotations and callback classes | The real files from Mixin 0.8.7 (FabricMC fork tag `0.15.2+mixin.0.8.7`, the version NeoForge 21.1 ships), copied with their MIT license headers intact and only documentation-only imports removed. The compiler therefore enforces the exact annotation shapes the game's Mixin expects. |
| `IMixinConfigPlugin`, `IMixinInfo`, `ClassNode` | Signatures from the same Mixin 0.8.7 sources. |
| `net/neoforged/**` (`ModContainer`, `ModConfig`, `ModConfigSpec`, `ConfigurationScreen`, ...) | Signatures from the NeoForge `1.21.1` and FancyModLoader `1.21.1` branches. |
| `com/seibel/distanthorizons/api/**` | Distant Horizons 3.3.2's public API (checked against the real jar by `tools/verify_targets.py`). |
| `net/minecraft/**` | Only the members the mod calls, each one also called with the same descriptor by a mod in the pack (e.g. `ClientLevel.calculateBlockTint` by VanillaBackport, `ClientLevel.getChunk` by ArPhEx). |
| `com/google/gson/**`, `org/slf4j/**`, `org/apache/commons/**` | The few Gson, SLF4J 2 and Commons Lang methods used. |

Fields in stubs are never initialised with constants, because javac would copy a constant into the
mod's own bytecode instead of reading the real field.
