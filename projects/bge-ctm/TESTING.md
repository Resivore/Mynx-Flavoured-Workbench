# Testing

The architecture audit is complete, but this project still has no implementation or artifact. Every case below is a future runtime acceptance target and remains untested. Source inspection, a build, generated resources, focused fixtures, and GameTests do not establish any in-game result.

## Controlled environment

Use the dedicated Minecraft Java 26.2 Fabric Workbench with:

- Continuity enabled and connected textures on;
- the current Matcha Flavoured and `Overlays_Matcha_STRICT_darkoak-bugfix` packs in their normal order;
- the exact BGE/Nibaru candidate pair required by the eventual compatibility artifact;
- shaders off for the first pass, then Complementary Unbound only for the final transparent-material check.

Before testing, record exact artifact hashes, enabled pack order, Continuity version/configuration, and shader state. Stop as failed or inconclusive on a crash, missing model/texture, resource-reload error, or unrelated stack drift.

## Focused inheritance matrix

1. **Base control — clear glass.** Place adjacent `minecraft:glass` full blocks and confirm the enabled Translucent Glass pack's active clear-glass CTM rule still connects normally.
2. **Identical BGE geometry — clear glass.** Test two matching-orientation glass Vertical Slabs and two matching-orientation glass Steps with coplanar exposed faces. Record each geometry separately; do not generalize one result to the other.
3. **Base ↔ BGE — clear glass.** Join base glass to a full four-Layer glass block, then to one representative partial glass geometry whose rendered face physically shares the boundary.
4. **Cross-geometry/contact — clear glass.** Join a full four-Layer glass block to a double Vertical Slab, then inspect one deliberately coplanar partial Layer ↔ Vertical Slab arrangement. Add one same-material, intentionally misaligned pair—different Layer depths/opposite anchoring or a non-coplanar Layer ↔ Vertical pair—and verify it does not falsely connect. Any apparent connection across non-touching planes is a failure, not success.
5. **Intentional non-connection.** Place clear-glass BGE geometry against white-stained-glass BGE geometry. The distinct canonical material families must not connect.
6. **Active solid CTM — chiseled sandstone.** Repeat the base control, identical-BGE, and one base ↔ BGE arrangement on side faces with chiseled sandstone. Its active Continuity rule combines `matchTiles`, `matchBlocks`, and `connect=block`, so it proves solid-material identity without glass rendering variables.
7. **Matcha overlay limits.** Place a full `minecraft:bricks` source immediately east of a full `minecraft:stone` receiver and inspect the stone's upward face along the shared east edge. Repeat with a four-Layer stone receiver, then fixed single-Layer and Vertical Slab receiver states while preserving the source direction and observed face; record partial faces as supported or deliberately skipped by Continuity's unit-square requirement. Replace the source with a partial BGE brick and confirm it does not masquerade as the required full-collision source; test a full four-Layer brick source separately.
8. **Shader/translucency.** After the shader-off glass cases are understood, repeat the clearest base ↔ BGE and BGE ↔ BGE glass cases with Complementary Unbound. Inspect both sides for missing faces, seams, rim corruption, alpha/order changes, and render-layer artifacts.

For every row, record exact block IDs, block states/orientations, physical arrangement, expected connection, actual result, and screenshots. Record PASS, FAIL, or INCONCLUSIVE per row; do not collapse partial evidence into overall validation.

## Acceptance boundary

The compatibility layer is ready for broader runtime coverage only when the focused matrix proves:

- existing base CTM behavior remains unchanged;
- canonical material inheritance works base ↔ BGE and for at least one safe BGE ↔ BGE pair;
- a distinct canonical material stays disconnected;
- partial geometry does not connect across non-coplanar surfaces in the tested arrangements;
- Matcha overlay behavior respects Continuity's unit-square receiver and full-collision source rules;
- the representative transparent case has no new obvious Continuity/culling/shader defect.

If the canonical-appearance bridge causes topology-specific false connections, keep the affected geometry/profile excluded and move that case to a bounded Continuity-specific contact-filter phase. Do not respond with per-block CTM rule lists.
