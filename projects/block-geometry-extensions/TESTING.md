# BGE C64 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.8-bge.canary64.bbb-beam-catalog+26.2.jar`, SHA-256 `bf3b67676031bb51089a5e5320f299d5638534b61f743d1bfe6d7a0f0d3c1458`, embedded version `4.2.8-bge.canary64.bbb-beam-catalog+26.2`, source checkpoint `330655d657f1b1b081fc9bfa26d21309320bda6f`. Exact C58 remains accepted; C62 remains external runtime-failed provenance for the supplied Wisteria/Silver Birch inventory observations.

C64 is `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; clean builds, archive checks, and GameTests are not Minecraft runtime evidence. The project remains `TESTING` only because its immutable UUID is assigned to an older Slot A cohort; that does not make C64 deployed or ready. Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile. Use the serialized Test Instance Manager, preserve the other slot exactly, and leave C64 undeployed if the IBF C4/C5 conflict still prevents a valid transition.

## Scope

- Confirm all twelve BBB beam parents — Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Crimson, Warped, Mangrove, Bamboo, Cherry, and Pale Oak — each expose exactly one Beam, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer.
- Confirm BBB remains the sole owner of each Beam Slab, Beam Stairs, and thin WoodenWallBlock. No duplicate BGE standard item, resource, menu entry, mapping, axis wall state, or thick BGE wall/post may appear.
- Confirm BGE-owned Vertical Slab, Step, Corner, Quarter Column, and Layer preserve beam side/end-grain orientation on X/Y/Z placement. Steps must be directional in-world and use a centred normal horizontal Step presentation in inventory/menu.

## Manual checks

1. With BBB present, inspect every beam parent in the CNM menu and ordinary inventory. Count the nine forms once per parent; include Pale Oak. Check normal horizontal Step preview and centred Vertical Slab preview.
2. Place representative Oak, Crimson, Bamboo, and Pale Oak BGE-owned forms on X, Y, and Z. Confirm side/end textures follow the beam axis and that Step direction changes in world as expected.
3. Place and connect representative BBB Walls. Confirm their existing thin WoodenWallBlock connection geometry, no BGE thick post/crossed presentation, and no `AXIS` state.
4. Recheck C63 inventory previews and accepted C58 Glass Corner behavior. Record exact `FAIL` or `INCONCLUSIVE` on any duplicate, missing form, orientation, wall, or regression; record `PASS` only after actual Minecraft observation.
