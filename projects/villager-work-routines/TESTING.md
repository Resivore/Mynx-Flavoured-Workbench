# Testing

Candidate `villager-work-routines-0.1.0-canary6.jar` is a user-managed Minecraft runtime trial. Verify its version, filename, SHA-256, and source checkpoint against `WORKBENCH_STATUS.json` before testing. Use the frog-villager resource pack, do not install the Real Working Villagers reference JAR, and retain the relevant `[VillagerWorkRoutines]` log lines. A clean build/static pass is not runtime evidence.

## Recorded Canary 5 runtime evidence

- **Shepherd — PASS.** Repeated completed cycles observed target sheep discovery outside the enclosure; correct target-driven gate selection; entry/exit traversal; the Sheep/Cow/Pig/Chicken/Rabbit VWR gate blocker; gate close/release; same-target movement recovery; temporary shears; actual shear; exact wool capture; and deposit to the loom-face-adjacent barrel. The old occupied-hand and lost-shears failures did not recur as fatal blockers.
- **Fisherman — FAIL (client renderer crash).** Valid water/bank selection, navigation, temporary rod, and a successful cast occurred. The VWR float (`floatEntity=1398`) spawned, then the client crashed in `FishingFloatRenderer` custom fishing-line geometry with `IllegalStateException: Missing elements in vertex`. Catch, retrieval, and barrel deposit were never reached and therefore have no verdict.

## Canary 6 Fisherman renderer and cycle trial

Run behavioral and visual checks at normal **20 TPS**. Accelerated ticks may reach vanilla WORK time, but restore 20 TPS before evaluating navigation, rod presentation, casting, rendering, or retrieval.

1. Give a Fisherman its own genuinely claimed barrel and ensure natural/open source water is within the bounded search radius with a valid adjacent bank. Enter vanilla WORK time.
2. Confirm it selects the open water and a legitimate bank, then reaches that bank. A small ordinary AI nudge during the rod telegraph may make it reposition to the same selected bank and restart that short telegraph; it must not choose arbitrary water/banks or cancel the entire task for that nudge.
3. Confirm the temporary rod appears, the Fisherman faces water, a VWR float spawns, and a restrained line renders from the villager approximation to the float. There must be no `Missing elements in vertex`, `Render Frame` crash, repeated client errors, stale float, or unrelated renderer/model/resource-pack change.
4. Leave the float visible through the bounded wait. At retrieval, confirm swing, splash, float removal, fish-only result, owned-output retention, return to its own claimed barrel, and insertion only into that barrel. No nearby/random chest fallback, dropped-item storage intermediary, junk, treasure, XP, or unauthorized fish is allowed.
5. If practical, observe several cycles: removed floats disappear cleanly, later floats/lines continue rendering, and there is no accumulation/leak. Also exercise full owned output/claimed-barrel capacity and a WORK-end interruption: no item deletion/fallback, float cleanup, rod restoration, stale client geometry, or crash.

## Shepherd regression observation

Do not redesign or re-test unrelated behavior as a prerequisite. During ordinary C6 use, confirm a Shepherd still retains an eligible sheep, uses only its target-driven VWR gate route, keeps the transient blocker limited to Sheep/Cow/Pig/Chicken/Rabbit while VWR owns an open gate, recovers locally from ordinary sheep movement, performs a real close-range shear with reversible shears, captures only that wool, closes/releases on exit, and deposits only into its loom-face-adjacent barrel. Any regression in these already-passing C5 behaviors is a C6 FAIL.

Record **FAIL** or **INCONCLUSIVE** for any renderer crash, incomplete-vertex error, missing/stale float or line, invalid water/bank safety, arbitrary storage/loot, hand-state loss, Shepherd/gate-blocker regression, or observations that cannot be bound to this exact C6 candidate. Do not mark Fisherman PASS until a real cast/retrieve/deposit cycle is observed.
