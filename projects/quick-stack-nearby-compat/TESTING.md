# QSN C17 runtime procedure — not currently deployed

**CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — RUNTIME_UNTESTED**

Exact candidate: `quick-stack-nearby-compat-0.1.0-canary17.jar`, 64,151 bytes, SHA-256 `9c133eee74922dfc0b0ac2624f3b02ef47e602fb87bcf2b995d1049e08d07bd9`, source `d4764be8a4e715b60e1276f28dae99c471a56d37`. Assign C17 only through a verified Test Instance Manager transition before collecting gameplay evidence.

1. In a nearby chest, put a shulker containing exactly 32 oak planks. Carry an otherwise component-identical shulker containing exactly 32 oak planks and press QSN. The carried shulker stays in the player inventory, becomes physically empty, and the chest shulker reaches 64 planks; neither outer carrier is moved, renamed, unlocked, or stripped of metadata.
2. During that same press, confirm the newly empty carried shulker does not stack. On a second QSN press, confirm it may stack normally when an exactly matching empty destination shulker is available under the installed stack-size rules. Also directly test two matching physically empty shulkers.
3. Lock a populated carried shulker with CCAR and repeat: neither its outer item nor internal contents move. Unlock it and confirm direct contents drain again. Also user-QSN-lock its carrier slot and confirm internals remain excluded.
4. Reserve a nearby CSR destination for an item that exists only inside an unlocked populated carried shulker; confirm that internal item routes there. Separately reserve the populated outer-shulker identity and confirm it is not seeded or moved.
5. Regression-test bundles, ordinary loose items, Inventory Extended storage, nested shulker targets, ShapeMap affinity, partial capacity/remainders, and the existing QSN inventory button. Record only direct Minecraft observations; automated checks are not runtime PASS.
