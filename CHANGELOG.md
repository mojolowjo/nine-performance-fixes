# Changelog

## 1.0.0 — 2026-09-27

First release.

- **GeckoLib:** skip resource namespaces whose `animations/` or `geo/` files GeckoLib can't read
  (always including *Mining & Placing Animations*). This makes GeckoLib animations load again and
  stops the per-frame "Unable to find animation" errors and stack traces in `latest.log`.
- **ArPhEx 5.0.2:** build the `RenderTest6Procedure` sphere mesh once instead of rebuilding and
  re-uploading it twice every frame.
