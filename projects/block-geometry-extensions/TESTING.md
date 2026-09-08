# BGE C65 runtime procedure

Current candidate: `cnm-nibaru-integration-4.2.9-bge.canary65.bbb-axis-resource-runtime-fix+26.2.jar`, SHA-256 `b4b74fbef6a3aaf385a63a718cb87acc915dc46760605351cfbd550ec37568bb`, embedded version `4.2.9-bge.canary65.bbb-axis-resource-runtime-fix+26.2`, source checkpoint `1fe7a09edff14e2dc6f1e34125816494f184f9b8`. Exact C58 remains accepted; C62 remains external runtime-failed provenance; exact C64 is external Minecraft runtime-failed provenance for the BBB client resource-reload crash.

C65 is `NOT_DEPLOYED` and `RUNTIME_UNTESTED`; two clean builds, archive checks, and 103/103 GameTests are not Minecraft runtime evidence. The project remains `TESTING` only because its immutable UUID is assigned to an older Slot A cohort; that does not make C65 deployed or ready. A serialized C65 deployment dry run failed closed before profile access because an unrelated project control log has an invalid unknown C3 implementation entry, so do not retry until repository control validation is repaired. Use only the dedicated Matcha Flavoured 26.2 Workbench. Never use or modify the protected Matcha Flavoured 26.1.2 profile.

## Scope

- Confirm all twelve BBB beam parents — Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Crimson, Warped, Mangrove, Bamboo, Cherry, and Pale Oak — each expose exactly one Beam, Slab, Stairs, Wall, Vertical Slab, Step, Corner, Quarter Column, and Layer.
- Confirm BBB remains the sole owner of each Beam Slab, Beam Stairs, and thin WoodenWallBlock. No duplicate BGE standard item, resource, menu entry, mapping, axis wall state, or thick BGE wall/post may appear.
- Confirm BGE-owned Vertical Slab, Step, Corner, Quarter Column, and Layer preserve beam side/end-grain orientation on X/Y/Z placement. Steps must be directional in-world and use a centred normal horizontal Step presentation in inventory/menu.

## Manual checks

1. With BBB present, inspect every beam parent in the CNM menu and ordinary inventory. Count the nine forms once per parent; include Pale Oak. Check normal horizontal Step preview and centred Vertical Slab preview.
2. Place representative Oak, Crimson, Bamboo, and Pale Oak BGE-owned forms on X, Y, and Z. Confirm side/end textures follow the beam axis and that Step direction changes in world as expected.
3. Place and connect representative BBB Walls. Confirm their existing thin WoodenWallBlock connection geometry, no BGE thick post/crossed presentation, and no `AXIS` state.
4. Confirm the client reaches the title/world screen without a resource-reload failure at `bbb:blockstates/warped_beam.json`; then recheck C63 inventory previews and accepted C58 Glass Corner behavior. Record exact `FAIL` or `INCONCLUSIVE` on any duplicate, missing form, orientation, wall, or regression; record `PASS` only after actual Minecraft observation.
