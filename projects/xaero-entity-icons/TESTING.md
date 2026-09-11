# Testing

Current candidate: `xaero-emf-entity-icon-compat-0.1.0-canary12.jar`, embedded
version `0.1.0-canary12`, SHA-256
`dcf65d6e97007c03bf490b27189f76ee88b882d1b59d4b88315093dea10180bc`,
source checkpoint `1bf0761f94eaf9442abc40f37f0e479509059b2f`.

Canary 12 is `ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED`. It is
not in a Test Slot. No Minecraft launch, managed deployment, visual test,
resource reload, or live cache recreation was performed for C12. Static model,
raster-policy, cache-policy, archive, and non-launching Mixin evidence do not
establish a visible icon pass.

## Preserved runtime provenance

Exact predecessor C11 is a user-reported/external aggregate `RUNTIME_FAIL`:
`xaero-emf-entity-icon-compat-0.1.0-canary11.jar`, embedded version
`0.1.0-canary11`, SHA-256
`bc58fdf8c8c3d13deb3d3e303732ad70d873da7103f1b374236b3f028b3b5e82`,
source `a82b08423bd572beb67096480aaf38bf411605e6`. Preserve only these supplied
row observations:

| C11 row | User-reported result |
| --- | --- |
| Fox | PASS — visually good. |
| Goat | PASS — visually good. |
| Frog | PASS — visually good. |
| Bogged | PASS — visually good. |
| Witch | PASS — visually good. |
| Ghast | PASS — visually good. |
| Happy Ghast | PASS — visually good. |
| Bee | PASS — visually good. |
| Rabbit | PASS — visually good. |
| Butcher villager | PASS — the underlying model has profession headwear, but the C11 icon is visually correct because that headwear is effectively not visible. Preserve this exact appearance; a visible chef hat is not required. |
| Florist | FAIL — the otherwise useful villager head has weird/random geometry above it rather than the intended gardener hat. |
| Farmer | FAIL — the otherwise useful villager head has weird/random geometry above it rather than the intended farmer hat. |
| Cleric | FAIL — the otherwise useful villager head has weird/random geometry above it rather than the intended sorcerer hat. |
| Mason/Stonemason | FAIL — the otherwise useful villager head has weird/random geometry above it rather than the intended prospector hat. |

Exact C9 remains the accepted baseline from the user-reported external aggregate
PASS of 2026-09-06: `xaero-emf-entity-icon-compat-0.1.0-canary9.jar`, SHA-256
`184ca0da6d6c055c0ba9b2d0e9fb3bccc3c30da9ea7f700a64792d4d1513551a`,
source `6c1b4e0ede3ada8470bc7b4ba54ea8b51f9d2e93`. C11's row evidence does not
rewrite C9, and neither release's evidence transfers to C12.

## Required C12 runtime procedure

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
| Florist | Coherent frog-villager face/head and the existing frog-eye portion, with no profession headwear geometry, no random geometry above the head, and no body, crossed arms or limbs. |
| Farmer | Coherent frog-villager face/head and the existing frog-eye portion, with no profession headwear geometry, no random geometry above the head, and no body, crossed arms or limbs. |
| Cleric | Coherent frog-villager face/head and the existing frog-eye portion, with no profession headwear geometry, no random geometry above the head, and no body, crossed arms or limbs. |
| Mason/Stonemason | Coherent frog-villager face/head and the existing frog-eye portion, with no profession headwear geometry, no random geometry above the head, and no body, crossed arms or limbs. |
| Butcher | Exact C11 appearance: coherent frog-villager face/head with no regression. Do not require the underlying chef hat to become visible. |
| Fox | Visually identical to the successful C11 icon. |
| Goat | Visually identical to the successful C11 icon. |
| Frog | Visually identical to the successful C11 icon. |
| Bogged | Visually identical to the successful C11 icon. |
| Witch | Visually identical to the successful C11 icon. |
| Ghast; Happy Ghast | Visually identical to C11, preserving the existing 1.50 presentation scale and excluding Happy Ghast harness/rope geometry. |
| Bee; Rabbit | Visually identical to C11, preserving the existing 0.75 presentation scale. |
| Locked controls | Ordinary villager, one unlisted villager profession, allay, vex, axolotl, sniffer, iron golem, wolf, bat, parrot, ravager, sheep, horse, sea turtle and creeper retain the exact C9 path and behavior. |
| Companion controls | Naturalist Xaero compatibility and standalone Ribbits Xaero compatibility remain unchanged; one representative icon from each companion's own matrix remains correct. |

Record only observations actually seen against this exact C12 identity. Do not
promote C12 until every required row has actual passing runtime evidence.
