# Glass Corner UV source provenance

This directory retains the minimum reproducible, redistribution-safe input for BGE Canary 58's glass-Corner model correction. The source was supplied by the user in `uv bbmodel.zip`; it is not derived from the defective C57 generated Blockbench export.

## Exact supplied input

- Archive: `uv bbmodel.zip`, 2,329 bytes, SHA-256 `259a87b3459b5ce6e797a93e117fa4ba4526ca6002baa6f58db15e9da79ba0e3`.
- Member: `uv bbmodel/`, 0 bytes, SHA-256 `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855`.
- Member: `uv bbmodel/glass_corner_north_east.bbmodel`, 6,562 bytes, SHA-256 `ed92231ff056e0e2f7cd278381e80bf0131ba62ce27e74bd1874625834a48e54`.
- Member: `uv bbmodel/BlockSprite_glass.png`, 140 bytes, SHA-256 `0be697e533f7de0ddbb27eef37f13166d7da3f75a47d3a74839d4753b8b05c07`.
- Embedded PNG data decoded to 238 bytes, SHA-256 `be65b86e763dec985c900ebd23115fd5a07b78a0b882228589abdab1799d3743`; its decoded pixels are identical to the sibling PNG.

The archive passed CRC, path-safety, uniqueness, JSON, PNG, and geometry checks. The PNG is a 16×16, 8-bit RGBA reference with binary alpha: 191 fully transparent pixels, 65 fully opaque pixels, and no partial alpha. It is used only to verify rotation identity.

## Retained fixture

`glass_corner_north_east.sanitized.bbmodel` retains the original Blockbench 5.0 / Java block 1.21.11 metadata, texture descriptor, element order, UUIDs, all 48 face slots, all eight bounds, texture/null visibility, UV rectangles, absent/default-zero face rotations, and raw cullface fields. It differs from the supplied member only by removal of the embedded `textures[0].source` data URL and addition of the repository's final line feed. The retained file is 6,209 bytes with SHA-256 `0de17962f9d569a95a1fe3004657af87ce9be7bcb3cc495f2b9f98fc6c6753b5`.

Neither PNG encoding, the archive, nor any embedded image bytes are retained or redistributed. The production JAR must contain none of them and does not load Blockbench data at runtime.

## Authoritative model contract

The authoritative orientation is `NORTH_EAST`. Its eight disjoint elements occupy exactly the full-height L union `z=[0,8] across x=[0,16]` plus `x=[8,16], z=[8,16]`, with `y=[0,16]`. The source is authoritative for geometry, visible-face topology, UV rectangles, and face rotations.

The supplied raw cullface metadata is preserved here for provenance but is not valid as production boundary metadata: several element-1 faces all name `west`, and one concave notch face names `south`. Production cullfaces are therefore normalized from the actual visible face and emitted only when that face lies on its matching 0/16 block boundary. Item models omit them.

Other orientations are derived deterministically by clockwise 90-degree Y rotations around the block center:

- bounds: `x0'=16-z1`, `x1'=16-z0`, `z0'=x0`, `z1'=x1`, with Y unchanged;
- horizontal face and normalized-cull directions: north → east → south → west → north;
- UV rectangles remain attached to their authored face;
- side-face UV rotations are unchanged, up adds 90 degrees, and down subtracts 90 degrees per turn, modulo 360.

Canonical `NORTH_EAST` uses zero turns; `SOUTH_EAST` one; `SOUTH_WEST` two; and `NORTH_WEST` three. Four turns must reproduce the exact canonical contract. Material texture slot 0 remains parameterized through the Nibaru material profile rather than bound to the reference PNG.
