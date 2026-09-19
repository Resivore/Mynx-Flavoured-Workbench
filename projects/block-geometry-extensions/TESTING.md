# BGE C82 generic CNM resource closure manual verification

Current candidate: `BGE C82.jar`

- Embedded version: `4.2.26-bge.canary82.cnm-generic-resource-closure+26.2`
- SHA-256: `165229966de7e02b0a42c2b5fc90cefa116f0b626cb55a6378caea6e38f6341c`
- Source checkpoint: `3646cae2cdd57ebbab9748f72ecb7c9352bf8270`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C80 `BGE C80.jar`, SHA-256 `5bc7c23a3724a1e20bc2142459f33d2a45a19295c27d40197c5fc48d6e43b0fc`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C82 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## Verified C80 owner runtime evidence — does not transfer to C82

The retained C80 SHA-256 above was verified before these supplied observations were recorded:

- PASS: Chiseled Resin Bricks renders/functions as intended.
- PASS: Chiseled Cinnabar renders/functions as intended.
- PASS: Purpur Pillar exposes its complete geometry registration.
- FAIL: Purpur Pillar derived geometry has an incorrect/broken side texture.
- FAIL: existing CNM families such as Moss Block omit Corner, Quarter Column, and Layer and expose only the six preexisting roles.

These are individual C80 observations, not an aggregate runtime result or acceptance. C82 has no Minecraft runtime observation.

## C82 real CNM family completion and generic closure

1. With stock Clutter No More 2.0.7+26.2, open the actual CNM family for `minecraft:moss_block`. It must contain exactly one each of Moss Block, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer; no duplicate or separate BGE family may appear.
2. Recheck an ordinary stone/brick family, one log/pillar family, and one optional-provider family. Their actual CNM switching menu must use one resolved component with all applicable roles once.
3. Exercise a newly admitted/untyped CNM-compatible material if one is supplied by the installed provider set. It may not infer a parent from an ID, model, texture, or provider. Before CNM resolves it, temporary BGE roles must remain inaccessible; after CNM selects a canonical component, only the canonical roles may remain visible, render with the resolved canonical material, and drop exactly that material. The catalog-only BGE role tags must not claim this profile-free component as a typed Nibaru material.
4. If a datapack removes or blacklists a CNM mapping for an otherwise admitted source, confirm no BGE candidate is exposed through family switching. Restore the datapack before ordinary checks.
5. Verify Layer, Corner, and Quarter Column blocks never recursively seed another CNM family.

## Purpur Pillar material-face regression

1. Obtain Purpur Pillar and every registered form: source, Slab, Stair, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer.
2. Across X/Y/Z placement, confirm world-facing material sides use the canonical `purpur_pillar_side` texture and end/cap faces use `purpur_pillar_top`; neither may substitute for the other or leave a missing texture.
3. Compare with Quartz Pillar and an ordinary log. Axis rotation must preserve each material's own side/end distinction. Purpur Wall remains an ordinary no-`AXIS` WallBlock with its normal wall inventory silhouette.

## Regression and evidence discipline

1. Recheck Chiseled Resin Bricks, Chiseled Cinnabar, existing Macaw's Paths, Mynx Trees, Ribbits, and Building But Better families for retained placement, collision, drops, ownership, and switching behavior.
2. Recheck representative C78 Stair and Wall forms and C71/C72 Farmland Slab behavior separately when those scopes are relevant.
3. For any observation, record the exact candidate SHA-256, source/mod versions, datapack configuration, reproducible sequence, observed result, and expected result. Do not infer acceptance or a C82 aggregate runtime pass from controlled validation.

## Retention and rollback

Retain `BGE C82.jar` by its exact SHA-256 before runtime work. Keep C80 as the immediate predecessor provenance, C72 as rollback provenance, and C70 as the accepted release; do not overwrite a retained artifact.
