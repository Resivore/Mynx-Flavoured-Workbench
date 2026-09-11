# BGE C67 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.11-bge.canary67.vanilla-family-coverage+26.2.jar`, SHA-256 `a7ddde452aa4c80f1d001ec3797e35747672acd6cb925aca750e1bb5f698b168`, embedded version `4.2.11-bge.canary67.vanilla-family-coverage+26.2`, source checkpoint `c516ffa118405a1c67d5fe9c9f3cf21bded77d98`. Exact C58 remains accepted; C62 and C64 remain external runtime-failed provenance.

C67 is `NOT_DEPLOYED` and `RUNTIME_UNTESTED`. Its clean Java 25 build, archive checks, and 106/106 Minecraft 26.2 GameTests are static evidence only. The project lifecycle remains `TESTING` solely because its immutable UUID is assigned to an older Slot A cohort; that does not deploy or ready C67. Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile.

## Manual checks

1. Confirm End Stone Bricks exposes its existing vanilla Slab, Stairs, and Wall plus exactly one BGE Layer, Corner, and Quarter Column. Check inventory models, world models/textures, placement/orientation, waterlogging, culling, and that no missing-model, missing-texture, registry-collision, or relevant exception occurs.
2. Repeat that full BGE trio check for Cobblestone, Polished Deepslate or Tuff Bricks, Nether Bricks or Prismarine, and Cinnabar or Sulfur. Confirm the source standard forms remain provider-owned and only BGE’s Layer/Corner/Quarter Column are new.
3. Recheck an existing MSSW-derived family and an optional-provider family. Confirm their Vertical Slab and Step behavior, BGE forms, placement, waterlogging, and generated resources remain unchanged.
4. Exercise Layer, Corner, and Quarter Column placement/orientation and neighbor-aware culling for a newly admitted family; confirm the established one-source-per-blockspace economy and normal drops. Record only observed `PASS`, `FAIL`, or `INCONCLUSIVE` evidence.
