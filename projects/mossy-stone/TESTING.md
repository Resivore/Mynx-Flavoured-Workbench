# Testing

## Successor-only gate

Exact Canary 2 (`mossy-stone-0.2.0-canary2.jar`, SHA-256
`0A337771301DFB1165ADED256908677B93B48F756E1C5A8EACFDE5D057178B59`)
is a closed failed candidate in runtime Slot B. Its implementation loaded and
functioned, its visuals and Stone-like behavior were correct, and no tested
geometry or gameplay defect was reported; it failed because Mossy Stone was
absent from the appropriate Creative inventory exposure and from JEI. Do not
redeploy, promote, or reclassify unchanged C2.

No Mossy Stone release is accepted, exact C1 is only a historical predecessor,
and no C3 exists. Run this procedure only after a separately authorized narrow
Creative/JEI exposure fix produces a new exact successor artifact with a new
version, source commit, and SHA-256. Update Slot B only; preserve Slot A and the
accepted baseline.

## Successor preflight

1. Use Minecraft Java 26.2, Fabric Loader 0.19.3 or later, Fabric API
   0.157.0+26.2, `clutternomore` 2.0.7+26.2, and the accepted Block Geometry
   Extensions stack. Keep Regions Unexplored absent.
2. Use the V2 physical manager's dry-run and apply paths, then require its
   focused readiness verification before launching Minecraft.
3. Confirm exactly one Mossy Stone candidate is enabled and record only the
   exact successor behavior actually observed. Static and readiness results do
   not imply runtime success.

## Focused successor matrix

1. Launch with the cumulative accepted stack and unchanged Heart Slot A;
   confirm no missing-registry, model, texture, recipe, loot, ShapeMap, or
   ownership error.
2. Confirm the Mossy Stone family appears in the appropriate Creative inventory
   location and every intended family entry is discoverable through JEI without
   duplicates or unrelated entries.
3. Confirm the full block, slab, stairs, wall, BGE Vertical Slab, and BGE Step
   retain the approved `mossy_stone` artwork and correct Stone-like mining,
   sound, tool, resistance, placement, shape, state, and waterlogging behavior.
4. Break the full block without Silk Touch and confirm exactly Mossy Cobblestone
   drops; with Silk Touch confirm the full block drops itself. Confirm every
   other geometry drops its own corresponding shape.
5. Verify Stone plus Moss Block, Stone plus Vine, smelting and blasting Mossy
   Cobblestone, and 2x2 Mossy Stone producing four vanilla Mossy Stone Bricks.
6. Verify ordinary and stonecutting slab, stair, and wall outputs plus matching
   recipe discovery/unlock behavior.
7. Confirm BGE exposes exactly the canonical parent, slab, Vertical Slab,
   stairs, Step, and wall without duplicate registration, recipes, recursive
   generation, or Mossy Stone ownership of the two BGE-derived shapes.
8. Smoke-check representative BGE/Nibaru and Interchangeable Block Families
   behavior for a Mossy Stone-attributable regression.

Stop and record `FAIL` or `INCONCLUSIVE` for missing Creative or JEI exposure,
load failure, incorrect artwork or Stone behavior, wrong loot or recipe output,
duplicate/missing derived geometry, ownership conflict, state/waterlogging
defect, crash, relevant log error, or unrelated stack regression.
