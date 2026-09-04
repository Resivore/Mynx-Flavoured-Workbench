# Testing

## Current gate

**STATIC PASS — NOT DEPLOYED — RUNTIME UNTESTED — READY FOR MANAGED RUNTIME TESTING — NOT READY FOR PROMOTION**

Test only Carry On Patch `0.1.0-canary2`, artifact `carry-on-patch-0.1.0-canary2.jar`, Fabric mod ID `carry_on_patch`. Before deployment, require its filename, byte size, SHA-256, and source checkpoint to match the current release recorded in `WORKBENCH_STATUS.json` and the revision 2 `CODEX_LOG.md` entry. Stop on any identity drift; do not rebuild or substitute the candidate during runtime testing.

The exact compatibility baseline is GrabAndGo `1.0.1` from Modrinth coordinate `maven.modrinth:199VyzmC:STumnqJf`. Require the enabled GrabAndGo JAR's verified Fabric mod ID, embedded version, filename, byte size, SHA-256, environment, nested-JAR inventory, and target method shape to match the revision 1 audit record. Do not use an alternate GrabAndGo build merely because it reports the same display version.

Canary 2 remains `ACTIVE / NOT_DEPLOYED / RUNTIME_UNTESTED` until a serialized Test Instance Manager transition places its immutable project UUID in a canonical slot cohort and verifies the exact physical deployment. Do not clear, replace, append to, or otherwise disturb an occupied cohort merely to run this test. The current canonical Ribbits candidate and all of its required dependencies may accompany Carry On Patch only as one legitimate, fully represented manager cohort. Never copy experimental patch, Ribbits, GrabAndGo, GeckoLib, or companion files into the dedicated profile outside manager ownership, and never access the protected Matcha Flavoured 26.1.2 gameplay profile.

## Managed preflight

1. Acquire explicit ownership of one serialized Test Instance Manager deployment to the dedicated Matcha Flavoured 26.2 Workbench.
2. Re-read authoritative `main` and canonical `tools/test_instance_manager/runtime-state.json`. Require a genuinely available slot or an explicitly authorized whole-cohort transition that preserves the other slot exactly.
3. Deploy through the Test Instance Manager and run its read-only verification. Require `PHYSICAL_STATE_VERIFIED`, this project's exact current release comparison to be `CURRENT_RELEASE_DEPLOYED`, the cohort deployment to be `READY_TO_TEST_VERIFIED`, and this member's independent result to remain `UNTESTED` before launching Minecraft.
4. Verify Minecraft `26.2`, Java `25`, GrabAndGo `1.0.1`, Carry On Patch Canary 2, and exactly one enabled provider of each required Fabric mod ID. Require no duplicate Carry On Patch JAR and no alternate GrabAndGo JAR.
5. Require the manager cohort to contain the current canonical Ribbits test candidate and every required companion, including its exact GeckoLib/provider dependencies, when performing the Ribbit rows. Stop rather than manually adding a missing dependency.
6. Use a disposable test world. Preserve the complete client log from startup through normal shutdown. Do not delete, reset, or rewrite player data to prepare the test.

## Startup and target reproduction

1. Start the dedicated Workbench and reach a disposable world. Require no Carry On Patch Mixin target, descriptor, injection-count, accessor, or application failure.
2. Spawn or locate one Ribbit and pick it up with GrabAndGo.
3. Hold the Ribbit continuously for at least 60 seconds while moving, turning, jumping, and changing camera direction.
4. During the continuous carry, exercise all of the following:
   - first-person carry view;
   - third-person rear view;
   - third-person front view;
   - inventory or another GUI picture-in-picture entity view;
   - ordinary world rendering of the carried Ribbit and surrounding entities.
5. Require the Ribbit model to remain visible and correctly animated. Missing rendering, exception suppression, a blank placeholder, the wrong model, or flicker consistent with changing render IDs is a failure.
6. Compare performance immediately before pickup, throughout the 60-second carry, and immediately after dropping. Acceptance requires removal of the carry-triggered exception/log loop and its associated severe frame collapse; do not invent a universal FPS threshold.

## Focused Ribbit alignment and clipping

The intended adjustment is translation only. `RibbitCarryPlacement` holds independent first-person local offsets `(0, 0.50, 0.40)` and third-person local offsets `(0, 0.60, 0.20)`, applied after upstream transforms at the entity submit call. With GrabAndGo 1.0.1 these produce +0.15 up / 0.12 farther from the camera in first person, and +0.30 up / 0.10 forward in third person. These are initial canary values based on inspected coordinate spaces, not visually accepted values. No scale, orientation, or animation correction is included.

1. Capture a baseline before pickup and matching held screenshots in first person, third-person rear, and third-person front. Record FOV, GUI scale, camera angle, shader setting, Ribbit profession/accessories, and exact artifact identities. Compare with the supplied low-held screenshots; require the body to look supported between the raised hands and the face to clear the HUD without entering the camera or hiding the useful view.
2. In each view, walk, turn slowly and quickly, strafe, jump, crouch/uncrouch, look up/down, and repeatedly change cameras. Check the player torso, each hand/forearm, Ribbit feet/body/face, and camera for overlap, clipping, snapping, or an unnatural floating gap. Inspect both sides and front rather than accepting one favorable angle.
3. Repeat with a plain/nitwit Ribbit, Merchant backpack, Guard, Chef, Farmer, Prospector, a tall-hat profession, instrument/umbrella variants where available, and the separate `ribbits:wandering_ribbit`. Record unavailable variants as untested. Require normal proportions, facing, model selection, accessories, and animations; do not hide accessories to pass clipping checks.
4. Open the inventory player preview while carrying each representative model, rotate the preview, close it, and switch cameras. Check alignment in the preview and correct arm/object placement afterward. Repeat with Complementary enabled and shaders disabled, inspecting the rendered object and visible shadows for displaced, detached, missing, or clipped geometry.
5. Carry continuously for at least 60 seconds in each shader mode and perform at least ten pickup/drop cycles. Compare responsiveness before/during/after carry; check logs for renewed render errors, confirm exact single-entity restoration, and inspect normal world appearance and animation after drop.
6. Compare a vanilla mob, another available GeckoLib mob, a normal block, and a filled container against their baseline. Their placement, arms, pickup/drop behavior, and contents must remain unchanged. If a legitimate managed setup without Ribbits is available, confirm the generic patch starts and carries these objects without requiring Ribbits/GeckoLib.

Stop for clipping into player/arms/camera, HUD intrusion, unsupported/floating appearance, wrong facing/proportions, or any generic placement regression. Record actual observations per view/variant; static matrices cannot pass these rows.

## Renderer coverage

1. Repeat the full Ribbit pickup, 60-second movement/camera exercise, GUI view, and drop with Complementary Unbound through Iris enabled.
2. Repeat the same exercise with shaders disabled.
3. Review the log for both runs and require zero new occurrences during the test of:
   - `Failed to render carried entity`;
   - `Tried to access entity ID before ID assignment`;
   - a Carry On Patch Mixin application or injection failure.
4. A recurrence confined to Iris shadow rendering, GUI picture-in-picture rendering, or one camera mode is still a failure.

## Identity stability and restoration

1. Drop or place the carried Ribbit and require exactly one Ribbit to be restored.
2. Require no ghost or duplicate entity, no wrong entity or model, and no synthetic render entity appearing in the client world's entity tracker.
3. Confirm the restored Ribbit's appearance and relevant entity state survive and ordinary AI resumes.
4. Repeat the Ribbit pickup/drop cycle several times, including quick successive cycles. Watch for model flicker, identity reuse across different carried objects, collision symptoms, retained ghosts, or progressively worsening performance.
5. If project diagnostics expose the render-only identity, confirm it stays stable for repeated render passes of one cached synthetic entity, differs for simultaneously cached carried entities, and is not persisted into GrabAndGo NBT or player data. Do not infer these properties from a merely crash-free frame.

## Generic and regression cases

1. Carry one vanilla living entity through pickup, movement, camera changes, GUI view, and drop. Require visible rendering and ordinary restoration.
2. Carry another available GeckoLib living entity through the same path, preferably the Animal Garden crocodile known to use the same render-state extraction path. Require the ID fix to remain generic, and placement to remain unchanged: only the two explicit Ribbit types receive the new translation.
3. Carry a normal block. Require its model, placement, and resulting block state to remain correct.
4. Carry a container with distinctive contents. Drop/place it and require every tested item and count to survive unchanged.
5. Where the test setup permits two carried synthetic entities to be cached concurrently, exercise both and require no wrong-model swap, flicker, disappearance, or other ID-collision symptom.

## Persistence and secondary warning

1. With a representative carried object, save and exit the disposable world normally, then re-enter it. Exercise the supported GrabAndGo resume/drop path and require no carried-state, entity-state, inventory, container-content, or NBT loss.
2. Inspect the complete log for `GrabAndGo_CarriedData`, especially `Expected field 'GrabAndGo_CarriedData' to contain value of type STRING, but got COMPOUND`.
3. Record that warning separately from the render-ID result. This canary does not change that independent persistence warning; do not report it fixed.
4. Do not delete or reset player data in response to the warning. If it prevents safe carry, resume, or drop validation, stop and classify the affected persistence result as `INCONCLUSIVE` or `RUNTIME_FAIL` according to the observed behavior.

## Stopping conditions and result

Stop and report `RUNTIME_FAIL` or `INCONCLUSIVE`, with only the observations actually made, for any of the following:

- crash, disconnect, startup failure, or new Mixin conflict;
- identity, dependency, cohort, deployment, readiness, or source-checkpoint drift;
- repeated carried-entity exceptions or renewed severe frame collapse;
- carried model missing, suppressed, wrong, or flickering;
- duplicate, ghost, missing, or incorrectly restored entity after dropping;
- appearance, AI, state, inventory, container-content, or NBT loss;
- synthetic carried entity appearing in the client world or entity tracker;
- render-ID collision or unstable-ID symptoms;
- shader-only, shadow-only, GUI-only, camera-only, or restart-only recurrence;
- vanilla entity, second GeckoLib entity, block, container, networking, persistence, or ordinary server-created identity regression.

Record a project `RUNTIME_PASS` only after every applicable required row above is actually completed against the exact verified candidate and baseline, the complete log remains free of the specified render loop and patch application failures, carried models remain visible, and carry/drop restoration remains correct. Keep the independent `GrabAndGo_CarriedData` observation explicit. Compilation, static tests, a Knot/Mixin application harness, artifact inspection, deterministic builds, manager readiness, and an unexecuted checklist are not Minecraft runtime evidence.
