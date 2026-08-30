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

## 2026-08-30T07:24:50Z — Reconcile lifecycle with Slot B occupancy
- Revision: 3
- Source checkpoint: `714d20eb4f9bdfd6696780f0d926f0240241e7be`
- Changes: Corrected only lifecycle from `ACTIVE` to `TESTING` because immutable project UUID `2371b6eb-a4fb-4f1e-8203-580b82ce846b` currently occupies canonical Test Slot B. No implementation, procedure, runtime state, slot ownership, accepted-baseline state, release identity, or evidence changed.
- Build/static: No rebuild or new static validation was performed; the recorded Java 25, Gradle 9.5.1, Loom 1.17.20, 16/16 focused-test, and byte-for-byte reproduction evidence remains current.
- Runtime: No physical Workbench mutation, Minecraft launch, or gameplay test occurred; Slot B remains `READY_TO_TEST_VERIFIED / UNTESTED`, represented by project validation `READY_TO_TEST_VERIFIED / RUNTIME_UNTESTED`.
- Artifact: Unchanged current and unaccepted `coal-consolidation-0.1.0-canary1.jar`, 7,488 bytes, SHA-256 `d7598f640184613f03f0ea2fb558cd4d94099ec54cabc189ef978dbb7f5d4f9d`, release source `ff8ea31e3c5959fdb4a2886b1113071bfda5770f`; no accepted or rollback release was created.
- Result: `TESTING` — current Coal Consolidation C1 occupies Test Slot B and remains runtime untested and unaccepted.
- Next state: Keep lifecycle `TESTING` while this UUID occupies either test slot; record the actual Slot B runtime result before acceptance, promotion, or removal derives the next lifecycle from remaining state.

## 2026-08-30T08:11:38Z — Record Coal C1 runtime failure and remove it from Slot B
- Revision: 4
- Source checkpoint: `5e1007888a847f00012a413214d51c810efa1b31`
- Changes: Recorded the user's exact C1 runtime result, removed Coal Consolidation from its last occupied test slot, changed lifecycle from `TESTING` to `ACTIVE`, and updated the retained runtime procedure for a future successor. No Coal implementation debugging or fix, rebuild, successor, promotion, accepted release, or rollback release was created.
- Build/static: No rebuild or new static validation was performed; the preserved Java 25, Gradle 9.5.1, Loom 1.17.20, 16/16 focused-test, and byte-for-byte reproduction evidence remains unchanged static evidence.
- Runtime: C1 is `RUNTIME_FAIL` because the user observed logs smelted through the vanilla furnace `minecraft:charcoal` route still produce `minecraft:charcoal` instead of coal. That result satisfies the runtime stopping condition. The separate Matcha `smoking:charcoal` route was not reported as tested, and no result is inferred for it or any other matrix row.
- Artifact: Current and unaccepted C1 remains exact retained `coal-consolidation-0.1.0-canary1.jar`, 7,488 bytes, SHA-256 `d7598f640184613f03f0ea2fb558cd4d94099ec54cabc189ef978dbb7f5d4f9d`, release source `ff8ea31e3c5959fdb4a2886b1113071bfda5770f`; it is no longer deployed, and no accepted or rollback release exists.
- Result: `ACTIVE` — exact C1 remains `STATIC_PASS / NOT_DEPLOYED / RUNTIME_FAIL`. This is a fixable implementation defect in an unaccepted development candidate, not an external blocker, and the project UUID occupies neither canonical test slot.
- Next state: In a separately authorized implementation task, diagnose only the demonstrated furnace override failure, create and statically verify a successor, then allocate that exact successor through the canonical manager before rerunning the full current matrix; do not redeploy or promote unchanged C1.

## 2026-08-30T22:43:30Z — Build Coal Consolidation Canary 2
- Revision: 5
- Source checkpoint: `03f70b54c774f766ea9edc3487622153a253afe2`
- Changes: Confirmed C1's packaged furnace JSON already returned coal; found no competing Matcha 1.12 or loaded-mod `minecraft:charcoal` resource; and replaced C1's unverified reliance on passive pack precedence with a required common-side mixin that changes only the effective post-resolution `minecraft:charcoal` smelting holder's result to one coal while retaining its identity, input, category/group, notification flag, experience, and timing. Added a competing-pack reload regression that proves a later charcoal-producing resource wins preparation before C2 enforces the final effective recipe.
- Build/static: Java 25, Gradle 9.5.1, and Loom 1.17.20 passed the focused effective-reload regression, all 17 tests in four suites with zero failures, errors, or skips, and the successor JAR build. Built and retained JARs are byte-identical. This is static evidence only.
- Runtime: No Minecraft launch, test-slot allocation, deployment, Test Instance Manager mutation, profile mutation, protected 26.1.2 instance access, or gameplay result occurred. C1's recorded furnace failure remains historical evidence; C2 is runtime untested.
- Artifact: Current and unaccepted `0.1.1-canary2` is `coal-consolidation-0.1.1-canary2.jar`, 11,217 bytes, SHA-256 `713b4f0f0cbf68a712a1ba8c75addf120899b8963afb01c88a14f2e6fceecc29`, from release source `03f70b54c774f766ea9edc3487622153a253afe2`; C1 was not overwritten, and no accepted or rollback release exists.
- Result: `ACTIVE` — C2 is `STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`, occupies neither test slot, has no formal blocker, and preserves the remaining C1 contracts without unrelated recipe rewrites.
- Next state: Under separate authorization, allocate only exact retained C2 through the canonical manager, verify the intended Matcha 1.12 stack and readiness, then run and record the full successor matrix; never redeploy or promote unchanged C1.
