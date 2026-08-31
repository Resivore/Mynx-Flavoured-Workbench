# Shulker Trowel C9 runtime procedure

Candidate: private `shulker-trowel-0.1.0-canary8-private.jar`, 44,748 bytes, SHA-256 `2e74d902c46cb3072ab33cdc56ac6ae55e2fd5ec0f4e5ee297cfebec338513ab`.

Use only the dedicated Minecraft 26.2 Workbench. Slot B must show Trowel Canary 8 as `READY_TO_TEST_VERIFIED`; Slot A must contain exact unified BGE C57. Keep CNM external and install no standalone Nibaru JAR.

## Matrix

1. Confirm selector order is unchanged: `Full Block`, `Slab`, `Stair`, `Wall`, `Vertical Slab`, `Step`, `Corner`, `Quarter Column`, `Layer`.
2. Confirm saved/network mode IDs remain 0–6 for the established modes, Corner remains 7, Quarter Column remains 8, and selector order does not rewrite identity.
3. For ordinary and specialized materials, verify every available mode shows the exact resolved unified-BGE item icon and targets that exact BlockItem.
4. Exercise normal delegated placement, orientation, state, sound, waterlogging, variants, collision, first-occupancy debit, compatible free growth, failed-attempt economy, and normal drops.
5. Confirm unavailable material/geometry combinations retain selected identity and fail closed without consuming, refunding, collapsing, or substituting a mode.
6. Confirm server authority, client/server synchronization, scrolling, overlay count, saved selection, invalid-data handling, and reconnect behavior remain unchanged.
7. Confirm the actual offhand shulker inventory is the quantity source and the trowel remains in the required hand through placement.
8. Confirm no standalone Nibaru dependency, JAR, loader error, or old-project path is required.
9. Inspect Trowel, CNM, BGE catalog, placement, and synchronization logs for missing descriptors, duplicate modes, wrong resolved items, or rejected dependencies.

## Private-resource and known-issue boundaries

The runnable candidate contains the separately authorized 346-byte private sprite; it must remain untracked and non-redistributable. Do not extract, publish, or replace it.

Unified BGE intentionally retains the known small glass-Corner visual issue. If Trowel displays and places the exact resolved BGE glass-Corner item unchanged, that appearance alone is not a Trowel regression or a fix; report any new Trowel-specific mismatch separately.

Record only actual runtime observations. Build, focused tests, GameTests, and readiness verification are not runtime evidence. Stop for identity/order drift, wrong icons or targets, consumption/refund changes, delegated-placement changes, authority/synchronization failures, dependency errors, or private-resource leakage. Do not promote C9 in this procedure.
