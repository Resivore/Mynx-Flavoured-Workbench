# Testing

Candidate `villager-work-routines-0.1.0-canary20.jar` / SHA-256 `929e4f5fee37a5a3500ffde15e8a3f023ff6219a33b85d1446fd8f5c93585535` is a user-managed Minecraft Java 26.2/Fabric runtime trial. Verify its filename, hash, version, and source checkpoint against `WORKBENCH_STATUS.json`, together with the exact retained paired Ribbits Canary 27 artifact `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar` / SHA-256 `088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd`. Use the normal Frog Villager resource pack. Build and static validation are not runtime evidence. Do not install the Real Working Villagers reference JAR or access a protected Minecraft profile.

## Canary 20 Frog Villager Ribbits rod and regression trial

### Historical evidence boundary

Against exact VWR C18 and Ribbits C28, the user reported that the rod was visible but severely displaced from the Frog Villager's folded hands and VWR's line could detach toward a high world/view point. Those scoped observations establish no C20 runtime result.

Run visual and behavioral checks at normal **20 TPS**. Accelerated ticks may reach vanilla WORK time only; restore 20 TPS before navigation, casting, rendering, retrieval, or observation.

1. Start Minecraft with exact VWR C20 and Ribbits C27. Reach normal client initialization with the normal Frog Villager resources enabled. There must be no VWR mixin, GeoLib, or entity-renderer failure and no VWR-caused resource-pack rollback.
2. Enter a world with ordinary villagers, then give one Fisherman its own claimed barrel, valid bounded open source water, and a safe adjacent bank. Confirm ordinary villagers and Ribbits' native Fisherman presentation remain unchanged when no VWR float is live.
3. Once the VWR Fisherman casts, confirm exactly one existing Ribbits `fishing_rod` shaft/reel/detail. Its handle must sit naturally at the visible folded hands, and its angle must match the supplied Blockbench Frog Villager reference: the rod follows the folded-arm hierarchy with the user-authored local `[0, -7, -6]` pixel grip, not a separate item-hand rotation. There must be no `fishing_rod_2` hanging line, `fishing_rod_3` terminal/bobber, vanilla hook/line, generic crossed-arms item, C17 stick, or duplicate rod.
4. Confirm VWR's existing 16-segment line begins exactly at the visible physical outer shaft tip without a gap, reaches only the existing VWR float, and never aims at an arbitrary high world/view point. Observe turning, idle hold, cast, retrieval, and one interruption; the rod, line, and float must clean up once.
5. Complete at least one Fisherman catch/deposit cycle. Water/bank selection, navigation, cast/wait/retrieve/swing/splash, one fish-only result, owned-output capture, return, claimed-barrel-only deposit, immediate barrel-open/glow-squid-ink feedback, and the existing delayed successful-only cod-flop must remain unchanged. No delayed cue may occur when no fish was inserted or the transfer is cancelled.
6. Give a Shepherd a genuinely claimed loom, a face-adjacent output barrel with capacity, and adult regrown sheep behind a closed fence gate. Observe its existing path, shears, gate/blocker safety, actual shear, capture, exit, and barrel feedback cycle. No Shepherd pose, tool, sound, storage, gate, or livestock-containment regression is permitted.

Record **FAIL** for any startup/client failure; resource-pack rollback; missing, duplicated, misgripped, or arbitrarily rotated rod; C17 stick; visible `fishing_rod_2`/`fishing_rod_3`; any duplicate line/hook/bobber; a line gap or high world/view endpoint; fishing lifecycle/output regression; or any Shepherd regression. Record only observations actually made; C20 is not a runtime pass until this user-managed trial is completed.
