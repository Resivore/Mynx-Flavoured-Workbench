# Codex Log

## 2026-08-29T18:03:48Z — Migrate frozen Naturalist port into Mynx Flavoured Workbench
- Revision: 4
- Source checkpoint: `a766495bcf36318301935b2bc922b9f605d6f1e9`
- Changes: Imported 451 maintainable source, resource, build, test, verifier, and license blobs from legacy `Resivore/Minecraft-26.2-Workbench` checkpoint `63a8b46ac939d6bd48e21d4840c480361ad852ef`; normalized `projects/naturalist-26.2` to `projects/naturalist` while retaining the existing preservation architecture and leaving legacy narratives, generated output, originals, and artifacts behind.
- Build/static: Exact file, blob, and mode comparison passed; Java 25 compilation reproduced 190 errors; hash-guarded staging reproduced 2,097 protected files, all 14 focused tests and six static verifiers passed, and 20 dynamic client-item roots were generated.
- Runtime: No Minecraft deployment or runtime test was performed; neither Minecraft instance nor Test Instance Manager state was touched.
- Artifact: No Canary, port JAR, accepted artifact, or rollback exists; the external `naturalist-2.0.3-fabric-1.21.1.jar` was referenced read-only at SHA-256 `3d16c975326e0df24486d44d8010d9e614fc9efdf891864de7b5a0efedfc12f9`.
- Result: BLOCKED — STATIC_FAIL; NOT_DEPLOYED; RUNTIME_UNTESTED. The remaining 190 Minecraft 26.2 API compile errors are the blocker.
- Next state: Resume the preservation port from exact v2 source checkpoint `a766495bcf36318301935b2bc922b9f605d6f1e9`; do not runtime-test until the build succeeds and a real retained, hashed Canary exists.
