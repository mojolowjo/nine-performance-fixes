# Changelog

## 1.1.0 — 2026-09-28

- **Settings screen:** Mods → NINE Performance Fixes → Config has an on/off switch for every fix
  (stored in `config/ninefix-startup.toml`; restart after changes). A switched-off fix isn't patched in.
- **Now loads on servers too**, for the new server-side fix below. Still optional on either side.
- New fixes, all found with a Java Flight Recorder profile of a 10-minute session on the NINE server:
  - **The Obsessed** (server): its player data was resent dozens of times per tick (~19 MB/s of game
    data per player). Now sent at most once per tick per player, with the end-of-tick values.
  - **Distant Horizons:** capped at half the CPU threads (the pack's config let it use every core, and
    the CPU sat at 100%).
  - **KubeJS:** its developer web server isn't started (its loop kept ~3/4 of a core busy).
  - **VanillaBackport:** no more throwaway colour cache per leaf block (~7% of all memory garbage).
  - **FancyMenu / SpiffyHUD** screen lookups, **ArPhEx** trophy search, **SkillExpNotifier** config
    reading and **KubeJS** idle highlight buffers (render-thread fixes from earlier profiles).
- `tools/verify_targets.py` checks every Mixin and every call into the fixed mods against their jars.

> **Not yet tested in-game.** Every Mixin target and call was checked against the exact mod versions
> in NINE 0.0.5, and the logic is covered by tests, but nobody has launched the game with 1.1.0 yet.

## 1.0.1 — 2026-09-27

- **Build fix, no behaviour change.** The `@ModifyArg` patch now stores its injection point as a
  single `@At`, the form Mixin 0.8.7 (the version NeoForge 21.1 ships) expects. 1.0.0 stored it as a
  list; that happened to load fine, but the same mistake in another mod crashed the game while
  loading, so it's corrected here before ninefix goes out to everyone.
- The compile-time stubs now use the real Mixin 0.8.7 annotation definitions, and `build.sh` refuses
  to build if any `@ModifyArg`/`@Redirect` stores its `@At` as a list.
- The mod's code is byte-for-byte identical to 1.0.0; only that annotation value differs.

> **Not yet tested in-game.** Verified by comparing the compiled classes with 1.0.0 and by the
> automated tests, but nobody has launched the game with 1.0.1 yet.

## 1.0.0 — 2026-09-27

First release.

- **GeckoLib:** skip resource namespaces whose `animations/` or `geo/` files GeckoLib can't read
  (always including *Mining & Placing Animations*). This makes GeckoLib animations load again and
  stops the per-frame "Unable to find animation" errors and stack traces in `latest.log`.
- **ArPhEx 5.0.2:** build the `RenderTest6Procedure` sphere mesh once instead of rebuilding and
  re-uploading it twice every frame.
