# Codex Log

## 2026-11-24T00:40:27Z — Prepare Matcha 1.12.5 Stage 1 Canary 1
- Revision: 1
- Source checkpoint: `e3a13df63e041949a8341a025ac4d937ebacdf83`
- Changes: Adapts six publisher-supplied writers to 26.2, directs the offhand write to the offhand and clears held scratch storage after writes and on load. All 146 call sites and all 78 same-name recipe overlaps are checked.
- Build/static: Java 25 and Gradle 9.5.1 build passed with 1 project tests. The ten-JAR distribution and migration-boundary fixture passed.
- Runtime: RUNTIME_UNTESTED on the isolated candidate branch.
- Artifact: C1 `matcha-intrinsic-update-compat-0.1.0-canary1.jar`, 5,086 bytes, SHA-256 `3ab950e214252adb89364bab563c14d3c500a2ad75844773555b98b7f78026ef`, built_at `2026-11-24T00:22:40.799872Z`.
- Result: ACTIVE, STATIC_PASS.
- Next state: For main hand, offhand and each armour slot, compare a freshly smithed item with a migrated item of the same type. Seed an intrinsic below and above its minimum and an unrelated enchantment. Verify the higher level and unrelated enchantment survive in the target slot, the other slots stay intact and the smithing marker is consumed as upstream specifies.
