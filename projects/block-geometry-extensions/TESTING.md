# BGE C58 focused glass-Corner runtime procedure

Candidate: `cnm-nibaru-integration-4.2.2-bge.canary58.glass-corner-uv+26.2.jar`, 6,036,614 bytes, SHA-256 `1a4e4d1cd9c8709720ec84975e70caffb5552ac676537b9bbae42dca96567e87`.

The broad nonvisual BGE behavior matrix was already user-tested on exact unified C57 and is not reopened by this model-only successor. Record only the focused visual observations below; static checks, controlled GameTests, exact artifact identity, and future manager readiness are not Minecraft runtime evidence.

## Deployment precondition

C58 is not currently deployed. Manager revision 61 leaves exact C57 in Slot A because unchanged Slot B Shulker Trowel C8 declares an exact dependency on `4.2.1-bge.canary57.unified+26.2`; replacing C57 alone would create a known Fabric loader incompatibility. Do not copy C58 manually or start this checklist until a compatible Trowel candidate is authorized and the canonical manager verifies C58 as `READY_TO_TEST_VERIFIED / UNTESTED` in the dedicated Minecraft 26.2 Workbench. Never use the protected 26.1.2 profile.

## Focused visual checklist

1. Start with Clutter No More plus the single unified C58 BGE JAR and no standalone Nibaru JAR; confirm one effective container supplies `cnm_terrain_slabs_compat` and alias `more_slabs_stairs_and_walls` without loader or duplicate-registration errors.
2. Place clear-glass Corner blocks in all four physical orientations: `NORTH_EAST`, `SOUTH_EAST`, `SOUTH_WEST`, and `NORTH_WEST`.
3. Inspect every outer border and both concave notch faces in each orientation.
4. Confirm no UV band is mirrored, rotated backward, missing, stretched, duplicated, or attached to the wrong physical face.
5. Check Corner-to-full-glass adjacency for correct shared-boundary culling and no missing exterior/notch faces.
6. Check representative same-state and differently oriented Corner-to-Corner arrangements for correct adjacency and culling.
7. Place at least one stained-glass Corner in all four orientations, or a representative orientation set sufficient to prove the authored mapping is texture-generic and uses that stained-glass texture.
8. Inspect the creative/inventory Corner item for the established facing, centering, scale, border identity, and material texture.
9. With a compatible Shulker Trowel build, confirm the Corner icon and placed result use C58 without rebuilding unrelated Trowel behavior or reopening its broad matrix.
10. Inspect relevant loader, resource, model, atlas, and rendering logs for missing textures, model bake errors, bad cullfaces, duplicate IDs, or dependency failures.

## Result and stopping conditions

Record `PASS`, `FAIL`, or `INCONCLUSIVE` only for the observations actually made. Stop on any startup/dependency error, incorrect orientation, wrong outer/notch border, mirrored or world-locked UV, missing/duplicated face, adjacency/culling regression, incorrect stained texture, or item regression. Do not promote C58 in this procedure.
