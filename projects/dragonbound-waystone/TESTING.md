# Testing

Canary 13 is statically validated but has not been deployed or runtime tested. Its exact retained artifact is `dragonbound-waystone-0.1.0-canary13.jar`, embedded version `0.1.0-canary13`, 83,574 bytes, SHA-256 `43BA4DBC7A11A8EDCF06F3388E72CEDE6B165E5C5D17FCC78F82A9D2A368BF85`. The accepted release remains exact Canary 12, `dragonbound-waystone-0.1.0-canary12.jar`, SHA-256 `6F621B16652BAF9CAAD89BA923B8792EBC0BA08706A9F507FD17AABFC3121AD6`; do not overwrite it.

Canary 13 stores only a donor block registry ID on the Waystone ItemStack. A component-less or unresolved donor Waystone intentionally uses the accepted End Stone Bricks appearance. Placement retains the exact stack through the existing block entity, so breaking or loss-protected return must retain the selected material too. Destination/anchor authority remains entirely separate.

## Preconditions

1. Test only under explicit runtime-slot ownership in the dedicated Minecraft 26.2 Fabric Workbench. Never use the protected gameplay instance.
2. Before deployment, verify the exact filename, embedded `dragonbound_waystone` / `0.1.0-canary13` identity, 83,574-byte size, and SHA-256 above.
3. Use Fabric Loader `0.19.3` or newer and Fabric API `0.156.0+26.2` or newer. Dragonbound has no current Matcha Heart or JEI runtime dependency.

## Focused C13 material-selection checklist

1. In a normal crafting grid, combine exactly one Dragonbound Waystone and one stone, sandstone, oak log, furnace, and glazed terracotta in separate attempts. Each result must be a count-one Waystone; the donor is consumed. Arrangement must not matter. A third ingredient, two donors, glass, a slab, and grass/leaves must not produce a result.
2. Inspect each materialized result in the inventory, hotbar, dropped-item view, and when placed. It must retain the exact accepted four-cuboid/four-pixel Waystone geometry while using the donor material—not the donor's own block geometry. The component-less Waystone must still look exactly like End Stone Bricks.
3. Confirm directional mapping: logs use end texture on upward/downward Waystone surfaces and bark on horizontal surfaces; sandstone uses its top, bottom, and side textures by Waystone face; a furnace's deterministic default north/front texture appears on north-facing Waystone surfaces; glazed terracotta follows the deterministic default-state horizontal orientation. Recraft a materialized Waystone with a different valid donor and confirm only the new donor remains.
4. Place a materialized Waystone, save/reload, break it, fill the player inventory so the loss-protected fallback is exercised if practical, then pick/replace it. In every normal existing preservation path, the returned/replaced Waystone must retain its selected material and still preserve its ordinary anchor behavior.
5. Recheck one Pearl and one Staff successful return, cancellation, and invalid start. All Canary 12 channel timing, safety, particles/sounds, success-only Pearl consumption, and Staff cooldown behavior must remain unchanged.

Stop and record `FAIL` or `INCONCLUSIVE` if a selected material falls back unexpectedly, inventory and placed views disagree, face direction is flattened or rotated incorrectly, accepted geometry changes, a rejected donor crafts, a material component is lost through an existing lifecycle path, or any return-system regression appears. Do not claim static validation as a Minecraft runtime PASS.
