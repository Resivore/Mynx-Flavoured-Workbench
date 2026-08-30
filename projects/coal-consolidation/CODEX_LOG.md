# Codex Log

## 2026-08-29T22:15:17Z — Migrate Coal Consolidation into Mynx Flavoured Workbench
- Revision: 1
- Source checkpoint: `ff8ea31e3c5959fdb4a2886b1113071bfda5770f`
- Changes: Imported all 20 maintainable build, production-source, resource, and focused-test Git blobs exactly from legacy source checkpoint `872f9ce6ede9eeef5352591b2f03ce39241ed00f`; retained the exact C1 binary from legacy head `8efd7ff3c00c7bb82356e06f3aaa19d3079be3f8`; preserved canonical UUID and name from identity checkpoint `92c3cd9fa710a3206d0ec05be49880b5046ccd90`; and normalized current controls without recreating legacy BUILD_PROVENANCE, global status files, canary manifest, or stale audit/status narratives.
- Build/static: Migration verification used Temurin Java 25.0.4.1+1, Gradle 9.5.1, and Loom 1.17.20; `clean test build --no-daemon` passed all 16 tests in four suites with zero failures, errors, or skips, and the fresh JAR reproduced the retained filename, 7,488-byte size, and SHA-256 exactly without replacing it. Separately preserved legacy evidence is Java 25, Gradle 9.5.1, Loom 1.17.20, 16/16 focused tests, and the same static artifact identity.
- Runtime: No Minecraft launch, deployment, Test Slot operation, Test Instance Manager mutation, accepted-stack change, runtime-profile access, protected 26.1.2 gameplay-instance access, originals change, resource-pack work, or unrelated STNC/QSN work was performed. C1 remains runtime untested, and neither legacy nor migration static evidence is treated as runtime validation.
- Artifact: Current and unaccepted `0.1.0-canary1` is `coal-consolidation-0.1.0-canary1.jar`, 7,488 bytes, SHA-256 `d7598f640184613f03f0ea2fb558cd4d94099ec54cabc189ef978dbb7f5d4f9d`, imported from legacy source `872f9ce6ede9eeef5352591b2f03ce39241ed00f` and retention checkpoint `8efd7ff3c00c7bb82356e06f3aaa19d3079be3f8`; no accepted or rollback release exists.
- Result: ACTIVE — C1 remains GENERATED / STATICALLY VALIDATED / RUNTIME UNTESTED, is not deployed, has no formal blocker, and preserves the narrow audited behavior without redesign.
- Next state: Under explicit runtime ownership, deploy only the exact retained C1 hash through the normal controlled Workbench path, run focused readiness verification and the applicable `TESTING.md` matrix, and record only observed behavior before considering acceptance or a successor Canary.

## 2026-08-30T06:51:09Z — Deploy exact Coal Consolidation C1 to Slot B
- Revision: 2
- Source checkpoint: `8fdfaaf4eddfb4b379ce5d50266f2c40a5294480`
- Changes: Deployed unchanged exact C1 to Slot B through the controlled manager path; no newer `main`-scoped Coal Consolidation change superseded release source `ff8ea31e3c5959fdb4a2886b1113071bfda5770f`, and no accepted or rollback Coal release was created.
- Build/static: No rebuild or new static validation was performed; the preserved Java 25, Gradle 9.5.1, Loom 1.17.20, 16/16 focused-test, and byte-for-byte reproduction evidence remains current.
- Runtime: Manager deployment and readiness verification passed for Slot B against the accepted Minecraft 26.2, Fabric, Fabric API, Matcha, and JEI dependency stack, but Minecraft was not launched and no gameplay result was recorded; C1 remains `RUNTIME_UNTESTED`.
- Artifact: Slot B contains `coal-consolidation-0.1.0-canary1.jar`, 7,488 bytes, SHA-256 `d7598f640184613f03f0ea2fb558cd4d94099ec54cabc189ef978dbb7f5d4f9d`, from release source `ff8ea31e3c5959fdb4a2886b1113071bfda5770f`.
- Result: `ACTIVE`; exact C1 is `STATIC_PASS / READY_TO_TEST_VERIFIED / RUNTIME_UNTESTED` in Slot B, remains unaccepted, and has no accepted or rollback release.
- Next state: Launch the dedicated Workbench only under explicit runtime ownership, execute the focused C1 matrix, and record Slot B as `PASS`, `FAIL`, or `INCONCLUSIVE` before any acceptance decision.
