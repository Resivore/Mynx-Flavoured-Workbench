# Testing

## Candidate and evidence boundary

Canonical C6 is the exact private Slot B candidate built from source `5e1007888a847f00012a413214d51c810efa1b31`:

- deployed private `shulker-trowel-0.1.0-canary5-private.jar`, 40,314 bytes, embedded version `0.1.0-canary5`, SHA-256 `4AB4BB8068F0660E0BBC6ED0CB059666B7112CC0E29744906972E396FC653664`, deployment `d66d7f25-918a-4452-9be6-81c72b28a40e`, artifact `fb86de68-38f3-4822-9127-c670396393d1`;
- retained clean `artifacts/shulker-trowel-0.1.0-canary5.jar`, 39,811 bytes, SHA-256 `48A8F9B842EB0309D2635ABFBDD4548E506B1B919399099D76D4DA3D558A808E`.

The clean artifact is build/provenance evidence and omits the separately authorized protected sprite; never substitute it for the exact private runtime candidate. Exact private C5 `shulker-trowel-0.1.0-canary4-private.jar`, SHA-256 `73C012C08CA5567E795711CF11C8EAAC625528E7350309114467129394F0AFBA`, remains unchanged as both accepted and rollback identity.

The Java 25 clean build passed 24/24 focused JUnit tests and 11/11 controlled GameTests against exact BGE C53 plus Nibaru C46. Manager revision 29 deployed C6 alongside that exact pair and returned `READY_TO_TEST_VERIFIED`; Slot B remains `UNTESTED`. Compilation, JUnit, GameTests, artifact assembly, dependency resolution, and manager readiness are not a Minecraft runtime pass. Accepted C5's historical pass and BGE C53's independent Slot A failure do not classify C6.

## Preconditions

1. Confirm Slot B contains the exact deployed private C6 identity above with runtime result `UNTESTED`; do not rebuild or substitute a same-version JAR.
2. Confirm exact Slot A BGE C53 `cnm-nibaru-integration-0.6.0-bge-canary53-layer.jar`, SHA-256 `57A4599ADB3F4C58AE7B99A0148A38760FDB3DA59847CA3EC046379DB323B7C8`, and exact Nibaru C46 `more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar`, SHA-256 `3281D110F062DB62E721D838A35CE915CA73DD41AF098A013B52952561CEAE7D`, remain loaded with CNM 2.0.7.
3. Confirm C6 declares BGE `>=0.5.46-nibaru-cnm-canary1.36-pale-coverage` with no finite maximum and Nibaru `>=4.2.0 <4.3.0-`. A Loader dependency rejection is a failure, not compatibility evidence.
4. Keep Jake's Build Tools disabled; its separately authorized sprite is already present only in the exact private candidate.
5. Record only behavior actually observed for C6. Do not infer unreported rows from static checks, startup, C5, or either dependency's result.

## Focused C6 runtime procedure

1. Reach the title screen, enter a disposable test world, and confirm there is no Loader dependency rejection, startup crash, or relevant initialization error with exact C53 and C46 present.
2. Put exactly two Oak Planks in the offhand shulker, select Step mode, and place two fitting Step geometries into one block space. Confirm the combined geometry forms, consumes one source block total, and leaves one plank.
3. Repeat the fitting same-block economy check for Horizontal Slab and Vertical Slab where their geometry permits combination. Each combined geometry must consume one source block total; the compatible same-block addition must not consume another full source item.
4. Place representative Full Block, Slab, Stair, Wall, Vertical Slab, and Step geometry into separate empty spaces. Confirm every successful ordinary placement consumes one source item, any compatible same-block combination consumes no additional full source item, and every failed or incompatible placement consumes nothing.
5. Exercise all six existing modes through CNM's configured shape input. Move the trowel between inventory slots and save/reload; confirm the selected mode remains server-authoritative and persists, and CNM's familiar overlay does not replace or mutate the trowel.
6. Mix Oak Log, Oak Wood, Stripped Oak Log, and distinct oxidation and wax-state Copper sources. Confirm exact variants remain separate and only candidates eligible for the selected shaped mode participate in quantity weighting.
7. Use a shaped-mode palette with no eligible source. Confirm there is no placement, sound, or consumption.
8. Place representative ordinary and shaped blocks against multiple faces and into water. Confirm orientation, waterlogging, resolved BGE geometry, and delegated normal `BlockItem` placement remain canonical, with one normal placement sound for the actor and nearby players.
9. Exercise the actual offhand shulker source with multiple eligible quantities and, where available, a second player observing. Confirm selection, consumption, placement, mode state, and synchronization remain server-authoritative without duplication or client-only mutation.
10. Smoke-check Full mode, the `S  ` / ` II` / `   ` recipe, the authorized trowel icon, and ordinary CNM switching with a non-trowel item.
11. Inspect `latest.log` for Loader dependency rejection, mixin, registry, CNM overlay, placement, payload, synchronization, or resource errors.

Stop and record `FAIL` or `INCONCLUSIVE` if a fitting combination consumes the second full source block, an ordinary success has wrong consumption, a failed placement consumes anything, exact variants collapse, an ineligible source participates, any of the six modes fails, orientation/waterlogging/delegated placement diverges, mode authority or persistence changes, ordinary CNM behavior regresses, the game crashes, or a relevant Loader, mixin, registry, placement, or synchronization error appears. Do not promote C6 without an explicit reported runtime pass.
