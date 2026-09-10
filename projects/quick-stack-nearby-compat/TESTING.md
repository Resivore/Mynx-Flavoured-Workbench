# QSN C14 runtime procedure — not currently deployed

**CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — RUNTIME_UNTESTED**

Exact candidate: `quick-stack-nearby-compat-0.1.0-canary14.jar`, 53,321 bytes, SHA-256 `f2af07adf68348f5f10abcadcb27ab799967734cca405839c1e8b21d34723361`, source `b4622a52fe65abeac2716d6dbcdd8c9618ef864e`. Assign C14 only through a verified Test Instance Manager transition before collecting gameplay evidence. C13 is immutable historical evidence but is not a viable client artifact: the supplied Minecraft 26.2 client launch failed while applying its chrome mixin's nonexistent `QuickStackCustomButtonBase.extractDefaultSprite(GuiGraphicsExtractor)` shadow.

The supplied visual reference is a 36x36 PNG captured at 2x GUI scale, SHA-256 `dbe6e9b7640bc6622edd4824f1df455727f4829ec06a15225af0dbcd9f69d7b1`. C14 uses the audited Minecraft 26.2 normal Button sprites and a direct logical-pixel transcription of its white `#FFFFFF` QSN glyph and `#3F3F3F` shadow; it does not package the screenshot.

1. Launch the dedicated Matcha Flavoured Minecraft 26.2 Workbench with exact QSN 0.4.0 and C14. Confirm it reaches the title screen with no `QuickStackCustomButtonChromeMixin` transformation error.
2. Open the survival InventoryScreen. Confirm the QSN action is 18x18 with normal Minecraft Button chrome and the centered C13 glyph; its existing tooltip is unchanged, left-click quick-stacks, and right-click opens the existing slot rules.
3. With Notebook installed, check representative GUI scales and window sizes. QSN must prefer `leftPos + imageWidth + 4`, `topPos + imageHeight - 40`; Notebook independently remains at `topPos + imageHeight - 18`, with a 4px gap. Reopen/resize and test Inventory Search and Inventory Extended: no duplicate or overlap; a collision searches upward.
4. In a C9-scanned nested shulker, reserve an empty slot for item A. With no other valid capacity, quick stack B: B remains in the source, the slot remains empty, and A's reservation remains intact. Then quick stack A into that slot.
5. Retest C9's physical merge, nested traversal, parent-first ordering, source rules, capacity/remainders, host metadata, and no-loss/no-duplication behavior. Record only direct Minecraft observations; build, static checks, and GameTests are not runtime PASS.
