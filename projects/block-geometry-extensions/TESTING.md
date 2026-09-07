# BGE C61 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.5-bge.canary61.runtime-fixes+26.2.jar`, SHA-256 `241033a28b80d638417cd74caed815b79796f0538ff012e172e9a9b714d3f77d`, embedded version `4.2.5-bge.canary61.runtime-fixes+26.2`, source checkpoint `e8bcce0f78e1e0235ac4f71c8ec8c6c194605bee`. C61 is not deployed: the Test Instance Manager correctly refuses any Slot A transition until the separate Slot B IBF C4 artifact is restored by its owner. Exact C58 remains the accepted release and C60 remains unaccepted failed provenance.

Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile.

## Scope

- 64 requested sources, with nine roles each: source Block, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer.
- Mynx Trees: Wisteria Log/Wood/Leaves and Silver Birch Log/Wood/Leaves.
- Ribbits: Mossy Oak Planks.
- Macaw's Paths: 52 full pattern blocks `mcwpaths:<material>_<pattern>` plus five full canonical parents `minecraft:podzol`, `minecraft:dirt`, `minecraft:gravel`, `minecraft:sand`, and `minecraft:red_sand`. Their provider reference blocks remain Path blocks only for optional-provider completion; no Path block is a BGE canonical parent.

## Manual checks

1. For Wisteria Log, Wisteria Wood, Silver Birch Log, and Silver Birch Wood, confirm Wall is a normal wall in-world and in inventory: post/side/tall multipart connection behavior, normal solid wall mesh, and normal wall inventory model. It must not be crossed/cross-plane geometry and must have no `AXIS` state.
2. Confirm those four source families retain `AXIS` on the other seven applicable roles (Slab, Stairs, Vertical Slab, Step, Corner, Quarter Column, Layer), including X/Y/Z rotations, side/end versus bark texture semantics, stripping, fire, fuel, sound, tags, and drops.
3. Confirm all nine Silver Birch Leaves roles exactly follow the provider's live world tint and inventory tint. Check more than one world position/biome so a constant fallback cannot appear correct accidentally. Wisteria Leaves must remain unchanged and untinted.
4. For representative Macaw materials and every pattern, verify each BGE role uses the corresponding full-block appearance/properties, not a lowered Path parent. Verify the five soil aliases preserve the native full-parent BGE family and provider tags without duplicate geometry.
5. Recheck accepted C58 registry/resource identities and representative native families, including Glass Corner UV/geometry, for regression.

Record only directly observed runtime results. Do not infer PASS from build, GameTests, archive inspection, or readiness verification. Stop and record `FAIL` or `INCONCLUSIVE` for the exact deployed artifact if any expected wall mesh, tint, full-parent semantic, provider family, or accepted C58 behavior differs.
