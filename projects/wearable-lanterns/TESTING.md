# Testing

Wearable Lanterns Canary 3 (`0.1.0-canary3`) is retained as `wearable-lanterns-0.1.0-canary3.jar`, 8,232 bytes, SHA-256 `0209e1e01acc412b887a0079089e08fcea70e44343381f5c19e53d39477be65c`. It has not been deployed or formally runtime-tested. External Canary 2 spot feedback reported continued good core wearable behavior, an unwanted GUI recolor, and visibly choppier worn-lantern light than a held torch; that is feedback, not a Workbench result.

Static analysis predicts two simultaneous paths when shaders are enabled: LambDynamicLights (LDL) supplies tick/update-driven player light through chunk rebuilds for held and Trinkets stacks, while Iris and Complementary can add a separate per-frame handheld shader term for main hand and off hand only. A worn `legs/lantern` stack reaches LDL but not the hand-specific shader term. The controlled matrix below must confirm that prediction before any lighting follow-up.

## Preconditions and evidence capture

1. In a later explicitly authorized Test Instance Manager task, deploy the exact retained Canary 3 JAR and record its Test Slot plus accepted-baseline identity. Do not install it by hand.
2. Before launch, verify and record the actually loaded Minecraft, Fabric Loader/API, Trinkets Updated, LDL, Iris, Sodium, and Complementary versions from the instance and logs. The audited targets are Minecraft 26.2, Trinkets Updated `4.1.0-beta.3+26.2`, LDL `4.12.2+26.2`, Iris `1.11.2+mc26.2`, and Complementary Unbound `r5.8.1`. Authoritative repository history records both Sodium `0.9.1+mc26.2` and `0.9.2-alpha.4+mc26.2` enabled; resolve and record the one version actually loaded before interpreting renderer behavior.
3. Save the original configuration, then record the exact Iris shader-pack selection/profile and Complementary Dynamic Handheld Lighting value (`HELD_LIGHTING_MODE`). Use explicit mode `2` (`Normal`) for shader-on matrix rows A and C unless testing another value is deliberately recorded.
4. For LDL-on rows A and B, enable local-player/self light and hold every other LDL setting constant. Record dynamic-light mode, update cadence, adaptive-ticking state and thresholds, background behavior, entity/distance limits, and any chunk-rebuild scheduling option. Do not compare rows with unrecorded setting changes.
5. Choose one dark location and a repeatable route with nearby surfaces that reveal motion. Keep world time, player, camera perspective and motion, field of view, frame-rate cap, render distance, route, and held/equipped item state consistent. Capture frame rate and enough video or frame-timed notes to distinguish smooth interpolation from tick/rebuild stepping.

## A-D renderer diagnostic matrix

Run rows A through D in order. Between item cases, wait until prior dynamic light is visibly gone and note any stale source. In each A-C row, compare the same three cases: held torch (vanilla luminance 14), held regular lantern (15), and that same regular lantern equipped in exact `legs/lantern` (15). Walk, sprint, jump, and turn through the fixed route; record light presence, apparent intensity/radius, movement smoothness, lag or stepping, and LDL source/debug evidence where available.

| Row | Complementary | LDL | Required comparison |
| --- | --- | --- | --- |
| A | ON, `HELD_LIGHTING_MODE=2` | ON, self light ON | Held torch, held regular lantern, worn regular lantern |
| B | OFF | ON, same LDL settings as A | Held torch, held regular lantern, worn regular lantern |
| C | ON, same shader settings as A | OFF | Held torch, held regular lantern, worn regular lantern |
| D | OFF | OFF | Unlit control under the same route/camera conditions; confirm no item-driven moving light |

The primary diagnostic is whether the held-versus-worn smoothness difference disappears in B, and whether held lighting remains present and smooth in C while worn lighting disappears. That combination confirms that the extra smooth effect is Complementary/Iris handheld lighting rather than a faster LDL held-item branch. If necessary, repeat only the shader-on comparison with Dynamic Handheld Lighting disabled (`HELD_LIGHTING_MODE=0`); disappearance of the extra held smoothness isolates that exact shader term.

If B still shows a held-versus-worn difference under genuinely identical LDL inputs, or C still lights the worn item, preserve the exact instance state, logs, video, loaded-mod list, and settings. Treat the audit as runtime-inconclusive and investigate the actual provider/scheduler before changing code. Do not add a second LDL source, fake a hand, or patch Iris/shader internals based on appearance alone.

## Canary 3 functional and regression checks

1. Open the survival inventory and confirm the empty `legs/lantern` slot uses the exact supplied 16x16 glyph: 226 transparent pixels and 30 opaque `#1B1511` pixels. It must not show Canary 2's four-color Matcha recolor.
2. With the ordinary belt slot assigned, confirm native Trinkets layout reveals lantern-left, leggings-center, and belt-right. Record the belt-absent native result separately; Canary 3 adds no custom GUI/layout mixin.
3. Directly equip and unequip one regular lantern and one soul lantern. Confirm amount one, exact eligibility, invalid-item rejection, independent belt contents, and no loss or duplication.
4. Repeat representative direct and shift-click transfers from Inventory Extended's upper rows, row six, hotbar, and creative inventory. Confirm the accepted dynamic menu-boundary behavior, native wrappers, storage-before-hotbar return routing, stack remainders, and absence of ghosts or fixed-index failures.
5. Relog, restart, and change dimensions; test death with keepInventory both disabled and enabled. Confirm normal Trinkets persistence, synchronization, drop/keep behavior, and belt independence for the local player and a representative observed remote player where available.
6. In third person, inventory preview, and another-player view, confirm the actual equipped ItemStack model renders at the intended hip attachment and follows resource-pack model/texture overrides. Cover representative standing, walking, sprinting, crouching, swimming, elytra, armor, and Fresh Animations/entity-model cases.
7. With LDL enabled, confirm a regular lantern in `legs/lantern` resolves through the item manager to luminance 15. Confirm a soul lantern and at least one supported data-extended lantern retain their own item/block-derived luminance rather than being forced to 15.
8. Equip and unequip repeatedly while observing LDL source/debug information where available. Light must appear and disappear promptly, source count must not double, and no duplicate, stale, or stuck source may remain after unequip, death, slot removal, relog, or dimension change.
9. Repeat representative motion with another nearby player wearing the lantern to separate local camera/shader perception from remote entity and chunk-rebuild behavior.
10. Confirm lighting remains client-rendered only: no lantern block, light block, changed block state, server luminance, persistent environmental light, or world mutation may be created.
11. Remove LDL and repeat representative equip, persistence, synchronization, and worn-render checks. Wearable Lanterns must load and function normally without errors or missing-class linkage.

Stop and preserve the exact state and logs on any crash, disconnect, item loss or duplication, invalid-item acceptance, belt/lantern collision, fixed-index failure, creative ghost slot, persistence or synchronization failure, duplicate or stuck light source, world/block-light mutation, or material rendering regression. A confirmed hand-shader differential is not a Wearable Lanterns failure; the supported follow-up target is an upstream Iris API for alternate equipped light stacks plus explicit shader-pack support. Do not implement a private mixin, fake-hand workaround, or global LDL behavior change in this project.
