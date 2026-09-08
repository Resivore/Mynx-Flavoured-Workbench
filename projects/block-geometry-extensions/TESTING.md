# BGE C63 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.7-bge.canary63.inventory-preview+26.2.jar`, SHA-256 `9ee7d471e62202d2813492414b3cdd363e4c25eeb05e3a47a63b3451061620ba`, embedded version `4.2.7-bge.canary63.inventory-preview+26.2`, source checkpoint `7b0ecf1d9521c423d06710507d6db2f6b0a21b6b`. Exact C58 remains accepted. Exact C62 is retained external runtime-failed provenance: Wisteria/Silver Birch Step inventory items appeared upright/vertical, and their Vertical Slab items appeared off-centre. The supplied observation says other checked C62 behavior appeared correct; it does not establish a C62 runtime PASS.

C63 is `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; its build, archive checks, and GameTests are not Minecraft runtime evidence. The project remains `TESTING` only because its immutable UUID is already assigned to the manager's older Slot A cohort; that does not make C63 deployed or ready. Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile. Before launch, use the serialized Test Instance Manager and require exact C63 as `READY_TO_TEST_VERIFIED` in the BGE slot while preserving the other slot byte-for-byte. Do not make a transition that changes another project's slot.

## Scope

- Compare Wisteria Log, Wisteria Wood, Silver Birch Log, and Silver Birch Wood directly with an established native Spruce family.
- In the CNM menu/inventory, each external Vertical Slab must use the centred native/CNM Vertical Slab presentation, and each external Step must use the normal horizontal native/CNM Step presentation.
- In-world material-axis behavior must remain unchanged: Logs keep end grain on their axis ends, Wood remains bark-on-all-sides, walls remain ordinary connected walls, and the other C62 geometry/economy/role corrections remain intact.
- Retain the useful C62 checks for Silver Birch leaf tint, provider-role reuse, independent Macaw Path families, ShapeMap ordering, stripping, fire/fuel, tags, drops, sounds, waterlogging, and accepted C58 regression coverage.

## Manual checks

1. For each of the four affected Log/Wood sources, place and inspect Vertical Slab and Step from the CNM menu and ordinary inventory. Compare their centring/orientation directly against Spruce.
2. Place the same four Vertical Slabs and Steps on each valid axis/state. Confirm axis-specific world models, Log end grain, and Wood bark behavior are unchanged; item-preview correction must not alter placed selectors.
3. Recheck representative C62 roles: normal Log/Wood/Leaf walls, Silver Birch leaf tint, Macaw/Ribbits provider-role reuse, five independent Macaw soil Paths, and accepted C58 Glass Corner behavior.

Stop and record exact C63 `FAIL` or `INCONCLUSIVE` if any affected item differs from native presentation, any axis world state changes, or any preserved C62/C58 behavior regresses. Do not record `PASS` until these behaviors are actually observed in Minecraft.
