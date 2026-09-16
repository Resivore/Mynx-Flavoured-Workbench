# Testing

Candidate `villager-work-routines-0.1.0-canary7.jar` is a user-managed Minecraft runtime trial. Before testing, verify its version, filename, SHA-256, and source checkpoint against `WORKBENCH_STATUS.json`. Use the frog-villager resource pack, do not install the Real Working Villagers reference JAR, and retain relevant `[VillagerWorkRoutines]` logs. A static build/test pass is not runtime evidence.

## Recorded Canary 6 runtime evidence

- **Shepherd — PASS retained from C5.** Repeated full target/gate/blocker/shear/exact-wool/loom-adjacent-barrel cycles passed.
- **Fisherman — FAIL / substantial partial success.** For exact C6 identity `0.1.0-canary6`, `villager-work-routines-0.1.0-canary6.jar`, SHA-256 `366a4d7ed3d9f7c28a4055e2efdf981f07c77f32b7bfd6c72c1c9458c4d25f99`, source `69ae70bd8412ac169a972985c328771dd8bc16d8`, the old `Missing elements in vertex` crash was fixed. A Fisherman claimed barrel `BlockPos{x=294, y=63, z=517}`, selected water/bank, navigated, cast, rendered the bobber and line, retrieved a salmon, retained it in VWR-owned output, then deposited `attempted=1 inserted=1 retained=0` to that claimed barrel. C6 later churned synthetic rod overlays and crashed the integrated server with `state.prop == null` in `WorkCoordinator.showProp`; the rod was not a convincing stable held visual. This is not a failure of the completed gameplay cycle.

## Canary 7 Fisherman trial

Run visual and behavioral checks at normal **20 TPS**. Accelerated ticks may reach vanilla WORK time only; restore 20 TPS before navigation, casting, rendering, retrieval, or observation.

1. **Full cycle:** Give a Fisherman its own genuinely claimed barrel, valid bounded open source water, and a safe adjacent bank. Confirm water/bank selection, bank navigation, one clear fishing-rod-shaped item at the crossed hands, cast, bobber, a line from approximately the rod tip to bobber, wait, retrieve/swing/splash, one fish-only result, owned-output capture, return, and deposit only to the claimed barrel.
2. **Watch the rod:** From close range, observe before/during/after cast. The rod must be unmistakably rod-shaped, move with the villager, appear once only, remain stable through the live float, and not float separately or remain after retrieval.
3. **Save during float:** While the bobber is live, cause an ordinary world save/pause if practical. There must be no crash, duplicate rod, durable synthetic rod in villager equipment, reset catch timing, or lost float ownership. Logs should show one action ID suspended/restored, rather than a new transaction every normal tick.
4. **Several cycles and interruption:** Observe several casts, then let WORK end or interrupt an active float. Each cast should have one rod transaction; float, rod, and line must clean up once on interruption and the legitimate hand state must be preserved. No prop NPE, overlay flicker/churn, memory/render accumulation, arbitrary storage, dropped-item intermediary, or resource deletion is allowed.

## Shepherd regression

Perform one ordinary gated sheep cycle if practical. Existing Shepherd PASS behavior must remain: visible shears, direct-route-first/target-driven gate ownership, Sheep/Cow/Pig/Chicken/Rabbit-only transient blocker, correct entry/exit/closure, real close-range shear, exact wool capture, and loom-adjacent barrel deposit. A shared prop regression is a C7 FAIL.

Record **FAIL** or **INCONCLUSIVE** for any prop NPE, server/client crash, incomplete-line vertex error, missing/stale bobber/line/rod, duplicate rod, unstable rod transaction, invalid water/bank safety, non-fish output, arbitrary storage, lost legitimate hand state, or Shepherd/gate regression. Do not mark C7 Fisherman PASS until a user actually observes a complete cycle.
