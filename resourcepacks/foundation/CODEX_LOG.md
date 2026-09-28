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
