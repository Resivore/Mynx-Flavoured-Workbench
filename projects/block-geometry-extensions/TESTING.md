# Testing

## Candidate and runtime gate

Exact BGE C53 is installed and readiness-verified in Slot A together with its required exact Nibaru C46 contract artifact:

- `artifacts/cnm-nibaru-integration-0.6.0-bge-canary53-layer.jar`, 258,709 bytes, SHA-256 `57A4599ADB3F4C58AE7B99A0148A38760FDB3DA59847CA3EC046379DB323B7C8`;
- `../nibaru/artifacts/more-slabs-stairs-and-walls-4.2.0+26.2-port-canary46-bge-layer-contract.jar`, 5,652,769 bytes, SHA-256 `3281D110F062DB62E721D838A35CE915CA73DD41AF098A013B52952561CEAE7D`.

Both artifacts come from release source `6e9b04ec3947f2514f90775bfeefb599242a4596`; no newer `main`-scoped BGE change supersedes them. The manager disabled accepted BGE C52 and Nibaru C45 under the Slot A dependency override and returned `READY_TO_TEST_VERIFIED`. Minecraft has not been launched, Slot A remains `RUNTIME_UNTESTED`, and neither candidate is accepted. The preserved Java 25 build, focused checks, and controlled GameTests are not Minecraft runtime evidence. Promotion requires a reported pass of the matrix below with no unresolved material, geometry, or switching regression.

## C53 Layer in-game matrix

1. Select representative ShapeMap families for an ordinary full cube, a pillar/axis material, a Glazed material, and specialized materials. Confirm each family offers exactly one Layer immediately after Step, the Layer has the expected material visuals and semantics, and switching does not produce duplicate geometry or recursively rediscover generated Layers.
2. For floor and ceiling placement, verify clicked-face and local-Y behavior. For each wall direction, verify ordinary placement. With secondary use, exercise up, down, north, south, east, and west and confirm placement faces opposite the nearest look direction. In every case, `facing` is the exposed/growth face, the opposite face stays anchored, and floating placement remains allowed.
3. In all six orientations, stack only the same Layer item through its exposed face. Confirm the 1/2/3/4 progression is exactly 4/8/12/16 pixels thick; a different Layer item or any non-exposed face does not merge. The fourth placement must remain a Layer, become dry, and reject further stacking.
4. Waterlog Layers 1–3 in source water and confirm water is retained and restored correctly. Confirm a full Layer rejects or clears water. At every thickness and facing, compare outline and collision geometry to the visible anchored volume.
5. Rotate and mirror representative ordinary, pillar/axis, and Glazed Layers, then save and reload. Confirm facing, thickness, material pattern/axis, serialization, and Glazed appearance remain correct without changing accepted Step or Vertical behavior.
6. Break one-through-four-layer states and confirm proportional layer-count drops. For Grass, Mycelium, Podzol, and Dirt Path, confirm typed `DROP_BASE` transitions yield the matching Dirt Layer count. Check Dirt Path survival and shovel-flattening behavior.
7. Exercise representative profile-driven special semantics, including at least leaves, copper weathering/waxing, concrete powder or another falling material, coral survival, ice/translucency, redstone, honey/slime movement, magma, and soul sand. Confirm the Layer preserves the source material's intended behavior at representative thicknesses and orientations.
8. Recheck ShapeMap ordering and population across representative families: one Layer follows Step, material switching reaches Vertical/Step/Layer deterministically, no family is missing or duplicated, and generated geometry never re-enters Nibaru material discovery.

Stop and leave C53 unaccepted on any incorrect direction, anchoring, thickness, water state, collision, visual/axis transform, drop count, specialized semantic, ShapeMap population, duplicate, or recursion result. Record only behavior actually observed in Minecraft.
