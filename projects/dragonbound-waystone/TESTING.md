# Testing

Canary 14 is statically validated but has not been deployed or runtime tested. Its exact retained artifact is `dragonbound-waystone-0.1.0-canary14.jar`, embedded version `0.1.0-canary14`, 85,394 bytes, SHA-256 `B9B8F2D14561CC023517DF30DE05E9738E5B1878236A83E8A816250712EA4C30`. The accepted release remains exact Canary 12, `dragonbound-waystone-0.1.0-canary12.jar`, SHA-256 `6F621B16652BAF9CAAD89BA923B8792EBC0BA08706A9F507FD17AABFC3121AD6`; do not overwrite it.

Canary 13 has user-reported `RUNTIME_FAIL` evidence limited to selectable-material rendering: a Warped Hyphae-crafted Waystone was malformed in inventory and placed as legacy End Stone Bricks. Canary 14 corrects only that visual path. It stores and synchronizes only a donor block registry ID; no anchor or destination authority crosses to the client. A component-less or unresolved donor Waystone intentionally uses the accepted End Stone Bricks appearance. Placement retains the exact stack through the existing block entity, so breaking or loss-protected return must retain the selected material too.

## Preconditions

1. Test only under explicit runtime-slot ownership in the dedicated Minecraft 26.2 Fabric Workbench. Never use the protected gameplay instance.
2. Before deployment, verify the exact filename, embedded `dragonbound_waystone` / `0.1.0-canary14` identity, 85,394-byte size, and SHA-256 above.
3. Use Fabric Loader `0.19.3` or newer and Fabric API `0.156.0+26.2` or newer. Dragonbound has no current Matcha Heart or JEI runtime dependency.

## Focused C14 material-selection checklist

1. First reproduce the C13 regression case: shapeless-craft exactly one Dragonbound Waystone with one `minecraft:warped_hyphae`. In the inventory and hotbar it must retain the accepted four-cuboid/four-pixel Waystone geometry with Warped Hyphae material; it must not deform, flatten, explode, duplicate, or use the donor's block geometry. Place it immediately: its geometry must remain unchanged and it must use Warped Hyphae rather than End Stone Bricks, without requiring a reload.
2. Repeat separately with Stone, Oak Log, and Furnace. Each result must be a count-one Waystone; the donor is consumed. Arrangement must not matter. For logs, upward/downward Waystone surfaces use end texture and horizontal surfaces bark. For Furnace, north-facing Waystone surfaces use the deterministic default north/front texture. The component-less Waystone must still look exactly like End Stone Bricks.
3. Recraft a materialized Waystone with a different valid donor and confirm only the new material remains. A third ingredient, two donors, glass, a slab, grass/leaves, a missing identity, or an otherwise invalid donor must not produce an unsafe visual result and must fall back to End Stone Bricks where applicable.
4. Place a materialized Waystone, save/reload, break it, fill the player inventory so the loss-protected fallback is exercised if practical, then pick/replace it. In every normal existing preservation path, the returned/replaced Waystone must retain its selected material and still preserve its ordinary anchor behavior.
5. Recheck one Pearl and one Staff successful return, cancellation, and invalid start. All Canary 12 channel timing, safety, particles/sounds, success-only Pearl consumption, and Staff cooldown behavior must remain unchanged.

Stop and record `FAIL` or `INCONCLUSIVE` if a selected material falls back unexpectedly, inventory and placed views disagree, face direction is flattened or rotated incorrectly, accepted geometry changes, a rejected donor crafts, a material component is lost through an existing lifecycle path, or any return-system regression appears. Do not claim static validation as a Minecraft runtime PASS.
