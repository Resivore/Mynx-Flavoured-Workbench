# Testing

**ACTIVE — CONTROLLED VALIDATION PASS — NOT DEPLOYED — RUNTIME UNTESTED**

Current CCAR is `carried-container-auto-routing-0.3.14-csr-panel-lock-canary1.jar`, SHA-256 `c0ab59fe9158982d60a66a92d1248009f13b646e5f76c4460127ffe65af93638`, source `9e74a9363616e8ce0be1de098e788fc08044c8d7`; pair it with CSR C18. Test only under explicit Test Instance Manager ownership in the dedicated Matcha Flavoured 26.2 Workbench. Never open or alter the protected Matcha Flavoured 26.1.2 profile.

1. Hover a supported carried shulker in the CSR panel. Confirm the supplied unlocked/locked sprite retains its exact art and dimensions, appears in the header rather than on a bundle, and its hitbox matches the visible sprite.
2. Left-click the unlocked badge, wait for ordinary server menu synchronization, confirm the exact host becomes locked and its sprite changes. Click again to unlock and confirm both the exact target and visual update. A badge click must not execute a panel-cell action behind it.
3. Verify the existing keyboard control still toggles a normal player-inventory target. Verify the badge on a shulker in another legitimate active non-fake menu slot only toggles that exact server stack; stale menu IDs, invalid indices, fake/result slots, or unsupported targets must make no change.
4. For each resulting state, test automatic routing with matching items: unlocked carriers accept per existing order, locked carriers do not. Recheck carrier contents, CSR reservations, effective capacities, Inventory Extended slots, and save/reload; bundles remain tooltip-only.
5. Recheck C18's pickup-audio matrix: ordinary custom and carried routing each have one appropriate cue, vanilla-take paths do not double, and no cue is emitted for no acquisition, QUICK_MOVE, locked/full/nonqualifying carriers. Stop on any lock-target, routing, audio, count, component, UI, or startup regression; record only observed runtime evidence.
