# BGE C83 runtime-resource closure and Enderscape manual verification

Current candidate: `BGE C83.jar`

- Embedded version: `4.2.27-bge.canary83.cnm-runtime-resource-closure-enderscape+26.2`
- SHA-256: `c0865a2677fbcdc82b53b751e145a3330757694fd73a064671e8fd6d9cd4b4c9`
- Source checkpoint: `c35161ffb995f3000192e22b4a25e832662e6b16`
- Lifecycle/evidence: `ACTIVE / CONTROLLED_VALIDATION_PASS / RUNTIME_UNTESTED`
- Immediate predecessor: exact C82 `BGE C82.jar`, SHA-256 `165229966de7e02b0a42c2b5fc90cefa116f0b626cb55a6378caea6e38f6341c`.
- Rollback: exact C72 `cnm-nibaru-integration-4.2.16-bge.canary72.farmland-slab-low-water+26.2.jar`, SHA-256 `f9f892fccbbee85f75f03c9b24752bbeab76f4fa60efd867414969b735ae85a3`.
- Accepted release: exact C70 `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`.

This checklist is lifecycle-neutral. Record only behavior actually observed for the exact C83 bytes above. It does not authorize inspecting, creating, selecting, or modifying any protected or retired Minecraft profile.

## Verified C80 owner runtime evidence — does not transfer to C83

The retained C80 SHA-256 above was verified before these supplied observations were recorded:

- PASS: Chiseled Resin Bricks renders/functions as intended.
- PASS: Chiseled Cinnabar renders/functions as intended.
- PASS: Purpur Pillar exposes its complete geometry registration.
- FAIL: Purpur Pillar derived geometry has an incorrect/broken side texture.
- FAIL: existing CNM families such as Moss Block omit Corner, Quarter Column, and Layer and expose only the six preexisting roles.

These are individual C80 observations, not an aggregate runtime result or acceptance. C83 has no Minecraft runtime observation.

## C83 runtime generated-resource closure

1. With stock Clutter No More 2.0.7+26.2, open actual CNM-resolved families for Moss Block, one stone/brick material, and one log/pillar material. Each must contain exactly one Corner, Quarter Column, and Layer as well as the preexisting roles; no duplicate or separate BGE family may appear.
2. Trigger the normal client resource-reload route after BGE/CNM generation, then recheck those forms and their inventory items. They must remain present and textured with no missing blockstate, model, or item-definition errors.
3. Recheck one BGE-generated external family. Its generated blockstate, model, and item resource must resolve under that resource's own namespace, never through a `clutternomore` namespace substitution.
4. Verify Layer, Corner, and Quarter Column blocks never recursively seed another CNM family.

## Enderscape exact catalog

1. With Enderscape 3.0.2+mc26.2 installed, verify Veiled Log, Veiled Wood, Celestial Stem, Celestial Hyphae, Murublight Stem, Murublight Hyphae, Shadoline Pillar, and Dusk Purpur Pillar across X/Y/Z placement. Their side/end material roles must remain correct; ordinary walls remain no-`AXIS` walls.
2. Verify all listed normal materials, especially actual `enderscape:nebulite_block` for Block of Nebulite, Veiled Leaves tinting, and Drift Jelly Block's translucent slime-style inset behavior. `enderscape:block_of_raw_magnia` is intentionally absent: the controlled provider bytes contain no block, item, or model for it.
3. Verify Veiled End Stone, Celestial Overgrowth, and Corrupt Overgrowth preserve their source side/top/bottom materials; Corrupt Overgrowth uses Enderscape Mirestone underneath. Verify Celestial Path and Corrupt Path retain lowered path surfaces and their source material roles; Corrupt Path also uses Enderscape Mirestone underneath.

## Purpur Pillar material-face regression

1. Obtain Purpur Pillar and every registered form: source, Slab, Stair, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer.
2. Across X/Y/Z placement, confirm world-facing material sides use the canonical `purpur_pillar_side` texture and end/cap faces use `purpur_pillar_top`; neither may substitute for the other or leave a missing texture.
3. Compare with Quartz Pillar and an ordinary log. Axis rotation must preserve each material's own side/end distinction. Purpur Wall remains an ordinary no-`AXIS` WallBlock with its normal wall inventory silhouette.

## Regression and evidence discipline

1. Recheck Chiseled Resin Bricks, Chiseled Cinnabar, existing Macaw's Paths, Mynx Trees, Ribbits, and Building But Better families for retained placement, collision, drops, ownership, and switching behavior.
2. Recheck representative C78 Stair and Wall forms and C71/C72 Farmland Slab behavior separately when those scopes are relevant.
3. For any observation, record the exact candidate SHA-256, source/mod versions, datapack configuration, reproducible sequence, observed result, and expected result. Do not infer acceptance or a C83 aggregate runtime pass from controlled validation.

## Retention and rollback

Retain `BGE C83.jar` by its exact SHA-256 before runtime work. Keep C82 as the immediate predecessor provenance, C72 as rollback provenance, and C70 as the accepted release; do not overwrite a retained artifact.
