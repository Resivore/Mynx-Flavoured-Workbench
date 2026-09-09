# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Exact current C12 is the single self-contained artifact
`map-marker-extension-0.4.0-canary12.jar` (118,016 bytes, SHA-256
`F5C539B7535149FB936E241F72722A87E0474AF855F2A98E6E3CF88C3290243E`),
from implementation checkpoint `87591808b60f7a7fc4ce8dd8b6a6ab8e3e8d12b1`.
It has no C12 companion `map-marker-extension-icons-*.zip` by design. Two
independent Java 25/Gradle 9.5.1 offline clean `test build` runs produced the
same JAR bytes. This is build/static evidence, not Minecraft runtime evidence.

The immutable C12 inventory-art input is
`originals/assets/MME-C11-ribbits-style-inventory-routing.zip`, 69,803 bytes,
SHA-256 `5864BF56D06B481E6B037E72C6E6826A54AE2B100CDA6B72615F8AEC72EC9C54`.
The user launched C11 with a manually corrected version of that external-pack
layout and observed the intended filled-map inventory sprites render correctly.
That is scoped positive user runtime evidence for the corrected external routing
only: it does not establish that the exact retained C11 JAR+ZIP pair passed, and
does not establish a C12 launch, POI scale, Xaero, Compass Ribbon, Ribbits,
persistence, or any other unobserved row.

C11 remains preserved historical provenance: its JAR is 57,903 bytes,
SHA-256 `92DF3CCA27F3D4F6CFCB0ED52BA26874DC5F86EB756601B3A75771CFD7E9CBAF`,
and its icon pack is 27,297 bytes, SHA-256
`A277BAF8AA3A12740EAD9CA0D721CDE98D013FBD45FE38F647921E1964598866`.
Its 17 custom POIs are byte-identical to C12's bundled POIs. Exact failed C10,
C9, C8, and accepted Treasure X C4 remain as recorded in `CODEX_LOG.md` and
`WORKBENCH_STATUS.json`; do not co-load C4 with C7 or later.

## Primary C12 runtime matrix

Run only under separately authorized Test Instance Manager deployment ownership,
in a disposable or restorable world, using the dedicated Minecraft 26.2
Workbench. Never access the protected Matcha Flavoured 26.1.2 gameplay profile.

**No MME resource pack installed or enabled.** Install only the exact C12 JAR;
there must be no `map-marker-extension-icons-*.zip` in the active resource-pack
set.

1. Launch, log in, reload resources, and shut down without MME missing-model,
   missing-texture, atlas, item-model, registration, or compatibility errors.
2. Check representative—and preferably all 18—MME exploration maps. Their
   inventory art must come from the JAR, remain crisp at the user-approved scale,
   and remain distinct.
3. Check all 17 custom opened-map POIs. With Compass Ribbon installed, compare
   their C11-corrected apparent scale to vanilla `minecraft:red_x`; buried
   treasure must remain vanilla `minecraft:red_x`.
4. Confirm coordinates, rotations, normalization, identities, Xaero Minimap,
   Xaero World Map's accepted positive 2x geometry, and save/reload behavior
   remain unchanged.
5. With Ribbits installed, confirm its explorer map keeps its mushroom-map item
   art and native `ribbits:ribbit_village` marker regardless of initializer
   order. MME must not mask it; MME must also launch and operate with Ribbits
   absent.

Record only directly observed rows. Stop and record `FAIL` or `INCONCLUSIVE` for
any missing asset/model, crash, holder rewrite, artwork masking, changed
coordinates/geometry, unwanted Ribbits dependency, stale target, persistence
loss, or inability to restore the disposable world.
