# Testing

Candidate `villager-work-routines-0.1.0-canary24.jar` / SHA-256 `aef1fabf9d5ca9fd596d842567b83420447f756ca08fb2cc50d968c7c9139616` / source `c747126e5685bffb8c1850d01d5794d988e7d2ef` is a user-managed Minecraft Java 26.2/Fabric trial. Verify that identity against `WORKBENCH_STATUS.json`, together with exact retained Ribbits Canary 27 `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar` / SHA-256 `088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd`. Use the normal Frog Villager resource pack. Build and static validation are not runtime evidence. Do not install the Real Working Villagers reference JAR or access a protected Minecraft profile.

## Canary 24 rod, fishing cadence, and physical barrel-return trial

### Historical evidence boundary

The supplied exact result is **C23 runtime FAIL evidence for rod attachment only**. Exact C27 resources resolved; the live `fishing_rod` baked 50-degree rotation was neutralized to `effective_rotation_rad=(0,0,0)`; and the authored grip and live rod pivot coincided. Nevertheless, the rod remained displaced high/across the Frog Villager after C23 selected `arms/EMF_arms/EMF_arms_rotation` with its shallowest-direct-geometry rule. C23's sole 16-segment line was already correct: `C23_line_start_authority=live_fishing_rod_physical_outer_tip` and line start minus visible tip was `(0,0,0)`, distance `0.000000`. Do not infer a C23 productivity, fish-deposit, Shepherd, or other runtime result. C24 is **ACTIVE / STATIC_PASS / RUNTIME_UNTESTED**.

Run at normal **20 TPS**. Accelerated ticks may reach vanilla WORK time only; restore 20 TPS before casting, timing, movement, transfer, or observation. Retain ordinary INFO logging.

### 1. Frog Villager rod and physical-tip line

1. Start with exact VWR C24 and Ribbits C27 and reach normal client initialization with the Frog Villager resources enabled. There must be no VWR mixin, GeoLib, EMF, renderer, model/resource-resolution, or authored-path failure.
2. Give one ordinary VWR Fisherman its exact claimed barrel, valid bounded open-source water, and a safe adjacent bank. Wait for one live cast and capture a screenshot showing villager, folded hands, rod, line, float, and diagnostic markers.
3. Verify the rod grip sits naturally at the visible folded hands and matches the supplied JEM/Blockbench pose. There must be no `fishing_rod_2` / `fishing_rod_3` decoration, vanilla fishing equipment, second line, or second bobber.
4. Retain the concise `[VWR C24 ROD DIAGNOSTIC: ...]` blocks. They must report EMF's semantic mapping from a top-level authored `arms` attached through `partToBeAttached=arms` to its direct authored `arms_rotation` child. Runtime child-map names may appear only as observations. The grip checkpoint must preserve authored `[0,-7,-6]px` and show the proven EMF `invertAxis="xy"` live mapping `[0,+7,-6]px`; no extra rotation or scale is permitted.
5. Verify the green-star physical marker remains at the visible `-9.5px` outer shaft tip and VWR's sole 16-segment line starts at exactly that live captured point. `C24_line_start_minus_visible_tip_camera_relative` must be zero or floating-point-equivalent zero.

### 2. Fisherman dwell and productive versus empty retrieves

1. Observe several completed casts without forcing the villager to stop fishing. Each float must dwell exactly within **180-419 ticks** (about 9.0-20.95 seconds at 20 TPS), followed by the unchanged **100-tick** post-retrieve cooldown.
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

Record **FAIL** for any startup/resource/authored-path error; rod absent or displaced from the visible folded hands; pose inconsistent with the immutable reference; duplicate rod/line/bobber geometry; line detached from the physical shaft tip; dwell outside 180-419 ticks; cooldown other than 100 ticks; an intentional-empty retrieve that skips normal visuals/cleanup or creates output; a nonzero gate outcome that reaches the fish loot roll; fish or wool inserted before the exact physical arrival/facing gate; wrong Shepherd receiver priority; loss of retained output on an unreachable destination; or an actually observed unrelated Fisherman/Shepherd regression. Return exact screenshots, relevant timing/output observations, and concise diagnostic/log blocks. C24 remains **ACTIVE / STATIC_PASS / RUNTIME_UNTESTED** until this owner-managed trial is performed.
