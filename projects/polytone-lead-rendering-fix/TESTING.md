# Testing

## Current gate

**NOT DEPLOYED — RUNTIME UNTESTED — NOT READY FOR PROMOTION**

Canary 3 is `polytone-lead-rendering-fix-0.1.0+26.2-canary3.jar`, embedded version
`0.1.0+26.2-canary3`, SHA-256
`c90aef0415345b9432169d590fb03a340a5902e8e5a34a07d90e1aa9b05691c9`, source
`31fee15e62444c4cbfe762f095d6d4d20f6faeb2`. Install it alongside Polytone only through a future
serialized Test Instance Manager transition in the dedicated Matcha Flavoured 26.2 Workbench. Do
not touch the protected Matcha Flavoured 26.1.2 gameplay profile. C3 keeps Polytone's textured
leash bypass and pairs an Iris ENTITY-format contract with Minecraft's exact native `TRIANGLE_STRIP`
topology; C2's `entitySolid`/`QUADS` route is not used.

## Runtime matrix

1. Leash an entity across several blocks. PASS requires one continuous narrow rope with normal sag: no black triangular shards, disconnected polygons, ribbon sheets, or jumping geometry. Any C2-style polygon immediately fails C3.
2. With the normal Workbench Iris + Complementary configuration, confirm a visibly brown rope: no rainbow/corruption, grayscale-only color, or solid black rope.
3. Repeat in direct sunlight, shade, and a darker environment. Normal brightness changes are expected, but the brown hue must remain visible.
4. Move and rotate the camera around the rope. Confirm stable geometry from every angle, no face-dependent disappearance, no large triangle, and no abrupt black lighting.
5. Move the leashed entity and/or player. Confirm normal sag, smooth continuity, correct attachment points, range, and detachment, with no stretched remnants.
6. Where practical, test both a directly held leash and a fence-knot leash.
7. Disable shaders and repeat the geometry/color checks. C3 must not work only with Iris + Complementary.

Stop and record `FAIL` or `INCONCLUSIVE` if the primitive topology appears corrupt, the rope is grayscale or black, rainbow coloration returns, geometry disappears/disconnects, camera movement exposes major artifacts, lighting is grossly incorrect, mechanics change, a mixin injection fails, or unrelated rendering changes. Build, artifact, bytecode, and launch-smoke evidence are not visual runtime validation.
