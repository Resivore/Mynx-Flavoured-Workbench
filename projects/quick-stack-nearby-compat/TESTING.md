# QSN C16 runtime procedure — not currently deployed

**CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — RUNTIME_UNTESTED**

Exact candidate: `quick-stack-nearby-compat-0.1.0-canary16.jar`, 61,709 bytes, SHA-256 `2dde3e5b18cd167ba2f743275eb892d50ba849f8213baa0fe5931b7d31aac792`, source `5cd8b43ddad12b868faf5bab0e144c22d300b85b`. Assign C16 only through a verified Test Instance Manager transition before collecting gameplay evidence.

The supplied visual reference is a 36x36 PNG captured at 2x GUI scale, SHA-256 `dbe6e9b7640bc6622edd4824f1df455727f4829ec06a15225af0dbcd9f69d7b1`. C14 uses the audited Minecraft 26.2 normal Button sprites and a direct logical-pixel transcription of its white `#FFFFFF` QSN glyph and `#3F3F3F` shadow; it does not package the screenshot.

1. Launch the dedicated Matcha Flavoured Minecraft 26.2 Workbench with exact QSN 0.4.0 and C15. Confirm it reaches the title screen with no client transformation error.
2. Open the survival InventoryScreen and keep it open long enough to detect frame-to-frame movement. Confirm the 18x18 QSN action remains fixed in the preferred upper slot `leftPos + imageWidth + 4`, `topPos + imageHeight - 40`; it retains normal Minecraft Button chrome, the centered C13 glyph, its tooltip, left-click quick-stack action, and right-click slot-rules action.
3. With Notebook installed, confirm Notebook remains in the lower slot `topPos + imageHeight - 18` with exactly 4px below the fixed QSN button. Reopen the inventory repeatedly, change GUI scale, resize the window, and repeat with Inventory Search and Inventory Extended. There must be no oscillation, duplicate QSN button, or overlap.
4. Put a genuine unrelated visible widget at QSN's preferred position and then at successive upper candidates. Confirm QSN moves upward only in 22px increments, honors each genuine collision, never falls into Notebook's lower slot, and remains stable while its own action occupies the chosen fallback.
5. In a C9-scanned nested shulker, reserve an empty slot for item A. With no other valid capacity, quick stack B: B remains in the source, the slot remains empty, and A's reservation remains intact. Then quick stack A into that slot.
6. Retest C9's physical merge, nested traversal, parent-first ordering, source rules, capacity/remainders, host metadata, and no-loss/no-duplication behavior. Record only direct Minecraft observations; build, static checks, and GameTests are not runtime PASS.
7. Verify the loose-inventory baseline first. Then place matching items in an unlocked carried shulker and an unlocked bundle (including a hotbar and an Inventory Extended ordinary-storage slot): the normal QSN button moves their direct contents to the existing nearby matching targets after loose sources, without moving or altering the outer carrier.
8. Lock each carrier with CCAR and repeat: nothing inside a locked carrier moves or seeds a CSR reserved-empty/ShapeMap target. Unlock it live, repeat, and confirm routing resumes. Check partial target capacity leaves the exact remainder; confirm a shulker's name, lore, custom data, CCAR lock, and CSR reservation survive unchanged.
