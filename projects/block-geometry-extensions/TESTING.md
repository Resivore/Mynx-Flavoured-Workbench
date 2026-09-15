# BGE C69 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.13-bge.canary69.bbb-beam-standard-axis+26.2.jar`, SHA-256 `202eecc72f77745617b0d34ebfb7c58d3369300aeb12e8300309ae1fad39ec52`, embedded version `4.2.13-bge.canary69.bbb-beam-standard-axis+26.2`, source checkpoint `f798f4179a079baa235717674f32b88ecb27689f`. C68 remains the accepted release.

C69 is `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; its Java 25 build, archive checks, and 107/107 Minecraft 26.2 GameTests are controlled validation, not gameplay evidence. Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile.

## Manual checks

1. With BBB present, test all 12 Beam materials. Confirm the canonical BGE Beam Slab and Beam Stair entries are used; BBB's original slab and stair entries remain available and unchanged.
2. For Beam Slabs, place bottom, top, and double forms from each of the six clicked faces. Confirm X, Y, and Z material grain axes are independent of slab type, survive compatible stacking, and retain BBB's side and end-grain textures.
3. For Beam Stairs, exercise four horizontal facings, top and bottom halves, and straight, inner, and outer neighbor shapes. Confirm material grain axes remain independent through placement, rotation, mirroring, and neighbor resolution.
4. Confirm Beam Walls remain BBB's normal thin connected WoodenWallBlock, with no material `AXIS` state and no axis-model behavior.
5. Check an existing C68 Layer and an ordinary non-axis material for an obvious regression. Record only actually observed `PASS`, `FAIL`, or `INCONCLUSIVE` evidence.
