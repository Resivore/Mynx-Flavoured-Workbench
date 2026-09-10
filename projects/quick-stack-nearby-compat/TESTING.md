# QSN C13 runtime procedure — not currently deployed

**CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — RUNTIME_UNTESTED**

Exact candidate: `quick-stack-nearby-compat-0.1.0-canary13.jar`, 52,780 bytes, SHA-256 `46a5c7e373f8b13d37e64ae5f0187f8b66e90b9ee636f598b1686de8bfa5f38a`, source `e941aa45a5b3640b462e20a4b56ed7229039aa58`. Assign C13 only through a verified Test Instance Manager transition before collecting gameplay evidence. C13 is derived from C12; C10/C11 are historical rejected experiments, not C13 behavior.

The supplied visual reference is a 36x36 PNG captured at 2x GUI scale, SHA-256 `dbe6e9b7640bc6622edd4824f1df455727f4829ec06a15225af0dbcd9f69d7b1`. C13 uses Minecraft's normal Button chrome and a direct logical-pixel transcription of its white `#FFFFFF` QSN glyph and `#3F3F3F` shadow; it does not package the screenshot.

1. With Notebook installed, open the survival inventory at representative GUI scales and window sizes. QSN must be 18x18 at `leftPos + imageWidth + 4`, above Notebook at `topPos + imageHeight - 40`; Notebook must remain lower at `topPos + imageHeight - 18`, with exactly 4px between the buttons.
2. Confirm QSN's chrome matches Notebook's normal Minecraft button chrome, its centered arrow is crisp and readable, its existing tooltip is unchanged, left-click quick-stacks, and right-click opens the existing slot rules.
3. Reopen/resize the survival inventory and test with Inventory Search and Inventory Extended. There must be no duplicate QSN button and no overlap with visible utility, recipe-book, status/effect, or inventory widgets; an unrelated collision must use the nearest free position above the preferred slot.
4. In a C9-scanned nested shulker, reserve an empty slot for item A. With no other valid capacity, quick stack a different item B: B must remain in the source, the slot must remain empty, and the A reservation must remain intact. Then quick stack A into that same reserved slot.
5. Retest C9's physical partial-stack merge, nested traversal, parent-first ordering, source rules, capacity/remainders, host metadata, and no-loss/no-duplication behavior. Record only directly observed C13 runtime results; controlled JUnit and Fabric GameTests do not constitute runtime PASS.
