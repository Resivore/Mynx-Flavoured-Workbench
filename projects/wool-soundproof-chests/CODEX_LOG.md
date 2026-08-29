# Codex Log

## 2026-08-29T21:03:18Z — Migrate frozen Wool Soundproof Chests into Mynx Flavoured Workbench
- Revision: 1
- Source checkpoint: `1a113058af6a6e8ba3250528967830b293c2f1a1`
- Changes: Imported the maintainable build, exact canonical source/resources, focused evaluator test, adapted artifact verifier, current testing guidance, and sole retained artifact from frozen legacy checkpoint `f0101f38c446dddaa4226450386402c63597ba7c`; the nine implementation/build/test blobs remain exact matches for legacy source commit `29484e4d67629ff4480f6ae7d5248a9a736ac4a4`.
- Build/static: Temurin Java 25.0.4.1+1 and Gradle 9.5.1 `clean test build --no-daemon` passed all 6 focused tests with zero failures, errors, or skips; all required retained-JAR entries, embedded mod ID/version, exact Git blob, size, and SHA-256 were verified. Fabric Loom resolved the maintained `1.17-SNAPSHOT` selector to 1.17.20 for this verification build; the fresh build did not replace the retained JAR.
- Runtime: No Minecraft deployment or runtime check was performed; neither test slot, either physical Workbench instance, Test Instance Manager state, nor accepted runtime ownership was changed. `READY_TO_TEST_VERIFIED` and the focused `RUNTIME_PASS` preserve the prior evidence from the exact same accepted bytes rather than claiming new migration-time validation.
- Artifact: Current, accepted, and retained rollback are the same sole release, C1 / legacy Alpha 1 / embedded version `0.1.0-alpha1`: `wool-soundproof-chests-0.1.0-alpha1.jar`, 8,288 bytes, SHA-256 `e8f0d10dff823e97a4f4ab39029bf7a71b13501f2294544c55e38f7e1ca9d020`, source commit `29484e4d67629ff4480f6ae7d5248a9a736ac4a4`.
- Result: ACCEPTED — `STATIC_PASS`; preserved `READY_TO_TEST_VERIFIED` and `RUNTIME_PASS`; no blocker. The new repository initializes this UUID at revision 1 under its migration protocol.
- Next state: Remain accepted at exact C1; reopen only for an explicit scope expansion, regression, or future Minecraft-version port, and do not infer validation for the deferred cases.
