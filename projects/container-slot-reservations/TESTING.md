# Testing

**ACTIVE — CONTROLLED VALIDATION PASS — NOT DEPLOYED — RUNTIME UNTESTED**

Current CSR C19 is `container-slot-reservations-0.1.0-canary19.jar`, SHA-256 `f93068f86d0da76e93d754a9c04cd5999fc72b31f39681fc65dbdb09b2443aeb`, source `804f3fe61bbc216d9d33df8d90326f9f1fc0e929`. It pairs with CCAR `0.3.14-csr-panel-lock-canary1`. Test only with explicit Test Instance Manager ownership in the dedicated Matcha Flavoured 26.2 Workbench; never open or alter the protected 26.1.2 profile.

1. Hover the same supported shulker in the Creative inventory and an ordinary container menu. Placement must retain C18's host-centered, viewport-clamped position and immediate host-or-panel lifetime.
2. With an empty cursor, left-click a visible stack. Its source cell must empty and that exact stack must immediately be visible on the real cursor.
3. Move the cursor inside and outside the panel while holding it. The stack must remain visibly attached to the cursor and never silently disappear.
4. Left-click an eligible empty panel cell. The exact cursor stack must enter it; pick it up again and click the original cell to confirm stable vanilla-like pickup/place behavior.
5. Repeat with a named/component-rich stack, a partial target, a reserved matching slot, and a mismatched reserved slot. Verify exact remainders and no mutation on rejection.
6. Quick regression: Shift-click still works; RMB single click/drag still works; CCAR's lock icon still toggles. Stop on an invisible cursor, loss/duplication, panel/server disagreement, unexpected reappearance, stale-fingerprint closure, or placement regression.
