# QSN C12 runtime procedure — not currently deployed

**CONTROLLED_VALIDATION_PASS — NOT_DEPLOYED — RUNTIME_UNTESTED**

Exact candidate: `quick-stack-nearby-compat-0.1.0-canary12.jar`, 46,773 bytes, SHA-256 `68b35bdc407157e103164c9c9705835a253d222e3e8feccf5388b275f411d713`, source `3a74391972ec80ac5eff71375d44dbb5f512f416`. Assign C12 only through a verified Test Instance Manager transition before collecting gameplay evidence. C12 is derived from C9; C10/C11 are historical rejected experiments, not C12 behavior.

1. In a C9-scanned nested shulker, reserve an empty slot for item A. With no other valid capacity, quick stack a different item B: B must remain in the source, the slot must remain empty, and the A reservation must remain intact.
2. Repeat with a genuinely unreserved empty nested slot. B may use that ordinary slot, but must not enter or alter the reserved-zero A slot.
3. After the incompatible B attempt, quick stack A. A must populate the same reserved slot, proving the reservation survived the zero-count state.
4. Retest C9's physical partial-stack merge, nested traversal, parent-first ordering, source rules, capacity/remainders, host metadata, and no-loss/no-duplication behavior. Record only directly observed C12 runtime results; controlled JUnit and Fabric GameTests do not constitute runtime PASS.
