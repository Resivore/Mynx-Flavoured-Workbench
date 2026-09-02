# BGE C58 focused glass-Corner runtime procedure

Verified Slot A cohort:

- BGE C58: `cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar`, 6,036,614 bytes, SHA-256 `1a4e4d1cd9c8709720ec84975e70caffb5552ac676537b9bbae42dca96567e87`.
- Shulker Trowel C10 / Private Canary 9: `shulker-trowel-0.1.0-canary9-private.jar`, 44,738 bytes, SHA-256 `b78679eaf6eaf6f7ff75a32ffae024e45515de3af38bf2ed92ac8727a5138df8`.

The broad nonvisual BGE behavior matrix was already user-tested on exact unified C57 and is not reopened by this model-only successor. Record only the focused visual observations below; static checks, controlled GameTests, exact artifact identity, deployment, and manager readiness are not Minecraft runtime evidence.

## Deployment precondition

Manager revision 62 verifies the exact two-member cohort above as `READY_TO_TEST_VERIFIED / UNTESTED` in Slot A, with Slot B empty. Before testing, run manager verification and require `PHYSICAL_STATE_VERIFIED`, the same exact filenames and hashes, the visible title `Slot A: Block Geometry Extensions (BGE) - Canary 58 + Shulker Trowel - Canary 9`, and no enabled C57 or standalone Nibaru JAR. Use only the dedicated Minecraft 26.2 Workbench; never use the protected 26.1.2 profile.

## Focused visual checklist

1. Start with Clutter No More plus the single unified C58 BGE JAR and no standalone Nibaru JAR; confirm one effective container supplies `cnm_terrain_slabs_compat` and alias `more_slabs_stairs_and_walls` without loader or duplicate-registration errors.
2. Place clear-glass Corner blocks in all four physical orientations: `NORTH_EAST`, `SOUTH_EAST`, `SOUTH_WEST`, and `NORTH_WEST`.
3. Inspect every outer border and both concave notch faces in each orientation.
4. Confirm no UV band is mirrored, rotated backward, missing, stretched, duplicated, or attached to the wrong physical face.
5. Check Corner-to-full-glass adjacency for correct shared-boundary culling and no missing exterior/notch faces.
6. Check representative same-state and differently oriented Corner-to-Corner arrangements for correct adjacency and culling.
7. Place at least one stained-glass Corner in all four orientations, or a representative orientation set sufficient to prove the authored mapping is texture-generic and uses that stained-glass texture.
8. Inspect the creative/inventory Corner item for the established facing, centering, scale, border identity, and material texture.
9. With exact Shulker Trowel C10/Private Canary 9, confirm the Corner icon and placed result use C58 without reopening the broad Trowel matrix.
10. Inspect relevant loader, resource, model, atlas, and rendering logs for missing textures, model bake errors, bad cullfaces, duplicate IDs, or dependency failures.

## Result and stopping conditions

Record `PASS`, `FAIL`, or `INCONCLUSIVE` only for the observations actually made. Stop on any startup/dependency error, incorrect orientation, wrong outer/notch border, mirrored or world-locked UV, missing/duplicated face, adjacency/culling regression, incorrect stained texture, or item regression. Do not promote C58 in this procedure.
