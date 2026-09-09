# Testing

Wearable Lanterns Canary 5 (`0.1.0-canary5`) is retained as `wearable-lanterns-0.1.0-canary5.jar`, 13,920 bytes, SHA-256 `0d05d2af86dfb8f41e7cb7c0dde75aed681cfb657fa2cb8de4a42f40344dd0e6`, built from source checkpoint `413c2dad2f5acf7285311c31921493ef5650eed5`. It is the policy-compliant metadata-only successor to Canary 4: the Iris bridge behavior is unchanged, while Fabric API/Trinkets/optional-LDL manifest predicates use stable capability/provider forms. It is `STATIC_PASS`, `NOT_DEPLOYED`, and `RUNTIME_UNTESTED`. The user reports Canary 3's worn lantern works but its direct light looks choppier than an actual held lantern under Iris + Complementary; that is external runtime feedback only, not a Workbench result.

## Required deployment context

Use a later explicitly authorized Test Instance Manager deployment into the dedicated Matcha Flavoured 26.2 Workbench. Do not install the JAR by hand and do not access the protected gameplay profile. Before launch, record the exact loaded Minecraft 26.2, Fabric Loader/API, Trinkets Updated, Iris, Sodium, LDL, and Complementary versions; confirm Iris is exactly `1.11.2+mc26.2`, Complementary Unbound is `r5.8.1`, and Dynamic Handheld Lighting is `HELD_LIGHTING_MODE=2` (Normal). Record the actual selected shader profile and LDL self-light/update settings.

## Primary visual comparison

In one dark repeatable route with the same time, camera, FOV, frame cap, and render distance, compare the same regular lantern in:

| Case | Equipment | Expected direct shader light |
| --- | --- | --- |
| A | Actual offhand | Existing Iris/Complementary handheld baseline |
| B | Exact `legs/lantern`, empty offhand | Tracks A with substantially the same per-frame smoothness |
| C | Exact `legs/lantern`, shield/non-light offhand | Matches B; the real offhand remains a shield |
| D | Exact `legs/lantern`, actual offhand torch or lantern | Deterministic coexistence: equal/brighter real offhand wins; no duplicate or stale contribution |

For A-D, walk, sprint, jump, turn, and stop. The acceptance criterion is that B's direct handheld shader illumination tracks interpolated player motion comparably to A, without Canary 3's conspicuous surface-by-surface stepping. LDL may continue to supply its ordinary chunk-rebuilt component; do not assess this task by trying to blur block light.

## Focused regression matrix

1. Empty `legs/lantern`, empty offhand: no synthetic secondary hand light.
2. Regular lantern in exact `legs/lantern`, empty offhand: expected per-frame shader contribution.
3. Soul lantern in exact `legs/lantern`, empty offhand: confirm its Iris/pack identity and emission are not forced to regular-lantern values.
4. A tag-extended supported light item, if available: confirm the actual stack's Iris-derived identity/emission is retained.
5. Equip and unequip repeatedly, change dimension, relog, and unload/re-enter a world: contribution appears/disappears immediately, with no stale light or retained stack.
6. Disable shaders: no error and ordinary Wearable Lanterns plus LDL behavior remains.
7. Disable LDL while retaining Iris + Complementary: if the pack's normal held-light mechanism is active, local worn-lantern shader light should remain; core behavior remains functional.
8. Remove Iris or use a non-audited Iris release: clean startup with no bridge activation; ordinary Wearable Lanterns behavior remains.
9. Recheck exact slot/amount/tag eligibility, belt independence, sprite, hip model, Inventory Extended transfers, Trinkets persistence/drop/sync, regular/soul/tag-extended support, and absence of world/server light mutation.

Stop and preserve logs/settings/video if startup fails, a mixin error occurs, the actual offhand changes, item loss/duplication occurs, a light source sticks after unequip, local shader light appears for remote players, a second LDL source appears, any world/block light is changed, or any existing Trinkets/Inventory Extended/rendering behavior regresses. Do not mark a runtime PASS until this exact retained release is tested and its observations are recorded.
