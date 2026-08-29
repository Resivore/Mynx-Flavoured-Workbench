# Testing

Exact `0.1.0-canary1` is the current unaccepted candidate. Its retained JAR is `bedrockify-nemos-bonemeal-compat-0.1.0-canary1.jar`, 2,947 bytes, SHA-256 `2720DAFFF71EF2779F08905899BC9D55BCB7DB80811020492DE47F977C669273`. Build and bytecode checks pass, but it is **NOT DEPLOYED** and **RUNTIME UNTESTED**. Run this matrix only after explicit Test Slot ownership with BedrockIfy `1.11.8+mc26.2`, Nemo's Blooming Blossom `26.2-1.3`, and normal block drops enabled.

1. Bonemeal a poppy on a clear 7×7 valid-support pad. Confirm one bonemeal is consumed, exactly one poppy item is emitted at the clicked flower, the original remains, and no nearby poppy is placed.
2. Block or invalidate every candidate space in the ±3 X/Z area and bonemeal the poppy. Confirm the same one-item result and no placement; obstruction must not change the outcome.
3. Bonemeal an open or closed eyeblossom. Confirm one matching item is emitted and no nearby propagation occurs.
4. Attempt to bonemeal a wither rose. Confirm rejection, no bonemeal consumption, and no item or nearby rose; this effective exclusion remains owned by Nemo's separate subclass mixin.
5. Bonemeal a representative vanilla tall flower. Confirm its existing one-item duplication is unchanged.
6. Bonemeal grass repeatedly in a Cherry Grove. Confirm Nemo's pink-petal behavior still occurs without a compatibility error.
7. With BedrockIfy's `common.features.fertilizableBlocks` still enabled, bonemeal eligible sugar cane. Confirm BedrockIfy's growth behavior still works and consumes bonemeal.
8. Review the log for mixin-application failures or unexpected ownership conflicts and confirm a normal shutdown.

If dedicated-server assurance is required, repeat steps 1–4 and 7 on a disposable playtest world on the actual server host and record that result separately. Do not infer a server pass from the common mixin, compilation, bytecode inspection, or client/singleplayer behavior.

Stop and record `RUNTIME_FAIL` or `INCONCLUSIVE` if the game crashes, the exact artifact or dependencies are not loaded, the compatibility mixin fails, a flower propagates instead of dropping one matching item, wither-rose rejection changes, BedrockIfy sugar-cane fertilization stops working, or unrelated grass/tall-flower behavior regresses. Do not deploy a rebuilt or differently hashed JAR under this Canary identity.
