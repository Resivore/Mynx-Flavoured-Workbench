# Codex Log

## 2026-11-24T16:34:20Z — Migrate to Matcha 1.12.5, Canary 1
- Revision: 1
- Source checkpoint: `1ce48a4c2b288c0a18d71e625531e56b618b7ae7`
- Changes: Adapts six publisher-supplied writers to 26.2, directs the offhand write to the offhand and clears held scratch storage after writes and on load. All 146 call sites and all 78 same-name recipe overlaps are checked.
- Build/static: Java 25 and Gradle 9.5.1 build passed with 1 project tests. The coordinated packaged-stack fixture passed.
- Runtime: RUNTIME_UNTESTED. No Minecraft profile or server deployment was used.
- Artifact: C1 `matcha-intrinsic-update-compat-0.1.0-canary1.jar`, 5,086 bytes, SHA-256 `3ab950e214252adb89364bab563c14d3c500a2ad75844773555b98b7f78026ef`, built_at `2026-11-24T00:22:40.799872Z`, artifact source `e3a13df63e041949a8341a025ac4d937ebacdf83`.
- Result: ACTIVE, STATIC_PASS.
- Next state: Follow TESTING.md and record owner-directed runtime observations against this release.
