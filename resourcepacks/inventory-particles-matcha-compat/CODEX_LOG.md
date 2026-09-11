# Codex Log

## 2026-09-11T06:30:00Z — Create Inventory Particles Matcha Compatibility C1
- Revision: 1
- Source checkpoint: `d69ffc468c59242abcd6ab35e3c7557a2256770a`
- Changes: Audited immutable `InventoryParticles-2.6.0+26.2+fabric.jar` (1,783,277 bytes; SHA-256 `b44d808e673805eea4949591abcb3325faa1abf8b9321266d06533ea5a34c802`; embedded id `inventory_particles`, version `2.6.0+26.2+fabric`). The exact artifact loads every JSON beneath `assets/*/iparticles`, supports exact item and `#tag` holders plus serialized-stack NBT/component predicates, and rebuilds its combined item map on client resource reload. It has no holder precedence or per-stack disable switch: every matching holder spawns. C1 therefore replaces only `dirt_sand.json` and `potion.json`, retaining every upstream holder but excluding the three exact Matcha `minecraft:item_model` values from their poisonous-potato holders, then adds the narrow replacement holders. Its Ribbits definitions use Inventory Particles' item-texture selector, which renders each cited item’s default stack rather than copying art.
- Build/static: Deterministic assembly SHA-256 `7f1c0ff583b3226ce083867b0e16f03c39bebf6a77d2ae5b1e6ca661d0818d6b`; focused archive/routing validator, `git diff --check`, and `python -B tools/workbench.py validate-repository --root .` all passed.
- Runtime: RUNTIME_UNTESTED; no Test Instance Manager transition, deployment, protected 26.1.2 instance access, or runtime claim occurred.
- Artifact: `inventory-particles-matcha-compat-c1.zip`; SHA-256 `7f1c0ff583b3226ce083867b0e16f03c39bebf6a77d2ae5b1e6ca661d0818d6b`.
- Result: ACTIVE / STATIC_PASS / NOT_DEPLOYED / RUNTIME_UNTESTED.
- Next state: retain the exact generated ZIP, integrate the administrative checkpoint to `main`, publish revision 1 through the main-only Sheet workflow, then perform the focused dedicated-26.2 visual checklist.
