# NINE Performance Fixes

A NeoForge mod with targeted performance fixes for the NINE modpack (Minecraft 1.21.1). Every fix
came out of profiling the pack with [spark](https://spark.lucko.me/) and Java Flight Recorder on
players' PCs, and every fix has its own on/off switch.

## Settings

**Mods → NINE Performance Fixes → Config** shows one switch per fix. Changes apply after restarting
the game. The switches are stored in `config/ninefix-startup.toml`, which can also be edited by hand
(on a server, that's the only way). A fix that's switched off isn't patched into the game at all.

## The fixes

| Switch | What's wrong | What the fix does |
|---|---|---|
| GeckoLib animations | GeckoLib reads every `.json` in any `animations/` or `geo/` folder. *Mining & Placing Animations* keeps its own files there, so GeckoLib throws its whole cache away: no GeckoLib mob animates, and each one writes an error and a stack trace to the log **every frame**. | Before the first resource load, tells GeckoLib (through its own `registerNamespaceExclusion` API) to skip namespaces that only contain files it can't read. Namespaces that mix both are never skipped, only reported. |
| ArPhEx: build its sphere once | `RenderTest6Procedure` rebuilds a 4,050-quad sphere and re-uploads it to the GPU twice every frame (21–31% of the render thread). | Builds it once and reuses it. |
| ArPhEx: slower trophy search | `WorldRenderTestProcedure` walks every block entity in every loaded chunk every frame, looking for mob trophies. | Re-checks each chunk about once a second; chunks with a trophy are still handed over every frame. |
| FancyMenu / SpiffyHUD: remember screen lookups | FancyMenu works out the current screen's identifier thousands of times per frame (~12% of the render thread). | Remembers the answer until the screen or FancyMenu's registry changes. |
| SkillExpNotifier: keep config in memory | Re-reads and re-parses `config.yml` from disk every frame, and checks the file 20 times a second. | Keeps the parsed config for up to 2 seconds; checks the file once a second. |
| KubeJS: skip idle highlight buffers | Clears and copies its highlight buffers every frame even when nothing is highlighted. | Skips that while nothing is highlighted. |
| KubeJS: turn off the web server | Starts a web server for script developers whose accept loop never waits: ~¾ of a CPU core busy, nonstop. | Doesn't start it (same as `"enabled": false` in `kubejs/config/web_server.json`). |
| VanillaBackport: no throwaway colour caches | Builds a brand-new colour cache for every leaf block Distant Horizons colours, uses it once and throws it away (~7% of all memory garbage). | Computes the colour directly. Same colour. |
| Distant Horizons: use at most half the CPU | The pack's config lets Distant Horizons use every core; on a 6-core PC the CPU sat at 100% for a whole session. | Caps Distant Horizons at half the CPU threads (its own default) through its API. Its config file isn't changed. |
| The Obsessed: stop the network flood *(server)* | The Obsessed resends each player's whole variable set, including stored chat text, dozens of times per tick: ~19 MB of game data per second per player. | Sends it at most once per tick per player, with the values as they are at the end of the tick. |

The Obsessed fix runs where the data is sent from: on the **server**, or in single player. Players
get the benefit only when the server has this mod too. Everything else runs on the players' PCs.
The mod adds no network channels, so players and servers can each have it or not.

## Installation

1. Download `ninefix-<version>.jar` from [Releases](https://github.com/mojolowjo/nine-performance-fixes/releases).
2. Put it in the `mods` folder of the game (in Prism Launcher: right-click the instance → **Folder** →
   `minecraft` → `mods`), and in the server's `mods` folder.

> **1.1.0 status:** built and checked against the real mod jars, but not launched in-game yet; see
> the [changelog](CHANGELOG.md).

### For the NINE pack's `assets.txt`

```
mods|ninefix-<version>.jar|<sha1 from the release notes>|https://github.com/mojolowjo/nine-performance-fixes/releases/download/v<version>/ninefix-<version>.jar
```

## Checking that it works

- `logs/latest.log` has a line like `[NINE Fix] Fixes on: [...], off: [...]`, and for the GeckoLib fix
  `Told GeckoLib to skip 'mining_and_placing_animations'`.
- ArPhEx creatures move their legs, and `latest.log` stays small.
- `Distant Horizons threads capped at 3 (configured: 8)` (numbers depend on the PC).
- In a spark profile, `RenderTest6Procedure`, `WorldRenderTestProcedure` and FancyMenu's
  `ScreenIdentifierHandler` are close to 0%.

## Compatibility

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x (profiled on 21.1.249) |
| Mixin | 0.8.7 (bundled with NeoForge 21.1) |
| GeckoLib | 4.x (verified against 4.9.2) |
| ArPhEx | 5.0.2 |
| FancyMenu | 3.9.x (verified against 3.9.12) |
| SkillExpNotifier | 8.1.7 |
| KubeJS | 2101.7.x (verified against 2101.7.2 build 374) |
| VanillaBackport | 1.1.7.10 |
| Distant Horizons | 3.x (verified against 3.3.2) |
| The Obsessed | 1.5.2d |

All of these mods are optional. Every fix is defensive: if its mod is missing, or a future version
changes the code it patches, that fix simply does nothing. It never stops the game from starting.

## Building from source

Only a JDK 17+ is needed: no Gradle, no downloads.

```sh
./build.sh          # -> build/libs/ninefix-<version>.jar (reproducible: same sources give the same jar)
```

The external APIs the code uses are provided as compile-time stubs; see [`stubs/README.md`](stubs/README.md).

### Tests

```sh
GSON_JAR=/path/to/gson-2.10.1.jar ./test.sh
tools/verify_targets.py path/to/ArPhEx.jar path/to/fancymenu.jar path/to/kubejs.jar ...
```

`test.sh` runs the GeckoLib folder scan against a fake game folder full of tricky mods and resource
packs, and logic tests for the settings file, the switches, and each fix's bookkeeping.
`tools/verify_targets.py` checks every Mixin against the real mod jars you give it: that each target
method exists with the right signature, that each patched call really occurs there, and that every
method the mod calls on those mods exists.

## Credits

This project is not affiliated with or endorsed by the authors of the mods it fixes:
[ArPhEx](https://modrinth.com/mod/k0RPP4IO) (Vllax), [GeckoLib](https://github.com/bernie-g/geckolib),
[Mining & Placing Animations](https://modrinth.com/mod/mining_and_placing_animations),
[FancyMenu](https://github.com/Keksuccino/FancyMenu), SkillExpNotifier,
[KubeJS](https://github.com/KubeJS-Mods/KubeJS), VanillaBackport,
[Distant Horizons](https://gitlab.com/distant-horizons-team/distant-horizons) and The Obsessed.
It contains none of their code. It only adjusts behaviour at runtime. All credit for those mods goes
to their authors.

## License

[MIT](LICENSE)
