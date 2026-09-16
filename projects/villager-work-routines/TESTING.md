# Testing

Candidate `villager-work-routines-0.1.0-canary9.jar` is a user-managed Minecraft Java 26.2/Fabric runtime trial. Before testing, verify its version, filename, SHA-256, and source checkpoint against `WORKBENCH_STATUS.json`. Use the normal frog-villager resource pack, do not install the Real Working Villagers reference JAR, and retain relevant `[VillagerWorkRoutines]` logs. A static build/test pass is not runtime evidence.

## Recorded Canary 7 startup evidence

Canary 7 is a user-reported external runtime **FAIL** for exact identity `0.1.0-canary7`, `villager-work-routines-0.1.0-canary7.jar`, SHA-256 `2ea2f9f9e2e44e357af8fad1643c76ff83958a290c8e7213ea932e4923561f4f`, source `a1c0d7c4dd5e7515179da3474269e2db7c800829`. During client startup, `villager_work_routines.mixins.json:client.VillagerRendererMixin` failed with `InvalidMixinException`: its `@Shadow addLayer(RenderLayer)` was not located on target `VillagerRenderer`. This caused the later `EntityRenderers` and resource-reload failures and resource-pack rollback. The startup failure prevented meaningful C7 Fisherman validation; it does not replace the retained C5 Shepherd PASS or C6's successful Fisherman cycle observations.

## Canary 9 client-start and Fisherman trial

Run visual and behavioral checks at normal **20 TPS**. Accelerated ticks may reach vanilla WORK time only; restore 20 TPS before navigation, casting, rendering, retrieval, or observation.

1. Start Minecraft and reach the main menu / normal client initialization. There must be no VWR mixin or `EntityRenderers` failure.
2. Confirm resource packs remain enabled; no VWR-caused resource-pack rollback may occur.
3. Enter a world with the normal frog-villager resource pack.
4. Confirm ordinary villagers render normally.
5. Give a Fisherman its own genuinely claimed barrel, valid bounded open source water, and a safe adjacent bank. Once the bobber appears, confirm one persistent plain stick at the crossed hands, angled from the hands to the line origin; it must have no duplicate vanilla line/hook or generic crossed-arms item. Confirm the bobber renders, the line begins at the stick tip, and the stick disappears after retrieval or interruption.
6. Complete at least one Fisherman catch/deposit cycle: water/bank selection, navigation, cast, wait, retrieve/swing/splash, one fish-only result, owned-output capture, return, and deposit only to the claimed barrel.
7. While the bobber is live, confirm the Fisherman remains at its vetted bank and keeps looking toward the water until retrieval; it must not resume ordinary wandering. Cause an ordinary world save/pause if practical. There must be no crash, lost float ownership, reset catch timing, or hand-state corruption. Then observe several cycles and an interruption; float, stick, and line must clean up once.
8. Perform one Shepherd regression cycle if practical. Its existing target/gate/blocker/shear/exact-wool/loom-adjacent-barrel behavior must remain intact.

Record **FAIL** for any startup/client crash, mixin application failure, `EntityRenderers` failure, VWR-caused resource-pack rollback, missing/duplicate/stale stick, a stick with a duplicate vanilla line/hook, moving/wandering during a live cast, fishing regression, line-geometry crash, legitimate-hand-state corruption, or Shepherd regression. Record **INCONCLUSIVE** only with the observed limiting condition. Do not mark C9 Fisherman PASS until a user observes the complete cycle.
