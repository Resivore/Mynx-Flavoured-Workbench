# Testing

C11 (`0.1.0-canary11`) is the exact current candidate: `slab-decorations-0.1.0-canary11.jar`, 67,533 bytes, SHA-256 `7e15d2f1cb93ad97656e1380b83f905df143c7df79e149b9187fc74c6d5e90ba`, built `2026-09-19T05:22:00.2476822Z`, from source checkpoint `5a41f357d23a96068f56eb75a5c4f9fe52de4b8e`. It is `ACTIVE / CONTROLLED_VALIDATION_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED` and is not accepted. C10 is its exact predecessor.

## Automated evidence

Using Java 25, the offline Gradle command below passed from a clean project output directory:

```powershell
gradle clean test runGameTest build --offline --no-daemon -Pterrain_slabs_reference_jar=<verified Terrain Slabs 3.3.2 reference JAR>
```

- JUnit: 5/5 passed. The architecture checks confirm the exact C72 BGE Farmland Slab public identity, the registered exact-state crop-fertility seam, normal-profile behavior, and the optional Ribbits mixin boundary.
- Fabric GameTests: 45/45 passed against the installed BGE C72 artifact (`4.2.16-bge.canary72.farmland-slab-low-water+26.2`, SHA-256 `f9f892fcdf3f78879ef19dd8cd6bf398de40b84f1a579d6cef8781f1313188f7`).
- The C11 direct `CropBlock.getGrowthSpeed` test proves full 3×3 fertility parity for wheat, carrots, potatoes, beetroot, torchflower, and pitcher crop paths: vanilla dry farmland is `4.0`, hydrated farmland is `10.0`, and BGE Farmland Slab BOTTOM/TOP/DOUBLE plus mixed vanilla/BGE neighborhoods match exactly while retaining their real `MOISTURE` values. The ordinary random-tick growth check remains secondary coverage.
- Crop-family item-placement tests retain canonical farmland survival, exact BOTTOM `-0.5 Y` projection and unshifted TOP/DOUBLE projection, vanilla bonemeal, mature wheat drops, BGE hydration/lifecycle behavior, and pumpkin/melon stem exclusion.
- The same GameTests reject pumpkin and melon stem families, including attached stems; ordinary stem-item use leaves the item stack and slab unchanged.
- Transaction tests verify that only the exact optional `ribbits:toadstool_stem` identity is a structural continuation: a successful BOTTOM transaction replaces its ground-contact slab with the generated exact stem state, while TOP/DOUBLE preserve their slab. Failed transactions restore the exact original slab and source state.

The optional mixin wraps Ribbits' shared private huge-growth placement seam on `ToadstoolBlock`; therefore both `ribbits:toadstool` (red) and `ribbits:small_brown_toadstool` (brown) retain Ribbits' own 0.4 roll, configured-feature selection, and spread fallback. C11 does not special-case their ordinary vegetation placement: their inherited `VegetationBlock` machinery continues to provide BOTTOM projection and unshifted TOP/DOUBLE placement.

## Runtime verification still required

No Minecraft testing profile, client, save/reload, or actual full Ribbits dependency stack was launched for C11. The following observations remain user-directed runtime evidence:

1. Place each small toadstool on an eligible BGE Farmland Slab BOTTOM state; confirm its visual, collision/outline, and targeting projection is exactly `-0.5 Y`. Repeat on TOP and DOUBLE and confirm no translation.
2. Bonemeal each color repeatedly on BOTTOM until the huge-growth roll succeeds. Confirm red uses Ribbits' red huge feature and brown uses its brown huge feature; when the generated exact stem contacts the support, only that BOTTOM slab becomes the generated `ribbits:toadstool_stem` state.
3. Repeat successful huge growth on TOP and DOUBLE; confirm the original support slab survives unchanged and the structure is not globally translated.
4. Force or find a failed huge-generation site for each color. Confirm the exact support slab and Ribbits source/fallback behavior are restored, with no canonical full block, missing support, or partial feature left behind.
5. Confirm a missed 0.4 huge-growth roll still follows Ribbits' ordinary same-color spreading path for both colors.
6. Check vanilla red and brown mushroom huge growth from BGE slabs remains unchanged, and repeat the Ribbits checks with Ribbits absent to confirm the optional mixin is inert.
7. Exercise crop placement, growth, bonemeal, harvest, and moisture transitions for all six ordinary crop families on BOTTOM/TOP/DOUBLE BGE Farmland Slabs in a normal client session; include a reload to confirm persistence and visual behavior.

Record only observed outcomes against this exact artifact identity in `WORKBENCH_STATUS.json`; runtime results alone do not change lifecycle or acceptance.
