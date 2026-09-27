# NINE Performance Fixes

A small **client-side** NeoForge mod with targeted performance fixes for the NINE modpack
(Minecraft 1.21.1). It fixes two problems found by profiling the pack with
[spark](https://spark.lucko.me/):

| Problem | Share of the render thread (measured on two players' PCs) |
|---|---|
| ArPhEx rebuilds the same sphere mesh twice every frame | 21% – 31% |
| GeckoLib animations never load, so every GeckoLib mob writes an error and stack trace to the log every frame | 4% – 8% (in bursts) |

Not needed on servers. Safe to add or remove at any time. It changes nothing on disk.

## What it fixes

### 1. GeckoLib animations failing to load (and the log spam)

At startup GeckoLib reads **every** `.json` file inside **any** `assets/<namespace>/animations/`
(and `geo/`) folder, from every mod and resource pack. If a single file isn't in GeckoLib's
format, GeckoLib discards its entire animation cache.

*Mining & Placing Animations* keeps its own block animations in exactly such a folder. With both
mods installed:

- no GeckoLib mob in the pack animates (ArPhEx creatures, Hollow Steve, The Obsessed, …), and
- every one of them on screen logs `Unable to find animation` plus a full stack trace to
  `latest.log` **every frame**. That writes to disk on the render thread, which is where the
  stutter and the huge log files come from.

**Fix:** before the first resource load, the mod scans your `mods` and `resourcepacks` folders
(including libraries bundled inside mods). For any namespace that only contains files GeckoLib
can't read, it tells GeckoLib to skip that namespace through GeckoLib's own
`GeckoLibCache.registerNamespaceExclusion` API. *Mining & Placing Animations* is always skipped.
Both mods then work as intended. Namespaces that mix valid and invalid files are never skipped;
they're only reported in the log.

### 2. ArPhEx rebuilding a sphere twice per frame

ArPhEx 5.0.2's `RenderTest6Procedure` runs twice per frame (after the sky and after particles).
Each time it rebuilds a 4,050-quad sphere with trigonometry and uploads it to the GPU as a new
vertex buffer, even though the sphere never changes and is the only shape that class draws.

**Fix:** a one-argument Mixin makes the sphere build once and reuses it afterwards. Nothing looks
different.

## Installation

1. Download `ninefix-<version>.jar` from [Releases](https://github.com/mojolowjo/nine-performance-fixes/releases).
2. Put it in the instance's `mods` folder (in Prism Launcher: right-click the instance →
   **Folder** → `minecraft` → `mods`).

### For the NINE pack's `assets.txt`

```
mods|ninefix-1.0.0.jar|<sha1 from the release notes>|https://github.com/mojolowjo/nine-performance-fixes/releases/download/v1.0.0/ninefix-1.0.0.jar
```

## Checking that it works

- `logs/latest.log` contains `[NINE Fix]` lines such as
  `Told GeckoLib to skip 'mining_and_placing_animations'`.
- ArPhEx creatures move their legs, and `latest.log` stays small.
- In a spark profile (`/sparkc profiler start` … `/sparkc profiler stop`),
  `net.arphex.procedures.RenderTest6Procedure` is close to 0%.

## Compatibility

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x (profiled on 21.1.249) |
| GeckoLib | 4.x (verified against 4.9.2) — optional |
| ArPhEx | 5.0.2 — optional |

Both fixes are defensive. If GeckoLib or ArPhEx is missing, or ArPhEx changes that code in a
future version, the affected fix simply does nothing. It never crashes the game.

## Building from source

Only a JDK 17+ is needed: no Gradle, no downloads.

```sh
./build.sh          # -> build/libs/ninefix-<version>.jar (reproducible: same sources give the same jar)
```

The few external APIs the code uses are provided as compile-time stubs; see
[`stubs/README.md`](stubs/README.md).

### Tests

```sh
GSON_JAR=/path/to/gson-2.10.1.jar ./test.sh
```

This builds a fake game folder with tricky mods and resource packs (a foreign animations folder,
one hidden inside a bundled library, a mixed namespace, a disabled mod, …) and checks exactly
which namespaces get skipped. Set `ARPHEX_JAR` as well to also check that a real mod is left alone.

## Credits

This project is not affiliated with or endorsed by the authors of
[ArPhEx](https://modrinth.com/mod/k0RPP4IO) (Vllax),
[GeckoLib](https://github.com/bernie-g/geckolib), or
[Mining & Placing Animations](https://modrinth.com/mod/mining_and_placing_animations).
It contains none of their code. It only adjusts behaviour at runtime. All credit for those mods
goes to their authors.

## License

[MIT](LICENSE)
