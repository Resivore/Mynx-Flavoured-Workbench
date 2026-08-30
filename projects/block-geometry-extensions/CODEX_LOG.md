# Codex Log

## 2026-08-29T17:14:33Z — Import frozen BGE C53 into v2
- Revision: 6
- Source checkpoint: `6e9b04ec3947f2514f90775bfeefb599242a4596`
- Changes: Imported the maintainable BGE C53 implementation from legacy `Resivore/Minecraft-26.2-Workbench` checkpoint `1b4c070d7ea5b0699f518f91d0bec35e9a0679a0`; flattened the canonical `alpha2` implementation into `projects/block-geometry-extensions/` and normalized relative paths and build-dependency resolution for the v2 layout.
- Build/static: Java 25 clean build passed; 52 focused architecture/resource checks passed; 66/66 required controlled GameTests passed; all exact retained artifact hashes were verified.
- Runtime: No Minecraft deployment or runtime test was performed; C53 remains `NOT_DEPLOYED` and `RUNTIME_UNTESTED`.
- Artifact: Preserved current `cnm-nibaru-integration-0.6.0-bge-canary53-layer.jar` SHA-256 `57a4599adb3f4c58ae7b99a0148a38760fdb3da59847ca3ec046379db323b7c8` and accepted/rollback `cnm-nibaru-integration-0.5.49-nibaru-cnm-canary1.39-native-directional-material-axis.jar` SHA-256 `0e84fb7b8c69e31c3c22a592d0667db66c2918c8fd9f461bd2c216d377722e69` as exact retained artifacts.
- Result: `ACTIVE` and `GENERATED / CONTROLLED VALIDATION PASS`; `CURRENT_DIFFERS_FROM_ACCEPTED`, with C52 retained as `ACCEPTED_IS_ROLLBACK`.
- Next state: Eventually deploy the paired Nibaru C46 and BGE C53 candidates under controlled runtime ownership and complete the Layer in-game matrix before promotion.

## 2026-08-30T06:51:09Z — Deploy exact BGE C53 and Nibaru C46 to Slot A
- Revision: 7
- Source checkpoint: `8fdfaaf4eddfb4b379ce5d50266f2c40a5294480`
- Changes: Deployed unchanged exact BGE C53 together with its required exact Nibaru C46 contract artifact as Slot A's two-artifact set; the manager used a slot-scoped dependency override to disable accepted BGE C52 and Nibaru C45 without changing their accepted/rollback identities, and no newer `main`-scoped BGE change superseded C53's release source.
- Build/static: No rebuild or new static validation was performed; preserved evidence remains the Java 25 clean build, 52 focused checks, and 66/66 required controlled GameTests recorded for release source `6e9b04ec3947f2514f90775bfeefb599242a4596`.
- Runtime: Manager deployment and readiness verification passed for Slot A, but Minecraft was not launched and no gameplay result was recorded; C53 remains `RUNTIME_UNTESTED`.
- Artifact: Slot A contains `cnm-nibaru-integration-0.6.0-bge-canary53-layer.jar`, 258,709 bytes, SHA-256 `57a4599adb3f4c58ae7b99a0148a38760fdb3da59847ca3ec046379db323b7c8`, and required `more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar`, 5,652,769 bytes, SHA-256 `3281d110f062db62e721d838a35ce915ca73dd41af098a013b52952561ceae7d`; both release sources are `6e9b04ec3947f2514f90775bfeefb599242a4596`.
- Result: `ACTIVE / TESTING`; exact C53 plus exact C46 is `READY_TO_TEST_VERIFIED / RUNTIME_UNTESTED` in Slot A, while accepted BGE C52 remains the BGE rollback and accepted Nibaru C45 remains disabled only for the slot-scoped dependency override.
- Next state: Launch the dedicated Workbench only under explicit runtime ownership, execute the focused C53 Layer matrix, and record Slot A as `PASS`, `FAIL`, or `INCONCLUSIVE` before any promotion.

## 2026-08-30T07:24:50Z — Reconcile lifecycle with Slot A occupancy
- Revision: 8
- Source checkpoint: `714d20eb4f9bdfd6696780f0d926f0240241e7be`
- Changes: Corrected only lifecycle from `ACTIVE` to `TESTING` because immutable project UUID `4b2342fc-7bdf-5ba6-9f37-d551109d214c` currently occupies canonical Test Slot A. No implementation, procedure, runtime state, slot ownership, accepted-baseline membership, release identity, dependency override, or evidence changed.
- Build/static: No rebuild or new static validation was performed; the recorded Java 25 clean build, 52 focused checks, and 66/66 controlled GameTests remain the current static evidence.
- Runtime: No physical Workbench mutation, Minecraft launch, or gameplay test occurred; Slot A remains `READY_TO_TEST_VERIFIED / UNTESTED`, represented by project validation `READY_TO_TEST_VERIFIED / RUNTIME_UNTESTED`.
- Artifact: Unchanged current BGE C53 `cnm-nibaru-integration-0.6.0-bge-canary53-layer.jar`, SHA-256 `57a4599adb3f4c58ae7b99a0148a38760fdb3da59847ca3ec046379db323b7c8`, release source `6e9b04ec3947f2514f90775bfeefb599242a4596`; accepted/rollback BGE C52 and exact Slot A Nibaru C46 dependency identities are unchanged.
- Result: `TESTING` — current BGE C53 occupies Test Slot A and remains runtime untested while distinct accepted BGE C52 provenance remains intact.
- Next state: Keep lifecycle `TESTING` while this UUID occupies either test slot; record the actual Slot A runtime result before promotion or removal derives the next lifecycle from remaining state.

## 2026-08-30T08:11:36Z — Record BGE C53 runtime failure and correct the Layer economy gate
- Revision: 9
- Source checkpoint: `5e1007888a847f00012a413214d51c810efa1b31`
- Changes: Recorded the user's exact C53 Slot A result, corrected `TESTING.md` to replace the stale proportional Layer-drop expectation with the established one-full-source-block combined-geometry economy, and left exact C53 plus exact Nibaru C46 physically and canonically unchanged in Slot A. No BGE source change, C54 build/deployment, promotion, or accepted/rollback mutation was made.
- Build/static: No rebuild or new static validation was performed; the recorded Java 25 clean build, 52 focused checks, and 66/66 controlled GameTests remain unchanged static evidence and do not override the runtime result.
- Runtime: The user reported gravity/falling-material behavior, grass lifecycle, glass/translucency, the otherwise observed Layer geometry behavior, and breaking the combined layered geometry to one full source block as correct. C53 is `RUNTIME_FAIL` because placing additional compatible Layer geometry into an already occupied compatible Layer block consumed additional source/layer items; no unreported matrix row is inferred as passed, failed, or tested.
- Artifact: Slot A retains current BGE C53 `cnm-nibaru-integration-0.6.0-bge-canary53-layer.jar`, SHA-256 `57a4599adb3f4c58ae7b99a0148a38760fdb3da59847ca3ec046379db323b7c8`, release source `6e9b04ec3947f2514f90775bfeefb599242a4596`, paired with exact Nibaru C46 `more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar`, SHA-256 `3281d110f062db62e721d838a35ce915ca73dd41af098a013b52952561ceae7d`; accepted/rollback BGE C52 and Nibaru C45 provenance remain unchanged.
- Result: `TESTING` — exact C53 remains `CONTROLLED_VALIDATION_PASS / READY_TO_TEST_VERIFIED / RUNTIME_FAIL` because this UUID still occupies Slot A; the failed candidate remains unaccepted and no formal blocker is recorded.
- Next state: Do not promote C53 or infer success for unreported rows. Keep the exact Slot A pair unchanged for the current cross-project test, and address the demonstrated same-block Layer-consumption defect only in a separately authorized BGE successor task.
