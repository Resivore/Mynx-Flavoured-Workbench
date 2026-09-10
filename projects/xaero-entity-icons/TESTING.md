# Testing

Current candidate: `xaero-emf-entity-icon-compat-0.1.0-canary11.jar`, embedded
version `0.1.0-canary11`, SHA-256
`bc58fdf8c8c3d13deb3d3e303732ad70d873da7103f1b374236b3f028b3b5e82`,
source checkpoint `a82b08423bd572beb67096480aaf38bf411605e6`.

Canary 11 is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. It is
not in a Test Slot. No Minecraft launch, managed deployment, visual test,
resource reload, or live cache recreation was performed for C11. Static model,
raster-policy, cache-policy, archive, and non-launching Mixin evidence do not
establish a visible icon pass.

## Preserved runtime provenance

Exact predecessor C10 is a user-reported/external aggregate `RUNTIME_FAIL`:
`xaero-emf-entity-icon-compat-0.1.0-canary10.jar`, embedded version
`0.1.0-canary10`, SHA-256
`488e7e207fbc1e5b03a03cb2893a86520d8f7eb431eaae2eb0f040523be19496`,
source `0aac323c2cd059008731711560b7abf5ba4ba609`. Preserve only these supplied
row observations:

| C10 row | User-reported result |
| --- | --- |
| Bee | PASS — icon size correction works. |
| Rabbit | PASS — icon size correction works. |
| Fox | FAIL — only the ears are visible. |
| Goat | FAIL — label only; no rendered icon. |
| Frog | FAIL — only the upper/top head is visible; the lower jaw/facial portion is missing. |
| Bogged | FAIL — side/back-like head; the face is not readable. |
| Witch | FAIL — only the hat is visible. |
| Ghast; Happy Ghast | FAIL — both remain too small. |
| Florist; Farmer; Stonemason; Cleric; Butcher | FAIL — weird/random model geometry rather than a coherent face plus profession hat. |

Exact C9 remains the accepted baseline from the user-reported external aggregate
PASS of 2026-09-06: `xaero-emf-entity-icon-compat-0.1.0-canary9.jar`, SHA-256
`184ca0da6d6c055c0ba9b2d0e9fb3bccc3c30da9ea7f700a64792d4d1513551a`,
source `6c1b4e0ede3ada8470bc7b4ba54ea8b51f9d2e93`. C10's row evidence does not
rewrite C9, and neither predecessor's evidence transfers to C11.

## Required C11 runtime procedure

Use only separately authorized Test Instance Manager ownership. Never touch the
protected Matcha Flavoured 26.1.2 gameplay instance. Confirm this effective
resource-pack precedence before testing:

1. Ribbit Villagers
2. Mizuno + Fresh Animations
3. Fresh Animations 1.10.5 BETA
4. Matcha Flavoured

Clear/recreate Xaero's icon cache and reload resources once before the matrix
and once after it. Verify normal Fresh Animations/EMF world rendering is
unchanged. Any wrong framing, label-only result, body/limb/tail/harness
contamination, locked-entity regression, Mixin error, or cache recreation
failure is `RUNTIME_FAIL`; incomplete visual evidence is `INCONCLUSIVE`.

| Case | PASS observation |
| --- | --- |
| Fox | Full recognizable front face/head, snout and ears; no torso, legs or tail. |
| Goat | Recognizable rendered head, never label-only; no body. |
| Frog | Upper head and lower jaw/facial portion both visible; no torso, arms, legs, tongue or croak geometry. |
| Bogged | Front-facing readable face/head with bounded headwear/mushrooms; no body or limbs. |
| Witch | Face/head and hat both visible, never hat-only; no body, crossed arms or legs. |
| Ghast; Happy Ghast | Same icon content as C9, approximately 1.50x displayed size; no Happy Ghast harness/rope inclusion. |
| Bee; Rabbit | Same icon content and approximately 0.75x displayed size, preserving their C10 row behavior. |
| Florist | Frog-villager face/head base, frog eyes and gardener hat. |
| Farmer | Frog-villager face/head base, frog eyes and farmer hat. |
| Stonemason | Frog-villager face/head base, frog eyes and prospector hat. |
| Cleric | Frog-villager face/head base, frog eyes and sorcerer hat. |
| Butcher | Frog-villager face/head base, frog eyes and chef hat. |
| Locked controls | Ordinary villager, one unlisted villager profession, allay, vex, axolotl, sniffer, iron golem, wolf, bat, parrot, ravager, sheep, horse, sea turtle and creeper retain C9 behavior. |
| Companion controls | One representative Naturalist Xaero icon from its own matrix and one representative standalone Ribbits Xaero icon from its own matrix remain correct. |

Record only observations actually seen against this exact C11 identity. Do not
promote C11 until every required row has actual passing runtime evidence.
