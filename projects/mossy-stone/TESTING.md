# Testing

## Canary 4 combined-session gate

Test only exact Canary 4:

- version `0.4.0-canary4`
- file `mossy-stone-0.4.0-canary4.jar`
- SHA-256 `B0E7BE5651D622789B848BA1A48073AF8078BA4D834E34C1255EC9CE72042F14`
- source `f789b290f72c508a563797b0c543125bc9c468c1`

Exact Canary 3 is a closed failed candidate: its intended visuals and block and
generated-geometry behavior passed, but its late JEI callback re-added Mossy
ShapeMap slab, stairs, and wall children that CNM intentionally represents only
through the parent. C4 removes only that direct JEI re-addition and preserves
C3 Creative exposure and gameplay behavior. No Mossy release is accepted.

Exact C4 is installed in Slot B alongside exact Matcha Death Rebalance Canary 9
in Slot A, and final manager physical/title re-verification passed. If managed
state changes before launch, require the normal live verifier to pass again.
Build, static tests, deployment, and readiness are not Minecraft runtime results.

## Focused Canary 4 matrix

1. In JEI, search for Mossy Stone. Confirm the parent Mossy Stone represents
   the ShapeMap family and Mossy's horizontal slab, stairs, and wall do not
   reappear as separate entries. Compare with another eligible BGE/CNM family.
2. Confirm unrelated legitimate JEI entries remain present and BGE remains the
   sole owner of derived Vertical Slab and Step geometry.
3. In Building Blocks and Creative search, confirm Mossy Stone, Mossy Stone
   Slab, Mossy Stone Stairs, and Mossy Stone Wall still appear exactly once;
   the JEI fix must not change this Creative behavior.
4. Confirm full block, slab, stairs, wall, BGE Vertical Slab, and BGE Step retain
   the approved artwork and Stone-like mining, sound, tool, resistance,
   placement, shape, state, and waterlogging behavior.
5. Break the full block without Silk Touch and confirm exactly Mossy
   Cobblestone drops; with Silk Touch confirm the full block drops itself.
   Confirm every other geometry drops its own corresponding shape.
6. Smoke-check the established acquisition, crafting, smelting/blasting,
   stonecutting, unlock, ShapeMap, BGE/Nibaru, and Interchangeable Block
   Families behavior for a Mossy-attributable regression.

Record Slot B independently as `PASS`, `FAIL`, or `INCONCLUSIVE`, even if Matcha
Canary 9 has a different result during the same launch. Do not promote C4
without its explicit runtime result.
