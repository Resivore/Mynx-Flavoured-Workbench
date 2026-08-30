# Testing

## Canary 3 combined-session gate

Test only exact Canary 3:

- version `0.3.0-canary3`
- file `mossy-stone-0.3.0-canary3.jar`
- SHA-256 `B24D8E411F4B83AC02D73DB7564F8F58BF4F2D67207621394FECA0BE0D9C4995`
- source `373fb600a71b25b204fd73cb5b4c17999969a442`

Canary 2 is a closed failed candidate. Its implementation, visuals,
Stone-like behavior, tested family gameplay, and tested geometry behavior
passed, but Creative inventory and JEI exposure failed. Do not redeploy,
promote, or reclassify C2.

Use the V2 physical manager to place exact C3 in Slot B and require final
combined readiness verification before launching Minecraft. The intended
session also has exact Heart Canary 8 in Slot A; preserve the accepted baseline
and keep Regions Unexplored absent. Build, static tests, deployment, and
readiness are not Minecraft runtime results.

## Focused Canary 3 matrix

1. In the Building Blocks Creative tab, confirm Mossy Stone, Mossy Stone Slab,
   Mossy Stone Stairs, and Mossy Stone Wall appear once each. Confirm all four
   are also present once each in Creative search, with no unrelated or
   BGE-derived entries added by Mossy Stone.
2. In JEI, search for Mossy Stone and confirm the same four Mossy-owned items
   are discoverable once each. Confirm BGE remains the sole owner of the
   Vertical Slab and Step and that neither shape is duplicated.
3. Confirm the full block, slab, stairs, wall, BGE Vertical Slab, and BGE Step
   retain the approved artwork and correct Stone-like mining, sound, tool,
   resistance, placement, shape, state, and waterlogging behavior.
4. Break the full block without Silk Touch and confirm exactly Mossy
   Cobblestone drops; with Silk Touch confirm the full block drops itself.
   Confirm every other geometry drops its own corresponding shape.
5. Verify Stone plus Moss Block, Stone plus Vine, smelting and blasting Mossy
   Cobblestone, and 2x2 Mossy Stone producing four vanilla Mossy Stone Bricks.
6. Verify ordinary and stonecutting slab, stair, and wall outputs plus matching
   recipe discovery and unlock behavior. Smoke-check the ShapeMap family and
   representative BGE/Nibaru and Interchangeable Block Families behavior for a
   Mossy-attributable regression.

Record Slot B independently as `PASS`, `FAIL`, or `INCONCLUSIVE`, even if Heart
Canary 8 in Slot A has a different result during the same launch. Stop and
record `FAIL` or `INCONCLUSIVE` for missing or duplicate Creative/JEI entries,
load failure, wrong artwork or Stone behavior, wrong loot or recipe output,
derived-geometry ownership conflict, state/waterlogging defect, crash, or a
relevant log error.
