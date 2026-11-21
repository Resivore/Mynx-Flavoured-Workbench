# Codex Log

## 2026-09-25T16:30:23.3327292Z — Build Foundation v5 Canary 1

- Revision: 1
- Source checkpoint: `8995d572ef5317779e49a896e6ca663b128a6508`
- Changes: Produced Foundation v5 from immutable Foundation v4. The three Minecraft 26.2 item definitions redirect only `enderscape:veiled_leaves`, `mynx_trees:silver_birch_leaves`, and `mynx_trees:wisteria_leaves` to their already-present Foundation bushy block models. Silver Birch preserves its exact upstream constant item tint; Veiled and Wisteria preserve their untinted behavior. Blockstates, block models, textures, and all unrelated resources remain source-identical.
- Build/static: JSON parsing, target item/model/texture-chain resolution, exact tint assertions, CRC validation, root-layout validation, and entry-by-entry v4-to-v5 diff validation passed. No Minecraft runtime validation was performed.
- Runtime: RUNTIME_UNTESTED.
- Artifact: `Foundation v5.zip`; SHA-256 `70178c497995083e6331d6412395d5b89039e60535877d14cc52cf5cf988ba80`; finalized `2026-09-25T16:30:23.3327292Z`.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED.
- Next state: Retain this exact local artifact and perform the targeted manual item-preview checks in TESTING.md before any owner acceptance decision.

## 2026-09-28T03:34:37.743917Z — Build Foundation v6 Canary 2

- Revision: 2
- Source checkpoint: `0ca58608af84e8a72a0fc72dbe689fe7d15ba2e8`
- Changes: Produced Foundation v6 from the exact retained Foundation v5 artifact and immutable Matcha Overlays v37 source ZIP. All 29 shared asset/resource paths use exact Matcha bytes, matching Matcha's prior higher resource-pack precedence. The only shared-path exceptions are root `pack.mcmeta`, updated solely for Foundation v6 identity, and `pack.png`, retained for Foundation branding. All non-colliding root credits, license, notes, and provenance files from both packs remain unchanged.
- Build/static: Foundation v5 and Matcha v37 source SHA-256 checks, complete entry-set/provenance comparison, 31-collision precedence validation, 2,710 JSON/metadata parses, ZIP CRC validation, duplicate-entry validation, and root-layout validation passed. No Minecraft runtime validation was performed.
- Runtime: RUNTIME_UNTESTED.
- Artifact: `Foundation v6.zip`; SHA-256 `ee2cd0ca5fdade2f66404fcd2f34ae950a1661080b4cc23cf1792a76305bf281`; finalized `2026-09-28T03:34:37.743917Z`.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED.
- Next state: Retain this exact local artifact and perform the representative Foundation-only, Matcha-only, and former-collision checks in TESTING.md before any owner acceptance decision.

## 2026-09-28T04:35:57.959389Z — Build Foundation v7 Canary 3

- Revision: 3
- Source checkpoint: `d7e49ed877e75837029df098a8ce6e0e169362ba`
- Changes: Built v7 from the exact retained v6 archive. Read-only inspection traced Minecraft 26.2 Oak Leaves through `assets/minecraft/items/oak_leaves.json` to Matcha Flavoured's `assets/minecraft/models/block/oak_leaves.json`, whose `minecraft:block/cross_leaves` parent supplies a central 16-cube plus four 32-card exterior foliage elements. Replaced only the three target item routes with new item models of that exact geometry, orientation, UV layout, and inherited standard `minecraft:block/block` display transforms. Each model uses its target leaf base texture and target existing bushy texture; Silver Birch retains its exact constant tint and Veiled/Wisteria remain untinted. No blockstate, world model, world geometry, texture, BGE, BGE × Bushy Leaves, or gameplay resource changed.
- Build/static: SHA-256-verified v6 base, target item/model/texture/tint-chain assertions, five-element cube-plus-foliage topology assertions, 2,713 JSON/metadata parses, ZIP CRC, root-layout, and exact v6-to-v7 diff validation passed. No Minecraft runtime validation was performed.
- Runtime: RUNTIME_UNTESTED.
- Artifact: `Foundation v7.zip`; SHA-256 `0d2ecfa31358c1e7d27ea9a97303a2bcee1864160d37c9797f39881dd0e31727`; finalized `2026-09-28T04:35:57.959389Z`.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED.
- Next state: Retain this exact local artifact and perform the targeted item-preview and placed-leaf checks in TESTING.md before any owner acceptance decision.

## 2026-11-21T03:07:46.883061Z — Finalize Foundation v8 Canary 4

- Revision: 4
- Source checkpoint: `5d11bd13c073782c06b924a923bb0dec599e4e6d`
- Changes: Copied the three base PNGs from the 26.2 client. Vanilla blockstates and item selectors already route each material to its cube model. Restored the four Brown Sandstone family names from the official US, AU, CA and GB English assets using per-key overrides. Copied Matcha's Red Sandstone side and top sprites to their existing IDs. The five non-shutter forms inherit the vanilla bottom used by Matcha. The shutter retains the top sprite on both caps and uses C25's five-slat lighting recipe with Matcha top grain at the original UVs.
- Build/static: Two builds from the verified v7 ZIP had identical SHA-256. Verified all 1,558 visible Netherrack overlay pixels against vanilla at the same UV and every alpha byte against v7. All 1,044 visible End Stone Bricks overlay pixels already matched vanilla. Ordinary Nether Bricks receive 53 overlay rules and have no dedicated source tile set. All 151 CTM property files retain their v7 bytes. Resolved the six placed and inventory model routes, all 19 AMC material models and the separate Gothic iron bars. Reproduced C25's original shutter pixels before substituting the Matcha substrate. Verified 2,717 JSON/metadata files, 5,042 PNGs, root layout, unique ZIP paths, CRCs and the complete member diff. Repository validation passed for 65 manifests with one Foundation transition. Artifact retention verified the complete ZIP in the primary checkout.
- Runtime: RUNTIME_UNTESTED.
- Artifact: `Foundation v8.zip`, SHA-256 `6ff2d732ab196204e0cfe7d1057953b5ce0d0ac09b10d476a234966913f36f92`, 15,368,722 bytes, finalized `2026-11-21T03:04:44.5325795Z`.
- Result: ACTIVE / STATIC_PASS / RUNTIME_UNTESTED.
- Next state: Check the CTM boundaries, localized names and all six AMC forms in an owner-directed Minecraft 26.2 session using TESTING.md.

**v7 to v8 archive paths** · 10 added, 18 modified, 0 removed, 7,920 byte-identical.

```text
A assets/architectural_material_closure/textures/block/red_sandstone_louvered_shutter.png
A assets/minecraft/lang/en_au.json
A assets/minecraft/lang/en_ca.json
A assets/minecraft/lang/en_gb.json
A assets/minecraft/lang/en_us.json
M assets/minecraft/optifine/ctm/overlays/netherrack/0.png
M assets/minecraft/optifine/ctm/overlays/netherrack/1.png
M assets/minecraft/optifine/ctm/overlays/netherrack/10.png
M assets/minecraft/optifine/ctm/overlays/netherrack/11.png
M assets/minecraft/optifine/ctm/overlays/netherrack/12.png
M assets/minecraft/optifine/ctm/overlays/netherrack/13.png
M assets/minecraft/optifine/ctm/overlays/netherrack/14.png
M assets/minecraft/optifine/ctm/overlays/netherrack/15.png
M assets/minecraft/optifine/ctm/overlays/netherrack/16.png
M assets/minecraft/optifine/ctm/overlays/netherrack/2.png
M assets/minecraft/optifine/ctm/overlays/netherrack/3.png
M assets/minecraft/optifine/ctm/overlays/netherrack/4.png
M assets/minecraft/optifine/ctm/overlays/netherrack/5.png
M assets/minecraft/optifine/ctm/overlays/netherrack/6.png
M assets/minecraft/optifine/ctm/overlays/netherrack/7.png
M assets/minecraft/optifine/ctm/overlays/netherrack/8.png
M assets/minecraft/optifine/ctm/overlays/netherrack/9.png
A assets/minecraft/textures/block/end_stone_bricks.png
A assets/minecraft/textures/block/nether_bricks.png
A assets/minecraft/textures/block/netherrack.png
A assets/minecraft/textures/block/red_sandstone.png
A assets/minecraft/textures/block/red_sandstone_top.png
M pack.mcmeta
```
