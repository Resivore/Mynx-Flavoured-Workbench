# Testing

Canary 1 implements the bounded canonical-appearance bridge and has controlled state/eligibility GameTest coverage. Every visual case below remains untested in a Minecraft client. A successful build, artifact inspection, dedicated-server GameTests, generated resources, or source inspection does not establish that Continuity connects or renders any texture correctly in game.

## Canary 1 boundary

The bridge asks BGE's typed runtime binding for the exact canonical material, starts from that material's default state, and changes appearance only for:

- a Layer with `layers=4`;
- a Vertical Slab with `double=true`.

Partial Layers, single Vertical Slabs, every Step state (including its disconnected double form), Corners, and Quarter Columns deliberately keep their derived appearance in this canary. They are negative controls, not promised coverage. The initial visual allowlist covers ordinary uniform/top-side-bottom/pillar materials, leaves, glazed patterns, clear or stained glass, and translucent-uniform materials. Grass overlays, paths, roots, honey/slime inset materials, cutout-uniform materials, and custom model contracts remain excluded pending evidence.

Only three material-state mappings are intentional: `axis` to canonical `axis`, BGE's `pattern_facing` to the canonical glazed facing, and leaf `distance`/`persistent` to their canonical leaf properties. Geometry facing, Layer depth, Vertical `double`, Step/slab form, waterlogging, and all similarly named but unmapped properties are never copied. An unfamiliar or unprojectable canonical property makes the state ineligible rather than guessed.

## Controlled environment

Use the dedicated Minecraft Java 26.2 Fabric Workbench with:

- Continuity enabled and connected textures on;
- the current Matcha Flavoured and `Overlays_Matcha_STRICT_darkoak-bugfix` packs in their normal order;
- Block Geometry Extensions `4.2.14-bge.canary70.stone-native-slab+26.2` and BGE × CTM `0.1.0-canary1` together with their declared Fabric dependencies;
- shaders off for the first pass, then Complementary Unbound only for the final transparent-material check.

Before testing, record exact artifact hashes, enabled pack order, Continuity version/configuration, and shader state. Stop as failed or inconclusive on a crash, missing model/texture, resource-reload error, or unrelated stack drift.

## Focused inheritance matrix

1. **Base control — clear glass.** Place adjacent `minecraft:glass` full blocks and confirm the enabled Translucent Glass pack's active clear-glass CTM rule still connects normally.
2. **Identical BGE geometry — clear glass.** Test two full four-Layer glass blocks and two double Vertical Slabs; both pairs are eligible and should inherit the clear-glass family. Then test matching-orientation single Vertical Slabs and Steps with coplanar exposed faces as deliberate exclusions: neither shape should gain canonical inheritance in Canary 1. Record every geometry separately; do not generalize one result to another.
3. **Base ↔ BGE — clear glass.** Join base glass first to a full four-Layer glass block and then to a double Vertical Slab; both are eligible. Repeat with one partial Layer, one single Vertical Slab, and one Step whose rendered face physically shares the boundary. Those three states are intentionally ineligible and must not gain a connection from this bridge.
4. **Cross-geometry/contact — clear glass.** Join a full four-Layer glass block to a double Vertical Slab and inspect the shared boundary. Then inspect one deliberately coplanar partial Layer ↔ single Vertical Slab arrangement plus one same-material, intentionally misaligned pair—different Layer depths/opposite anchoring or a non-coplanar Layer ↔ Vertical pair. The partial/misaligned controls must not inherit or falsely connect. Any apparent connection across non-touching planes is a failure, not success.
5. **Intentional non-connection.** Place clear-glass BGE geometry against white-stained-glass BGE geometry. The distinct canonical material families must not connect.
6. **Active solid CTM — chiseled sandstone.** Repeat the base control, eligible full four-Layer/double-Vertical pairs, and base ↔ BGE arrangements on side faces with chiseled sandstone. Also retain single-Vertical and Step negative controls. Its active Continuity rule combines `matchTiles`, `matchBlocks`, and `connect=block`, so it proves solid-material identity without glass rendering variables.
7. **Matcha overlay limits.** Place a full `minecraft:bricks` source immediately east of a full `minecraft:stone` receiver and inspect the stone's upward face along the shared east edge. Repeat with a four-Layer stone receiver, then fixed single-Layer, single-Vertical, and Step receiver states while preserving the source direction and observed face. The four-Layer receiver is appearance-eligible, while the partial states deliberately are not; still record whether Continuity's unit-square requirement independently permits or skips the overlay. Replace the source with a partial BGE brick and confirm it does not masquerade as the required full-collision source; test a full four-Layer brick source separately.
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
