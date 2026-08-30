# Testing

## Accepted Canary 4 regression baseline

The physically verified Stack v3 baseline contains only this exact Mossy Stone
release:

- version `0.4.0-canary4`
- file `mossy-stone-0.4.0-canary4.jar`
- SHA-256 `B0E7BE5651D622789B848BA1A48073AF8078BA4D834E34C1255EC9CE72042F14`
- source `f789b290f72c508a563797b0c543125bc9c468c1`

Exact Canary 3 is a closed failed candidate: its intended visuals and block and
generated-geometry behavior passed, but its late JEI callback re-added Mossy
ShapeMap slab, stairs, and wall children that CNM intentionally represents only
through the parent. C4 removes only that direct JEI re-addition and preserves
C3 Creative exposure and gameplay behavior.

The user reported exact C4 as an aggregate PASS, and the manager promoted that
same artifact to the accepted Stack v3 baseline before final physical
verification. No individual matrix row is inferred from the aggregate result,
and no separate Mossy rollback is designated.

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

Retain this matrix for future accepted-baseline regression sessions. Any new
runtime result must be recorded against exact C4 without inferring individual
rows from the historical aggregate PASS or from another project tested in the
same launch.
