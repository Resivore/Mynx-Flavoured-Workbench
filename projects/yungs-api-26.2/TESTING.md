# Testing

Compat.2 is the current unaccepted candidate and is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. Use only `YungsApi-26.2-Fabric-6.1.1-compat.2.jar`, 1,260,939 bytes, SHA-256 `FF22A6B509BA559988D7A9352DC94AC612C4B099517ACAC7DA7C81322D797ED7`, from source checkpoint `f0be1f9c6c1f3e843a0e44791a650df237836537`.

Temurin Java 25.0.4.1+1 and the tracked Gradle 9.2.0 wrapper completed `:Fabric:compileJava :Fabric:compileConsumerFixtureJava --offline --no-daemon` and `:Common:clean :Fabric:clean :Fabric:build --offline --no-daemon`. The normal Fabric server fixture completed `:Fabric:runConsumerFixtures --offline --no-daemon`; all five fixture groups passed: automatic registration, JSON randomization, structure-processor codec, jigsaw generation, and terrain adaptation. Terrain-adaptation coverage includes legitimate empty input, independent fresh cursors, representative nonempty carve/bury behavior, 256 repeated evaluations, and a deterministic two-thread overlap against one Beardifier. These are automated build/static results, not Minecraft gameplay or cohort runtime validation.

## Runtime preconditions

1. Acquire explicit Test Instance Manager ownership of a disposable slot/cohort in the dedicated Matcha Flavoured 26.2 Workbench. Require this exact YUNG artifact to report `CURRENT_RELEASE_DEPLOYED / READY_TO_TEST_VERIFIED` with an independent `UNTESTED` result before launching Minecraft.
2. Verify Minecraft 26.2, Fabric Loader 0.19.3, the exact manager-selected Ribbits successor, and exactly one enabled `yungsapi` provider. Stop if the YUNG filename, SHA-256, embedded version, or source checkpoint differs.
3. Create a fresh disposable world or visit wholly ungenerated chunks. Do not rely only on the partially generated chunk from the predecessor failure, alter the accepted baseline outside the serialized manager transition, or access the protected Matcha Flavoured 26.1.2 profile.

## Focused runtime matrix

1. Start the dedicated client/server pair and review startup and data-reload logs for YUNG Mixin, registry, codec, dependency, or duplicate-provider errors.
2. Locate `ribbits:ribbit_village`, teleport to it, and enter its newly generated chunks. Move around every side far enough to trigger surrounding chunk noise generation and enhanced terrain adaptation.
3. Repeat the generation path at one additional Ribbit Village or in a second fresh-world reproduction. Record the seed and coordinates for both attempts.
4. Require no `NoSuchElementException` from `EnhancedBeardifierHelper.computeDensity` and no secondary `Parent chunk missing` failure in either client or server logs.
5. Inspect both villages and surrounding terrain. Require ordinary structure generation and visible terrain blending/carving to remain present; a crash-free result caused by disabled enhanced adaptation is a failure.
6. Generate ordinary chunks with no applicable enhanced structure pieces and confirm generation remains stable, then revisit already generated village and non-village chunks to exercise repeated sampling without a delayed exception.
7. Run the paired Ribbits Onion Town, village, registry, save/reload, restart, and broader smoke checks from that exact Ribbits candidate's current `TESTING.md`, recording YUNG-relevant observations separately.
8. Stop cleanly and review the complete logs once more before assigning this project's independent slot result.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` for any identity/readiness mismatch, YUNG Mixin or codec failure, enhanced-beardifier exception, `Parent chunk missing`, stalled or missing chunk, disabled or visibly broken terrain adaptation, repeat-generation failure, broader YUNG-dependent structure regression, crash, or relevant shutdown error. Do not infer a runtime pass from compilation, the automated Fabric fixture, successful startup, or an unexecuted checklist, and do not promote Compat.2 until the applicable matrix has actually passed.
