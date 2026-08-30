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

## 2026-08-30T01:38:14Z — Complete the Naturalist 26.2 preservation port and retain Canary 1
- Revision: 5
- Source checkpoint: `2d239bcbbec10b4a15716924549bf3147c440465`
- Changes: Migrated the remaining item/recipe, block/block-entity, particle, advancement/effect/potion, registry/spawn/platform, optional compatibility, GUI, mixin, and snail-shell renderer APIs to Minecraft 26.2 without feature deletion or inert stubs; routed all items and blocks through ResourceKey-aware registration; preserved capture/release, spawn-egg, bucket, persistence, rendering, and optional-integration semantics; enabled only the one audited 26.2 access-widener entry. Work began from authoritative `main` checkpoint `c3e05af0c7a043a071e63f4a6af8009475000ef3` and preserved legacy checkpoint `63a8b46ac939d6bd48e21d4840c480361ad852ef` plus v2 checkpoint `a766495bcf36318301935b2bc922b9f605d6f1e9`. A private generator reads all 47 spawn-egg color tuples from the Naturalist registry, hash-verifies and stages only the legacy template plus two texture layers from external Minecraft 1.21.1 client JAR `499f6897d1837516680f3114072d8106e11c9adcd933fe5cf051b551089b0c99`, and emits 47 two-tint roots within the 67 generated client-item roots; no protected source asset is tracked.
- Build/static: Temurin 25.0.4.1, Gradle 9.6.1, and Loom 1.17.20 reduced the exact 190-diagnostic baseline to zero; `clean build persistenceTest itemModelTest` passed with all 14 focused tests, all six preservation verifiers passed, and the retained JAR's metadata, mixins, access widener, staged template assets, and generated roots were inspected successfully.
- Runtime: No Minecraft deployment or gameplay validation was performed; no profile, runtime slot, or Test Instance Manager state was touched.
- Artifact: Private retained `naturalist-2.0.3+26.2-port-canary1.jar`, 11,368,203 bytes, SHA-256 `b5f136f042f28617b9fb130d8d298e70f8538824c5c03dcdbf53fec266d5c7ac`, reproduced from implementation commit `2d239bcbbec10b4a15716924549bf3147c440465`.
- Result: ACTIVE — STATIC_PASS; NOT_DEPLOYED; RUNTIME_UNTESTED. No static blocker remains.
- Next state: Deploy this exact retained Canary through the Test Instance Manager, run the normal focused readiness verifier, then execute the focused Naturalist runtime matrix without rebuilding or substituting the binary.
