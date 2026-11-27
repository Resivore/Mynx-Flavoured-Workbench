# Testing

Candidate **VWR C35** (`VWR C35.jar`, internal version `0.1.0-canary35`) is identified by the finalized SHA-256, build time and source checkpoint in `WORKBENCH_STATUS.json`. Use the normal Frog Villager resource pack. Build, contract fixtures, dependency predicates and isolated JVM classloading are automated/static evidence, not Minecraft runtime evidence. Do not access or launch any Minecraft testing profile.

## Ribbits integration contract v1

Ribbits is a required Fabric provider (`ribbits: "*"`), not a specific Canary. Its version is diagnostic information only. VWR has no Ribbits compile dependency, entity/class reference, reflection bridge, mixin target, trade hook, or native AI/animation hook. VWR never packages Ribbits assets. The client consumes installed resources through Minecraft's resource manager and GeckoLib >=5.5.1; common server entrypoints and common mixins do not reach that client code.

| Surface | Actual requirement and handling |
| --- | --- |
| Model | `ribbits:fisherman_ribbit`, resolved from `assets/ribbits/geckolib/models/fisherman_ribbit.geo.json`. A canonical JSON structural digest checks geometry format/description (including 128x128 UV dimensions), bone identities/order/parents, pivots, rotations, visible cubes and UVs. Whitespace, object-key order and numeric spelling are immaterial. Unknown fields/bones fail closed. |
| Bones | `main -> body -> accessories -> fishing_rod`; `left_arm`, `right_arm`, `left_leg`, `right_leg`, `fishing_rod_2`, `fishing_rod_3` retain their known hierarchy and transforms. VWR hides main/body/limb cubes and the decorative rod descendants exactly as before. Their suppressed cube payloads are excluded from the structural digest. All visible rod cubes remain authored, unchanged and provider-owned. |
| Texture | `assets/ribbits/textures/entity/ribbit.png`; conservative exact resource-byte digest protects the rod's UV artwork. Any texture edit or PNG re-encoding currently requires VWR contract review, including changes elsewhere in that shared atlas. No texture replacement or approximate fallback is used. |
| Animation | `RodVisual` registers no controllers; VWR does not play Ribbits animations. `getAnimationResource` retains `ribbits:ribbit` as the GeoModel identifier but no named animation is requested. Native Ribbits animation/controller/AI changes do not enter this static prop. |
| Attachment | Unchanged VWR/EMF authored `arms -> arms_rotation` path, `[0,-7,-6]px`, invertAxis=xy, C30/C31 correction, C33 `(0,0,-4)px` rod-local translation, live baked rotation neutralization, root/grip rebasing and physical `-9.5px` shaft tip. Existing GeckoLib per-bone render capture supplies the sole 16-segment line endpoint. These are VWR/EMF/GeckoLib contracts, not Ribbits version gates. |
| Mixins/entities | VWR common mixins target vanilla Villager, Sheep and WalkNodeEvaluator. Client mixins target vanilla model/render classes. No Ribbits class or mixin target exists. Native Ribbit work routines and vanilla routine behavior are unchanged. |

Automatic compatibility covers Ribbits version changes, trades, recipes, gameplay changes, unrelated entities/assets, native animations, model JSON formatting, and changes to suppressed body/decorative cubes, provided the guarded rendering surface is unchanged. It does **not** promise arbitrary future Ribbits, Minecraft, GeckoLib, EMF or Frog Villager compatibility. Geometry paths, visible rod geometry/UVs, transforms, hierarchy, bone names, model schema, texture content and renderer API changes require review.

A failed contract returns before submitting the VWR rod; its physical-tip line is consequently omitted. The VWR float, fishing simulation, claimed-barrel output, Shepherd routine and native Ribbits behavior remain active. The client warning identifies installed version, resource paths, source packs and the failed contract; restore compatible assets/remove the overriding pack and reload, or maintain VWR for intentional changes. A successful client resource reload clears the cached decision and warning; checks then rerun on the next cast. No guessed prop or line substitutes for a failed contract. This guard isolates VWR's consumption; it cannot prevent Ribbits or GeckoLib itself rejecting globally malformed resources during their own reload.

A new Ribbits code API was evaluated and is unnecessary: the existing namespaced asset seam is sufficient, no Ribbits implementation class is consumed, and C27/C28 have identical consumed assets. Adding a provider API would require a Ribbits release without improving this static-asset boundary. Ribbits source and artifact remain untouched.

### Automated verification

Run `gradle -p projects/villager-work-routines test build --offline` with Java 25 and the existing dependency cache. The project does not need a Ribbits JAR to compile. Its artifact fixtures intentionally require retained C27, canonical C28, and whatever artifact the current Ribbits manifest names; each whole JAR is verified against its authoritative SHA-256 before assets are read. Never substitute retired files with similar Canary labels.

- C27: `ribbits-private-reconstruction-4.1.6+26.2-mynx-canary27.jar`, SHA-256 `088c4b7e88c432d6275395e29273597cf42575f59350fe8b4f48c46e6df7f9fd`, source checkpoint `9fe0bf18b3799e749fdf736a3b330d5bee8e23e2`.
- Canonical C28/current at this task: `ribbits-private-reconstruction-4.1.6+26.2-matcha125-menu-canary28.jar`, SHA-256 `9e462df87c704542fa0f85d97d16857823c59b4192f2b03a4c333f3af3b91236`, source checkpoint `1ce48a4c2b288c0a18d71e625531e56b618b7ae7`.
- The actual model, texture and native animation members match byte-for-byte across those JARs. Source inspection found C28 changes in trade catalogs, Sorcerer persistence/rank migration and a recipe; model/renderer source is unchanged. The historical C28/C29 VWR bridge experiments are retired and are not canonical C28.
- Executable fixtures check those assets, arbitrary version-only changes using Fabric Loader's real version predicate, formatting/hidden-cube changes, altered bone names/parents/pivots/rotations/visible cubes/UVs, extra bones, texture dimensions, texture corruption, malformed/missing assets and recovery.
- The server audit walks compiled reference closure from the actual common metadata entrypoint and mixins, rejects client/GeckoLib/Ribbits class references, and loads that VWR closure without initialization in an isolated JVM loader denying those packages. It does not launch a server, apply mixins in Minecraft, or claim runtime validation.
- Build checks reject packaged Ribbits payload. Existing authored pose, physical-tip line, equipment, shearing and routine regression fixtures remain enabled.

### Owner-directed compatibility trial (pending; no profile access by Codex)

When the owner elects to test, compare C35 with both verified Ribbits providers in an owner-controlled environment. Confirm dedicated-server startup independently from client visuals, then follow the visual/routine checks below. In a disposable client resource overlay, change a guarded rod bone or texture: expect one actionable warning, no VWR rod/line, and continuing float/routines. Restore resources and reload: the original authored rod/line should return. Record exact artifacts, resource packs and only observations made. The malformed-resource fixture is automated; do not deliberately break global GeckoLib loading to infer VWR behavior.

## Canary 35 Fisherman solid-pose and routine regression trial

### Historical evidence boundary

The supplied C31 screenshot is **C31 Fisherman rod-placement runtime evidence only**: the grip is an acceptable baseline, but the desired remaining movement is parallel to the physical shaft from the grip toward its outer tip. It establishes no Fisherman productivity/deposit, Shepherd, or unrelated runtime result. C31's exact retained post-C24 correction is C30 `(X 0px, Y +1px, Z +7px)` plus its live folded-arm-basis-derived parent-space `(0,-2,0)px` delta; in the normal inspected 43-degree basis that extra local delta is `(ΔX 0, ΔY -1.462707, ΔZ +1.363997)px`, yielding C31 `(X 0, Y -0.462707, Z +8.363997)px` relative to C24. C35 retains C34/C33's solid two-step shaft pose: `(0,0,-4px)` rod-local or `(0,0,-0.25)` model/block units relative to C31. It removes the comparison ghost and all remaining visual diagnostic tools; the single solid rod and its real physical-tip line remain. C35 is **ACTIVE / STATIC_PASS / RUNTIME_UNTESTED** until this owner-managed trial occurs.

Run at normal **20 TPS**. Accelerated ticks may reach vanilla WORK time only; restore 20 TPS before casting, timing, movement, transfer, or observation. Retain ordinary INFO logging.

### 1. Frog Villager rod and physical-tip line

1. Start exact VWR C35 and one of the verified compatible Ribbits providers with Frog Villager resources enabled. There must be no VWR mixin, GeoLib, EMF, renderer, model/resource-resolution, or authored-path failure.
2. Give one ordinary VWR Fisherman its exact claimed barrel, valid bounded open-source water, and a safe adjacent bank. At normal 20 TPS, wait for one live cast and capture a screenshot showing villager, folded hands, rod, line, and float.
3. Verify the one solid C35 rod retains C33's C31-relative `(0,0,-4px)` rod-local / `(0,0,-0.25)` model-block-unit shaft pose. Orientation and apparent rod length must remain unchanged: no extension, deformation, rotation, or scale is valid. It must still use the semantic EMF `arms -> arms_rotation` attachment, authored `[0,-7,-6]px` reference / `invertAxis="xy"` mapping, and neutralized Ribbits C27 baked rotation.
4. Verify exactly one solid C35 rod and one real line are visible. No translucent comparison ghost, colored diagnostic axes, crosses, stars, squares, diamonds, grip markers, transform rays, or comparison-marker clutter may be visible; `fishing_rod_2` / `fishing_rod_3` decoration, vanilla fishing equipment, a second line, or a second bobber are invalid.
5. Verify the real VWR 16-segment line begins at the translated visible physical `-9.5px` outer shaft tip of the solid C35 rod. The translated physical tip remains its sole authority; do not expect diagnostic markers or per-cast transform diagnostics in the normal release.

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

Record **FAIL** for any startup/resource/authored-path error; a ghost or visual diagnostic tool appears; solid rod absent, misplaced, rotated, scaled, extended, deformed, duplicated, or no longer at its C33 `(0,0,-4px)` shaft pose; a solid line detached from its translated physical shaft tip; dwell outside 180-419 ticks; cooldown other than 100 ticks; an intentional-empty retrieve that skips normal visuals/cleanup or creates output; a nonzero gate outcome that reaches the fish loot roll; fish or wool inserted before the exact physical arrival/facing gate; wrong Shepherd receiver priority; loss of retained output on an unreachable destination; or an actually observed unrelated Fisherman/Shepherd regression. Return the C31 screenshot and only observations actually made.
