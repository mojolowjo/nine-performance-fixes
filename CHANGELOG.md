# Changelog

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
