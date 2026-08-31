# Unified BGE C57 runtime procedure

Candidate: `cnm-nibaru-integration-4.2.1-bge.canary57.unified+26.2.jar`, 6,025,701 bytes, SHA-256 `7cd01479531ec26975326b882e0a18406c17de26695b1f4fae29b72edb76cbc2`.

Use only the dedicated Minecraft 26.2 Workbench. Slot A must show BGE Canary 57 as `READY_TO_TEST_VERIFIED`; keep Clutter No More external and install no standalone Nibaru JAR. Static checks, controlled GameTests, exact artifact identity, and manager readiness are not a gameplay pass.

## Known-issue boundary

BGE C56 had a user-reported small glass-Corner visual issue. This consolidation intentionally preserves that appearance and does not diagnose or fix it. Record an unchanged reproduction as the known deferred issue, not as a new consolidation regression or a fix. Stop for any new or worsened glass-Corner difference.

## Matrix

1. Startup and ownership: start with CNM plus the single unified BGE JAR and no standalone Nibaru JAR. Confirm one effective mod container supplies both `cnm_terrain_slabs_compat` and alias `more_slabs_stairs_and_walls` without duplicate registry, entrypoint, mixin, or resource errors.
2. Legacy native identity: resolve representative and edge-case `more_slabs_stairs_and_walls:*` blocks, items, tags, loot, models, textures, and language keys. Confirm exact old IDs pick, place, save, reload, and break without missing or remapped content.
3. Native slabs, stairs, and walls: exercise crafting and stonecutting, placement, orientation, pick-block, breaking, drops, and item economy across ordinary and specialized families.
4. CNM selector order: confirm exactly `Full Block`, `Slab`, `Stair`, `Wall`, `Vertical Slab`, `Step`, `Corner`, `Quarter Column`, `Layer`, with no duplicate or missing role.
5. Vertical Slab and Step: recheck placement faces, rotation, waterlogging where supported, collision, switching, drops, and established economy.
6. Layer: check all six orientations, thickness states, waterlogging, same-block free growth, first-occupancy debit, failed-attempt economy, item presentation, collision, pick-block, and one-source drop.
7. Corner: check all four orientations, full-height L footprint, placement, waterlogging, collision, first-occupancy debit, failed-attempt economy, pick-block, and one-source drop.
8. Quarter Column: check four singleton quadrants, terminal diagonals, compatible free growth, failed-attempt economy, collision, waterlogging, pick-block, and one-source drop.
9. Canonical material frame: sample TOP/SIDE/BOTTOM roles, pillar axes, glazed patterns, overlays/tints, translucency, honey/slime insets, copied settings, and geometry rotation independent from material/UV orientation.
10. Specialized behavior: exercise grass and analogous spreading, path conversion, leaves, falling/concrete, copper oxidation/waxing/scraping, coral, redstone, magma, soul sand, ice, stripping, and typed transitions including `DROP_BASE`.
11. Separated-world compatibility: load a disposable world created with exact BGE C56 plus Nibaru C46; confirm no missing blocks, remaps, duplicate registration, lost states, or changed family identity.
12. Shulker Trowel integration: when Slot B contains C9, confirm all nine modes resolve the exact unified catalog, icons, placement targets, economy, and fail-closed behavior without a standalone Nibaru JAR.
13. Logs: inspect loader, registry, mixin, resource, ShapeMap, transition, and catalog logs for missing or duplicate IDs, failed mixins, unresolved resources, recursive generated-material discovery, or dependency errors.

## Result and stopping conditions

Record only behavior actually observed. Stop and record `FAIL` for startup/registry errors, missing or remapped legacy content, duplicate registration, changed selector identity/order, economy or geometry regressions, new glass-Corner output, or material-profile collapse. Do not promote C57 in this procedure.
