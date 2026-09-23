# Testing

Candidate `villager-work-routines-0.1.0-canary31.jar` / SHA-256 `3e3081619c3b1e90d8b348e8c073ad38f9fcc489c7b88a9290e386dcdf18a996` / source `ad61618988ff0c3f3711bf90746f713cbbb14f6e` is a user-managed Minecraft Java 26.2/Fabric trial. Verify its finalized identity against `WORKBENCH_STATUS.json`, together with exact retained Ribbits Canary 27 `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar` / SHA-256 `088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd`. Use the normal Frog Villager resource pack. Build and static validation are not runtime evidence. Do not install the Real Working Villagers reference JAR or access a protected Minecraft profile.

## Canary 31 Fisherman parent-space rod translation and routine regression trial

### Historical evidence boundary

The supplied annotated C30 screenshot is **C30 Fisherman rod-placement runtime evidence only**: the rod is close but its grip remains somewhat outside the folded arms, and the orange-arrow remaining movement is approximately two model pixels straight upward in villager/entity parent space. It establishes no Fisherman productivity/deposit, Shepherd, or unrelated runtime result. C30's exact retained post-C24 local correction is `(X 0px, Y +1px, Z +7px)`. C31 applies the parent-space desired vector `(0,-2,0)px`; the actual folded-arm-to-parent linear basis is inverted at the existing post-grip seam. In the normal inspected 43-degree basis this derives `(ΔX 0, ΔY -1.462707, ΔZ +1.363997)px`, yielding C31 `(X 0, Y -0.462707, Z +8.363997)px` relative to C24. C31 is **ACTIVE / STATIC_PASS / RUNTIME_UNTESTED** until this owner-managed trial occurs.

Run at normal **20 TPS**. Accelerated ticks may reach vanilla WORK time only; restore 20 TPS before casting, timing, movement, transfer, or observation. Retain ordinary INFO logging.

### 1. Frog Villager rod and physical-tip line

1. Start exact VWR C31 and Ribbits C27 with Frog Villager resources enabled. There must be no VWR mixin, GeoLib, EMF, renderer, model/resource-resolution, or authored-path failure.
2. Give one ordinary VWR Fisherman its exact claimed barrel, valid bounded open-source water, and a safe adjacent bank. At normal 20 TPS, wait for one live cast and capture a screenshot showing villager, folded hands, rod, line, and float.
3. Verify the translucent violet C30 ghost is the exact `(0,+1,+7)px` retained pose and the solid C31 rod is approximately two model pixels straight upward in villager/entity vertical space from it, rather than traveling along one local diagonal axis. Its grip should sit farther inside the folded arms. Orientation must remain unchanged. It must still use the semantic EMF `arms -> arms_rotation` attachment, authored `[0,-7,-6]px` reference / `invertAxis="xy"` mapping, and neutralized Ribbits C27 baked rotation: no added rotation or scale is valid.
4. Verify one solid C31 rod, its translucent C30 comparison ghost, and one real line are visible. The ghost has no fishing line. There must be no colored diagnostic axes, crosses, stars, squares, diamonds, grip markers, transform rays, or comparison-marker clutter; `fishing_rod_2` / `fishing_rod_3` decoration, vanilla fishing equipment, a second line, or a second bobber are invalid.
5. Verify the real VWR 16-segment line begins at the visible physical `-9.5px` outer shaft tip. The physical tip remains its sole authority; do not expect diagnostic markers or per-cast transform diagnostics in the normal release.

### 2. Fisherman dwell and productive versus empty retrieves

1. Observe completed casts without forcing the villager to stop fishing. Each float must dwell exactly within **180-419 ticks** (about 9.0-20.95 seconds at 20 TPS), followed by the unchanged **100-tick** post-retrieve cooldown.
2. Every completed retrieve, including an intentional empty one, must visibly swing/reel, splash, play retrieval audio, remove the float, remove the rod/line, and enter the ordinary cooldown.
3. Use the server log to distinguish `productionGate=loot_attempt` from `productionGate=intentional_empty`. Only gate roll 0 of the four bounded outcomes may call the existing `FISHING_FISH` loot path. An intentional empty retrieve must create no owned output and therefore start no barrel return.
4. A successful gate outcome must retain the existing fish-only allowlist and create at most the existing single fish result. Because the gate is random, record only outcomes actually observed; do not mark failure merely because a short sample does not contain both outcomes.

### 3. Fisherman claimed-barrel return

1. On an actually successful catch, watch the Fisherman return only toward its exact claimed job-site barrel. No adjacent or nearby-container fallback is valid.
2. Inspect the claimed barrel before physical arrival: no owned fish may enter while the live minimum villager-body-to-barrel-block-AABB boundary gap is greater than **0.5 blocks**.
3. Verify normal navigation reaches a reachable collision-clear standing position beside the barrel. The villager must remain within the boundary-gap rule into a later tick and visibly face the barrel before transfer.
4. Only a real positive insertion may trigger the existing swing/barrel-open particles/sound and delayed fish feedback. If the barrel is missing, full, unloaded, or cannot be approached, the fish must remain in VWR-owned storage for retry; it must not drop, vacuum, teleport, or move to another barrel.

### 4. Shepherd actual-receiver return

1. Let a Shepherd obtain wool through the existing eligible-sheep, telegraph, exact-capture, and any applicable gate-route flow. Those behaviors must remain unchanged.
2. Provide multiple valid face-adjacent barrels at the claimed loom, including an earlier-direction barrel with an empty slot and a later-direction barrel with a component-identical partial wool stack. Verify the matching stack remains the first receiver; otherwise preserve deterministic `DOWN, UP, NORTH, SOUTH, WEST, EAST` tie order and matching-before-empty precedence.
3. Before each transfer, the Shepherd must navigate to the actual next receiving barrel, reach a collision-clear standing position with live body-to-that-barrel AABB gap at most **0.5**, remain through a later tick, and visibly face it. Distance from the loom is not transfer authority.
4. If retained wool needs more than one receiver, verify transfer stops while the Shepherd retargets and physically approaches each next barrel. An unreachable selected higher-priority receiver must retain the wool rather than being bypassed. Feedback must occur only after that barrel accepts wool.

Record **FAIL** for any startup/resource/authored-path error; C30 ghost absent or not translucent; solid rod absent, misplaced, rotated, scaled, duplicated, or not approximately two parent-space pixels vertically above the ghost; any ghost fishing line; diagnostic axes/marker clutter; a solid line detached from the physical shaft tip; dwell outside 180-419 ticks; cooldown other than 100 ticks; an intentional-empty retrieve that skips normal visuals/cleanup or creates output; a nonzero gate outcome that reaches the fish loot roll; fish or wool inserted before the exact physical arrival/facing gate; wrong Shepherd receiver priority; loss of retained output on an unreachable destination; or an actually observed unrelated Fisherman/Shepherd regression. Return the C31 screenshot and only observations actually made.
