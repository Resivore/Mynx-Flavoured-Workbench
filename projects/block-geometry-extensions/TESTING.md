# BGE C70 accepted release and regression procedure

Exact accepted C70: `cnm-nibaru-integration-4.2.14-bge.canary70.stone-native-slab+26.2.jar`, SHA-256 `d304552e29e76c4165675415215439ac2d73b5a6ebc4abc9787f0fa1124cf266`, embedded version `4.2.14-bge.canary70.stone-native-slab+26.2`, source checkpoint `f798f4179a079baa235717674f32b88ecb27689f`. It is accepted by explicit owner direction with its existing 108/108 controlled GameTests; no runtime PASS is supplied or inferred, and deployment remains `NOT_DEPLOYED`. C69 is the exact rollback/predecessor provenance release; its historical user-reported aggregate PASS remains bound to C69 only.

Use only the dedicated Matcha Flavoured 26.2 Workbench for future regression testing. Never use or modify the protected Matcha Flavoured 26.1.2 profile.

## Manual checks

1. With BBB present, test all 12 Beam materials. Confirm the canonical BGE Beam Slab and Beam Stair entries are used; BBB's original slab and stair entries remain available and unchanged.
2. For Beam Slabs, place bottom, top, and double forms from each of the six clicked faces. Confirm X, Y, and Z material grain axes are independent of slab type, survive compatible stacking, and retain BBB's side and end-grain textures.
3. For Beam Stairs, exercise four horizontal facings, top and bottom halves, and straight, inner, and outer neighbor shapes. Confirm material grain axes remain independent through placement, rotation, mirroring, and neighbor resolution.
4. Confirm Beam Walls remain BBB's normal thin connected WoodenWallBlock, with no material `AXIS` state and no axis-model behavior.
5. Check a C68-equivalent Layer and an ordinary non-axis material for an obvious regression. Record only actually observed `PASS`, `FAIL`, or `INCONCLUSIVE` evidence for the exact tested identity.
