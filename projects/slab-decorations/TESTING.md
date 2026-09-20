# Testing

C12 (`0.1.0-canary12`) is the exact current candidate: `slab-decorations-0.1.0-canary12.jar`, 79,591 bytes, SHA-256 `34ec81c28c548a9e291e344b44bce5f1577592a1ca5eb14f49ec2bacd212c611`, built `2026-09-20T05:03:16.2234250Z`, from source checkpoint `53c1dc4b183f1da5cbce6afcb49aca0bb333592a`. It is `ACTIVE / CONTROLLED_VALIDATION_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED` and is not accepted. C11 is its exact predecessor.

## Automated evidence

Using Java 25, the offline Gradle command below passed from a clean project output directory:

```powershell
gradle clean test runGameTest build --offline --no-daemon -Pterrain_slabs_reference_jar=<verified Terrain Slabs 3.3.2 reference JAR>
```

- JUnit: 7/7 passed. The checks pin the exact Enderscape 3.0.2+mc26.2 reference SHA-256, verify the optional behavior-class contracts and direct Void Torch base class, and verify that C12 declares no Enderscape production dependency.
- Fabric GameTests: 47/47 passed against the controlled BGE C72 artifact. Coverage includes native Snow Layer BOTTOM/TOP/DOUBLE stacking, geometry and collision parity, and generated-pillar reconciliation: BOTTOM upward and TOP downward Enderscape-style continuations consume only the matching support after success, while failed transactions restore exact source and support states.
- The existing canonical survival, root/anchor, floor/ceiling surface, crop, aquatic, generic lantern/sign, floor-torch, and optional Ribbits coverage remains green. Void and Bulb Lantern generic paths and Enderscape standing/ceiling hanging-sign contracts stay delegated to those shared paths.
- The exact Enderscape reference JAR was a static/JUnit input only. The headless server deliberately did not load Enderscape because its full runtime dependency stack was unavailable; therefore this is not Enderscape gameplay-runtime evidence and it does not exercise client particles or rendering.

## Runtime verification still required

No Minecraft testing profile, client, save/reload, Enderscape runtime stack, deployment, or acceptance was launched or changed for C12. The following observations remain user-directed runtime evidence:

1. On BGE native horizontal slabs, place and stack vanilla Snow Layers and Enderscape Veiled Leaf Piles on BOTTOM, TOP, and DOUBLE supports. Confirm native layering behavior and the expected `-0.5 Y` BOTTOM representation with unshifted TOP/DOUBLE behavior.
2. Test Enderscape directional vegetation whose real `FACING` is UP and DOWN, including a Murublight Chanterelle. Confirm its surface selection, model/outline/collision/targeting alignment, growth, and cleanup. Verify a horizontal facing remains unmodified and unsupported by this compatibility seam.
3. Trigger Veiled, Celestial, and Murublight structure growth on matching supports. Confirm success preserves Enderscape feature logic while consuming only a matching BOTTOM-upward or TOP-ceiling generated pillar contact; confirm all other support states and failed attempts restore exactly without global structure translation.
4. Exercise full Puruberry chains: vine, flower, unripe berry, attached ripe berry, and a detached ripe berry. Confirm the attached chain uses one topmost vine anchor and detached ripe berries are not projected as supported attachments.
5. Confirm Void Torch floor placement and particles align to the slab surface, while Void Wall Torch remains excluded. Verify Bulb Lantern particles align and generic Void/Bulb Lantern, Enderscape standing-sign, and ceiling-hanging-sign behavior remains intact.
6. Repeat the existing vanilla, crop, aquatic, lantern/sign, floor-torch, and optional Ribbits regressions in a normal client session, then save/reload to verify persistence and client geometry.

Record only observed outcomes against this exact artifact identity in `WORKBENCH_STATUS.json`; runtime results alone do not change lifecycle or acceptance.
